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

public class SubtaskCreateScreen extends Screen {
	private final Screen parent;
	private final UUID projectId;
	private final UUID ticketId;
	private TextFieldWidget nameField;
	private TextFieldWidget descriptionField;

	public SubtaskCreateScreen(Screen parent, UUID projectId, UUID ticketId) {
		super(Text.literal("Add Subtask"));
		this.parent = parent;
		this.projectId = projectId;
		this.ticketId = ticketId;
	}

	@Override
	protected void init() {
		super.init();
		int left = this.width / 2 - 140;
		int top = this.height / 2 - 30;
		nameField = new TextFieldWidget(textRenderer, left, top + 14, 280, 16, Text.literal("Subtask name"));
		descriptionField = new TextFieldWidget(textRenderer, left, top + 42, 280, 16, Text.literal("Subtask description"));
		styleField(nameField);
		styleField(descriptionField);
		addDrawableChild(nameField);
		addDrawableChild(descriptionField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
				.position(left, top + 70)
				.size(60, 18)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> returnToParent())
				.position(left + 70, top + 70)
				.size(60, 18)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		int left = this.width / 2 - 140;
		int top = this.height / 2 - 30;
		context.drawText(textRenderer, Text.literal("Name"), left, top, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Description"), left, top + 28, 0xFFE6E6E6, false);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0xC0101010);
	}

	private void save() {
		String name = nameField.getText();
		if (name.isBlank()) {
			returnToParent();
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddSubtaskPayload(projectId, ticketId, name, descriptionField.getText()));
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
		if (handleFieldKeyPress(nameField, input)) {
			return true;
		}
		if (handleFieldKeyPress(descriptionField, input)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (handleFieldCharTyped(nameField, input)) {
			return true;
		}
		if (handleFieldCharTyped(descriptionField, input)) {
			return true;
		}
		return super.charTyped(input);
	}

	private boolean handleFieldKeyPress(TextFieldWidget field, KeyInput input) {
		if (field == null || !field.isFocused()) {
			return false;
		}
		if (field.keyPressed(input)) {
			return true;
		}
		if (client != null && client.options.inventoryKey.matchesKey(input)) {
			return true;
		}
		return false;
	}

	private boolean handleFieldCharTyped(TextFieldWidget field, CharInput input) {
		return field != null && field.isFocused() && field.charTyped(input);
	}
}
