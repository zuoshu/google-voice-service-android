package com.oneguy.recognize.engine;

import java.io.IOException;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.oneguy.recognize.BuildConfig;
import com.oneguy.recognize.engine.StreamResponse.Status;
import com.oneguy.recognize.recognize.EngineResultListener;


public class GoogleStreamingEngine extends AbstractEngine implements Runnable,
		StreamListener {
	private static final String TAG = "GoogleStreamingEngine";
	private static final int TIMEOUT = 10 * 1000;
	private static final int TRY_STOP_TIME = 100;
	private static final int THREAD_TRY_TIME = 20;

	public static final int EVENT_START = 3001;
	public static final int EVENT_STOP = 3002;
	public static final int EVENT_SHUTDOWN = 3003;
	public static final int EVENT_AUDIO_CHUNK = 3004;
	public static final int EVENT_RESPONSE = 3005;
	public static final int EVENT_STREAM_ERROR = 3006;

	private static final int STATE_IDLE = 1001;
	private static final int STATE_UP_STREAM_CONNECTED = 1002;
	private static final int STATE_WAITING_DOWNSTREAM_RESULTS = 1003;
	private static final int STATE_SHUTDOWN = 1004;

	private static final String GOOGLE_API_KEY = "AIzaSyBHDrl33hwRp4rMQY0ziRbj8K9LPA6vUCY";
	private static final String BASE_URL = "https://www.google.com/speech-api/full-duplex/v1";
	private static final String UP_STREAM_PRFX = "/up?";
	private static final String DOWN_STREAM_PRFX = "/down?";

	private int mState;
	private EngineResultListener mRecognizeListener;
	private Handler mHandler;
	private GetResponseRunnable downStreamRunnable;
	private Stream upStream;
	private boolean firstRun = true;
	private String mPairKey;

	public GoogleStreamingEngine() {
		// set url when connect streams
		upStream = new Stream("upStream", this, "");
		upStream.setConnectTimeout(TIMEOUT);
		upStream.setSocketTimeout(TIMEOUT);
		upStream.setMethod(Stream.METHOD_POST);
		upStream.setStreamListener(this);
		// downStreamRunnable = new GetResponseRunnable();
		firstRun = true;
		mState = STATE_IDLE;
	}

	@Override
	public void setRecognizeListener(EngineResultListener l) {
		mRecognizeListener = l;
	}

	public void setMimeType(String mimeType) {
		super.setMimeType(mimeType);
		upStream.setContentType(mimeType);
	}

	private void initThread() {
		new Thread(this).start();
		int tryTimes = 0;
		while (mHandler == null && tryTimes < THREAD_TRY_TIME) {
			try {
				Log.d(TAG, "wait engine init!");
				Thread.sleep(TRY_STOP_TIME);
				tryTimes++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void start() {
		if (firstRun) {
			initThread();
			firstRun = false;
		}
		if (mHandler == null) {
			Log.e(TAG, "can not start,run engine fail!");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_START);
	}

	@Override
	public void stop() {
		if (mHandler == null) {
			Log.e(TAG, "can not stop,run engine in a thread first!");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_STOP);
	}

	@Override
	public void shutdown() {
		if (mHandler == null) {
			Log.e(TAG, "can not shutdown,run engine in a thread first!");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_SHUTDOWN);
	}

	@Override
	public void takeAudioChunk(byte[] chunk) {
		if (mHandler == null) {
			Log.e(TAG, "can not transmit,run engine in a thread first!");
			return;
		}
		if (chunk == null) {
			return;
		}
		Message msg = new Message();
		msg.what = EVENT_AUDIO_CHUNK;
		msg.obj = chunk;
		mHandler.sendMessage(msg);
	}

	@Override
	public void run() {
		Looper.prepare();
		mHandler = new Handler() {
			@Override
			public synchronized void handleMessage(Message msg) {
				int event = msg.what;
				Object arg = msg.obj;
				Log.d(TAG, "engine state:" + getStateName(mState) + " event:"
						+ getEventName(event));
				switch (mState) {
				case STATE_IDLE:
					switch (event) {
					case EVENT_START:
						connectUpStream();
						break;
					case EVENT_SHUTDOWN:
						shutAll();
						break;
					case EVENT_STREAM_ERROR:
						abortWithError();
						break;
					case EVENT_STOP:
					case EVENT_AUDIO_CHUNK:
					case EVENT_RESPONSE:
						Log.d(TAG, "drop event:" + event + " state:IDLE");
						break;
					}
					break;
				case STATE_UP_STREAM_CONNECTED:
					switch (event) {
					case EVENT_STOP:
						sendStopPacketAndGetResponse();
						break;
					case EVENT_SHUTDOWN:
						shutAll();
						break;
					case EVENT_AUDIO_CHUNK:
						transmitAudioUpstream((byte[]) arg);
						break;
					case EVENT_STREAM_ERROR:
						abortWithError();
						break;
					case EVENT_START:
					case EVENT_RESPONSE:
						Log.d(TAG, "drop event:" + event + " state:IDLE");
						break;
					}
					break;
				case STATE_WAITING_DOWNSTREAM_RESULTS:
					switch (event) {
					case EVENT_START:
						break;
					case EVENT_STOP:
						break;
					case EVENT_SHUTDOWN:
						shutAll();
						break;
					case EVENT_AUDIO_CHUNK:
						break;
					case EVENT_RESPONSE:
						processResponse((StreamResponse) arg);
						break;
					case EVENT_STREAM_ERROR:
						abortWithError();
						break;
					}
					break;
				case STATE_SHUTDOWN:
					Log.e(TAG,
							"impossible,STATE_SHUTDOWN should not receive event!");
					break;
				}
			}

		};
		Looper.loop();
	}

	public Handler getHandler() {
		return mHandler;
	}

	private void connectUpStream() {
		mPairKey = generateRequestPairKey();
		upStream.setUrl(buildUpUrl(mPairKey));
		upStream.setPairKey(mPairKey);
		upStream.setMethod(Stream.METHOD_POST);
		if (upStream.connect2()) {
			Log.d(TAG, "engine->STATE_UP_STREAM_CONNECTED");
			mState = STATE_UP_STREAM_CONNECTED;
		}
	}

	boolean firstTransmit = true;

	private synchronized void transmitAudioUpstream(byte[] chunk) {
		upStream.transmitData(chunk);
//		if (firstTransmit) {
//			firstTransmit = false;
//			new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					fetchResults();
//				}
//			}).start();
//		}
	}

	private void sendStopPacketAndGetResponse() {
		sendStopPacket();
		// fetchResults();
		upStream.getResponse();
		Log.d(TAG, "engine->STATE_WAITING_DOWNSTREAM_RESULTS");
		mState = STATE_WAITING_DOWNSTREAM_RESULTS;
	}

	private void sendStopPacket() {
		byte[] endPacket = new byte[100];
		transmitAudioUpstream(endPacket);
		Log.d(TAG, "engine->STATE_WAITING_DOWNSTREAM_RESULTS");
		mState = STATE_WAITING_DOWNSTREAM_RESULTS;
		// upStream.getResponse();
	}

	private void fetchResults() {
		Log.d(TAG, "engine->fetchResults");
		byte[] data = getHttpData(buildDownUrl(mPairKey));
		// TODO post data to handler
		if (data != null) {
			StreamResponse response = new StreamResponse(null, Status.SUCCESS);
			response.setResponse(data);
			onStreamResponse(response);
		} else {
			StreamResponse response = new StreamResponse(null, Status.ERROR);
			onStreamResponse(response);
		}
	}

	private void shutAll() {
		upStream.disconnect();
		mHandler.getLooper().quit();
	}

	private void abortSilently() {
		abort(SPEECH_RECOGNITION_ERROR_NONE);
	}

	private void abortWithError() {
		Log.d(TAG, "abortWithError");
		abort(SPEECH_RECOGNITION_ERROR_NETWORK);
	}

	private void abort(int errorCode) {
		if (errorCode != SPEECH_RECOGNITION_ERROR_NONE) {
			if (mRecognizeListener != null) {
				mRecognizeListener.onError(errorCode);
			}
		}
		upStream.reset();
		Log.d(TAG, "engine->IDLE");
		mState = STATE_IDLE;
	}

	private String generateRequestPairKey() {
		Random random = new Random(System.currentTimeMillis());
		long value = random.nextLong();
		return Long.toHexString(value);
	}

	private String buildUpUrl(String pairKey) {
		StringBuilder upUrl = new StringBuilder();
		upUrl.append(BASE_URL);
		upUrl.append(UP_STREAM_PRFX);
		upUrl.append("key=" + GOOGLE_API_KEY);
		upUrl.append("&pair=");
		upUrl.append(pairKey);
		upUrl.append("&output=pb");
		upUrl.append("&lang=en-us");
		upUrl.append("&pFilter=0");
		upUrl.append("&client=chromium");
		upUrl.append("&maxAlternatives=1");
		upUrl.append("&continuous");
		upUrl.append("&interim");
		return upUrl.toString();
	}

	private String buildDownUrl(String pairKey) {
		StringBuilder downUrl = new StringBuilder();
		downUrl.append(BASE_URL);
		downUrl.append(DOWN_STREAM_PRFX);
		// downUrl.append("key=" + GOOGLE_API_KEY);
		downUrl.append("pair=");
		downUrl.append(pairKey);
		downUrl.append("&output=pb");
		return downUrl.toString();
	}

	private void reset() {
		firstTransmit = true;
		upStream.reset();
		mState = STATE_IDLE;
	}

	@Override
	public void onStreamResponse(StreamResponse response) {
		if (mState == STATE_SHUTDOWN) {
			Log.d(TAG,
					"engine is SHUTDOWN,ignore response:" + response.toString());
		} else {
			Log.d(TAG, "response:" + response.toString());
			Message msg = new Message();
			msg.what = EVENT_RESPONSE;
			msg.obj = response;
			mHandler.sendMessage(msg);
		}
	}

	@Override
	public void onStreamError(int errorCode) {
		if (mState == STATE_SHUTDOWN) {
			Log.d(TAG, "engine is SHUTDOWN,ignore errorcode:" + errorCode);
		} else {
			mHandler.sendEmptyMessage(EVENT_STREAM_ERROR);
		}
	}

	private void processResponse(StreamResponse response) {
//		if (response != null && response.getSource() == upStream) {
//			// ignore upstream response
//			return;
//		}

		if ((response == null || response.getStatus() == Status.ERROR)
				&& (response.getResponse() == null || response.getResponse().length == 0)) {
			abortWithError();
			return;
		}

//		if (response.getSource() == upStream) {
//			// ignore upstream response
//			Log.d(TAG, "response.getSource() == upStream");
//			return;
//		} else {
//			Log.d(TAG, "response source:" + response.getSource());
//		}
		ChunkBuffer cb = new ChunkBuffer();
		cb.transform(response.getResponse());
		if (cb.hasChunk()) {
			String result = cb.getBestResult();
			if (mRecognizeListener != null) {
				mRecognizeListener.onResult(result);
			}
		} else {
			if (mRecognizeListener != null) {
				mRecognizeListener.onError(SPEECH_RECOGNITION_ERROR_RECOGNIZE);
			}
		}
		reset();
	}

	byte[] getHttpData(String url) {
		String TAG = "getHttpData";
		HttpGet httpGet = new HttpGet(url);
		byte[] result = null;
		try {
			HttpClient client = new DefaultHttpClient();
			client.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT);
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
					TIMEOUT);
			Log.d(TAG,
					"HTTP GET:" + httpGet.getURI() + " method:"
							+ httpGet.getMethod());
			HttpResponse httpResponse = client.execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				result = EntityUtils.toByteArray(httpResponse.getEntity());
				Log.d(TAG, new String(result));
				StreamResponse response = new StreamResponse(null,
						Status.SUCCESS);
				response.setResponse(result);
				response.setResponseCode(httpResponse.getStatusLine()
						.getStatusCode());
				Message msg = new Message();
				msg.what = EVENT_RESPONSE;
				msg.obj = response;
				mHandler.sendMessage(msg);
			}
		} catch (ClientProtocolException e) {
			onError(e);
			e.printStackTrace();
		} catch (IOException e) {
			onError(e);
			e.printStackTrace();
		}
		return result;
	}

	boolean hasGetResponse = false;

	private static final int GET_REPONSE_INTERNAL = 100;

	class GetResponseRunnable implements Runnable {
		String url = "";

		public void setUrl(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			getHttpData(url);
			if (!hasGetResponse) {
				mHandler.postDelayed(this, GET_REPONSE_INTERNAL);
			}
		}

		byte[] getHttpData(String url) {
			String TAG = "getHttpData";
			HttpGet httpGet = new HttpGet(url);
			byte[] result = null;
			try {
				HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter(
						CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT);
				client.getParams().setParameter(
						CoreConnectionPNames.SO_TIMEOUT, TIMEOUT);
				Log.d(TAG, "HTTP GET:" + httpGet.getURI() + " method:"
						+ httpGet.getMethod());
				HttpResponse httpResponse = client.execute(httpGet);
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					result = EntityUtils.toByteArray(httpResponse.getEntity());
					StreamResponse response = new StreamResponse(null,
							Status.SUCCESS);
					response.setResponse(result);
					response.setResponseCode(httpResponse.getStatusLine()
							.getStatusCode());
					Message msg = new Message();
					msg.what = EVENT_RESPONSE;
					mHandler.sendMessage(msg);
				}
			} catch (ClientProtocolException e) {
				onError(e);
				e.printStackTrace();
			} catch (IOException e) {
				onError(e);
				e.printStackTrace();
			}
			return result;
		}

	}

	private void onError(Exception e) {
		e.printStackTrace();
		onStreamError(SPEECH_RECOGNITION_ERROR_NETWORK);
	}

	private String getEventName(int event) {
		switch (event) {
		case EVENT_START:
			return "EVENT_START";
		case EVENT_STOP:
			return "EVENT_STOP";
		case EVENT_SHUTDOWN:
			return "EVENT_SHUTDOWN";
		case EVENT_AUDIO_CHUNK:
			return "EVENT_AUDIO_CHUNK";
		case EVENT_RESPONSE:
			return "EVENT_DOWNSTREAM_RESPONSE";
		case EVENT_STREAM_ERROR:
			return "EVENT_STREAM_ERROR";
		default:
			return "unknown:" + event;
		}
	}

	private String getStateName(int state) {
		switch (state) {
		case STATE_IDLE:
			return "STATE_IDLE";
		case STATE_UP_STREAM_CONNECTED:
			return "STATE_UP_STREAM_CONNECTED";
		case STATE_WAITING_DOWNSTREAM_RESULTS:
			return "STATE_WAITING_DOWNSTREAM_RESULTS";
		default:
			return "unknown:" + state;
		}
	}
}
