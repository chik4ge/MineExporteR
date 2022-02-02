package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.TextureHandler;
import com.chikage.mineexporter.utils.ExportContext;
import com.chikage.mineexporter.utils.Texture;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

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
    @Override
    public boolean export(Map<Texture, Set<float[][][]>> faces){

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
                    NWu = 0.0f;
                    NWv = 0.0f;
                    SWu = NWu;
                    SWv = 1.0f;
                    SEu = 1.0f;
                    SEv = SWv;
                    NEu = SEu;
                    NEv = NWv;
                } else {
                    float f21 = MathHelper.sin(slopeAngle) * 0.25F;
                    float f22 = MathHelper.cos(slopeAngle) * 0.25F;
                    NWu = 0.5F + (-f22 - f21);
                    NWv = 0.5F + (-f22 + f21);
                    SWu = 0.5F + (-f22 + f21);
                    SWv = 0.5F + (f22 + f21);
                    SEu = 0.5F + (f22 + f21);
                    SEv = 0.5F + (f22 - f21);
                    NEu = 0.5F + (f22 - f21);
                    NEv = 0.5F + (-f22 - f21);
                }

                Texture tex = getTexture(textureatlassprite);
                if (textureatlassprite.hasAnimationMetadata()) {
                    tex.setFrameCount(textureatlassprite.getFrameCount());
                }

                float[][][] face = new float[][][]{
                        {
                            {offset[0], offset[1] + NWy, offset[2]},
                            {NWu, NWv, 0}
                        },
                        {
                            {offset[0], offset[1] + SWy, offset[2] + 1F},
                            {SWu, SWv, 0}
                        },
                        {
                            {offset[0] + 1F, offset[1] + SEy, offset[2] + 1F},
                            {SEu, SEv, 0}
                        },
                        {
                            {offset[0] + 1F, offset[1] + NEy, offset[2]},
                            {NEu, NEv, 0}
                        }
                };

                if (faces.containsKey(tex)) {
                    faces.get(tex).add(face);
                } else {
                    faces.put(tex, new CopyOnWriteArraySet<>(Collections.singletonList(face)));
                }
            }

            if (renderDOWN) {
                TextureAtlasSprite textureatlassprite = atextureatlassprite[0];

                Texture tex = getTexture(textureatlassprite);
                if (textureatlassprite.hasAnimationMetadata()) {
                    tex.setFrameCount(textureatlassprite.getFrameCount());
                }

                float[][][] face = new float[][][]{
                        {
                                {offset[0], offset[1], offset[2] + 1F},
                                {0.0f, 1.0f, 0}
                        },
                        {
                                {offset[0], offset[1], offset[2]},
                                {0.0f, 0.0f, 0}
                        },
                        {
                                {offset[0] + 1F, offset[1], offset[2]},
                                {1.0f, 0.0f, 0}
                        },
                        {
                                {offset[0] + 1F, offset[1], offset[2] + 1F},
                                {1.0f, 1.0f, 0}
                        }
                };

                if (faces.containsKey(tex)) {
                    faces.get(tex).add(face);
                } else {
                    faces.put(tex, new CopyOnWriteArraySet<>(Collections.singletonList(face)));
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

                    float f41 = 0.0f;
                    float f27 = 0.5f;
                    float f28 = (1.0f - f39) * 0.5f;
                    float f29 = (1.0f - f40) * 0.5f;
                    float f30 = 0.5f;
//                                int j = state.getPackedLightmapCoords(access, blockpos);
//                                int k = j >> 16 & 65535;
//                                int l = j & 65535;
//                                float f31 = i1 < 2 ? 0.8F : 0.6F;
                    Texture tex = getTexture(textureatlassprite1);
                    if (textureatlassprite1.hasAnimationMetadata()) {
                        tex.setFrameCount(textureatlassprite1.getFrameCount());
                    }

                    float[][][] face = new float[][][]{
                            {
                                    {(float) d3, offset[1] + f39, (float) d4},
                                    {f41, f28, 0}
                            },
                            {
                                    {(float) d5, offset[1] + f40, (float) d6},
                                    {f27, f29, 0}
                            },
                            {
                                    {(float) d5, offset[1], (float) d6},
                                    {f27, f30, 0}
                            },
                            {
                                    {(float) d3, offset[1], (float) d4},
                                    {f41, f30, 0}
                            }
                    };

                    if (faces.containsKey(tex)) {
                        faces.get(tex).add(face);
                    } else {
                        faces.put(tex, new CopyOnWriteArraySet<>(Collections.singletonList(face)));
                    }
                }
            }
        }
        return true;
    }

    private Texture getTexture(TextureAtlasSprite sprite) {
        String iconName = sprite.getIconName();
        return new Texture(new ResourceLocation(iconName));
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
