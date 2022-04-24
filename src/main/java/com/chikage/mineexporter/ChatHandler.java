package com.chikage.mineexporter;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class ChatHandler {
    private static ICommandSender commandSender;

    public static void setCommandSender(ICommandSender sender) {
        ChatHandler.commandSender = sender;
    }

    public static void sendMessage(TextFormatting tf, String s) {
        if (ChatHandler.commandSender != null) {
            commandSender.sendMessage(new TextComponentString(tf + s));
        }
    }

    public static void sendErrorMessage(String s) {
        sendMessage(TextFormatting.RED, s);
    }

    public static void sendSuccessMessage(String s) {
        sendMessage(TextFormatting.GREEN, s);
    }
}
