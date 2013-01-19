package com.oneguy.recognize.recognize;

public interface EngineResultListener {
	/**
	 * 
	 * @param errorCode
	 *            Typically 2 types of error will
	 *            return,SPEECH_RECOGNITION_ERROR_NETWORK and
	 *            SPEECH_RECOGNITION_ERROR_RECOGNIZE. see VoiceRecognitionEngine
	 */
	public void onError(int errorCode);

	/**
	 * 
	 * @param result
	 *            The result will return in String
	 */
	public void onResult(String result);
}
