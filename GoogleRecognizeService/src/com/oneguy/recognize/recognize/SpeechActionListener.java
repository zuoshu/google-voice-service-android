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

import com.oneguy.recognize.recognize.AutoDetector.SpeechStatus;

public interface SpeechActionListener {
	public void onInit();

	public void onSilence();

	public void onStartSpeech(SpeechStatus status);

	public void onSpeech(SpeechStatus status);

	public void onWaitSpeechResult();

	public void onEnd();

}