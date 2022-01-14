package com.chikage.mineexporter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RenderHandler {

    public static void renderSelectedRegion(BlockPos pos1, BlockPos pos2, float partialTicks){
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if (entity == null) return;

        Vec3d cameraPos = new Vec3d(
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX)* partialTicks,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY)* partialTicks,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ)* partialTicks
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

    private static void drawBoundingBox(BufferBuilder bufferBuilder, Vec3d posA, Vec3d posB, Color c, float width) {
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

    public static void renderProgressText() {

    }
}
