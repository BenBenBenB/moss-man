package com.example.client.ui;

import com.google.gson.JsonObject;

public interface MossuraUiActionHandler {
	boolean handleUiAction(String action, JsonObject payload);
}
