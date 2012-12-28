package engine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.text.TextUtils;
import android.util.Log;
import engine.StreamResponse.Status;

public class Stream {
	private static final String TAG = "Stream";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_GET = "GET";
	private static final String CONTENT_TYPE_STR = "Content-Type";

	private static final int STATE_IDLE = 1001;
	private static final int STATE_CONNECTED = 1002;

	private int mState;
	private StreamListener mStreamListener;
	private String url;
	private HttpURLConnection connection;
	private int connectTimeout;
	private int socketTimeout;
	private String method;
	private String contentType;
	private String name;

	private DataInputStream inStream;
	private DataOutputStream outStream;

	public Stream(String name, StreamListener listener, String url) {
		this.name = name;
		mStreamListener = listener;
		this.url = url;
		connectTimeout = 0;
		socketTimeout = 0;
		inStream = null;
		outStream = null;
		method = "POST";// default post
		mState = STATE_IDLE;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public boolean connect() {
		Log.d(TAG, name + " doConnect url:" + url + " method:" + method
				+ " contentType:" + contentType);
		if (url == null || url.length() == 0) {
			onError("no url specified!");
			return false;
		}
		try {
			URL uri = new URL(url);
			connection = (HttpURLConnection) uri.openConnection();
			if (connectTimeout > 0) {
				connection.setConnectTimeout(connectTimeout);
			}
			if (socketTimeout > 0) {
				connection.setReadTimeout(socketTimeout);
			}
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(true);
			if (!TextUtils.isEmpty(method)) {
				connection.setRequestMethod(method);
			}
			if (!TextUtils.isEmpty(contentType)) {
				connection.setRequestProperty(CONTENT_TYPE_STR, contentType);
			}
			if (outStream == null) {
				try {
					outStream = new DataOutputStream(
							connection.getOutputStream());
				} catch (IOException e) {
					onError(e);
					return false;
				}
			}
			mState = STATE_CONNECTED;
			Log.d(TAG, name + "->STATE_CONNECTED");
			return true;
		} catch (MalformedURLException e) {
			onError(e);
			return false;
		} catch (IOException e) {
			onError(e);
			return false;
		}
	}

	public void transmitData(byte[] data) {
		Log.d(TAG, "transmitData:" + data.length);
		if (data == null || connection == null) {
			return;
		}
		if (outStream == null) {
			onError("connect first");
			return;
		}
		try {
			outStream.write(data);
			outStream.flush();
			Log.d(TAG, name + "->write:" + data.length);
		} catch (IOException e) {
			getResponse();
			onError(e);
			return;
		}
	}

	public void disconnect() {
		Log.d(TAG, name + ":disconnect");
		if (inStream != null) {
			try {
				inStream.close();
				inStream = null;
			} catch (IOException e) {
				onError(e);
			}
		}
		if (outStream != null) {
			try {
				outStream.close();
				outStream = null;
			} catch (IOException e) {
				onError(e);
			}
		}
		if (connection != null) {
			connection.disconnect();
		}
		mState = STATE_IDLE;
		Log.d(TAG, name + "->STATE_IDLE");
	}

	public void getResponse() {
		if (connection == null) {
			onError("connection null!");
			return;
		}
		try {
			if (inStream == null) {
//				connection.connect();
				inStream = new DataInputStream(connection.getInputStream());
			}
			if (connection.getResponseCode() == 200) {
				Log.d(TAG, name + ": in doGetResponse connect to "
						+ connection.getURL().toString());
			} else {
				if (mStreamListener != null) {
					mStreamListener
							.onStreamError(Engine.SPEECH_RECOGNITION_ERROR_NETWORK);
				}
				return;
			}
			byte[] data1 = new byte[0];
			inStream.read(data1);
			byte[] data = new byte[inStream.available()];
			inStream.read(data);
			Log.d(TAG, name + " response:" + new String(data));
			StreamResponse response = new StreamResponse(this, Status.SUCCESS);
			response.setResponseCode(connection.getResponseCode());
			response.setResponse(data);
			if (mStreamListener != null) {
				mStreamListener.onStreamResponse(response);
			}
			Log.d(TAG, name + "->read:" + data.length);
		} catch (IOException e) {
			onError(e);
			return;
		}
	}

	public void reset(String url, String method) {
		disconnect();
		this.url = url;
		this.method = method;
		Log.d(TAG, name + "->reset");
	}

	public void reset() {
		reset("", "");
	}

	public StreamListener getStreamListener() {
		return mStreamListener;
	}

	public void setStreamListener(StreamListener stateListener) {
		this.mStreamListener = stateListener;
	}

	public String getName() {
		return name;
	}

	private void onError(String tip) {
		reset();
		mState = STATE_IDLE;
		Log.e(TAG, tip);
	}

	private void onError(Exception e) {
		e.printStackTrace();
		mStreamListener.onStreamError(Engine.SPEECH_RECOGNITION_ERROR_NETWORK);
		mState = STATE_IDLE;
	}

}
