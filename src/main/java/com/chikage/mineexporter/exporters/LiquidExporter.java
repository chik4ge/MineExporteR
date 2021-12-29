package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.utils.ExportContext;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class LiquidExporter extends BlockExporter{
    public LiquidExporter() {
    }

//    TODO implement
    @Override
    public boolean export(ExportContext expCtx, IBlockState state, BlockPos pos) {
/*
        TextureHandler texHandler;

        TextureAtlasSprite[] atlasSpritesLava = new TextureAtlasSprite[2];
        TextureAtlasSprite[] atlasSpritesWater = new TextureAtlasSprite[2];
        TextureMap texturemap = getMinecraft().getTextureMapBlocks();
        atlasSpritesLava[0] = texturemap.getAtlasSprite("minecraft:blocks/lava_still");
        atlasSpritesLava[1] = texturemap.getAtlasSprite("minecraft:blocks/lava_flow");
        atlasSpritesWater[0] = texturemap.getAtlasSprite("minecraft:blocks/water_still");
        atlasSpritesWater[1] = texturemap.getAtlasSprite("minecraft:blocks/water_flow");
        TextureAtlasSprite atlasSpriteWaterOverlay = texturemap.getAtlasSprite("minecraft:blocks/water_overlay");

//                    BlockFluidRenderほぼそのまま実装
//                    BlockLiquid blockliquid = (BlockLiquid)aState.getBlock();
        boolean isLava = aState.getMaterial() == Material.LAVA;
        TextureAtlasSprite[] atextureatlassprite = isLava ? atlasSpritesLava : atlasSpritesWater;
        IBlockAccess access = sender.getEntityWorld();
//                    int tintRGB = getMinecraft().getBlockColors().colorMultiplier(aState, sender.getEntityWorld(), pos, 0);
//                    float tintR = (float)(tintRGB >> 16 & 255) / 255.0F;
//                    float tintG = (float)(tintRGB >> 8 & 255) / 255.0F;
//                    float tintB = (float)(tintRGB & 255) / 255.0F;

        boolean renderUP = aState.shouldSideBeRendered(access, pos, EnumFacing.UP);
        boolean renderDOWN = aState.shouldSideBeRendered(access, pos, EnumFacing.DOWN);
        boolean[] renderSIDEs = new boolean[] {
                aState.shouldSideBeRendered(access, pos, EnumFacing.NORTH),
                aState.shouldSideBeRendered(access, pos, EnumFacing.SOUTH),
                aState.shouldSideBeRendered(access, pos, EnumFacing.WEST),
                aState.shouldSideBeRendered(access, pos, EnumFacing.EAST)
        };

        if (!renderUP && !renderDOWN && !renderSIDEs[0] && !renderSIDEs[1] && !renderSIDEs[2] && !renderSIDEs[3]) {
            continue;
        } else {
            Material material = aState.getMaterial();
            float NWy = getFluidHeight(access, pos, material);
            float SWy = getFluidHeight(access, pos.south(), material);
            float SEy = getFluidHeight(access, pos.east().south(), material);
            float NEy = getFluidHeight(access, pos.east(), material);

            if (renderUP) {
                float slopeAngle = BlockLiquid.getSlopeAngle(access, pos, material, aState);
                TextureAtlasSprite textureatlassprite = slopeAngle > -999.0F ? atextureatlassprite[1] : atextureatlassprite[0];
                texHandler = new TextureHandler(textureatlassprite);
                String texName = texHandler.getTextureName();
                if (!mtls.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {
                    BufferedImage texture;
                    try {
                        texture = texHandler.getBaseTextureImage(resourceManager);
                    } catch (IOException e) {
                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + aState.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        continue;
                    }
                    if (texture == null) continue;

                    String texLocation = "textures/" + texName + ".png";
                    try {
                        texHandler.save(texture, Paths.get("MineExporteR/" + texLocation));
                    } catch (IOException e) {
                        noError = false;
                        Main.logger.error(TextFormatting.RED + "failed to save texture image: " + texLocation);
                        e.printStackTrace();
                    }

                    Mtl mtl = Mtls.create(texName);
                    mtl.setMapKd(texLocation);
                    mtls.add(mtl);
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
                obj.addVertex((float) xOffset, (float) yOffset + NWy, (float) zOffset);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(NWu, NWv);
                obj.addVertex((float) xOffset, (float) yOffset + SWy, (float) zOffset + 1F);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(SWu, SWv);
                obj.addVertex((float) xOffset + 1F, (float) yOffset + SEy, (float) zOffset + 1F);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(SEu, SEv);
                obj.addVertex((float) xOffset + 1F, (float) yOffset + NEy, (float) zOffset);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(NEu, NEv);
                obj.setActiveMaterialGroupName(texName);
                obj.addFaceWithTexCoords(4 * faceindex, 4 * faceindex + 1, 4 * faceindex + 2, 4 * faceindex + 3);
                faceindex += 1;
            }

            if (renderDOWN) {
                texHandler = new TextureHandler(atextureatlassprite[0]);
                String texName = texHandler.getTextureName();
                if (!mtls.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {
                    BufferedImage texture;
                    try {
                        texture = texHandler.getBaseTextureImage(resourceManager);
                    } catch (IOException e) {
                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + aState.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        continue;
                    }
                    if (texture == null) continue;

                    String texLocation = "textures/" + texName + ".png";
                    try {
                        texHandler.save(texture, Paths.get("MineExporteR/" + texLocation));
                    } catch (IOException e) {
                        noError = false;
                        Main.logger.error(TextFormatting.RED + "failed to save texture image: " + texLocation);
                        e.printStackTrace();
                    }

                    Mtl mtl = Mtls.create(texName);
                    mtl.setMapKd(texLocation);
                    mtls.add(mtl);
                }

                float minU = atextureatlassprite[0].getMinU();
                float maxU = atextureatlassprite[0].getMaxU();
                float minV = atextureatlassprite[0].getMinV();
                float maxV = atextureatlassprite[0].getMaxV();
                obj.addVertex((float) xOffset, (float) yOffset, (float) zOffset + 1F);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(minU, maxV);
                obj.addVertex((float) xOffset, (float) yOffset, (float) zOffset);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(minU, minV);
                obj.addVertex((float) xOffset + 1F, (float) yOffset, (float) zOffset);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(maxU, minV);
                obj.addVertex((float) xOffset + 1F, (float) yOffset, (float) zOffset + 1F);
//                            obj.addNormal(0, 0, 0);
                obj.addTexCoord(maxU, maxV);
                obj.setActiveMaterialGroupName(texName);
                obj.addFaceWithTexCoords(4 * faceindex, 4 * faceindex + 1, 4 * faceindex + 2, 4 * faceindex + 3);
                faceindex += 1;
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
                    IBlockState lavaState = access.getBlockState(blockpos);

                    if (lavaState.getBlockFaceShape(access, blockpos, EnumFacing.VALUES[i1+2].getOpposite()) == BlockFaceShape.SOLID)
                    {
                        textureatlassprite1 = atlasSpriteWaterOverlay;
                    }
                }

                texHandler = new TextureHandler(textureatlassprite1);
                String texName = texHandler.getTextureName();
                if (!mtls.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {
                    BufferedImage texture;
                    try {
                        texture = texHandler.getBaseTextureImage(resourceManager);
                    } catch (IOException e) {
                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + aState.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        continue;
                    }
                    if (texture == null) continue;

                    String texLocation = "textures/" + texName + ".png";
                    try {
                        texHandler.save(texture, Paths.get("MineExporteR/" + texLocation));
                    } catch (IOException e) {
                        noError = false;
                        Main.logger.error(TextFormatting.RED + "failed to save texture image: " + texLocation);
                        e.printStackTrace();
                    }

                    Mtl mtl = Mtls.create(texName);
                    mtl.setMapKd(texLocation);
                    mtls.add(mtl);
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
                        d3 = xOffset;
                        d5 = xOffset + 1.0D;
                        d4 = zOffset + 0.0010000000474974513D;
                        d6 = zOffset + 0.0010000000474974513D;
                    }
                    else if (i1 == 1)
                    {
                        f39 = SEy;
                        f40 = SWy;
                        d3 = xOffset + 1.0D;
                        d5 = xOffset;
                        d4 = zOffset + 1.0D - 0.0010000000474974513D;
                        d6 = zOffset + 1.0D - 0.0010000000474974513D;
                    }
                    else if (i1 == 2)
                    {
                        f39 = SWy;
                        f40 = NWy;
                        d3 = xOffset + 0.0010000000474974513D;
                        d5 = xOffset + 0.0010000000474974513D;
                        d4 = zOffset + 1.0D;
                        d6 = zOffset;
                    }
                    else
                    {
                        f39 = NEy;
                        f40 = SEy;
                        d3 = xOffset + 1.0D - 0.0010000000474974513D;
                        d5 = xOffset + 1.0D - 0.0010000000474974513D;
                        d4 = zOffset;
                        d6 = zOffset + 1.0D;
                    }

                    float f41 = textureatlassprite1.getInterpolatedU(0.0D);
                    float f27 = textureatlassprite1.getInterpolatedU(8.0D);
                    float f28 = textureatlassprite1.getInterpolatedV((1.0F - f39) * 16.0F * 0.5F);
                    float f29 = textureatlassprite1.getInterpolatedV((1.0F - f40) * 16.0F * 0.5F);
                    float f30 = textureatlassprite1.getInterpolatedV(8.0D);
//                                int j = aState.getPackedLightmapCoords(access, blockpos);
//                                int k = j >> 16 & 65535;
//                                int l = j & 65535;
//                                float f31 = i1 < 2 ? 0.8F : 0.6F;

                    obj.addVertex((float) d3, (float) yOffset + f39, (float) d4);
//                                obj.addNormal(0, 0, 0);
                    obj.addTexCoord(f41, f28);
                    obj.addVertex((float) d5, (float) yOffset + f40, (float) d6);
//                                obj.addNormal(0, 0, 0);
                    obj.addTexCoord(f27, f29);
                    obj.addVertex((float) d5, (float) yOffset, (float) d6);
//                                obj.addNormal(0, 0, 0);
                    obj.addTexCoord(f27, f30);
                    obj.addVertex((float) d3, (float) yOffset, (float) d4);
//                                obj.addNormal(0, 0, 0);
                    obj.addTexCoord(f41, f30);
                    obj.setActiveMaterialGroupName(texName);
                    obj.addFaceWithTexCoords(4 * faceindex, 4 * faceindex + 1, 4 * faceindex + 2, 4 * faceindex + 3);
                    faceindex += 1;
                }
            }
        }
*/
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
