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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

public abstract class ScrollableList extends GameCanvas {

	protected final int SELECTED_COLOR = 0xACC6F6;
	protected static final int HILIGHT_COLOR = 0xCBE8FF;
	protected static final int BACKGROUND = 0xFFFFFF;
	protected static final int FOREGROUND = 0x000000;
	protected static final int PADDING = 5;
	protected static final int PAGE_SIZE = 10; 
	
	protected int offset = 0;
	protected int selectedIndex = -1;
	protected int cursor = 0;
	protected int lineHeight;
	protected int lines;
	
	protected ScrollableList(boolean suppressKeyEvents) {
		super(suppressKeyEvents);
	}
	
	abstract Vector getValues();
	abstract void doIt(int cursor) throws Exception;
	
	public void paint(Graphics g) {
		
		int width = getWidth() - 5;
		int height = getHeight();
		
		Vector values = getValues();
		
		int size = values.size();
		
		Font font = g.getFont();
		
		lineHeight = (font.getHeight() + (PADDING * 2));
		lines = height / lineHeight;
		int start = Math.max(0, Math.min(offset, size - lines));
		int end = Math.min(start + lines, size);
		
		g.setColor(BACKGROUND);
		g.fillRect(0, 0, width, height);
		
		g.setColor(FOREGROUND);
		
		int y = PADDING;
		int lineY;
		Song song;
		Directory dir;
		Object obj;
		
		for(int i=start; i<end; i++) {

			if(i == cursor) {
				g.setColor(HILIGHT_COLOR);
				g.fillRect(0, y - PADDING + 1, width, lineHeight - 1);
				g.setColor(FOREGROUND);
			} else
			if(i == selectedIndex) {
				g.setColor(SELECTED_COLOR);
				g.fillRect(0, y - PADDING + 1, width, lineHeight - 1);
				g.setColor(FOREGROUND);
			}
			
			obj = values.elementAt(i);
			if(obj instanceof Song) {
				song = (Song)values.elementAt(i);
				g.drawString((i + 1) + ". " + song.humanName(), PADDING, y, 0);
			} else
			if(obj instanceof Directory) {
				dir = (Directory)values.elementAt(i);
				g.drawString((i + 1) + ". [" + dir.name + "]", PADDING, y, 0);
			} else {
				g.drawString(obj.toString(), PADDING, y, 0);
			}
			
			lineY = y + lineHeight - PADDING;
			g.drawLine(0, lineY, width, lineY);
			
			y += lineHeight;
		}
		
		int pxVisible = Math.max(5, (int)(((float)lines / (float)size) * height));
		int travel = Math.max(0, height - pxVisible);
		int pos = (int)(((float)cursor / (float)size) * travel);
		
		g.setColor(FOREGROUND);
		g.fillRect(width, 0, 5, height);
		
		g.setColor(SELECTED_COLOR);
		g.fillRect(width + 1, pos, 4, pxVisible);
		
	}
	
	public void setSelectedIndex(int idx) {
		
		if(idx != selectedIndex) {
			selectedIndex = idx;
			if(selectedIndex >= offset && selectedIndex <= offset + lines) {
				repaint();
			}
		}
	}
	
	public String getSelected() {
		Object obj = getValues().elementAt(cursor);
		if(obj instanceof Directory) {
			return ((Directory)obj).name;
		} else {
			return ((Song)obj).file;
		}
	}
	
	public int getCursor() {
		return cursor;
	}
	
	public void keyPressed(int key) {
		int gameAction = getGameAction(key);
		Vector values = getValues();
		int page;
		switch(gameAction) {
			case Canvas.UP: 
				if(cursor > 0) { 
					cursor--;
					if(offset + 2 > cursor) {
						offset = Math.max(0, cursor - 2);
					}
				}
			break;
			case Canvas.DOWN: 
				if(cursor < values.size() - 1) { 
					cursor++;
					if((offset + lines) - 3 < cursor) {
						offset = (cursor - lines) + 3;
					}
				}
			break;
			case Canvas.LEFT:
				
				if(cursor > 0) {
					page = Math.max(cursor - PAGE_SIZE, 0);
					cursor = (cursor < PAGE_SIZE) ? 0 : page + 2;
					offset = page;
				}
				
			break;
			case Canvas.RIGHT: 
				
				if(cursor < values.size() - 1) {
					page = Math.min(values.size() - 1, cursor + PAGE_SIZE);
					cursor = Math.min(page + 2, values.size() - 1);
					offset = page;
				}
				
			break;
			case Canvas.FIRE:
				
				try {
					doIt(cursor);
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			break;
		}
		
		repaint();
	}

}
