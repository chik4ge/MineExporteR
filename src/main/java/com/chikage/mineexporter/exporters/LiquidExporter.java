package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.Main;
import com.chikage.mineexporter.TextureHandler;
import com.chikage.mineexporter.utils.ExportContext;
import com.chikage.mineexporter.utils.Face;
import com.chikage.mineexporter.utils.UV;
import com.chikage.mineexporter.utils.Vertex;
import de.javagl.obj.Mtl;
import de.javagl.obj.Mtls;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.client.Minecraft.getMinecraft;

public class LiquidExporter extends BlockExporter{
    static final TextureAtlasSprite[] atlasSpritesLava = new TextureAtlasSprite[2];
    static final TextureAtlasSprite[] atlasSpritesWater = new TextureAtlasSprite[2];
    static final TextureAtlasSprite atlasSpriteWaterOverlay;

    static {
        TextureMap texturemap = getMinecraft().getTextureMapBlocks();
        atlasSpritesLava[0] = texturemap.getAtlasSprite("minecraft:blocks/lava_still");
        atlasSpritesLava[1] = texturemap.getAtlasSprite("minecraft:blocks/lava_flow");
        atlasSpritesWater[0] = texturemap.getAtlasSprite("minecraft:blocks/water_still");
        atlasSpritesWater[1] = texturemap.getAtlasSprite("minecraft:blocks/water_flow");
        atlasSpriteWaterOverlay = texturemap.getAtlasSprite("minecraft:blocks/water_overlay");
    }


