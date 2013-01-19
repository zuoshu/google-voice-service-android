package com.oneguy.recognize.engine;



public interface Engine {
	public static final int RECOGNIZER_STREAM = 1;
	public static final int RECOGNIZER_ONESHOT = 2;
	
	public static final int SPEECH_RECOGNITION_ERROR_NONE = 1002;
	public static final int SPEECH_RECOGNITION_ERROR_NETWORK = 1003;
	public static final int SPEECH_RECOGNITION_ERROR_RECOGNIZE = 1004;

	public void start();

	public void takeAudioChunk(byte[] chunk);

	public void stop();

	public void shutdown();

}
