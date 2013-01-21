package com.oneguy.recognize.recognize;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.oneguy.recognize.engine.AbstractEngine;
import com.oneguy.recognize.engine.GoogleOneshotEngine;
import com.oneguy.recognize.engine.GoogleStreamingEngine;
import com.oneguy.recognize.recognize.AutoDetector.SpeechStatus;
import com.oneguy.recognize.record.AudioDataListener;
import com.oneguy.recognize.record.RecorderImpl;

public class GoogleVoiceRecognizer implements Runnable, AudioDataListener,
		Recognizer, EngineResultListener {
	private static final String TAG = "GoogleVoiceRecognizer";
	public static final int EVENT_START = 1;
	public static final int EVENT_STOP = 2;
	public static final int EVENT_SHUTDOWN = 3;
	public static final int EVENT_AUDIO_DATA = 4;
	private static final int STATE_IDLE = 1;
	private static final int STATE_RECOGNIZING = 2;
	private static final int WAIT_INTERNAL = 100;
	private static final int THREAD_START_TRY_TIME = 20;

	private Handler mHandler;
	private RecorderImpl mRecorder;
	private AbstractEngine mEngine;
	private int mState;
	private boolean selfThreadStarted;
	private AutoDetector detector;
	private SpeechActionListener mSpeechActionListener;
	private boolean mShowSpeechActionPrompt;

	private EngineResultListener mResultListener;
	private Activity mActivity;

	public GoogleVoiceRecognizer(Config config, AbstractEngine engine) {
		mRecorder = new RecorderImpl(config.sampleRate, config.nChannelConfig,
				config.audioConfig);
		mRecorder.setAudioDataListener(this);
		mEngine = engine;
		mEngine.setRecognizeListener(this);
		mShowSpeechActionPrompt = true;

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
		this(Config.getDefault(), new GoogleOneshotEngine());
	}

	public GoogleVoiceRecognizer(Config config) {
		this(config, new GoogleStreamingEngine());
	}

	public GoogleVoiceRecognizer(AbstractEngine engine) {
		this(Config.getDefault(), engine);
	}

	public void enableSpeechActionPrompt(Activity act) {
		mActivity = act;
		mShowSpeechActionPrompt = true;
		mSpeechActionListener = new SpeechActionPrompt(mActivity);
	}

	public void disableSpeechActionPrompt() {
		mShowSpeechActionPrompt = false;
		mSpeechActionListener = null;
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
		if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
			mSpeechActionListener.onInit();
		}
		// start self first
		if (!selfThreadStarted) {
			startSelf();
			selfThreadStarted = true;
		}
		if (mHandler == null) {
			Log.d(TAG, "recognizer not inited,can not start");
			if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
				mSpeechActionListener.onEnd();
			}
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
		SpeechStatus status = detector.determineSpeechState(data);
		switch (status.state) {
		case AutoDetector.SILENT:
			if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
				mSpeechActionListener.onSilence();
			}
			break;
		case AutoDetector.SPEAKING:
		case AutoDetector.START_SPEECH:
			if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
				mSpeechActionListener.onSpeech(status);
			}
			Message msg = new Message();
			msg.what = EVENT_AUDIO_DATA;
			msg.obj = data;
			mHandler.sendMessage(msg);
			break;
		case AutoDetector.END_SPEECH:
			if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
				mSpeechActionListener.onWaitSpeechResult();
			}
			stop();
			break;
		}
	}

	public void setResultListener(EngineResultListener listener) {
		mResultListener = listener;
	}

	private void startRecorderAndEngine() {
		new Thread(mRecorder).start();
		int tryTimes = 0;
		detector = new AutoDetector();
		while ((mRecorder.getHandler() == null || detector == null)
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
		// if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
		// mSpeechActionListener.onEnd();
		// }
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

	@Override
	public void onError(int errorCode) {
		if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
			mSpeechActionListener.onEnd();
		}
		if (mResultListener != null) {
			mResultListener.onError(errorCode);
		}
	}

	@Override
	public void onResult(String result) {
		if (mResultListener != null) {
			mResultListener.onResult(result);
		}
		if (mSpeechActionListener != null && mShowSpeechActionPrompt) {
			mSpeechActionListener.onEnd();
		}
	}

}
