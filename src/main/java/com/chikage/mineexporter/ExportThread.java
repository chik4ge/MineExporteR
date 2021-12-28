package com.chikage.mineexporter;

import com.chikage.mineexporter.ctm.CTMHandler;
import com.chikage.mineexporter.utils.Face;
import com.chikage.mineexporter.utils.Range;
import com.chikage.mineexporter.utils.UV;
import com.chikage.mineexporter.utils.Vertex;
import de.javagl.obj.*;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ExportThread extends Thread {
    private final MinecraftServer server;
    private final ICommandSender sender;
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final int dimensionId;

    private final boolean isCTMSupport = true;

    public ExportThread(MinecraftServer server, ICommandSender sender, BlockPos pos1, BlockPos pos2) {
        this.sender = sender;
        this.server = server;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.dimensionId = sender.getEntityWorld().provider.getDimension();
    }

    public void run() {
//        long startTime = System.currentTimeMillis();
        long countStart = System.currentTimeMillis();
        boolean noError = true;
        Main.logger.info("exporting from (" + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ() + ") to (" + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ() + ")");

//        delete texture file
        deleteFile(new File("MineExporteR/textures"));

        IResourceManager resourceManager = getMinecraft().getResourceManager();
        ResourcePackRepository rpRep = getMinecraft().getResourcePackRepository();
        BlockModelShapes bms = getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
        Obj obj = Objs.create();
        CopyOnWriteArraySet<Mtl> mtls = new CopyOnWriteArraySet<>();
        int faceindex = 0;

        Range range = new Range(pos1, pos2);

        Main.logger.info("creating ctm cache...");
        CTMHandler ctmHandler = new CTMHandler(rpRep);
        Main.logger.info("successfully created ctm cache.");

        IChunkProvider provider = server.getWorld(dimensionId).getChunkProvider();
        CopyOnWriteArraySet<Vertex> vertices = new CopyOnWriteArraySet<>();
        CopyOnWriteArraySet<UV> uvs = new CopyOnWriteArraySet<>();
        ConcurrentHashMap<String, ArrayList<Face>> faces = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            Set<int[]> processed = new HashSet<>();
            while (!range.chunks.equals(processed)) {
                for (int[] chunkXZ : range.chunks) {
                    Chunk chunk = provider.getLoadedChunk(chunkXZ[0], chunkXZ[1]);
                    if (chunk != null && chunk.isLoaded()) {
                        executor.execute(new ExportChunk(range, chunk, vertices, uvs, faces));
                        processed.add(chunkXZ);
                    }
                }
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HashMap<Vertex, Integer> vertexIdMap = new HashMap<>();
        int id = 0;
        for (Vertex vertex : vertices) {
            if (!vertexIdMap.containsKey(vertex)) {
                obj.addVertex(vertex.x, vertex.y, vertex.z);
                vertexIdMap.put(vertex, id);
                id++;
            }
        }

        HashMap<UV, Integer> uvIdMap = new HashMap<>();
        id = 0;
        for (UV uv : uvs) {
            if (!uvIdMap.containsKey(uv)) {
                obj.addTexCoord(uv.u, uv.v);
                uvIdMap.put(uv, id);
                id++;
            }
        }

        for (Map.Entry<String, ArrayList<Face>> facesOfMtl : faces.entrySet()) {
            obj.setActiveMaterialGroupName(facesOfMtl.getKey());
            for (Face face : facesOfMtl.getValue()) {
                int[] vertexIndices = new int[4];
                int[] uvIndices = new int[4];
                for (int i=0; i<4; i++) {
                    vertexIndices[i] = vertexIdMap.get(face.vertex[i]);
                    uvIndices[i] = uvIdMap.get(face.uv[i]);
                }
                obj.addFace(vertexIndices, uvIndices, null);
            }
        }

        /*long blockIndex = 0;
        for (BlockPos pos: range) {
            blockIndex++;

            if (System.currentTimeMillis() - countStart > 5000) {
                countStart = System.currentTimeMillis();
                sender.sendMessage(new TextComponentString("processed " + blockIndex + "/" + range.getSize() +  " blocks (" + (100*blockIndex)/range.getSize() + "%)"));
            }

            IBlockState state = sender.getEntityWorld().getBlockState(pos);

            if (state.toString().equals("minecraft:air")) continue;

            IBlockState aState = state.getActualState(sender.getEntityWorld(), pos);

            IBakedModel model = bms.getModelForState(aState);

            Vec3d offset = aState.getOffset(sender.getEntityWorld(), pos);

            double xOffset = pos.getX() - range.getMinX() + offset.x;
            double yOffset = pos.getY() - range.getMinY() + offset.y;
            double zOffset = pos.getZ() - range.getMinZ() + offset.z;

    //            TODO TileEntityは正常に描画されない
            switch (aState.getRenderType()) {
                case MODEL: {
                    for (EnumFacing facing : ArrayUtils.addAll(EnumFacing.VALUES, new EnumFacing[]{null})) {
                        if (facing != null && !aState.shouldSideBeRendered(sender.getEntityWorld(), pos, facing)) continue;

                        for (BakedQuad quad : model.getQuads(aState, facing, 0)) {
                            CTMContext ctx = new CTMContext(sender.getEntityWorld(), quad, pos);
                            TextureHandler texHandler = new TextureHandler(quad.getSprite(), ctmHandler, ctx);

                            String texName = texHandler.getTextureName();

                            BufferedImage texture;
                            int tintRGB = -1;

                            String ctmName = texHandler.getCTMName();
                            if (!ctmName.equals("none")) texName += "-" + ctmName;

//                        TODO colormap実装
//                    Biome biome = sender.getEntityWorld().getBiome(pos);
//                    float temperature = biome.getTemperature(pos);
//                    float rainfall = biome.getRainfall();
//
//                    float adjTemp = Math.max(Math.min(temperature, 1F), 0F);
//                    float adjRainfall = Math.max(Math.min(rainfall, 1F), 0F)*adjTemp;

                            if (quad.hasTintIndex()) {
                                tintRGB = getMinecraft().getBlockColors().colorMultiplier(aState, sender.getEntityWorld(), pos, quad.getTintIndex());
                                texName += "-" + Integer.toHexString(tintRGB);
                            }
//                    TODO 草の側面が正しく描画されない、ctmのoverlayの様子も見ながら実装
//                    普通にあとからレンダリングされてるからレイヤーっぽく見えるだけだった
//                    同じブロック内で重なる箇所があり、かつレンダリング方法がCUTOUT系ならその面についてテクスチャをまとめる処理を入れる

                            if (!mtls.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {

                                try {
                                    texture = texHandler.getBaseTextureImage(resourceManager);
                                } catch (IOException e) {
                                    noError = false;
                                    Main.logger.error("failed to find texture image. block: " + aState.getBlock().getRegistryName().toString());
                                    e.printStackTrace();
                                    continue;
                                }
                                if (texture == null) continue;

                                try {
                                    if (!ctmName.equals("none")) texHandler.setConnectedImage(resourceManager, texture, ctmHandler);
                                } catch (IOException | ArrayIndexOutOfBoundsException e) {
                                    noError = false;
                                    Main.logger.error("failed to find ctm image. block: " + aState.getBlock().getRegistryName().toString());
                                    e.printStackTrace();
                                }

                                if (tintRGB != -1) {
                                    texHandler.setColormapToImage(texture, tintRGB);
                                }

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

                            int textureWidth = texHandler.getTextureWidth();
                            int textureHeight = texHandler.getTextureHeight();
                            int[] vData = quad.getVertexData();

                            //            VertexDataの構造覚え書き
                            //            基本的に28要素のint配列で保存
                            //            1頂点あたり7要素(全部で7*32bitの情報を保持)し、それが4つある
                            //            以下インデックスごとの要素
                            //            0...x座標(Float)
                            //            1...y座標(Float)
                            //            2...z座標(Float)
                            //            3...頂点色(rgba)(Byte*4)
                            //            4...U座標(Float)
                            //            5...V座標(Float)
                            //            6...法線ベクトル(Byte*3) + あまり8bit
                            for (int i = 0; i < 4; i++) {
                                int index = i * 7;

                                float x = (float) (Float.intBitsToFloat(vData[index]) + xOffset);
                                float y = (float) (Float.intBitsToFloat(vData[index + 1]) + yOffset);
                                float z = (float) (Float.intBitsToFloat(vData[index + 2]) + zOffset);

                                float u = (float) Math.round((quad.getSprite().getUnInterpolatedU((float) ((Float.intBitsToFloat(vData[index + 4]) - Float.intBitsToFloat(vData[(index+14)%21 + 4])*.001)/.999))/16.0)*textureWidth)/textureWidth;
                                float v = 1F-(float) Math.round((quad.getSprite().getUnInterpolatedV((float) ((Float.intBitsToFloat(vData[index + 5]) - Float.intBitsToFloat(vData[(index+14)%21 + 5])*.001)/.999))/16.0)*textureHeight)/textureHeight;

//                                int nv = vData[index + 6];
//                                float nx = (byte) ((nv) & 0xFF) / 127.0F;
//                                float ny = (byte) ((nv >> 8) & 0xFF) / 127.0F;
//                                float nz = (byte) ((nv >> 16) & 0xFF) / 127.0F;

                                obj.addVertex(x, y, z);
                                obj.addTexCoord(u, v);
//                                obj.addNormal(nx, ny, nz);
                            }

                            obj.setActiveMaterialGroupName(texName);
                            obj.addFaceWithTexCoords(4 * faceindex, 4 * faceindex + 1, 4 * faceindex + 2, 4 * faceindex + 3);
                            faceindex += 1;
                        }
                    }
                    break;
                }
                case LIQUID: {
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

                    break;
                }
                case ENTITYBLOCK_ANIMATED: {

                }
            }

        }*/

        File objFile = new File("MineExporteR/export.obj");
        File mtlFile = new File("MineExporteR/export.mtl");
        obj.setMtlFileNames(Collections.singletonList("export.mtl"));
        try {
            OutputStream objOutput = new BufferedOutputStream(new FileOutputStream(objFile));
            OutputStream mtlOutput = new BufferedOutputStream(new FileOutputStream(mtlFile));
            ObjWriter.write(obj, objOutput);
            MtlWriter.write(mtls, mtlOutput);
            mtlOutput.close();
            objOutput.close();
        } catch (FileNotFoundException e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "failed to find output file."));
            e.printStackTrace();
            return;
        } catch (IOException e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "failed to write output file."));
            e.printStackTrace();
            return;
        }
        if (noError) {
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "successfully exported."));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "exported with some error. see the latest.log."));
        }

//        long endTime = System.currentTimeMillis();
//        sender.sendMessage(new TextComponentString("elapsed " + (endTime-startTime)/1000.0 + "s"));
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

    private void deleteFile(File f) {
        if (!f.exists()) return;
        if (f.isFile()) f.delete();
        else if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File cFile: files) {
                deleteFile(cFile);
            }
            f.delete();
        }
    }
}
