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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStoreException;

import org.ben.mpd.event.Event;
import org.ben.mpd.event.EventDispatcher;
import org.ben.mpd.event.EventListener;
import org.ben.mpd.event.StatusUpdateEvent;

public class Main extends MIDlet 
	implements EventListener, CommandListener, ItemStateListener, ItemCommandListener, Runnable {
	
	public static final Command cmd_play = new Command("Play/Pause", Command.ITEM, 1);
	public static final Command cmd_next = new Command("Next", Command.ITEM, 2);
	public static final Command cmd_prev = new Command("Prev", Command.ITEM, 2);
	public static final Command cmd_save_playlist = new Command("Save playlist", Command.ITEM, 4);
	public static final Command cmd_load_playlist = new Command("Load playlist", Command.ITEM, 5);
	public static final Command cmd_clear_playlist = new Command("Clear playlist", Command.ITEM, 6);
	public static final Command cmd_add_to_playlist = new Command("Add to playlist", Command.ITEM, 7);
	public static final Command cmd_rm_from_playlist = new Command("Remove from playlist", Command.ITEM, 8);
	public static final Command cmd_descend = new Command("Enter directory", Command.ITEM, 9);
	public static final Command cmd_ascend = new Command("Parent directory", Command.ITEM, 10);
	
	public static final Command cmd_stop = new Command("Stop", Command.BACK, 4);
	public static final Command cmd_disconnect = new Command("Disconnect", Command.BACK, 5);
	
	public static final Command cmd_np = new Command("Now Playing", Command.ITEM, 3);
	public static final Command cmd_playlist = new Command("Playlist", Command.ITEM, 3);
	public static final Command cmd_library = new Command("Library", Command.ITEM, 3);
	
	public static final Command cmd_connect = new Command("Connect", Command.SCREEN, 1);
	
	private int state = MPDSocket.STATE_STOPPED;

	private static final Timer timer = new Timer();
	
	private Display display;
	private Form form;
	private Form loginForm;
	private TextField host;
	private TextField port;
	private TextField password;
	private Ticker ticker;
	private Gauge seek;
	private Gauge volume;
	private Song currentSong;
	private Playlist.View playlist;
	private LibraryView library;
	private TimerTask statusUpdater, guiUpdater;
	
	public Main() {
		this.display = Display.getDisplay(this);
	}

	protected void destroyApp(boolean force) throws MIDletStateChangeException {
		pauseApp();
		try { Database.close(); } catch(Exception e) { e.printStackTrace(); }
	}

	protected void pauseApp() {
		
			if(guiUpdater != null) { guiUpdater.cancel(); }
			if(statusUpdater != null) { statusUpdater.cancel(); }
		
			if(MPDSocket.getSocket() != null) {
				MPDSocket.getSocket().close(false);
			}
	}

	protected void startApp() throws MIDletStateChangeException {
		
		try {
			
			if(form == null) {
				buildLoginForm();
				buildNPForm();
			} else {
// when this starts up it'll force a reconnect
				timer.schedule(statusUpdater = new PlayerStatus(), 0, PlayerStatus.PERIOD);
			}
			
			timer.schedule(guiUpdater = new GUIUpdater(), 0, 100);
			
		} catch(Exception e) {
			e.printStackTrace();
			throw new MIDletStateChangeException(e.getMessage());
		}

	}
	
	private void buildNPForm() {
		form = new Form("MPDControl");
		ticker = new Ticker("Connecting...");
		form.append(ticker);
		
		seek = new Gauge("Seek  ", true, 100, 0);
		seek.setPreferredSize(320, 25);
		form.append(seek);
		
		volume = new Gauge("Volume", true, 10, 0);
		volume.setPreferredSize(320, 25);
		form.append(volume);

		form.addCommand(cmd_play);
		form.addCommand(cmd_next);
		form.addCommand(cmd_prev);
		form.addCommand(cmd_stop);
		form.addCommand(cmd_playlist);
		form.addCommand(cmd_library);
		form.addCommand(cmd_disconnect);
		
		form.setCommandListener(this);
		form.setItemStateListener(this);
		EventDispatcher.addListener(this);		
	}
	
	private void buildLoginForm() {
		
		try {
			Database.loadStoredCredentials();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		}
		
		loginForm = new Form("Connect to MPD");
		loginForm.append(host = new TextField("Host", Database.getHost(), 255, 0));
		loginForm.append(password = new TextField("Password (Optional)", "", 255, TextField.PASSWORD | TextField.SENSITIVE));
		loginForm.append(port = new TextField("Port", String.valueOf(Database.getPort()), 5, TextField.NUMERIC));
		loginForm.addCommand(cmd_connect);
		loginForm.setCommandListener(this);
		
		display.setCurrent(loginForm);
		
	}
	
	private void connect() throws IOException {
		
		try {
			MPDSocket.connect(host.getString(), password.getString(), Integer.parseInt(port.getString()));
		} catch(RuntimeException e) {
			e.printStackTrace();
			display.setCurrent(Utils.handleException(e));
			return;
		}
		
		timer.schedule(statusUpdater = new PlayerStatus(), 0, PlayerStatus.PERIOD);
		
		Playlist.init();
		
		playlist = Playlist.getPlaylist().getView();
		playlist.setCommandListener(this);
		
		library = new LibraryView();
		library.setCommandListener(this);
		
		display.setCurrent(form);
		
		try {
			Database.setCredentials(host.getString(), Integer.parseInt(port.getString()));
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		System.gc();
	}
	
	public static Timer getTimer() {
		return timer;
	}
	
	private class GUIUpdater extends TimerTask {
		public void run() {
			ticker.doRepaint();
		}
	}
	
	public void commandAction(Command command, Displayable displayable) {
		try {
			
			MPDSocket sock = MPDSocket.getSocket();
			
			if(command == cmd_play) {
				if(state == MPDSocket.STATE_STOPPED) {
					sock.play();
					state = MPDSocket.STATE_PLAYING;
				} else
				if(state == MPDSocket.STATE_PLAYING) {
					sock.pause();
					state = MPDSocket.STATE_PAUSED;
				} else {
					sock.pause();
					state = MPDSocket.STATE_PLAYING;
				}
			} else
			if(command == cmd_stop && state != MPDSocket.STATE_STOPPED) {
				sock.stop();
				state = MPDSocket.STATE_STOPPED;
			} else
			if(command == cmd_next) {
				sock.next();
			} else
			if(command == cmd_prev) {
				sock.prev();
			} else
			if(command == cmd_playlist) {
				display.setCurrent(playlist);
			} else
			if(command == cmd_np) {
				display.setCurrent(form);
			} else 
			if(command == cmd_library) {
				library.download("");
				display.setCurrent(library);
			} else
			if(command == cmd_add_to_playlist) {
				sock.addToPlaylist(library.getSelected());
			} else
			if(command == cmd_descend) {
				library.download(library.getSelected());
			} else
			if(command == cmd_clear_playlist) {
				sock.clearPlaylist();
			} else
			if(command == cmd_rm_from_playlist) {
				sock.removeFromPlaylist(playlist.getCursor());
			} else
			if(command == cmd_ascend) {
				library.parentDirectory();
			} else
			if(command == cmd_connect) {
				new Thread(this).start();
			} else
			if(command == cmd_disconnect) {
				statusUpdater.cancel();
				sock.close(true);
				buildLoginForm();
			}
			
		} catch(IOException e) {
			Utils.handleException(e);
		}
	}
	
	public void commandAction(Command command, Item item) {
		commandAction(command, (Displayable)null);
	}
	
	public void eventFired(Event evt) {
		if(!(evt instanceof StatusUpdateEvent)) { return; }
		StatusUpdateEvent se = (StatusUpdateEvent)evt;
		Hashtable values = se.getValues();
		
		String strState = (String)values.get("state");
		String time = (String)values.get("time");
		String strvol = (String)values.get("volume");
		String strSongId = (String)values.get("songid");
		String strSong = (String)values.get("song");
		
		if(time != null) { 
			
			int at = Integer.parseInt(time.substring(0, time.indexOf(':')));
			int of = Integer.parseInt(time.substring(time.indexOf(':') + 1));
			
			seek.setMaxValue(of / 10);
			seek.setValue(at / 10);
			
		}
		
		if(strvol != null) {
			volume.setValue(Integer.parseInt(strvol) / 10);
		}
		
		if("pause".equals(strState)) { 
			state = MPDSocket.STATE_PAUSED; 
		} else 
		if("stop".equals(strState)) { 
			state = MPDSocket.STATE_STOPPED; 
		} else {
			state = MPDSocket.STATE_PLAYING;
		}
		
		if(strSongId != null) {
			int songId = Integer.parseInt(strSongId);
			currentSong = Playlist.getPlaylist().getSongById(songId);
		}
		
		if(strSong != null) {
			int song = Integer.parseInt(strSong);
			playlist.setSelectedIndex(song);
		}
		
		ticker.updateState(currentSong, state);
	}

	public void itemStateChanged(Item item) {
		try {
			if(volume == item) {
				MPDSocket.getSocket().setVolume(volume.getValue() * 10);
			} else
			if(seek == item && currentSong != null) {
				MPDSocket.getSocket().seek(currentSong.id, seek.getValue() * 10);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try { connect(); } catch(Exception e) {
			Utils.handleException(e);
			buildLoginForm();
		}
	}
}
