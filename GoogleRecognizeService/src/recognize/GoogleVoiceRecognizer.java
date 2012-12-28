package recognize;

import record.AudioDataListener;
import record.RecorderImpl;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.SyncStateContract.Constants;
import android.util.Log;

import com.oneguy.recognize.BuildConfig;
import com.oneguy.recognize.R.menu;

import engine.AbstractEngine;
import engine.GoogleOneshotEngine;
import engine.GoogleStreamingEngine;
import engine.Engine;

public class GoogleVoiceRecognizer implements Runnable, AudioDataListener,
		Recognizer {
	private static final String TAG = "GoogleVoiceRecognizer";

	private Handler mHandler;
	private RecorderImpl mRecorder;
	private AbstractEngine mEngine;
	private int mState;
	private boolean selfThreadStarted;

	public static final int EVENT_START = 1;
	public static final int EVENT_STOP = 2;
	public static final int EVENT_SHUTDOWN = 3;
	public static final int EVENT_AUDIO_DATA = 4;

	private static final int STATE_IDLE = 1;
	private static final int STATE_RECOGNIZING = 2;
	private static final int WAIT_INTERNAL = 100;
	private static final int THREAD_START_TRY_TIME = 20;

	public GoogleVoiceRecognizer(Config config, AbstractEngine engine) {
		mRecorder = new RecorderImpl(config.sampleRate, config.nChannelConfig,
				config.audioConfig);
		mRecorder.setAudioDataListener(this);
		mEngine = engine;

		String mimeType = "";
		if (config.encodeType.equals(Config.ENCODE_WAV)) {
			mimeType = "audio/L16;rate=";
		} else {
			Log.e(TAG, "unsupport encode type:" + config.encodeType);
		}
		mimeType += config.sampleRate;
		mEngine.setMimeType(mimeType);
		selfThreadStarted = false;
	}

	public GoogleVoiceRecognizer() {
		this(Config.getDefault(), new GoogleStreamingEngine());
	}

	public GoogleVoiceRecognizer(Config config) {
		this(config, new GoogleStreamingEngine());
	}

	public GoogleVoiceRecognizer(AbstractEngine engine) {
		this(Config.getDefault(), engine);
	}

	@Override
	public void run() {
		Looper.prepare();
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				int event = msg.what;
				switch (mState) {
				case STATE_IDLE:
					switch (event) {
					case EVENT_START:
						startRecorderAndEngine();
						break;
					case EVENT_STOP:
						break;
					case EVENT_SHUTDOWN:
						shutdownAll();
						break;
					case EVENT_AUDIO_DATA:
						break;
					}
					break;
				case STATE_RECOGNIZING:
					switch (event) {
					case EVENT_START:
						break;
					case EVENT_STOP:
						stopRecorderAndEngine();
						break;
					case EVENT_SHUTDOWN:
						stopAndShutdown();
						break;
					case EVENT_AUDIO_DATA:
						transmitAudio((byte[]) (msg.obj));
						break;
					}
					break;
				}
			}
		};
		mState = STATE_IDLE;
		Looper.loop();
	}

	public void start() {
		// start self first
		if (!selfThreadStarted) {
			startSelf();
			selfThreadStarted = true;
		}
		if (mHandler == null) {
			Log.d(TAG, "recognizer not inited,can not start");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_START);
	}

	private void startSelf() {
		new Thread(this).start();
		int tryTime = 0;
		while (mHandler == null && tryTime < THREAD_START_TRY_TIME) {
			try {
				Log.d(TAG, "wait GoogleVoiceRecognizer start!");
				Thread.sleep(WAIT_INTERNAL);
				tryTime++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		if (mHandler == null) {
			Log.d(TAG, "recognizer not inited,can not stop");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_STOP);
	}

	public void shutdown() {
		if (mHandler == null) {
			Log.d(TAG, "recognizer not inited,can not shutdown");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_SHUTDOWN);
	}

	@Override
	public void onAudioData(byte[] data) {
		if (mHandler == null) {
			Log.d(TAG, "recognizer not inited,can not receive audio");
		}
		Message msg = new Message();
		msg.what = EVENT_AUDIO_DATA;
		msg.obj = data;
		mHandler.sendMessage(msg);
	}

	public void setRecognizeListener(RecognizeListener listener) {
		if (mEngine != null) {
			mEngine.setRecognizeListener(listener);
		}
	}

	private void startRecorderAndEngine() {
		new Thread(mRecorder).start();
		int tryTimes = 0;
		while (mRecorder.getHandler() == null
				&& tryTimes < THREAD_START_TRY_TIME) {
			try {
				Log.d(TAG, "wait recorder start!");
				Thread.sleep(WAIT_INTERNAL);
				tryTimes++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mRecorder.start();
		mEngine.start();
		Log.d(TAG, "recognizer->STATE_RECOGNIZING");
		mState = STATE_RECOGNIZING;
	}

	private void transmitAudio(byte[] data) {
		mEngine.takeAudioChunk(data);
	}

	private void stopRecorderAndEngine() {
		mRecorder.stop();
		mEngine.stop();
		Log.d(TAG, "recognizer->STATE_IDLE");
		mState = STATE_IDLE;
	}

	private void shutdownAll() {
		mRecorder.shutdown();
		mEngine.shutdown();
		mHandler.getLooper().quit();
		Log.d(TAG, "recognizer->shutdownAll");
	}

	private void stopAndShutdown() {
		stopRecorderAndEngine();
		shutdownAll();
	}

}
