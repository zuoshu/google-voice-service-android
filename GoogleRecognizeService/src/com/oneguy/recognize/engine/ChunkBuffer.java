package com.oneguy.recognize.engine;

import java.util.ArrayList;

public class ChunkBuffer {

	private ArrayList<Chunk> chunkList = null;

	// 0 0 0 0 at the very front
	private static final int HEADER_LENGTH = 4;

	public ChunkBuffer() {
		chunkList = new ArrayList<Chunk>();
	}

	public void transform(byte[] data) {
		int current = 0;
		int chunkLength = 0;
		if (data == null || data.length < 4) {
			return;
		}
		// read 0 0 0 0
		current += HEADER_LENGTH;
		while (current + HEADER_LENGTH < data.length) {
			chunkLength = ((data[current] & 0xff) << 24)
					| ((data[current + 1] & 0xff) << 16)
					| ((data[current + 2] & 0xff) << 8)
					| (data[current + 3] & 0xff);
			// read 4byte length
			current += HEADER_LENGTH;
			if (current + chunkLength > data.length) {
				return;
			}
			// read chunk content
			Chunk c = new Chunk(data, current - HEADER_LENGTH, current
					+ chunkLength - 1);
			chunkList.add(c);
			current += chunkLength;
		}
	}

	public Chunk getLast() {
		return chunkList.get(chunkList.size() - 1);
	}

	public Chunk get(int i) {
		return chunkList.get(i);
	}

	public int size() {
		return chunkList.size();
	}

	public String getBestResult() {
		if (hasChunk()) {
			return chunkList.get(chunkList.size() - 1).getContent();
		} else {
			return "";
		}
	}

	public boolean hasChunk() {
		return chunkList.size() > 0;
	}
}
