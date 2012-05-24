/*
Copyright (c) 2008 Ben Clarke

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package org.ben.mpd;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class MPDSocket {
	
	public static final int STATE_STOPPED = 0;
	public static final int STATE_PAUSED = 1;
	public static final int STATE_PLAYING = 2;
	
	private static final byte[] OK = "OK".getBytes();
	private static final byte[] ACK = "ACK".getBytes();
	private static final byte[] COLON = ":".getBytes();
	private static final byte[] PLAY = "play".getBytes();
	private static final byte[] STOP = "stop".getBytes();
	private static final byte[] NEXT = "next".getBytes();
	private static final byte[] PREV = "previous".getBytes();
	private static final byte[] PAUSE = "pause".getBytes();
	private static final byte[] VOLUME = "setvol    ".getBytes();
	private static final byte[] BLANK = "   ".getBytes();
	private static final byte[] CLEAR = "clear".getBytes();
	private static final byte[] PING = "ping".getBytes();
	
	private static final int BUF_SIZE = 1024;
	private static MPDSocket sock;
	private static String password;
	
	private int bytes_up = 0;
	private int bytes_down = 0;
	
	public static MPDSocket connect(String host, String password, int port) throws IOException {
		
		if(sock != null) {
			sock.close(true);
		}
		
		sock = new MPDSocket(host, port);
		
		if(password != null && password.length() > 0) {
//			System.out.println("Using password");
			MPDSocket.password = password;
		}
		
//		System.out.println("Connecting");
		
		sock.reconnect();
		
		return sock;
	}
	
	public static MPDSocket getSocket() {
		return sock;
	}

	private StreamConnection con;
	private DataOutputStream out;
	private DataInputStream in;
	private byte[] buf = new byte[BUF_SIZE];
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	
	private String host;
	private int port;
	
	private MPDSocket(String host, int port) throws IOException {
	
		this.host = host;
		this.port = port;
		
	}
	
	public void reconnect() throws IOException {
		
		if(con != null) {
			close(false);
		}
		
		String url = "socket://" + host + ":" + port;
		con = (StreamConnection)Connector.open(url);
		
		in = con.openDataInputStream();
		out = con.openDataOutputStream();
		
//		System.out.println("Socket opened");
		
		System.out.println(readResponse());
		
		if(password != null) {
//			System.out.println("Sending password");
			System.out.println(write(("password " + password).getBytes()));
		}
	}
	
	public void close(boolean forgetPassword) {
		
		if(forgetPassword) {
			password = null;
		}
		
//		System.out.println("Closing socket");
		
		try { in.close(); } catch(Exception e) {}
		try { out.close(); } catch(Exception e) {}
		try { con.close(); } catch(Exception e) {}
	}
	
	private void doWrite(byte[] b, boolean retry) throws IOException {
		
//		System.out.println(new String(b));
		
		try {
		
			out.write(b);
			
			bytes_up += b.length;
			
			if(b[b.length - 1] != '\n') {
				out.write('\n');
				bytes_up++;
			}
			
			out.flush();
			
		} catch(IOException e) {
			
			if(retry) {
				reconnect();
				doWrite(b, false);
			} else {
				throw e;
			}
			
		}
	}

	public String write(String str) throws IOException {
		return write(str.getBytes());
	}
	
	public String write(byte[] b) throws IOException {
		doWrite(b, true);
		return readResponse();
	}
	
	private String readResponse() throws IOException {
		return readResponseStart(OK);
	}
	
	private String readResponseStart(byte[] eof) throws IOException {
		synchronized(bout) {
			
			bout.reset();
			int n;
			
			synchronized(buf) {
				
				while((n = Utils.readLine(in, buf)) > 0) {
					
					bytes_down += n + 1;

					if(bout.size() > 0) {
						bout.write('\n');
					}
					bout.write(buf, 0, n);
					
					if(Utils.startsWith(buf, eof)) { 
						break; 
					} else
					if(Utils.startsWith(buf, ACK)) {
						throw new RuntimeException(Utils.toString(buf, 0, n));
					}
					
				}
			}
			
			return bout.toString();
		}
	}

	public void getSongList(byte[] command, Vector dst) throws IOException {
		
		doWrite(command, true);
		parseSongListResponse(dst);

	}
	
	private void parseSongListResponse(Vector dst) throws IOException {
		
		Song song = null;
		Directory dir = null;
		
		synchronized(buf) {
			
			int n, x;
			String name;
			String value;
			
			while((n = Utils.readLine(in, buf)) > 0) {
				
				bytes_down += n + 1;
				
				if(Utils.startsWith(buf, OK)) { 
					break; 
				}else
				if(Utils.startsWith(buf, ACK)) {
					throw new RuntimeException(Utils.toString(buf, 0, n));
				}
				
				x = Utils.indexOf(buf, COLON);
				if(x > -1) {
					name = Utils.toString(buf, 0, x);
					value = Utils.toString(buf, x + 2, n - (x + 2));
					
					if("directory".equals(name)) {
						dir = new Directory();
						dir.name = value;
						dst.addElement(dir);
					} else
					if("file".equals(name)) {
						if(song != null) {
							dst.addElement(song);
						}
						song = new Song();
						song.file = value;
					} else 
					if("Id".equals(name)) {
						song.id = Integer.parseInt(value);
					} else
					if("Time".equals(name)) {
						song.time = Integer.parseInt(value);
					} else
					if("Artist".equals(name)) {
						song.artist = value;
					} else
					if("Title".equals(name)) {
						song.title = value;
					} else
					if("Pos".equals(name)) {
						song.sequence = Integer.parseInt(value);
					} else {
//						System.out.println("Ignoring field " + name);
					}
				}
			}
			
			if(song != null) {
				dst.addElement(song);
			}
			
		}
	}
	
	public int getBytesUp() {
		return bytes_up;
	}

	public int getBytesDown() {
		return bytes_down;
	}
	
	public void play() throws IOException {
		write(PLAY);
	}
	
	public void stop() throws IOException {
		write(STOP);
	}
	
	public void pause() throws IOException {
		write(PAUSE);
	}
	
	public void next() throws IOException {
		write(NEXT);
	}
	
	public void prev() throws IOException {
		write(PREV);
	}
	
	public void setVolume(int volume) throws IOException {
		if(volume < 0 || volume > 100) { 
			throw new RuntimeException("Don't set the volume to weird levels"); 
		}
		
		byte[] strvol = String.valueOf(volume).getBytes();
		
		synchronized(VOLUME) {
			System.arraycopy(BLANK, 0, VOLUME, VOLUME.length - 3, BLANK.length);
			System.arraycopy(strvol, 0, VOLUME, VOLUME.length - 3, strvol.length);
			write(VOLUME);
		}
	}
	
	public void seek(int songId, int secs) throws IOException {
		write(("seekid " + songId + " " + secs).getBytes());
	}
	
	public void playPos(int pos) throws IOException {
		write(("play " + pos).getBytes());
	}
	
	public void playId(int id) throws IOException {
		write(("playid " + id).getBytes());
	}
	
	public void loadLibrary(String dir, Vector dst) throws IOException {
		doWrite(("lsinfo \"" + dir + "\"").getBytes(), true);
		parseSongListResponse(dst);
	}
	
	public void addToPlaylist(String dir) throws IOException {
		write(("add \"" + dir + "\"").getBytes());
	}
	
	public void removeFromPlaylist(int idx) throws IOException {
		write(("delete " + idx).getBytes());
	}
	
	public void clearPlaylist() throws IOException {
		write(CLEAR);
	}
	
	public void ping() throws IOException {
		write(PING);
	}
}
