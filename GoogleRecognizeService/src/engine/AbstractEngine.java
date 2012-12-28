package engine;

import recognize.RecognizeListener;

public abstract class AbstractEngine implements Engine {
	protected RecognizeListener mListener;
	protected String mimeType;

	public void setRecognizeListener(RecognizeListener l) {
		this.mListener = l;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
}
