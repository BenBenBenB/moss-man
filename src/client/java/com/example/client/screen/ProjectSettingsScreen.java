package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import org.lwjgl.glfw.GLFW;

public class ProjectSettingsScreen extends Screen {
	private final ProjectScreen parent;
	private ProjectData project;
	private List<String> statuses;
	private List<String> ticketTypes;
	private TextFieldWidget projectNameField;
	private TextFieldWidget addMemberField;
	private TextFieldWidget removeMemberField;
	private TextFieldWidget addStatusField;
	private TextFieldWidget removeStatusField;
	private TextFieldWidget addTypeField;
	private TextFieldWidget removeTypeField;

	public ProjectSettingsScreen(ProjectScreen parent) {
		super(Text.literal("Project Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return;
		}
		statuses = new ArrayList<>(project.getStatuses());
		ticketTypes = new ArrayList<>(project.getTicketTypes());
		int left = this.width / 2 - 140;
		int top = 92;
		projectNameField = new TextFieldWidget(textRenderer, left, top, 200, 16, Text.literal("Project Name"));
		projectNameField.setText(project.getName());
		styleField(projectNameField);
		addDrawableChild(projectNameField);

		addMemberField = new TextFieldWidget(textRenderer, left, top + 34, 120, 16, Text.literal("Add member"));
		removeMemberField = new TextFieldWidget(textRenderer, left + 124, top + 34, 120, 16, Text.literal("Remove member"));
		styleField(addMemberField);
		styleField(removeMemberField);
		addDrawableChild(addMemberField);
		addDrawableChild(removeMemberField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Add"), button -> sendAddMember())
				.position(left + 250, top + 34)
				.size(40, 16)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), button -> sendRemoveMember())
				.position(left + 294, top + 34)
				.size(60, 16)
				.build());

		addStatusField = new TextFieldWidget(textRenderer, left, top + 68, 120, 16, Text.literal("Add status"));
		removeStatusField = new TextFieldWidget(textRenderer, left + 124, top + 68, 120, 16, Text.literal("Remove status"));
		styleField(addStatusField);
		styleField(removeStatusField);
		addDrawableChild(addStatusField);
		addDrawableChild(removeStatusField);
		addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> addStatus())
				.position(left + 250, top + 68)
				.size(20, 16)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> removeStatus())
				.position(left + 274, top + 68)
				.size(20, 16)
				.build());

		addTypeField = new TextFieldWidget(textRenderer, left, top + 102, 120, 16, Text.literal("Add type"));
		removeTypeField = new TextFieldWidget(textRenderer, left + 124, top + 102, 120, 16, Text.literal("Remove type"));
		styleField(addTypeField);
		styleField(removeTypeField);
		addDrawableChild(addTypeField);
		addDrawableChild(removeTypeField);
		addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> addType())
				.position(left + 250, top + 102)
				.size(20, 16)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> removeType())
				.position(left + 274, top + 102)
				.size(20, 16)
				.build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveSettings())
				.position(left, top + 138)
				.size(60, 18)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> returnToParent())
				.position(left + 70, top + 138)
				.size(60, 18)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		int left = this.width / 2 - 140;
		int top = 92;
		if (project != null) {
			int y = 14;
			context.drawText(textRenderer, Text.literal("Owner: " + project.getOwnerName()), left, y, 0xFFE0E0E0, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Members: " + String.join(", ", project.getMembers().values())), left, y, 0xFFCCCCCC, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Statuses: " + String.join(", ", statuses)), left, y, 0xFFCCCCCC, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Types: " + String.join(", ", ticketTypes)), left, y, 0xFFCCCCCC, false);
		}
		context.drawText(textRenderer, Text.literal("Project Name"), left, top - 10, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Members"), left, top + 24, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Statuses"), left, top + 58, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Ticket Types"), left, top + 92, 0xFFE6E6E6, false);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0xC0101010);
	}

	private void sendAddMember() {
		String name = addMemberField.getText();
		if (name.isBlank()) {
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddProjectMemberPayload(parent.getProjectId(), name));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(parent.getProjectId()));
		addMemberField.setText("");
	}

	private void sendRemoveMember() {
		String name = removeMemberField.getText();
		if (name.isBlank()) {
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.RemoveProjectMemberPayload(parent.getProjectId(), name));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(parent.getProjectId()));
		removeMemberField.setText("");
	}

	private void addStatus() {
		String status = addStatusField.getText();
		if (!status.isBlank() && !statuses.contains(status)) {
			statuses.add(status);
		}
		addStatusField.setText("");
	}

	private void removeStatus() {
		String status = removeStatusField.getText();
		statuses.removeIf(value -> value.equalsIgnoreCase(status));
		removeStatusField.setText("");
	}

	private void addType() {
		String type = addTypeField.getText();
		if (!type.isBlank() && !ticketTypes.contains(type)) {
			ticketTypes.add(type);
		}
		addTypeField.setText("");
	}

	private void removeType() {
		String type = removeTypeField.getText();
		ticketTypes.removeIf(value -> value.equalsIgnoreCase(type));
		removeTypeField.setText("");
	}

	private void saveSettings() {
		if (project == null) {
			return;
		}
		project.setName(projectNameField.getText());
		project.setStatuses(statuses);
		project.setTicketTypes(ticketTypes);
		ClientPlayNetworking.send(new MossuraPayloads.UpdateProjectSettingsPayload(parent.getProjectId(), projectNameField.getText(), statuses, ticketTypes));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(parent.getProjectId()));
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
			saveSettings();
			return true;
		}
		if (handleFieldKeyPress(projectNameField, input)) {
			return true;
		}
		if (handleFieldKeyPress(addMemberField, input)) {
			return true;
		}
		if (handleFieldKeyPress(removeMemberField, input)) {
			return true;
		}
		if (handleFieldKeyPress(addStatusField, input)) {
			return true;
		}
		if (handleFieldKeyPress(removeStatusField, input)) {
			return true;
		}
		if (handleFieldKeyPress(addTypeField, input)) {
			return true;
		}
		if (handleFieldKeyPress(removeTypeField, input)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (handleFieldCharTyped(projectNameField, input)) {
			return true;
		}
		if (handleFieldCharTyped(addMemberField, input)) {
			return true;
		}
		if (handleFieldCharTyped(removeMemberField, input)) {
			return true;
		}
		if (handleFieldCharTyped(addStatusField, input)) {
			return true;
		}
		if (handleFieldCharTyped(removeStatusField, input)) {
			return true;
		}
		if (handleFieldCharTyped(addTypeField, input)) {
			return true;
		}
		if (handleFieldCharTyped(removeTypeField, input)) {
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
