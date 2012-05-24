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

import javax.microedition.lcdui.CustomItem;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Ticker extends CustomItem {
	
	private static final int STEP = 2;
	
	private static final int BORDER_X = 20;
	private static final int BORDER_Y = 3;
	
	Image offscreen;
	Graphics offscreeng;
	Font font;
	String message;
	
	int state = MPDSocket.STATE_STOPPED;
	int offset = 0;
	int msgWidth;
	int msgHeight;

	public Ticker(String message) {
		super(null);
		setMessage(message);
	}
	
	public void setMessage(String message) {
		if(this.message == null || !this.message.equals(message)) {
			this.message = message;
			this.offscreeng = null;
		}
	}
	
	public void paint(Graphics g, int width, int height) {
		
		if(offscreeng == null) {
			
			font = g.getFont();
			
			msgWidth = font.stringWidth(this.message) + BORDER_X;
			msgHeight = font.getHeight() + BORDER_Y;
			
			offscreen = Image.createImage(msgWidth, msgHeight);
			offscreeng = offscreen.getGraphics();
			
			offscreeng.setColor(0xffffff);
			offscreeng.fillRect(0, 0, msgWidth, msgHeight);
			
			offscreeng.setFont(font);
			offscreeng.setColor(0x000000);
			offscreeng.drawString(this.message, BORDER_X / 2, BORDER_Y / 2, 0);
			
		}
		
		offset -= STEP;
		
		
		int y = (height - msgHeight) / 2;
		
		g.drawImage(offscreen, offset, y, 0);
		
		int dist = Math.max(width, msgWidth);
		
		if(offset + msgWidth <= width) {
			g.drawImage(offscreen, offset + dist, y, 0);
		}
		
		if(offset + dist <= 0) {
			offset = offset + dist;
		}
		
		g.setColor(0x000000);
		g.drawRect(0, 0, width - 1, height - 1);
		
	}
	
	public void updateState(Song song, int state) {

		StringBuffer buf = new StringBuffer();
		
		this.state = state;
		
		if(state == MPDSocket.STATE_STOPPED) {
			buf.append("(Stopped)  ");
		} else
		if(state == MPDSocket.STATE_PAUSED) {
			buf.append("(Paused)  ");
		} else {
			buf.append("(Playing)  ");
		}
		
		if(song != null) {
			buf.append(song.humanName());
		}
		
		setMessage(buf.toString());
	}

	protected int getMinContentHeight() {
		return 25;
	}

	protected int getMinContentWidth() {
		return 40;
	}

	protected int getPrefContentHeight(int width) {
		return 25;
	}

	protected int getPrefContentWidth(int height) {
		return 320;
	}
	
	public void doRepaint() {
		repaint();
	}
	
}
