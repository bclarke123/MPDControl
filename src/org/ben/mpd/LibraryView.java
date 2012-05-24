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

import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class LibraryView extends ScrollableList implements Runnable {

	Vector values = new Vector();
	String currDir;
	
	public LibraryView() {
		
		super(false);
		
		addCommand(Main.cmd_np);
		addCommand(Main.cmd_playlist);
		addCommand(Main.cmd_play);
		addCommand(Main.cmd_next);
		addCommand(Main.cmd_prev);
		addCommand(Main.cmd_stop);
		addCommand(Main.cmd_add_to_playlist);
		addCommand(Main.cmd_descend);
		addCommand(Main.cmd_ascend);
	}
	
	public void parentDirectory() {
		
		if(!"".equals(currDir)) {
			int slash = currDir.lastIndexOf('/');
			if(slash == -1) {
				download("");
			} else {
				download(currDir.substring(0, slash));
			}
		}
		
	}
	
	public void download(String dir) {
		this.currDir = dir;
		this.values.removeAllElements();
		this.cursor = this.offset = 0;
		
		new Thread(this).start();
	}
	
	public void run() {
		try {
			
			MPDSocket.getSocket().loadLibrary(this.currDir, this.values);
			repaint();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void paint(Graphics g) {
		
		int width = getWidth() - 5;
		int height = getHeight();
		Font font = g.getFont();
		
		if(values == null || values.size() == 0) {
			String msg = "Loading...";
			g.setColor(BACKGROUND);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(FOREGROUND);
			int strWidth = font.stringWidth(msg);
			g.drawString(msg, (width - strWidth) / 2, (height - font.getHeight()) / 2, 0);
			return;
		}
		
		super.paint(g);
		
	}
	
	void doIt(int cursor) throws Exception {
		MPDSocket.getSocket().addToPlaylist(getSelected());
	}

	Vector getValues() {
		return values;
	}

}
