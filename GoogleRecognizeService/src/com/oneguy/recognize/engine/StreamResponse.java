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

public class StreamResponse {
	public enum Status {
		SUCCESS, ERROR
	}

	private Stream source;
	private int responseCode;
	private byte[] response;
	private Status status;

	public StreamResponse(Stream source, Status status) {
		this.source = source;
		this.status = status;
	}

	public Stream getSource() {
		return source;
	}

	public void setSource(Stream source) {
		this.source = source;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public byte[] getResponse() {
		return response;
	}

	public void setResponse(byte[] response) {
		this.response = response;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("source:");
		sb.append(source == null ? "null" : source.toString());
		sb.append(" responseCode:" + responseCode);
		sb.append(" Status:" + status);
		sb.append(" content:");
		if (response == null) {
			sb.append("null");
		} else {
			sb.append(new String(response));
		}
		return sb.toString();
	}

}