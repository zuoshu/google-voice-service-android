package engine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class ConnectionHelper {
	private static final String HEADER_TYPE = "Content-Type";
	private static final String PROTOCOL = "https://";
//	private static final String URL_BASE = "/speech-api/full-duplex/v1/up?key=AIzaSyBHDrl33hwRp4rMQY0ziRbj8K9LPA6vUCY&output=pb&lang=en-us&pFilter=2&client=chromium&maxAlternatives=2&continuous&interim&pair=";
	private static final String URL_BASE = "/speech-api/full-duplex/v1/down?key=AIzaSyBHDrl33hwRp4rMQY0ziRbj8K9LPA6vUCY&output=pb&pair=";
	private static final String SERVER = "www.google.com";
	private static int TIMEOUT = 5000;
	private Object mutex = new Object();
	private Object resultMutex = new Object();
	private volatile boolean hasResult = false;
	private int failTimes = 0;
	private HttpsURLConnection connection = null;

	public static void main(String[] args) {
		ConnectionHelper t = new ConnectionHelper();
		String pairKey = t.generateRequestKey();
		HttpsURLConnection conn;
		Test2.logTime("start");
		try {
			conn = t.getAvaiableGoogleVoiceServiceConnection(pairKey,
					"audio/L16;rate=8000", "POST");
			System.out.println("getAvaiableGoogleVoiceServiceAddress->"
					+ conn.getURL().getHost());
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Test2.logTime("end");
	}

	public HttpsURLConnection getAvaiableGoogleVoiceServiceConnection(
			String pairKey, String contentType, String method)
			throws IOException {
		connection = null;
		hasResult = false;
		failTimes = 0;

		InetAddress[] addresses = InetAddress.getAllByName(SERVER);
		if (addresses == null || addresses.length == 0) {
			return connection;
		}
		int addCount = addresses.length;
		synchronized (mutex) {
			for (InetAddress address : addresses) {
				if (!hasResult) {
					threadConnect(address.getHostAddress(), pairKey,
							contentType, method);
				}
			}
			mutex.notifyAll();
		}
		synchronized (resultMutex) {
			while (!hasResult && failTimes < addCount) {
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

		return connection;
	}

	private void threadConnect(final String ip, final String pairKey,
			final String contentType, final String method) {
		synchronized (mutex) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					connect(ip, pairKey, contentType, method);
				}
			}).start();
		}
		mutex.notifyAll();
	}

	private void connect(String ip, String pairKey, String contentType,
			String method) {

		String url_send = PROTOCOL + ip + URL_BASE + pairKey;
		HttpsURLConnection conn = null;
		DataOutputStream outStream_send = null;

		try {
			URL url = new URL(url_send);
			conn = (HttpsURLConnection) url.openConnection();
			conn.setReadTimeout(TIMEOUT); // 缓存的最长时间
			conn.setConnectTimeout(TIMEOUT);
			conn.setRequestMethod(method);
			conn.setRequestProperty(HEADER_TYPE, contentType);
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			});
			conn.connect();
			setResult(conn, ip);
		} catch (MalformedURLException e) {
			addFailCount();
		} catch (IOException e) {
			addFailCount();
		} finally {
			if (outStream_send != null) {
				try {
					outStream_send.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private void setResult(HttpsURLConnection conn, String ip) {
		synchronized (resultMutex) {
			if (!hasResult) {
				hasResult = true;
				connection = conn;
			}
			resultMutex.notifyAll();
		}
	}

	private void addFailCount() {
		synchronized (resultMutex) {
			failTimes++;
			resultMutex.notifyAll();
		}
	}

	private String generateRequestKey() {
		Random random = new Random(System.currentTimeMillis());
		long value = random.nextLong();
		return Long.toHexString(value);
	}

}