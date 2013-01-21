/*******************************************************************************
 * Copyright (c) 2012 Zuoshu (zuoshu.wuhan@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * Contributors:
 *     Zuoshu - initial API and implementation
 ******************************************************************************/

package com.oneguy.recognize.engine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class ConnectionHelper {
	private static final String HEADER_TYPE = "Content-Type";
	private static final String PROTOCOL = "http://";
	// private static final String URL_BASE =
	// "/speech-api/full-duplex/v1/up?key=AIzaSyBHDrl33hwRp4rMQY0ziRbj8K9LPA6vUCY&output=pb&lang=en-us&pFilter=2&client=chromium&maxAlternatives=2&continuous&interim&pair=";
	// private static final String URL_BASE =
	// "/speech-api/full-duplex/v1/down?key=AIzaSyBHDrl33hwRp4rMQY0ziRbj8K9LPA6vUCY&output=pb&pair=";
	private static final String URL_BASE = "/speech-api/full-duplex/v1/";
	private static final String FILE_DOWN = "down?key=AIzaSyBHDrl33hwRp4rMQY0ziRbj8K9LPA6vUCY&output=pb&pair=";
	private static final String FILE_UP = "up?key=AIzaSyBHDrl33hwRp4rMQY0ziRbj8K9LPA6vUCY&output=pb&lang=en-us&pFilter=2&client=chromium&maxAlternatives=2&continuous&interim&pair=";
	private static final String SERVER = "www.google.com";
	private static int TIMEOUT = 50000;
	private Object mutex = new Object();
	private Object resultMutex = new Object();
	private volatile boolean hasResult = false;
	private HttpURLConnection connection = null;
	private int totalAvaliableIp;
	private int triedTimes;

	public HttpURLConnection getAvaiableGoogleVoiceServiceUPConnection(
			String pairKey, String contentType, String method)
			throws IOException {
		// Test2.Log.d(Thread.currentThread().toString(), "UP");
		return getAvaiableGoogleVoiceServiceConnection(FILE_UP, pairKey,
				contentType, method);
	}

	public HttpURLConnection getAvaiableGoogleVoiceServiceDOWNConnection(
			String pairKey, String contentType, String method)
			throws IOException {
		// Test2.Log.d(Thread.currentThread().toString(), "DOWN");
		return getAvaiableGoogleVoiceServiceConnection(FILE_DOWN, pairKey,
				contentType, method);
	}

	// public HttpURLConnection getAvaiableGoogleVoiceServiceConnection(
	// String file, String pairKey, String contentType, String method)
	// throws IOException {
	// System.out.println("contentType:" + contentType + " method:" + method);
	// connection = null;
	// hasResult = false;
	// failTimes = 0;
	// triedTimes = 0;
	// totalAvaliableIp = 1;
	// threadConnect("www.google.com", file, pairKey, contentType, method);
	// synchronized (resultMutex) {
	// while (!hasResult && triedTimes < totalAvaliableIp) {
	// System.out.println("wait!hasResult:" + hasResult);
	// try {
	// resultMutex.wait();
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// throw new IOException("Unable to connect to server!");
	//
	// }
	// System.out.println("awake hasResult:" + hasResult
	// + " failTimes:" + failTimes + " triedTimes:"
	// + triedTimes);
	// }
	// resultMutex.notifyAll();
	// }
	// if (connection == null) {
	// throw new IOException("Unable to connect to server!");
	// }
	// Test2.Log.d(Thread.currentThread().toString(), "connection:"
	// + connection.getURL().toExternalForm());
	// return connection;
	// }

	public HttpURLConnection getAvaiableGoogleVoiceServiceConnection(
			String file, String pairKey, String contentType, String method)
			throws IOException {
		connection = null;
		hasResult = false;
		triedTimes = 0;

		InetAddress[] addresses = InetAddress.getAllByName(SERVER);
		if (addresses == null || addresses.length == 0) {
			return connection;
		}
		int addCount = addresses.length;
		totalAvaliableIp = addCount;
		synchronized (mutex) {
			for (InetAddress address : addresses) {
				if (!hasResult) {
					threadConnect(address.getHostAddress(), file, pairKey,
							contentType, method);
				}
			}
			mutex.notifyAll();
		}
		synchronized (resultMutex) {
			while (!hasResult && triedTimes < totalAvaliableIp) {
				// System.out.println("wait!hasResult:" + hasResult);
				try {
					resultMutex.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new IOException("Unable to connect to server!");

				}
				// System.out.println("awake hasResult:" + hasResult
				// + " failTimes:" + failTimes + " addCount:" + addCount);
			}
			resultMutex.notifyAll();
		}
		if (connection == null) {
			throw new IOException("Unable to connect to server!");
		}
		// Test2.Log.d(Thread.currentThread().toString(), "connection:"
		// + connection.getURL().toExternalForm());
		return connection;
	}

	private void threadConnect(final String ip, final String file,
			final String pairKey, final String contentType, final String method) {
		synchronized (mutex) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					connect(ip, file, pairKey, contentType, method);
				}
			}).start();
			mutex.notifyAll();
		}
	}

	private void connect(String ip, String file, String pairKey,
			String contentType, String method) {

		String connUrl = PROTOCOL + ip + URL_BASE + file + pairKey;
		// String connUrl =
		// "http://www.google.com/speech-api/v1/recognize?lang=en-us&client=chromium";
		HttpURLConnection conn = null;
		DataOutputStream outStream_send = null;
		// Test2.Log.d(Thread.currentThread().toString(),
		// "-----------------connect:" + file);
		try {
			URL url = new URL(connUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(TIMEOUT); // 缓存的最长时�?
			conn.setConnectTimeout(TIMEOUT);
			conn.setRequestMethod(method);
			if (contentType != null && contentType.length() != 0) {
				// System.out.println(HEADER_TYPE + "->" + contentType);
				conn.setRequestProperty(HEADER_TYPE, contentType);
			}
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(true);
			conn.setAllowUserInteraction(true);
			// conn.setHostnameVerifier(new HostnameVerifier() {
			// @Override
			// public boolean verify(String arg0, SSLSession arg1) {
			// return true;
			// }
			// });
			// conn.connect();
			conn.getOutputStream();
			setResult(conn, ip);
		} catch (MalformedURLException e) {
			if (conn != null) {
				conn.disconnect();
			}
			addFailCount();
		} catch (IOException e) {
			if (conn != null) {
				conn.disconnect();
			}
			addFailCount();
		} finally {
			increaseTriedTimes();
		}
	}

	private void setResult(HttpURLConnection conn, String ip) {
		synchronized (resultMutex) {
			hasResult = true;
			connection = conn;
			// Test2.Log.d(Thread.currentThread().toString(),
			// connection.getURL()
			// .toExternalForm());
			resultMutex.notifyAll();
		}
	}

	private void addFailCount() {
		synchronized (resultMutex) {
			resultMutex.notifyAll();
		}
	}

	private String generateRequestKey() {
		Random random = new Random(System.currentTimeMillis());
		long value = random.nextLong();
		return Long.toHexString(value);
	}

	void increaseTriedTimes() {
		synchronized (resultMutex) {
			triedTimes++;
			resultMutex.notifyAll();
		}
	}
}