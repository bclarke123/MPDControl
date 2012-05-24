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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;

public class Utils {
	
    public static int readLine(InputStream is, byte[] ret) throws IOException {
        int i=0, n, len = ret.length;

        while((n = is.read()) != '\n' && i < len) {
                if(n == -1) {
                        throw new EOFException();
                }
                ret[i++] = (byte)n;
        }

        if(i == len) {
                return 0;
        }

        if(i > 0 && ret[i - 1] == '\r') {
                i--;
        }

        return i;
    }
    
    public static int indexOf(byte[] arr, byte[] pattern) {
    	
    	if(arr == null || pattern == null) return -1;
    	
    	int al = arr.length;
    	int pl = pattern.length;
    	
    	if(al == 0 || pl == 0) return -1;
    	if(pl > al) return -1;
    	
    	boolean match;
    	
    	for(int i=0; i<al - pl + 1; i++) {
    		if(arr[i] == pattern[0]) {
    			match = true;
    			for(int j=1; j<pl; j++) {
    				if(arr[i + j] != pattern[j]) {
    					match = false;
    					break;
    				}
    			}
    			if(match) {
    				return i;
    			}
    		}
    	}
    	return -1;
    }
    
    public static int lastIndexOf(byte[] arr, byte[] pattern) {
    	if(arr == null || pattern == null) return -1;
    	
    	int al = arr.length;
    	int pl = pattern.length;
    	
    	if(al == 0 || pl == 0) return -1;
    	if(pl > al) return -1;
    	
    	boolean match;
    	
    	for(int i=al - 1; i >= 0; i--) {
    		if(arr[i] == pattern[0]) {
    			match = true;
    			for(int j=1; j<pl; j++) {
    				if(arr[i + j] != pattern[j]) {
    					match = false;
    					break;
    				}
    			}
    			if(match) {
    				return i;
    			}
    		}
    	}
    	return -1;
    }
    
    public static boolean startsWith(byte[] arr, byte[] pattern) {
    	return indexOf(arr, pattern) == 0;
    }
    
    public static boolean endsWith(byte[] arr, byte[] pattern) {
    	int idx = lastIndexOf(arr, pattern);
    	return idx > 0 && idx == arr.length - pattern.length;
    }
    
    public static boolean equals(byte[] arr, byte[] arr2) {
    	return arr.length == arr2.length && indexOf(arr, arr2) == 0;
    }
    
    public static String toString(byte[] arr, int off, int len) {
    	byte[] ret = new byte[len];
    	System.arraycopy(arr, off, ret, 0, len);
    	return new String(ret);
    }
    
    public static void parseResponseList(String response, Hashtable dst) {
    	int start = 0, end, next;
		String line;
		
		while((end = response.indexOf('\n', start)) > -1) {
			line = response.substring(start, end);
			if((next = line.indexOf(':')) > -1) {
				dst.put(
					line.substring(0, next),
					line.substring(next + 1).trim()
				);
			}
			start = end + 1;
		}
    }
    
    public static Alert handleException(Exception e) {
    	e.printStackTrace();
    	Alert a = new Alert("Error", e.getMessage(), null, AlertType.ERROR);
    	a.setTimeout(Alert.FOREVER);
    	return a;
    }
}
