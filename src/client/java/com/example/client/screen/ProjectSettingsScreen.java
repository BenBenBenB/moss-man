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
		int top = 28;
		projectNameField = new TextFieldWidget(textRenderer, left, top, 200, 16, Text.literal("Project Name"));
		projectNameField.setText(project.getName());
		addDrawableChild(projectNameField);

		addMemberField = new TextFieldWidget(textRenderer, left, top + 28, 120, 16, Text.literal("Add member"));
		removeMemberField = new TextFieldWidget(textRenderer, left + 124, top + 28, 120, 16, Text.literal("Remove member"));
		addDrawableChild(addMemberField);
		addDrawableChild(removeMemberField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Add"), button -> sendAddMember())
				.position(left + 250, top + 28)
				.size(40, 16)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), button -> sendRemoveMember())
				.position(left + 294, top + 28)
				.size(60, 16)
				.build());

		addStatusField = new TextFieldWidget(textRenderer, left, top + 60, 120, 16, Text.literal("Add status"));
		removeStatusField = new TextFieldWidget(textRenderer, left + 124, top + 60, 120, 16, Text.literal("Remove status"));
		addDrawableChild(addStatusField);
		addDrawableChild(removeStatusField);
		addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> addStatus())
				.position(left + 250, top + 60)
				.size(20, 16)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> removeStatus())
				.position(left + 274, top + 60)
				.size(20, 16)
				.build());

		addTypeField = new TextFieldWidget(textRenderer, left, top + 88, 120, 16, Text.literal("Add type"));
		removeTypeField = new TextFieldWidget(textRenderer, left + 124, top + 88, 120, 16, Text.literal("Remove type"));
		addDrawableChild(addTypeField);
		addDrawableChild(removeTypeField);
		addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> addType())
				.position(left + 250, top + 88)
				.size(20, 16)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> removeType())
				.position(left + 274, top + 88)
				.size(20, 16)
				.build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveSettings())
				.position(left, top + 124)
				.size(60, 18)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> returnToParent())
				.position(left + 70, top + 124)
				.size(60, 18)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		if (project != null) {
			int left = this.width / 2 - 140;
			int y = 14;
			context.drawText(textRenderer, Text.literal("Owner: " + project.getOwnerName()), left, y, 0xE0E0E0, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Members: " + String.join(", ", project.getMembers().values())), left, y, 0xCCCCCC, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Statuses: " + String.join(", ", statuses)), left, y, 0xCCCCCC, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Types: " + String.join(", ", ticketTypes)), left, y, 0xCCCCCC, false);
		}
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
		addMemberField.setText("");
	}

	private void sendRemoveMember() {
		String name = removeMemberField.getText();
		if (name.isBlank()) {
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.RemoveProjectMemberPayload(parent.getProjectId(), name));
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
		returnToParent();
	}

	private void returnToParent() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(parent);
		}
	}

}
