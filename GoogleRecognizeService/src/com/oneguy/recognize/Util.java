package com.oneguy.recognize;

import java.nio.ByteBuffer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

public class Util {
	private static final String TAG = Util.class.toString();

	public static ByteBuffer doubleSize(ByteBuffer buffer) {
		if (buffer == null) {
			return null;
		}
		byte[] content = new byte[buffer.position()];
		buffer.flip();
		buffer.get(content);
		ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
		newBuffer.put(content);
		return newBuffer;
	}

	public static ByteBuffer putData(ByteBuffer buffer, int data) {
		if (buffer == null) {
			return buffer;
		}
		// sizeof(int) == 4
		while (buffer.capacity() < buffer.position() + 4 - 1) {
			buffer = doubleSize(buffer);
		}
		buffer.putInt(data);
		return buffer;
	}

	public static ByteBuffer putData(ByteBuffer buffer, String data) {
		return putData(buffer, data.getBytes());
	}

	public static ByteBuffer putData(ByteBuffer buffer, short data) {
		if (buffer == null) {
			return buffer;
		}
		// sizeof(int) == 2
		while (buffer.capacity() < buffer.position() + 2 - 1) {
			buffer = doubleSize(buffer);
		}
		buffer.putShort(data);
		return buffer;
	}

	public static ByteBuffer putData(ByteBuffer buffer, byte[] data) {
		if (buffer == null || data == null || data.length == 0) {
			return buffer;
		}
		while (buffer.capacity() < buffer.position() + data.length - 1) {
			buffer = doubleSize(buffer);
		}
		buffer.put(data);
		return buffer;
	}

	public static String getUtterance(String jsonData) {
		String TAG = "Util.getUtterance";
		String result = "";
		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(jsonData);
			JSONArray hypotheses = (JSONArray) jsonObject.get("hypotheses");
			if (hypotheses != null && hypotheses.length() > 0) {
				JSONObject jsObject = (JSONObject) hypotheses.get(0);
				result = (String) jsObject.get("utterance");
			}
		} catch (JSONException e) {
			Log.d(TAG, "can not read utterance from:" + jsonData);
			e.printStackTrace();
		}
		result = result.replace("'", "");
		result = replaceInnerBlankInDigits(result);
		return result;
	}

	public static String replaceInnerBlankInDigits(String data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length(); i++) {
			if (i == 0 && data.charAt(i) == ' ') {
				continue;
			}

			sb.append(data.charAt(i));
			if (Character.isDigit(data.charAt(i)) && i < data.length() - 1
					&& data.charAt(i + 1) == ' ') {
				i++;
			}
		}
		return sb.toString();
	}

	public static boolean isNetworkEnable(Context context) {
		ConnectivityManager con = (ConnectivityManager) context
				.getSystemService(Activity.CONNECTIVITY_SERVICE);
		boolean wifi = con.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnectedOrConnecting();
		boolean internet = con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.isConnectedOrConnecting();
		return wifi | internet;
	}

	public static void callJsFunction(WebView webview, String function) {
		if (webview == null) {
			Log.w(TAG, "can not call js on null webview");
			return;
		}
		webview.loadUrl("javascript:" + function);
	}

	public static void d(String tag, String content) {
		System.out.println(tag + ":" + content);
	}

	static long time;
	final static String TIME_TAG = "timer";

	public static void timerInit() {
		Log.d(TIME_TAG, "timer init");
		time = System.currentTimeMillis();
	}

	public static void logTime(String content) {
		Log.d(TIME_TAG, content + ":" + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
	}
}
