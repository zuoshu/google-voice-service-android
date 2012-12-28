package engine;

public interface StreamListener {

	public void onStreamError(int errorCode);

	public void onStreamResponse(StreamResponse response);

}
