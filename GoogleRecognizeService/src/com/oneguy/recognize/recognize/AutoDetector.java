package com.oneguy.recognize.recognize;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.LinkedList;

import android.util.Log;

public class AutoDetector {
	private static final String TAG = "AutoDetector";
	private final double SILENCE_FACTOR = 0.50;
	private final double NOISE_FACTOR = 5;
	private double maxRMS = 0.0001;
	private double staticAverage = 0.0001;
	LinkedList<Double> rmsDeque = new LinkedList<Double>();
	double total = 0;
	int sampleCount = 0;
	long startTime = System.currentTimeMillis();
	private int mState = SILENT;
	/* current state */
	public static final int SILENT = 1;
	public static final int START_SPEECH = 2;
	public static final int SPEAKING = 3;
	public static final int END_SPEECH = 4;
	/* event */
	private static final int VOL_LOUD = 1;
	private static final int VOL_MEDIUM = 2;
	private static final int VOL_LOW = 3;
	private static final int TIMEOUT = 4;

	private static final long LONG_SPEECH_TIMEOUT = 10 * 1000;
	private static final long NO_SPEECH_TIMEOUT = 10 * 1000;

	public SpeechStatus determineSpeechState(byte[] buffer) {
		long time = System.currentTimeMillis() - startTime;
		short[] fArray = getShortArray(buffer);
		Double rms = getRMS(fArray);
		rmsDeque.addFirst(rms);
		if (rmsDeque.size() > 5) {
			maxRMS = getMaxRMS(rmsDeque);
			maxRMS = Math.max(maxRMS, rms);
			staticAverage = total / sampleCount;
			rmsDeque.removeLast();
		}
		total += rms;
		sampleCount++;
		int event;
		if (time > NO_SPEECH_TIMEOUT && mState == SILENT) {
			event = TIMEOUT;
		} else if (time > LONG_SPEECH_TIMEOUT && mState == SPEAKING) {
			event = TIMEOUT;
		} else if (maxRMS > staticAverage * NOISE_FACTOR) {
			event = VOL_LOUD;
		} else if (maxRMS < staticAverage * SILENCE_FACTOR) {
			event = VOL_LOW;
		} else {
			event = VOL_MEDIUM;
		}
		switch (mState) {
		case SILENT:
			switch (event) {
			case VOL_LOUD:
				mState = START_SPEECH;
				break;
			case VOL_MEDIUM:
			case VOL_LOW:
				mState = SILENT;
				break;
			case TIMEOUT:
				mState = END_SPEECH;
				break;
			}
			break;
		case SPEAKING:
			switch (event) {
			case VOL_LOUD:
			case VOL_MEDIUM:
				mState = SPEAKING;
				break;
			case VOL_LOW:
				mState = END_SPEECH;
				break;
			case TIMEOUT:
				mState = END_SPEECH;
				break;
			}
			break;
		case START_SPEECH:
			switch (event) {
			case VOL_LOUD:
			case VOL_MEDIUM:
			case VOL_LOW:
				mState = SPEAKING;
				break;
			case TIMEOUT:
				mState = END_SPEECH;
				break;
			}
			break;
		case END_SPEECH:
			switch (event) {
			case VOL_LOUD:
			case VOL_MEDIUM:
			case VOL_LOW:
				mState = SILENT;
				break;
			case TIMEOUT:
				mState = END_SPEECH;
				break;
			}
			break;
		}
		Log.d(TAG, "state " + mState + " vol:" + event + " rms:" + (maxRMS));
		return new SpeechStatus(mState, maxRMS, maxRMS / staticAverage);
	}

	private short[] getShortArray(byte[] audioDataByteArray) {
		ShortBuffer sBuffer = ByteBuffer.wrap(audioDataByteArray)
				.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		short[] sArray = new short[sBuffer.capacity()];
		sBuffer.get(sArray);
		return sArray;
	}

	/** Calculate the Root Mean Square and return it as double */
	private static double getRMS(short[] fArray) {

		double total = 0.0;

		/* Iterate through the array and square and sum all the terms */
		for (short sh : fArray) {
			float sh2 = (((float) sh) / 0x8000);
			total += (sh2 * sh2);
		}

		/* Divide by the length of array and square root and return RMS */
		double rms = Math.pow(total / (fArray.length), 0.5d);
		return rms;
	}

	/** Iterator through the linkedlist and determine the maximum */
	private double getMaxRMS(LinkedList<Double> rmsDeque) {

		/*
		 * Assume first is max, iterator through and replace max with maximum
		 * RMS value of the last five
		 */
		double max = rmsDeque.getFirst();
		for (double dd : rmsDeque) {
			if (dd > max) {
				max = dd;
			}
		}
		return max;
	}

	public class SpeechStatus {
		public SpeechStatus(int state, double maxRms, double factor) {
			this.state = state;
			this.maxRms = maxRms;
			this.factor = factor;
		}

		public int state;
		public double maxRms;
		public double factor;
	}
}
