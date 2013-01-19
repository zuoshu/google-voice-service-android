package com.oneguy.recognize.recognize;

import android.media.AudioFormat;

public class Config {
	/** values for sampleRate */
	public static final int SAMPLE_RATE_8000 = 8000;
	public static final int SAMPLE_RATE_16000 = 16000;

	public int sampleRate;

	/**
	 * AudioFormat.CHANNEL_IN_MONO(default)
	 * AudioFormat.CHANNEL_IN_STEREO
	 */
	public int nChannelConfig;

	/** AudioFormat.ENCODING_PCM_16BIT(default), AudioFormat.ENCODING_PCM_8BIT */
	public int audioConfig;

	// TODO add other encode type
	public static final String ENCODE_WAV = "wav";
	public String encodeType;

	private static Config mDefaultConfig;

	public synchronized static Config getDefault() {
		if (mDefaultConfig == null) {
			mDefaultConfig = new Config();
			mDefaultConfig.sampleRate = SAMPLE_RATE_8000;
			mDefaultConfig.nChannelConfig = AudioFormat.CHANNEL_IN_MONO;
			mDefaultConfig.audioConfig = AudioFormat.ENCODING_PCM_16BIT;
			mDefaultConfig.encodeType = ENCODE_WAV;
		}
		return mDefaultConfig;
	}
}
