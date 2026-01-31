package com.example.client.ui;

import com.cinemamod.mcef.MIMEUtil;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.slf4j.Logger;

public class MossuraScheme implements CefResourceHandler {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final String url;
	private String contentType;
	private InputStream input;

	public MossuraScheme(String url) {
		this.url = url;
	}

	@Override
	public boolean processRequest(CefRequest cefRequest, CefCallback cefCallback) {
		String loc = url.substring("mossura://".length());
		loc = removeSlashes(loc);
		if (loc.isEmpty() || loc.charAt(0) == '.') {
			LOGGER.warn("Invalid URL {}", url);
			cefCallback.cancel();
			return false;
		}

		String resourcePath = "assets/mossura/html/" + loc.toLowerCase(Locale.US);
		input = MossuraScheme.class.getClassLoader().getResourceAsStream(resourcePath);
		if (input == null) {
			LOGGER.warn("Resource {} NOT found!", url);
			cefCallback.cancel();
			return false;
		}

		contentType = null;
		int pos = loc.lastIndexOf('.');
		if (pos >= 0 && pos < loc.length() - 1) {
			contentType = MIMEUtil.mimeFromExtension(loc.substring(pos + 1));
		}

		cefCallback.Continue();
		return true;
	}

	private String removeSlashes(String loc) {
		int i = 0;
		while (i < loc.length() && loc.charAt(i) == '/') {
			i++;
		}
		return loc.substring(i);
	}

	@Override
	public void getResponseHeaders(CefResponse cefResponse, IntRef contentLength, StringRef redir) {
		if (contentType != null) {
			cefResponse.setMimeType(contentType);
		}
		cefResponse.setStatus(200);
		cefResponse.setStatusText("OK");
		contentLength.set(0);
	}

	@Override
	public boolean readResponse(byte[] output, int bytesToRead, IntRef bytesRead, CefCallback cefCallback) {
		try {
			int read = input.read(output, 0, bytesToRead);
			if (read <= 0) {
				input.close();
				bytesRead.set(0);
				return false;
			}
			bytesRead.set(read);
			return true;
		} catch (IOException e) {
			LOGGER.error("Failed reading resource {}", url, e);
			return false;
		}
	}

	@Override
	public void cancel() {
		if (input == null) {
			return;
		}
		try {
			input.close();
		} catch (IOException ignored) {
			// Ignore close errors.
		}
	}
}
