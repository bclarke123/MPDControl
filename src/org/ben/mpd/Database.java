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

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class Database {

	private static final String DB_NAME = "MPDControl";
	private static RecordStore recordStore;
	
	private static String host;
	private static int port = 6600;
	
	public static void loadStoredCredentials() throws RecordStoreException {
		try {
			byte[] record = recordStore.getRecord(1);
			Database.port = ((record[0] & 0xff) << 8) + (record[1] & 0xff);
			Database.host = Utils.toString(record, 2, record.length - 2);
			System.out.println("Found record");
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Didn't find record");
		}
	}
	
	public static void setCredentials(String host, int port) throws RecordStoreException {
		
		Database.host = host;
		Database.port = port;
		
		byte[] b = new byte[host.length() + 2];
		b[0] = (byte)((port >> 8) & 0xff);
		b[1] = (byte)((port & 0xff));
		
		System.arraycopy(host.getBytes(), 0, b, 2, host.length());
		
		if(recordStore.getNumRecords() > 0) {
			recordStore.setRecord(1, b, 0, b.length);
		} else {
			recordStore.addRecord(b, 0, b.length);
		}
		
		System.out.println("Saved record");
	}
	
	public static void close() throws RecordStoreException {
		recordStore.closeRecordStore();
	}
	
	public static String getHost() {
		return host;
	}

	public static void setHost(String host) {
		Database.host = host;
	}

	public static int getPort() {
		return port;
	}

	public static void setPort(int port) {
		Database.port = port;
	}

	static {
		try {
			recordStore = RecordStore.openRecordStore(DB_NAME, true);
		} catch (Exception e) {
			Utils.handleException(e);
		}
	}
}
