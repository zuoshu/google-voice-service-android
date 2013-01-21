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