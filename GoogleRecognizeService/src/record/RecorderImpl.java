package record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class RecorderImpl implements Runnable, Recorder {
	private static final String TAG = "Recorder";
	private static final int PACKAGE_SIZE = 320;

	private volatile boolean isRecording;
	private Handler mHandler;
	private AudioDataListener mAudioListener;
	private AudioRecord recordInstance;
	private byte[] dataBuffer;
	private OnRecordPositionUpdateListener updateListener;
	/** 8000 ,16000 */
	private int sampleRate;
	/**
	 * AudioFormat.CHANNEL_IN_MONO(default),
	 * AudioFormat.CHANNEL_CONFIGURATION_STEREO
	 */
	private int nChannelConfig;
	/** AudioFormat.ENCODING_PCM_16BIT(default), AudioFormat.ENCODING_PCM_8BIT */
	private int audioConfig;
	private int framePeriod;
	private static final int TIMER_INTERVAL = 20;

	public static final int EVENT_START = 1;
	public static final int EVENT_STOP = 2;
	public static final int EVENT_SHUTDOWN = 3;

	public RecorderImpl(int sampleRate, int nChannelConfig, int audioConfig) {
		this.sampleRate = sampleRate;
		this.framePeriod = sampleRate * TIMER_INTERVAL / 1000;
		this.nChannelConfig = nChannelConfig;
		this.audioConfig = audioConfig;
	}

	public void setAudioDataListener(AudioDataListener dataListener) {
		mAudioListener = dataListener;
	}

	public Handler getHandler() {
		return mHandler;
	}

	@Override
	public void run() {
		Looper.prepare();
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case EVENT_START:
					doStart();
					break;
				case EVENT_STOP:
					doStop();
					break;
				case EVENT_SHUTDOWN:
					doShutdown();
					break;
				default:
					Log.e(TAG, "unknown what:" + msg.what);
					break;
				}
			}
		};
		Looper.loop();
	}

	private void doStart() {
		int bSamples;
		int nChannels;
		if (audioConfig == AudioFormat.ENCODING_PCM_16BIT) {
			bSamples = 16;
		} else {
			bSamples = 8;
		}
		if (nChannelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO) {
			nChannels = 1;
		} else {
			nChannels = 2;
		}
		int bufferSize1 = AudioRecord.getMinBufferSize(sampleRate,
				nChannelConfig, audioConfig) * 10;
		int bufferSize2 = framePeriod * 2 * bSamples * nChannels / 8;
		int bufferSize = Math.max(bufferSize1, bufferSize2);
		Log.d(TAG, "bufferSize1:" + bufferSize1 + " bufferSize2:" + bufferSize2
				+ " sampleRate:" + sampleRate + " nChannelConfig:"
				+ nChannelConfig + " audioConfig:" + audioConfig);
		dataBuffer = new byte[PACKAGE_SIZE];
		recordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC,
				sampleRate, nChannelConfig, audioConfig, bufferSize);
		// TODO may get null record instance
		updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
			public void onPeriodicNotification(AudioRecord recorder) {
				if (!isRecording) {
					Log.d(TAG, "not recording,discard data!");
					return;
				}
				int readsize = recordInstance.read(dataBuffer, 0,
						dataBuffer.length);
				sendDataMessage(dataBuffer, readsize);
			}

			public void onMarkerReached(AudioRecord recorder) {
				// NOT USED
			}
		};
		recordInstance.setRecordPositionUpdateListener(updateListener);
		recordInstance.setPositionNotificationPeriod(framePeriod);
		recordInstance.startRecording();
		recordInstance.read(dataBuffer, 0, dataBuffer.length);
		isRecording = true;
		Log.d(TAG, "recorder->start");
	}

	private void doStop() {
		isRecording = false;
		recordInstance.stop();
		recordInstance.release();
		recordInstance = null;
		Log.d(TAG, "recorder->stop");
	}

	private void doShutdown() {
		mHandler.getLooper().quit();
		Log.d(TAG, "recorder->shutdown");
	}

	private boolean sendDataMessage(byte[] tempBuffer, int bufferRead) {
		if (tempBuffer == null || mAudioListener == null) {
			return false;
		}
		byte[] buffer = new byte[tempBuffer.length];
		System.arraycopy(tempBuffer, 0, buffer, 0, bufferRead);
		mAudioListener.onAudioData(buffer);
		return true;
	}

	public void start() {
		if (mHandler == null) {
			Log.d(TAG, "can not inited,can not start");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_START);
	}

	public void stop() {
		if (mHandler == null) {
			Log.d(TAG, "can not inited,can not stop");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_STOP);
	}

	public void shutdown() {
		if (mHandler == null) {
			Log.d(TAG, "can not inited,can not shutdown");
			return;
		}
		mHandler.sendEmptyMessage(EVENT_SHUTDOWN);
	}
}
