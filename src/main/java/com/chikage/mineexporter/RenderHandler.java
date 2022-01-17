package com.chikage.mineexporter;

import com.chikage.mineexporter.utils.Range;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Set;

public class RenderHandler {

    public static void renderSelectedRegion(BlockPos pos1, BlockPos pos2, Set<int[]> chunks, float partialTicks){
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

        if (pos1 != null && pos2 != null) {
            double Mx = Math.max(pos1.getX(), pos2.getX())+1;
            double My = Math.max(pos1.getY(), pos2.getY())+1;
            double Mz = Math.max(pos1.getZ(), pos2.getZ())+1;

            double mx = Math.min(pos1.getX(), pos2.getX());
            double my = Math.min(pos1.getY(), pos2.getY());
            double mz = Math.min(pos1.getZ(), pos2.getZ());

            if (chunks != null) {
                for (int[] chunkXZ : chunks) {
                    Color c = new Color(255, 255, 0,128);
                    drawChunkRange(bufferBuilder, mx, my, mz, Mx, My, Mz, chunkXZ, c, .1f);
                }
            } else {
                chunks = new Range(pos1, pos2).getChunks();
                for (int[] chunkXZ : chunks) {
                    Color c = new Color(255, 255, 0,128);
                    drawChunkRange(bufferBuilder, mx, my, mz, Mx, My, Mz, chunkXZ, c, .1f);
                }
            }

            Color c = new Color(255, 255, 255, 255);
            drawBoundingBox(bufferBuilder, mx, my, mz, Mx, My, Mz, c, 3);
        }

        if (pos1 != null) {
            Color c = new Color(255, 0, 0, 255);
            drawBoundingBox(bufferBuilder, pos1.getX(), pos1.getY(), pos1.getZ(), c, 2);
        }

        if (pos2 != null) {
            Color c = new Color(0, 0, 255, 255);
            drawBoundingBox(bufferBuilder, pos2.getX(), pos2.getY(), pos2.getZ(), c, 2);
        }

        tessellator.draw();
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
    }

    private static void drawBoundingBox(BufferBuilder bufferBuilder, double mx, double my, double mz, double Mx, double My, double Mz, Color c, float width) {
        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        GL11.glLineWidth(width);

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

    private static void drawBoundingBox(BufferBuilder bufferBuilder, double x, double y, double z, Color c, float width) {
        drawBoundingBox(bufferBuilder, x, y, z, x+1, y+1, z+1, c, width);
    }

    private static void drawRect(BufferBuilder bufferBuilder, double x1, double y1, double z1, double x2, double y2, double z2, Color c, float width) {
        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        GL11.glLineWidth(width);

        bufferBuilder.pos(x1, y1, z1).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(x2, y1, z2).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(x1, y1, z1).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(x1, y2, z1).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(x2, y1, z2).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(x2, y2, z2).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(x1, y2, z1).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(x2, y2, z2).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
    }

    private static void drawChunkRange(BufferBuilder bufferBuilder, double mx, double my, double mz, double Mx, double My, double Mz, int[] chunkXZ, Color c, float width) {
        double maxX = Math.min(Mx, 16*chunkXZ[0]+16);
        double maxZ = Math.min(Mz, 16*chunkXZ[1]+16);
        double minX = Math.max(mx, 16*chunkXZ[0]);
        double minZ = Math.max(mz, 16*chunkXZ[1]);

        drawBoundingBox(bufferBuilder, minX, my, minZ, maxX, My, maxZ, c, width);
    }
}
