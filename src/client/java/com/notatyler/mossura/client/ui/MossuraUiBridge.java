package com.notatyler.mossura.client.ui;

import com.cinemamod.mcef.MCEF;
import com.notatyler.mossura.project.MossuraUiState;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicReference;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public final class MossuraUiBridge {
	private static final Gson GSON = new Gson();
	private static final AtomicReference<MossuraUiActionHandler> ACTIVE = new AtomicReference<>();
	private static boolean registered;

	private MossuraUiBridge() {
	}

	public static void setActiveHandler(MossuraUiActionHandler handler) {
		ACTIVE.set(handler);
	}

	public static void clearActiveHandler(MossuraUiActionHandler handler) {
		ACTIVE.compareAndSet(handler, null);
	}

	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		Runnable register = () -> {
			CefMessageRouter router = CefMessageRouter.create();
			router.addHandler(new CefMessageRouterHandlerAdapter() {
				@Override
				public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
					MossuraUiActionHandler handler = ACTIVE.get();
					if (handler == null) {
						callback.failure(404, "No active UI handler");
						return true;
					}
					JsonObject root = null;
					try {
						root = GSON.fromJson(request, JsonObject.class);
					} catch (Exception ignored) {
						// Ignore parse failures; treat request as action string.
					}
					String action = request;
					JsonObject payload = new JsonObject();
					if (root != null) {
						JsonElement actionElement = root.get("action");
						if (actionElement != null && actionElement.isJsonPrimitive()) {
							action = actionElement.getAsString();
						}
						JsonElement payloadElement = root.get("payload");
						if (payloadElement != null && payloadElement.isJsonObject()) {
							payload = payloadElement.getAsJsonObject();
						}
					}
					boolean handled = handler.handleUiAction(action, payload);
					if (handled) {
						callback.success("ok");
					} else {
						callback.failure(400, "Unhandled action");
					}
					return true;
				}
			}, true);
			MCEF.getClient().getHandle().addMessageRouter(router);
		};

		if (MCEF.isInitialized()) {
			register.run();
		} else {
			MCEF.scheduleForInit(success -> {
				if (success) {
					register.run();
				} else {
					MCEF.getLogger().warn("MCEF did not initialize; UI bridge not registered.");
				}
			});
		}
	}
}