    public LiquidExporter(ExportContext expCtx, IBlockState state, BlockPos pos) {
        super(expCtx, state, pos);
    }

//    TODO implement
    @Override
    public boolean export(Set<Vertex> verticesIn, Set<UV> uvsIn, Map<String, Set<Face>> facesIn, Set<Mtl> mtlsIn) {
        TextureHandler texHandler;

//                    BlockFluidRenderほぼそのまま実装
//                    BlockLiquid blockliquid = (BlockLiquid)state.getBlock();
        boolean isLava = state.getMaterial() == Material.LAVA;
        TextureAtlasSprite[] atextureatlassprite = isLava ? atlasSpritesLava : atlasSpritesWater;
//                    int tintRGB = getMinecraft().getBlockColors().colorMultiplier(state, sender.getEntityWorld(), pos, 0);
//                    float tintR = (float)(tintRGB >> 16 & 255) / 255.0F;
//                    float tintG = (float)(tintRGB >> 8 & 255) / 255.0F;
//                    float tintB = (float)(tintRGB & 255) / 255.0F;

        boolean renderUP = state.shouldSideBeRendered(expCtx.worldIn, pos, EnumFacing.UP);
        boolean renderDOWN = state.shouldSideBeRendered(expCtx.worldIn, pos, EnumFacing.DOWN);
        boolean[] renderSIDEs = new boolean[] {
                state.shouldSideBeRendered(expCtx.worldIn, pos, EnumFacing.NORTH),
                state.shouldSideBeRendered(expCtx.worldIn, pos, EnumFacing.SOUTH),
                state.shouldSideBeRendered(expCtx.worldIn, pos, EnumFacing.WEST),
                state.shouldSideBeRendered(expCtx.worldIn, pos, EnumFacing.EAST)
        };
        float[] offset = getOffset(expCtx.worldIn, state, pos, expCtx.range.getOrigin());

        if (!renderUP && !renderDOWN && !renderSIDEs[0] && !renderSIDEs[1] && !renderSIDEs[2] && !renderSIDEs[3]) {
            return true;
//            continue;
        } else {
            Material material = state.getMaterial();
            float NWy = getFluidHeight(expCtx.worldIn, pos, material);
            float SWy = getFluidHeight(expCtx.worldIn, pos.south(), material);
            float SEy = getFluidHeight(expCtx.worldIn, pos.east().south(), material);
            float NEy = getFluidHeight(expCtx.worldIn, pos.east(), material);

            if (renderUP) {
                float slopeAngle = BlockLiquid.getSlopeAngle(expCtx.worldIn, pos, material, state);
                TextureAtlasSprite textureatlassprite = slopeAngle > -999.0F ? atextureatlassprite[1] : atextureatlassprite[0];
                texHandler = new TextureHandler(textureatlassprite);
                String texName = texHandler.getTextureName();
                if (!mtlsIn.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {
                    BufferedImage texture;
                    try {
                        texture = texHandler.getBaseTextureImage(expCtx.rm);
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + state.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        return false;
//                        continue;
                    }
                    if (texture == null) return false;

                    String texLocation = "textures/" + texName + ".png";
                    try {
                        texHandler.save(texture, Paths.get("MineExporteR/" + texLocation));
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error(TextFormatting.RED + "failed to save texture image: " + texLocation);
                        e.printStackTrace();
                        return false;
                    }

                    Mtl mtl = Mtls.create(texName);
                    mtl.setMapKd(texLocation);
                    mtlsIn.add(mtl);
                }

                NWy -= 0.001F;
                SWy -= 0.001F;
                SEy -= 0.001F;
                NEy -= 0.001F;
                float NWu;
                float SWu;
                float SEu;
                float NEu;
                float NWv;
                float SWv;
                float SEv;
                float NEv;
                if (slopeAngle < -999.0F) {
                    NWu = textureatlassprite.getInterpolatedU(0.0D);
                    NWv = textureatlassprite.getInterpolatedV(0.0D);
                    SWu = NWu;
                    SWv = textureatlassprite.getInterpolatedV(16.0D);
                    SEu = textureatlassprite.getInterpolatedU(16.0D);
                    SEv = SWv;
                    NEu = SEu;
                    NEv = NWv;
                } else {
                    float f21 = MathHelper.sin(slopeAngle) * 0.25F;
                    float f22 = MathHelper.cos(slopeAngle) * 0.25F;
                    NWu = textureatlassprite.getInterpolatedU(8.0F + (-f22 - f21) * 16.0F);
                    NWv = textureatlassprite.getInterpolatedV(8.0F + (-f22 + f21) * 16.0F);
                    SWu = textureatlassprite.getInterpolatedU(8.0F + (-f22 + f21) * 16.0F);
                    SWv = textureatlassprite.getInterpolatedV(8.0F + (f22 + f21) * 16.0F);
                    SEu = textureatlassprite.getInterpolatedU(8.0F + (f22 + f21) * 16.0F);
                    SEv = textureatlassprite.getInterpolatedV(8.0F + (f22 - f21) * 16.0F);
                    NEu = textureatlassprite.getInterpolatedU(8.0F + (f22 - f21) * 16.0F);
                    NEv = textureatlassprite.getInterpolatedV(8.0F + (-f22 - f21) * 16.0F);
                }

                Vertex v1 = new Vertex(offset[0], offset[1] + NWy, offset[2]);
                Vertex v2 = new Vertex(offset[0], offset[1] + SWy, offset[2] + 1F);
                Vertex v3 = new Vertex(offset[0] + 1F, offset[1] + SEy, offset[2] + 1F);
                Vertex v4 = new Vertex(offset[0] + 1F, offset[1] + NEy, offset[2]);
                UV uv1 = new UV(NWu, NWv);
                UV uv2 = new UV(SWu, SWv);
                UV uv3 = new UV(SEu, SEv);
                UV uv4 = new UV(NEu, NEv);
                Vertex[] vertices = {v1, v2, v3, v4};
                UV[] uvs = {uv1, uv2, uv3, uv4};

                Collections.addAll(verticesIn, vertices);
                Collections.addAll(uvsIn, uvs);

                Face face = new Face(vertices, uvs);
                if (facesIn.containsKey(texName)) {
                    facesIn.get(texName).add(face);
                } else {
                    facesIn.put(texName, new HashSet<>(Arrays.asList(face)));
                }
            }

            if (renderDOWN) {
                texHandler = new TextureHandler(atextureatlassprite[0]);
                String texName = texHandler.getTextureName();
                if (!mtlsIn.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {
                    BufferedImage texture;
                    try {
                        texture = texHandler.getBaseTextureImage(expCtx.rm);
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + state.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        return false;
                    }
                    if (texture == null) return false;

                    String texLocation = "textures/" + texName + ".png";
                    try {
                        texHandler.save(texture, Paths.get("MineExporteR/" + texLocation));
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error(TextFormatting.RED + "failed to save texture image: " + texLocation);
                        e.printStackTrace();
                        return false;
                    }

                    Mtl mtl = Mtls.create(texName);
                    mtl.setMapKd(texLocation);
                    mtlsIn.add(mtl);
                }

                float minU = atextureatlassprite[0].getMinU();
                float maxU = atextureatlassprite[0].getMaxU();
                float minV = atextureatlassprite[0].getMinV();
                float maxV = atextureatlassprite[0].getMaxV();

                Vertex v1 = new Vertex(offset[0], offset[1], offset[2] + 1F);
                Vertex v2 = new Vertex(offset[0], offset[1], offset[2]);
                Vertex v3 = new Vertex(offset[0] + 1F, offset[1], offset[2]);
                Vertex v4 = new Vertex(offset[0] + 1F, offset[1], offset[2] + 1F);
                UV uv1 = new UV(minU, maxV);
                UV uv2 = new UV(minU, minV);
                UV uv3 = new UV(maxU, minV);
                UV uv4 = new UV(maxU, maxV);
                Vertex[] vertices = {v1, v2, v3, v4};
                UV[] uvs = {uv1, uv2, uv3, uv4};

                Collections.addAll(verticesIn, vertices);
                Collections.addAll(uvsIn, uvs);

                Face face = new Face(vertices, uvs);
                if (facesIn.containsKey(texName)) {
                    facesIn.get(texName).add(face);
                } else {
                    facesIn.put(texName, new HashSet<>(Arrays.asList(face)));
                }
            }

            for (int i1 = 0; i1 < 4; ++i1) {

                int j1 = 0;
                int k1 = 0;

                if (i1 == 0)
                {
                    --k1;
                }

                if (i1 == 1)
                {
                    ++k1;
                }

                if (i1 == 2)
                {
                    --j1;
                }

                if (i1 == 3)
                {
                    ++j1;
                }

                BlockPos blockpos = pos.add(j1, 0, k1);
                TextureAtlasSprite textureatlassprite1 = atextureatlassprite[1];

                if (!isLava)
                {
                    IBlockState lavaState = expCtx.worldIn.getBlockState(blockpos);

                    if (lavaState.getBlockFaceShape(expCtx.worldIn, blockpos, EnumFacing.VALUES[i1+2].getOpposite()) == BlockFaceShape.SOLID)
                    {
                        textureatlassprite1 = atlasSpriteWaterOverlay;
                    }
                }

                texHandler = new TextureHandler(textureatlassprite1);
                String texName = texHandler.getTextureName();
                if (!mtlsIn.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {
                    BufferedImage texture;
                    try {
                        texture = texHandler.getBaseTextureImage(expCtx.rm);
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + state.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        return false;
                    }
                    if (texture == null) return false;

                    String texLocation = "textures/" + texName + ".png";
                    try {
                        texHandler.save(texture, Paths.get("MineExporteR/" + texLocation));
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error(TextFormatting.RED + "failed to save texture image: " + texLocation);
                        e.printStackTrace();
                        return false;
                    }

                    Mtl mtl = Mtls.create(texName);
                    mtl.setMapKd(texLocation);
                    mtlsIn.add(mtl);
                }

                if (renderSIDEs[i1])
                {
                    float f39;
                    float f40;
                    double d3;
                    double d4;
                    double d5;
                    double d6;

                    if (i1 == 0)
                    {
                        f39 = NWy;
                        f40 = NEy;
                        d3 = offset[0];
                        d5 = offset[0] + 1.0D;
                        d4 = offset[2] + 0.0010000000474974513D;
                        d6 = offset[2] + 0.0010000000474974513D;
                    }
                    else if (i1 == 1)
                    {
                        f39 = SEy;
                        f40 = SWy;
                        d3 = offset[0] + 1.0D;
                        d5 = offset[0];
                        d4 = offset[2] + 1.0D - 0.0010000000474974513D;
                        d6 = offset[2] + 1.0D - 0.0010000000474974513D;
                    }
                    else if (i1 == 2)
                    {
                        f39 = SWy;
                        f40 = NWy;
                        d3 = offset[0] + 0.0010000000474974513D;
                        d5 = offset[0] + 0.0010000000474974513D;
                        d4 = offset[2] + 1.0D;
                        d6 = offset[2];
                    }
                    else
                    {
                        f39 = NEy;
                        f40 = SEy;
                        d3 = offset[0] + 1.0D - 0.0010000000474974513D;
                        d5 = offset[0] + 1.0D - 0.0010000000474974513D;
                        d4 = offset[2];
                        d6 = offset[2] + 1.0D;
                    }

                    float f41 = textureatlassprite1.getInterpolatedU(0.0D);
                    float f27 = textureatlassprite1.getInterpolatedU(8.0D);
                    float f28 = textureatlassprite1.getInterpolatedV((1.0F - f39) * 16.0F * 0.5F);
                    float f29 = textureatlassprite1.getInterpolatedV((1.0F - f40) * 16.0F * 0.5F);
                    float f30 = textureatlassprite1.getInterpolatedV(8.0D);
//                                int j = state.getPackedLightmapCoords(access, blockpos);
//                                int k = j >> 16 & 65535;
//                                int l = j & 65535;
//                                float f31 = i1 < 2 ? 0.8F : 0.6F;

                    Vertex v1 = new Vertex((float) d3, offset[1] + f39, (float) d4);
                    Vertex v2 = new Vertex((float) d5, offset[1] + f40, (float) d6);
                    Vertex v3 = new Vertex((float) d5, offset[1], (float) d6);
                    Vertex v4 = new Vertex((float) d3, offset[1], (float) d4);
                    UV uv1 = new UV(f41, f28);
                    UV uv2 = new UV(f27, f29);
                    UV uv3 = new UV(f27, f30);
                    UV uv4 = new UV(f41, f30);
                    Vertex[] vertices = {v1, v2, v3, v4};
                    UV[] uvs = {uv1, uv2, uv3, uv4};

                    Collections.addAll(verticesIn, vertices);
                    Collections.addAll(uvsIn, uvs);

                    Face face = new Face(vertices, uvs);
                    if (facesIn.containsKey(texName)) {
                        facesIn.get(texName).add(face);
                    } else {
                        facesIn.put(texName, new HashSet<>(Arrays.asList(face)));
                    }
                }
            }
        }
        return true;
    }

    private float getFluidHeight(IBlockAccess blockAccess, BlockPos blockPosIn, Material blockMaterial) {
        int i = 0;
        float f = 0.0F;

        for (int j = 0; j < 4; ++j)
        {
            BlockPos blockpos = blockPosIn.add(-(j & 1), 0, -(j >> 1 & 1));

            if (blockAccess.getBlockState(blockpos.up()).getMaterial() == blockMaterial)
            {
                return 1.0F;
            }

            IBlockState iblockstate = blockAccess.getBlockState(blockpos);
            Material material = iblockstate.getMaterial();

            if (material != blockMaterial)
            {
                if (!material.isSolid())
                {
                    ++f;
                    ++i;
                }
            }
            else
            {
                int k = iblockstate.getValue(BlockLiquid.LEVEL);

                if (k >= 8 || k == 0)
                {
                    f += BlockLiquid.getLiquidHeightPercent(k) * 10.0F;
                    i += 10;
                }

                f += BlockLiquid.getLiquidHeightPercent(k);
                ++i;
            }
        }

        return 1.0F - f / (float)i;
    }
}
