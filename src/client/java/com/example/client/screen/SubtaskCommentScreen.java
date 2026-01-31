package com.example.client.screen;

import com.example.network.MossuraPayloads;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SubtaskCommentScreen extends Screen {
	private final Screen parent;
	private final UUID projectId;
	private final UUID ticketId;
	private final UUID subtaskId;
	private TextFieldWidget commentField;

	public SubtaskCommentScreen(Screen parent, UUID projectId, UUID ticketId, UUID subtaskId) {
		super(Text.literal("Add Subtask Comment"));
		this.parent = parent;
		this.projectId = projectId;
		this.ticketId = ticketId;
		this.subtaskId = subtaskId;
	}

	@Override
	protected void init() {
		super.init();
		int left = this.width / 2 - 140;
		int top = this.height / 2 - 20;
		commentField = new TextFieldWidget(textRenderer, left, top + 14, 280, 16, Text.literal("Subtask comment"));
		styleField(commentField);
		addDrawableChild(commentField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
				.position(left, top + 40)
				.size(60, 18)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> returnToParent())
				.position(left + 70, top + 40)
				.size(60, 18)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		int left = this.width / 2 - 140;
		int top = this.height / 2 - 20;
		context.drawText(textRenderer, Text.literal("Comment"), left, top, 0xFFE6E6E6, false);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0xC0101010);
	}

	private void save() {
		String message = commentField.getText();
		if (message.isBlank()) {
			returnToParent();
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddSubtaskCommentPayload(projectId, ticketId, subtaskId, message));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(projectId));
		returnToParent();
	}

	private void returnToParent() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(parent);
		}
	}

	private static void styleField(TextFieldWidget field) {
		field.setEditableColor(0xFFFFFFFF);
		field.setUneditableColor(0xFFB0B0B0);
		field.setDrawsBackground(true);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
			save();
			return true;
		}
		if (commentField != null && commentField.isFocused()) {
			if (commentField.keyPressed(input)) {
				return true;
			}
			if (client != null && client.options.inventoryKey.matchesKey(input)) {
				return true;
			}
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (commentField != null && commentField.isFocused() && commentField.charTyped(input)) {
			return true;
		}
		return super.charTyped(input);
	}
}
