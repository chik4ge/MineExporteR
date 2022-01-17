package com.chikage.mineexporter;

import com.chikage.mineexporter.commands.CommandMexp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod(
        modid = Main.MOD_ID,
        name = Main.MOD_NAME,
        version = Main.VERSION,
        clientSideOnly = true
)
public class Main {

    public static final String MOD_ID = "mineexporter";
    public static final String MOD_NAME = "MineExporteR";
    public static final String VERSION = "0.1.0-alpha";

    public final CommandMexp mexpCommand = new CommandMexp();

    public static final ExportThread exportThread = new ExportThread();

    public static Logger logger;

    /**
     * This is the instance of your mod as created by Forge. It will never be null.
     */
    @Mod.Instance(MOD_ID)
    public static Main INSTANCE;

    /**
     * This is the first initialization event. Register tile entities here.
     * The registry events below will have fired prior to entry to this method.
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    /**
     * This is the second initialization event. Register custom recipes
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(mexpCommand);
        MinecraftForge.EVENT_BUS.register(this);

        Path path = Paths.get("MineExporteR");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int exportProgressPercent = -1;
    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent.Pre event) {
        FontRenderer renderer = Minecraft.getMinecraft().fontRenderer;
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();

        int x = width/2;
        int y = height - 60;
        if (exportProgressPercent != -1) {
            String text = "EXPORTING..." + exportProgressPercent + "%";
            renderer.drawStringWithShadow(text, (float) (x - renderer.getStringWidth(text) / 2), (float) y, 0xE0E0E0);
        }
    }

    @SubscribeEvent
    public void onPlayerTicks(TickEvent.PlayerTickEvent event) {
        exportProgressPercent = exportThread.getProgressPercent();
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
//        Main.logger.info("draw");
        BlockPos pos1 = exportThread.getPos1();
        BlockPos pos2 = exportThread.getPos2();
        RenderHandler.renderSelectedRegion(pos1, pos2, event.getPartialTicks());
    }

    /**
     * This is the final initialization event. Register actions from other mods here
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }
}
