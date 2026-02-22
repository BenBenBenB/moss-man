package com.mossman.adapters.tui;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TuiHelper {

    /**
     * Creates a clickable text component that runs a command.
     */
    public static MutableText createRunLink(String display, String command, String hoverText, Formatting... formatting) {
        return createRunLink(display, command, Text.literal(hoverText), formatting);
    }

    public static MutableText createRunLink(String display, String command, Text hoverText, Formatting... formatting) {
        MutableText text = Text.literal(display).formatted(formatting);
        text.styled(style -> style
                .withClickEvent(new net.minecraft.text.ClickEvent.RunCommand(command))
                .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(hoverText)));
        return text;
    }

    /**
     * Creates a clickable text component that suggests a command.
     */
    public static MutableText createSuggestLink(String display, String command, String hoverText, Formatting... formatting) {
        return createSuggestLink(display, command, Text.literal(hoverText), formatting);
    }

    public static MutableText createSuggestLink(String display, String command, Text hoverText, Formatting... formatting) {
        MutableText text = Text.literal(display).formatted(formatting);
        text.styled(style -> style
                .withClickEvent(new net.minecraft.text.ClickEvent.SuggestCommand(command))
                .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(hoverText)));
        return text;
    }

    /**
     * Creates a standard translatable component.
     */
    public static MutableText translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }
}
