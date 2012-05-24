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
import java.util.Vector;

import org.ben.mpd.event.Event;
import org.ben.mpd.event.EventDispatcher;
import org.ben.mpd.event.EventListener;
import org.ben.mpd.event.StatusUpdateEvent;

public class Playlist implements EventListener {

	private static final String GET_CHANGES = "plchanges ";
	
	private static Playlist playlist;
	
	public static void init() {
		playlist = new Playlist();
	}
	
	public static Playlist getPlaylist() {
		return playlist;
	}
	
	private Vector values;
	private View view;
	private Vector tmp;
	private Hashtable byId;
	private int id = 0;
	
	private Playlist() {
		byId = new Hashtable();
		values = new Vector();
		tmp = new Vector();
		view = new View();
		EventDispatcher.addListener(this);
	}
	
	public View getView() {
		return view;
	}
	
	public void download(int version, int length) throws IOException {
		
		synchronized(this) {
			
			MPDSocket.getSocket().getSongList((GET_CHANGES + version).getBytes(), tmp);
			
			Song song;
			int pos;
			
			for(int i=0; i<tmp.size(); i++) {
				song = (Song)tmp.elementAt(i);
				pos = song.sequence;
				
				if(pos >= values.size()) {
					values.addElement(song);
				} else {
					values.removeElementAt(pos);
					values.insertElementAt(song, pos);
				}
			}
			
			while(length < values.size()) {
				values.removeElementAt(length);
			}
			
			tmp.removeAllElements();
			byId.clear();
			
			for(int i=0; i<values.size(); i++) {
				song = (Song)values.elementAt(i);
				byId.put(new Integer(song.id), song);
			}
			
		}
		
	}
	
	public Song getSongById(int id) {
		return (Song)byId.get(new Integer(id));
	}
	
	public Vector getSongs() {
		return values;
	}

	public void eventFired(Event evt) {
		if(!(evt instanceof StatusUpdateEvent)) { return; }
		
		StatusUpdateEvent sEvt = (StatusUpdateEvent)evt;
		Hashtable values = sEvt.getValues();
		
		int playlistId = Integer.parseInt((String)values.get("playlist"));
		int length = Integer.parseInt((String)values.get("playlistlength"));
		
//TODO warn the user if you're about to download thousands of songs' metadata

		try {
			if(playlistId != id) {
//				System.out.println("Getting playlist changes from version " + id + " to " + playlistId);
				download(id, length);
				id = playlistId;
				
				view.repaint();
				
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public class View extends ScrollableList {
		
		public View() {
			super(false);
			addCommand(Main.cmd_np);
			addCommand(Main.cmd_library);
			addCommand(Main.cmd_play);
			addCommand(Main.cmd_next);
			addCommand(Main.cmd_prev);
			addCommand(Main.cmd_stop);
			addCommand(Main.cmd_save_playlist);
			addCommand(Main.cmd_load_playlist);
			addCommand(Main.cmd_rm_from_playlist);
			addCommand(Main.cmd_clear_playlist);
		}
		
		void doIt(int cursor) throws Exception {
			MPDSocket.getSocket().playPos(cursor);
		}

		Vector getValues() {
			return values;
		}

	}
}
