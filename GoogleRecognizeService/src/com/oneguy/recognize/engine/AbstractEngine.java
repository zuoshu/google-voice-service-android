/*******************************************************************************
 * Copyright (c) 2012 Zuoshu (zuoshu.wuhan@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * Contributors:
 *     Zuoshu - initial API and implementation
 ******************************************************************************/

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