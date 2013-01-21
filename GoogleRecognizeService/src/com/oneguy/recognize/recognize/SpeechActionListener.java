package com.oneguy.recognize.recognize;

import com.oneguy.recognize.recognize.AutoDetector.SpeechStatus;

public interface SpeechActionListener {
	public void onInit();

	public void onSilence();

	public void onStartSpeech(SpeechStatus status);

	public void onSpeech(SpeechStatus status);

	public void onWaitSpeechResult();

	public void onEnd();

}
