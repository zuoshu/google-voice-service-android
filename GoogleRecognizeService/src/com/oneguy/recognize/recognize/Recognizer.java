package com.oneguy.recognize.recognize;

public interface Recognizer {
	/**
	 * Start a recognizer task
	 */
	public void start();

	/**
	 * Stop a recognizer task, result will return to listener if listener not
	 * null. Call start() again to do another recognize
	 */
	public void stop();

	/**
	 * Stop a recognizer and shutdown ,result will not return ,recognition
	 * thread will exit, the recognizer can not start() again
	 */
	public void shutdown();

	public void setResultListener(EngineResultListener listener);
}
