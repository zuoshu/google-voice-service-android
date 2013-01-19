package com.oneguy.recognize.engine;

import com.oneguy.recognize.recognize.EngineResultListener;

public abstract class AbstractEngine implements Engine {
	protected EngineResultListener mListener;
	protected String mimeType;

	public void setRecognizeListener(EngineResultListener l) {
		this.mListener = l;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
}
