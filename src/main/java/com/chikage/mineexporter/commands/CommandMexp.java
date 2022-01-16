package com.chikage.mineexporter.commands;

import com.chikage.mineexporter.Main;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.IClientCommand;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.minecraft.command.CommandBase.getListOfStringsMatchingLastWord;

public class CommandMexp implements IClientCommand {
    List<String> aliases;
    private String usage = "/mexp pos1 - set current position as pos1\n" +
            "/mexp pos2 - set current position as pos1\n" +
            "/mexp export - export blocks in range";

    public CommandMexp() {
        aliases = new ArrayList<>();
        aliases.add("mexp");
        aliases.add("mineexport");
    }

    @Override
    public String getName() {
        return "mineexport";
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        if (!sender.getEntityWorld().isRemote) {
            return usage;
        } else {
            return "";
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(usage));
            return;
        }
        switch (args[0]) {
            case "pos1":
                setPos1(sender);
                break;
            case "pos2":
                setPos2(sender);
                break;
            case "export":
                export(server, sender);
                break;
            default:
                sender.sendMessage(new TextComponentString(usage));
                break;
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "pos1", "pos2", "export");
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    private void setPos1(ICommandSender sender) {
        if (sender.getCommandSenderEntity() != null) {
            int x = (int) Math.floor(sender.getCommandSenderEntity().posX);
            int y = (int) Math.floor(sender.getCommandSenderEntity().posY)-1;
            int z = (int) Math.floor(sender.getCommandSenderEntity().posZ);
            Main.exportThread.setPos1(new BlockPos(x, y, z));
            sender.sendMessage(new TextComponentString("pos1 set to (" + x + ", " + y + ", " + z + ")"));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "cannnot set pos1 successfully."));
        }
    }

    private void setPos2(ICommandSender sender) {
        if (sender.getCommandSenderEntity() != null) {
            int x = (int) Math.floor(sender.getCommandSenderEntity().posX);
            int y = (int) Math.floor(sender.getCommandSenderEntity().posY)-1;
            int z = (int) Math.floor(sender.getCommandSenderEntity().posZ);
            Main.exportThread.setPos2(new BlockPos(x, y, z));
            sender.sendMessage(new TextComponentString("pos2 set to (" + x + ", " + y + ", " + z + ")"));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "cannnot set pos2 successfully."));
        }
    }

    private void export(MinecraftServer server, ICommandSender sender) {

        ExecutorService ex = Executors.newSingleThreadExecutor();

        Main.exportThread.setWorld(sender.getEntityWorld());
        Main.exportThread.setCommandSender(sender);
        ex.execute(Main.exportThread);
    }

    @Override
    public boolean allowUsageWithoutPrefix(ICommandSender sender, String message) {
        return true;
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }
}
