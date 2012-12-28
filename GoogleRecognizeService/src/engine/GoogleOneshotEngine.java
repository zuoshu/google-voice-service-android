package engine;

import recognize.RecognizeListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import engine.StreamResponse.Status;

public class GoogleOneshotEngine extends AbstractEngine implements Runnable,
		StreamListener {
	private static final String TAG = "GoogleOneshotEngine";
	public static final int EVENT_START = 3001;
	public static final int EVENT_STOP = 3002;
	public static final int EVENT_SHUTDOWN = 3003;
	public static final int EVENT_TAKE_AUDIO_CHUNK = 3004;
	public static final int EVENT_RESULTS = 3005;
	public static final int EVENT_CONNECTION_ERROR = 3006;

	private static final int STATE_IDLE = 1001;
	private static final int STATE_CONNECTED = 1002;
	private static final int STATE_WAITING_RESULTS = 1003;
	private static final int STATE_SHUTDOWN = 1004;

	private int mState;
	private RecognizeListener mRecognizeListener;
	private Handler mHandler;
	private Stream mStream;
	private boolean firstRun;
	private static final int TIMEOUT = 15 * 1000;
	private static final int TRY_STOP_TIME = 100;
	private static final String ONE_SHOT_SPEECH_API_URL = "http://www.google.com/speech-api/v1/recognize?lang=en-us&client=chromium";
	private static final String METHOD_POST = "POST";

	public GoogleOneshotEngine() {
		mStream = new Stream("oneshot_stream", this, "");
		mStream.setConnectTimeout(TIMEOUT);
		mStream.setSocketTimeout(TIMEOUT);
		mStream.reset(ONE_SHOT_SPEECH_API_URL, METHOD_POST);
		firstRun = true;
		mState = STATE_IDLE;
	}

	@Override
	public void setRecognizeListener(RecognizeListener l) {
		mRecognizeListener = l;
	}

	public void setMimeType(String mimeType) {
		super.setMimeType(mimeType);
		mStream.setContentType(mimeType);
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

	private void initThread() {
		new Thread(this).start();
		int tryTimes = 0;
		while (mHandler == null && tryTimes < 20) {
			try {
				Log.d(TAG, "wait engine start!");
				Thread.sleep(TRY_STOP_TIME);
				tryTimes++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
		msg.what = EVENT_TAKE_AUDIO_CHUNK;
		msg.obj = chunk;
		mHandler.sendMessage(msg);
	}

	@Override
	public void stop() {
		if (mHandler == null) {
			Log.e(TAG, "can not stop,run engine in a thread first!");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_STOP);
	}

	/**
	 * stop but no response
	 */
	@Override
	public void shutdown() {
		mHandler.sendEmptyMessage(EVENT_SHUTDOWN);
	}

	@Override
	public void run() {
		Looper.prepare();
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				int event = msg.what;
				Object arg = msg.obj;
				Log.d(TAG, "mState:" + mState + " event:" + event);
				switch (mState) {
				case STATE_IDLE:
					switch (event) {
					case EVENT_START:
						connectStream();
						break;
					case EVENT_SHUTDOWN:
						shutAll();
						break;
					case EVENT_TAKE_AUDIO_CHUNK:
					case EVENT_STOP:
					case EVENT_RESULTS:
					case EVENT_CONNECTION_ERROR:
						Log.d(TAG, "drop event:" + event + " state:IDLE");
						break;
					}
					break;
				case STATE_CONNECTED:
					switch (event) {
					case EVENT_SHUTDOWN:
						stopAndShutAll();
						break;
					case EVENT_TAKE_AUDIO_CHUNK:
						transmitAudioUpstream((byte[]) arg);
						break;
					case EVENT_STOP:
						closeUpstreamAndWaitResults();
						break;
					case EVENT_CONNECTION_ERROR:
						abortWithError();
						break;
					case EVENT_START:
					case EVENT_RESULTS:
						Log.d(TAG, "drop event:" + event
								+ " state:STATE_CONNECTED");
						break;
					}
					break;
				case STATE_WAITING_RESULTS:
					switch (event) {
					case EVENT_SHUTDOWN:
						stopAndShutAll();
						break;
					case EVENT_RESULTS:
						processDownstreamResponse((StreamResponse) arg);
						break;
					case EVENT_CONNECTION_ERROR:
						abortWithError();
						break;
					case EVENT_START:
					case EVENT_TAKE_AUDIO_CHUNK:
					case EVENT_STOP:
						Log.d(TAG, "drop event:" + event
								+ " state:STATE_WAITING_RESULTS");
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

	private void connectStream() {
		mStream.connect();
		Log.d(TAG, "engine->STATE_CONNECTED");
		mState = STATE_CONNECTED;
	}

	private void transmitAudioUpstream(byte[] chunk) {
		mStream.transmitData(chunk);
	}

	private void processDownstreamResponse(StreamResponse response) {
		boolean isGoodResponse = (response != null && response.getStatus() != Status.SUCCESS);
		if (isGoodResponse) {
			if (mRecognizeListener != null) {
				mRecognizeListener.onError(SPEECH_RECOGNITION_ERROR_RECOGNIZE);
			}
		}
		if (mRecognizeListener != null) {
			mRecognizeListener.onResult(new String(response.getResponse()));
		} else {
			Log.d(TAG, "recognize listener null");
		}
		mStream.disconnect();
		Log.d(TAG, "engine->STATE_IDLE");
		mState = STATE_IDLE;
	}

	private void closeUpstreamAndWaitResults() {
		Log.d(TAG, "engine->STATE_WAITING_RESULTS");
		mState = STATE_WAITING_RESULTS;
		mStream.getResponse();
	}

	private void abortSilently() {
		abort(SPEECH_RECOGNITION_ERROR_NONE);
	}

	private void abortWithError() {
		abort(SPEECH_RECOGNITION_ERROR_NETWORK);
	}

	private void abort(int errorCode) {
		if (errorCode != SPEECH_RECOGNITION_ERROR_NONE) {
			if (mRecognizeListener != null) {
				mRecognizeListener.onError(errorCode);
			}
		}
		mStream.reset(ONE_SHOT_SPEECH_API_URL, METHOD_POST);
		Log.d(TAG, "engine->IDLE");
		mState = STATE_IDLE;
	}

	private void shutAll() {
		if (mHandler != null) {
			mHandler.getLooper().quit();
		}
		Log.d(TAG, "engine shut down!");
	}

	private void stopAndShutAll() {
		// if there is response, onStreamResponse will be called, but no
		// response will result to listener
		mStream.getResponse();
		shutAll();
		mState = STATE_SHUTDOWN;
		Log.d(TAG, "engine->SHUTDOWN");
	}

	@Override
	public void onStreamError(int errorCode) {
		if (mState == STATE_SHUTDOWN) {
			Log.d(TAG, "engine is SHUTDOWN,ignore errorcode:" + errorCode);
		} else {
			mHandler.sendEmptyMessage(EVENT_CONNECTION_ERROR);
		}
	}

	@Override
	public void onStreamResponse(StreamResponse response) {
		if (mState == STATE_SHUTDOWN) {
			Log.d(TAG,
					"engine is SHUTDOWN,ignore response:" + response.toString());
		} else {
			Log.d(TAG, "response:" + response.toString());
			Message msg = new Message();
			msg.what = EVENT_RESULTS;
			msg.obj = response;
			mHandler.sendMessage(msg);
		}
	}

}
