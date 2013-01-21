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

public class Chunk {

	private byte[] data;
	private int start;
	private int end;
	private String content;
	private static final int CONTENT_LEN_BYTE_OFFSET = 9;

	public Chunk(byte[] data, int start, int end) {
		this.data = data;
		this.start = start;
		this.end = end;
		content = extractString();
	}

	private String extractString() {
		String result = "";
		int contentStart = start + CONTENT_LEN_BYTE_OFFSET + 1;
		if (contentStart > end) {
			return result;
		}

		int strLength = data[contentStart - 1] & 0xff;
		int contentEnd = contentStart + strLength;
		if (contentEnd > end) {
			return result;
		}
		byte[] resultByte = new byte[strLength];
		System.arraycopy(data, contentStart, resultByte, 0, strLength);
		return new String(resultByte);
	}

	public String getContent() {
		return content;
	}
}