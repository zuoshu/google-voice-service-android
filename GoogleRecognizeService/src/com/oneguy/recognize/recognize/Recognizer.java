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