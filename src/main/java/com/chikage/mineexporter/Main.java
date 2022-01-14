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

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent.Pre event) {
        FontRenderer renderer = Minecraft.getMinecraft().fontRenderer;
        int x = 0;
        int y = 0;
        String text = "test";
        renderer.drawStringWithShadow(text, (float)(x - renderer.getStringWidth(text) / 2), (float)y, 0xE0E0E0);
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
//        Main.logger.info("draw");
        BlockPos pos1 = mexpCommand.getPos1();
        BlockPos pos2 = mexpCommand.getPos2();
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if (entity == null) return;

        Vec3d cameraPos = new Vec3d(
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX)* event.getPartialTicks(),
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY)* event.getPartialTicks(),
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ)* event.getPartialTicks()
        );

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslated(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        GL11.glDepthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        if (pos1 != null) {
            Color c = new Color(255, 0, 0, 255);
            drawBoundingBox(bufferBuilder, new Vec3d(pos1), new Vec3d(pos1), c, 2);
        }

        if (pos2 != null) {
            Color c = new Color(0, 0, 255, 255);
            drawBoundingBox(bufferBuilder, new Vec3d(pos2), new Vec3d(pos2), c, 2);
        }

        if (pos1 != null && pos2 != null) {
            Color c = new Color(255, 255, 255, 255);
            drawBoundingBox(bufferBuilder, new Vec3d(pos1), new Vec3d(pos2), c, 3);
        }

        tessellator.draw();
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
    }

    public static void drawBoundingBox(BufferBuilder bufferBuilder, Vec3d posA, Vec3d posB, Color c, float width) {
        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        GL11.glLineWidth(width);

        double Mx = Math.max(posA.x, posB.x)+1;
        double My = Math.max(posA.y, posB.y)+1;
        double Mz = Math.max(posA.z, posB.z)+1;

        double mx = Math.min(posA.x, posB.x);
        double my = Math.min(posA.y, posB.y);
        double mz = Math.min(posA.z, posB.z);

        //AB
        bufferBuilder.pos(mx, my, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();          //A
        bufferBuilder.pos(mx, my, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //B
        //BC
        bufferBuilder.pos(mx, my, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //B
        bufferBuilder.pos(Mx, my, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //C
        //CD
        bufferBuilder.pos(Mx, my, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //C
        bufferBuilder.pos(Mx, my, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //D
        //DA
        bufferBuilder.pos(Mx, my, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //D
        bufferBuilder.pos(mx, my, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();          //A
        //EF
        bufferBuilder.pos(mx, My, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //E
        bufferBuilder.pos(mx, My, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //F
        //FG
        bufferBuilder.pos(mx, My, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //F
        bufferBuilder.pos(Mx, My, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex(); //G
        //GH
        bufferBuilder.pos(Mx, My, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex(); //G
        bufferBuilder.pos(Mx, My, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //H
        //HE
        bufferBuilder.pos(Mx, My, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //H
        bufferBuilder.pos(mx, My, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //E
        //AE
        bufferBuilder.pos(mx, my, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();          //A
        bufferBuilder.pos(mx, My, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //E
        //BF
        bufferBuilder.pos(mx, my, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //B
        bufferBuilder.pos(mx, My, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //F
        //CG
        bufferBuilder.pos(Mx, my, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //C
        bufferBuilder.pos(Mx, My, Mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex(); //G
        //DH
        bufferBuilder.pos(Mx, my, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();       //D
        bufferBuilder.pos(Mx, My, mz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();    //H
    }

    /**
     * This is the final initialization event. Register actions from other mods here
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }
}
