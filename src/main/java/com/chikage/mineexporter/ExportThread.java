package com.chikage.mineexporter;

import com.chikage.mineexporter.utils.*;
import de.javagl.obj.*;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ExportThread implements Runnable {
    private BlockPos pos1;
    private BlockPos pos2;
    private World world;
    private ICommandSender commandSender;
    private Set<int[]> exportingChunks;
    private Set<int[]> unExportedChunks;
    private boolean isRunning = false;

    private ExportContext expCtx;

//    private final boolean isCTMSupport = true;

    public void setCommandSender(ICommandSender sender) {
        this.commandSender = sender;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2;
    }

    public void run(){
        long startTime = System.currentTimeMillis();
        try {
            if (!isPosSet()) {
                sendErrorMessage("set pos1 and pos2 first.");
                return;
            }
            if (isRunning) {
                sendErrorMessage("export process is already running!");
                return;
            }

            Main.logger.info("exporting from (" + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ() + ") to (" + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ() + ")");
            Range range = new Range(pos1, pos2);

            isRunning = true;
            initChunksData(range.getChunks());

//            delete texture file
            deleteFile(new File("MineExporteR/textures"));

            Set<float[]> vertices = new CopyOnWriteArraySet<>();
            Set<float[]> uvs = new CopyOnWriteArraySet<>();
            Map<Texture, Set<float[][][]>> faces = new ConcurrentHashMap<>();

            Obj obj = Objs.create();
            Set<Mtl> mtls = new CopyOnWriteArraySet<>();

            expCtx = new ExportContext(
                    getMinecraft().getResourceManager(),
                    getMinecraft().getResourcePackRepository(),
                    getMinecraft().getBlockRendererDispatcher().getBlockModelShapes(),

                    world,

                    range,

                    faces,
                    mtls
                    );

            IChunkProvider provider = ((World)expCtx.worldIn).getChunkProvider();

            Main.logger.info("start chunk loading");

            ExecutorService executor = Executors.newFixedThreadPool(6);
            Set<int[]> c = unExportedChunks;
            while (!c.isEmpty()) {
                Set<int[]> exportedChunks = new CopyOnWriteArraySet<>();
                for (int[] chunkXZ : c) {
                    Chunk chunk = provider.getLoadedChunk(chunkXZ[0], chunkXZ[1]);
                    if (chunk != null) {
                        exportedChunks.add(chunkXZ);
                        executor.execute(new ExportChunk(expCtx, chunk) {
                            @Override
                            public void run() {
                                int[] chunkXZ = new int[]{chunk.x, chunk.z};
                                exportingChunks.add(chunkXZ);
                                super.run();
                                exportingChunks.remove(chunkXZ);
                            }
                        });
                    }
                }
                c.removeAll(exportedChunks);
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            Main.logger.info("finished chunk loading");

//            float配列だとHashCodeが想定通りに動作しないためFloatBufferを使用
            HashMap<FloatBuffer, Integer> vertexIdMap = new HashMap<>();

            HashMap<FloatBuffer, Integer> uvIdMap = new HashMap<>();
            int vertexId = 0;
            int uvId = 0;
            Map<String, Set<float[][][]>> fixedFaces = mergeTextures(faces, mtls);
            for (Map.Entry<String, Set<float[][][]>> facesOfMtl : fixedFaces.entrySet()) {
                obj.setActiveMaterialGroupName(facesOfMtl.getKey());
                for (float[][][] face : facesOfMtl.getValue()) {
                    int[] vertexIndices = new int[4];
                    int[] uvIndices = new int[4];
                    for (int i=0; i<4; i++) {
                        FloatBuffer vertex = FloatBuffer.wrap(face[i][0]);
                        if (!vertexIdMap.containsKey(vertex)) {
                            obj.addVertex(vertex.get(0), vertex.get(1), vertex.get(2));
                            vertexIdMap.put(vertex, vertexId);
                            vertexId++;
                        }
                        vertexIndices[i] = vertexIdMap.get(vertex);

                        FloatBuffer uv = FloatBuffer.wrap(face[i][1]);
                        if (!uvIdMap.containsKey(uv)) {
                            obj.addTexCoord(uv.get(0), uv.get(1));
                            uvIdMap.put(uv, uvId);
                            uvId++;
                        }
                        uvIndices[i] = uvIdMap.get(uv);
                    }
                    obj.addFace(vertexIndices, uvIndices, null);
                }
            }

            File objFile = new File("MineExporteR/export.obj");
            File mtlFile = new File("MineExporteR/export.mtl");
            obj.setMtlFileNames(Collections.singletonList("export.mtl"));

            OutputStream objOutput = new BufferedOutputStream(new FileOutputStream(objFile));
            OutputStream mtlOutput = new BufferedOutputStream(new FileOutputStream(mtlFile));
            ObjWriter.write(obj, objOutput);
            MtlWriter.write(mtls, mtlOutput);
            mtlOutput.close();
            objOutput.close();
        } catch (Exception e) {
            sendErrorMessage("something went wrong! see latest.log");
            e.printStackTrace();
            return;
        } finally {
            isRunning = false;
            unExportedChunks = null;
        }
        sendSuccessMessage("successfully exported.");

        long endTime = System.currentTimeMillis();
        sendSuccessMessage("elapsed " + (endTime-startTime)/1000.0 + "s");
    }

    private Map<String, Set<float[][][]>> mergeTextures(Map<Texture, Set<float[][][]>> faces, Set<Mtl> mtls){
        Map<String, Set<float[][][]>> result = new HashMap<>();
        Map<String, Set<Texture>> texturesForMtl = new HashMap<>();

        for (Texture texture : faces.keySet()) {
            texturesForMtl.putIfAbsent(texture.getId(), new HashSet<>(Arrays.asList(texture)));

            Set<Texture> textures = texturesForMtl.get(texture.getId());
            textures.add(texture);
        }

        for (Map.Entry<String, Set<Texture>> e : texturesForMtl.entrySet()) {
            String mtlName = e.getKey();
            Set<Texture> textures = e.getValue();

            List<Texture> sortedTextures = textures.stream()
                    .sorted(
                            Comparator.comparing(Texture::getCTMName)
                                    .thenComparing(Texture::getCTMIndex)
                                    .thenComparing(Texture::getTintLuminance))
                    .collect(Collectors.toList());

            Set<float[][][]> facesForMtl = new HashSet<>();
            result.put(mtlName, facesForMtl);

            int texWidth = 16;
            int texHeight = 16;
            int mergedWidth = 16;
            int mergedHeight = 16;
            int COLUMN_NUM = 4;

            BufferedImage image = null;
            int texNum = textures.size();

            int i = 0;
            for (Texture texture : sortedTextures) {
                ResourceLocation baseLocation = texture.getBaseTexLocation();
                ResourceLocation location = new ResourceLocation(baseLocation.getNamespace(), "textures/"+ baseLocation.getPath()+".png");

                BufferedImage baseImage = null;
                try {
                    baseImage = TextureHandler.fetchImageCopy(expCtx.rm, location);
                } catch (IOException ioException) {
                    continue;
                }
                if (baseImage == null) continue;

                if (texture.getTextureType() == Texture.TextureType.CTM) {
                    try {
                        TextureHandler.setConnectedImage(baseImage, expCtx.rm, expCtx.ctmHandler, texture.getId(), texture.getCTMIndex());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        continue;
                    }
                }
                if (texture.getTintColor() != -1) {
                    TextureHandler.setColormapToImage(baseImage, texture.getTintColor());
                }

                int column = i % COLUMN_NUM;
                int row    = i / COLUMN_NUM;
                if (i == 0) {
                    texWidth = baseImage.getWidth();
                    texHeight = baseImage.getHeight();
//                    TODO 2のべき乗になるよう調整
                    mergedWidth = texWidth * (texNum/COLUMN_NUM + 1);
                    mergedHeight = texHeight * (Math.min(texNum, COLUMN_NUM));

                    image = new BufferedImage(mergedWidth, mergedHeight, baseImage.getType());
                }
                TextureHandler.pasteImage(row*texWidth, column*texHeight, baseImage, image);

                Set<float[][][]> rawFaces = faces.get(texture);

                for (float[][][] rawFace : rawFaces) {
                    for (int j = 0; j < 4; j++) {
                        float u = rawFace[j][1][0];
                        float v = rawFace[j][1][1];
//                        texwidth = 16 ; u = 1.0 ; mergedWidth = 48 ; row = 0 -> 0.5
//                        texHeight = 16, v = 1.0, mergedWidrh = 16, column = 0 -> 1.0
                        rawFace[j][1][0] = MathHandler.round((u + row) * texWidth  / mergedWidth , mergedWidth);
                        rawFace[j][1][1] = MathHandler.round(((v-column-1) * texHeight + mergedHeight) / mergedHeight, mergedHeight);
                    }
                }

                facesForMtl.addAll(rawFaces);
                i++;
            }

            String texLocation = "textures/" + mtlName.replace(":", "/") + ".png";
            try {
                TextureHandler.save(image, Paths.get("MineExporteR/" + texLocation));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            Mtl mtl = Mtls.create(mtlName);
            mtl.setMapKd(texLocation);
            mtls.add(mtl);
        }
        return result;
    }

    private void initChunksData(Set<int[]> initVal) {
        unExportedChunks = initVal;
        exportingChunks = new CopyOnWriteArraySet<>();
    }

    public Set<int[]> getUnExportedChunks() {
        return unExportedChunks;
    }

    public Set<int[]> getExportingChunks() {
        return exportingChunks;
    }

    private void sendMessage(TextFormatting tf, String s) {
        if (this.commandSender != null) {
            commandSender.sendMessage(new TextComponentString(tf + s));
        }
    }

    private void sendErrorMessage(String s) {
        sendMessage(TextFormatting.RED, s);
    }

    private void sendSuccessMessage(String s) {
        sendMessage(TextFormatting.GREEN, s);
    }

    private void deleteFile(File f) {
        if (!f.exists()) return;
        if (f.isFile()) f.delete();
        else if (f.isDirectory()) {
            File[] files = f.listFiles();
            assert files != null;
            for (File cFile: files) {
                deleteFile(cFile);
            }
            f.delete();
        }
    }

    public int getProgressPercent() {
        if (!isRunning || expCtx == null) return -1;

        long size = expCtx.range.getSize();
        long processed = expCtx.getProcessedBlocks();
        return (int)(100*processed/size);
    }

    public boolean isPosSet() {
        return this.pos1 != null && this.pos2 != null;
    }
}
