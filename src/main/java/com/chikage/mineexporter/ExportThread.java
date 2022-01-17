package com.chikage.mineexporter;

import com.chikage.mineexporter.utils.*;
import de.javagl.obj.*;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ExportThread implements Runnable {
    private BlockPos pos1;
    private BlockPos pos2;
    private World world;
    private ICommandSender commandSender;
    private Set<int[]> chunks;
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
            setUnExportedChunks(range.getChunks());

//            delete texture file
            deleteFile(new File("MineExporteR/textures"));

            Set<Vertex> vertices = new CopyOnWriteArraySet<>();
            Set<UV> uvs = new CopyOnWriteArraySet<>();
            Map<String, Set<Face>> faces = new ConcurrentHashMap<>();

            Obj obj = Objs.create();
            Set<Mtl> mtls = new CopyOnWriteArraySet<>();

            expCtx = new ExportContext(
                    getMinecraft().getResourceManager(),
                    getMinecraft().getResourcePackRepository(),
                    getMinecraft().getBlockRendererDispatcher().getBlockModelShapes(),

                    world,

                    range,

                    vertices,
                    uvs,
                    faces,
                    mtls
                    );

            IChunkProvider provider = ((World)expCtx.worldIn).getChunkProvider();

            Main.logger.info("start chunk loading");

            ExecutorService executor = Executors.newFixedThreadPool(8);
            Set<int[]> c = getUnExportedChunks();
            while (!c.isEmpty()) {
                Iterator<int[]> it = c.iterator();
                while (it.hasNext()) {
                    int[] chunkXZ = it.next();
                    Chunk chunk = provider.getLoadedChunk(chunkXZ[0], chunkXZ[1]);
                    if (chunk != null) {
                        executor.execute(new ExportChunk(expCtx, chunk));
                        it.remove();
                    }
                }
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            Main.logger.info("finished chunk loading");

            HashMap<Vertex, Integer> vertexIdMap = new HashMap<>();

            HashMap<UV, Integer> uvIdMap = new HashMap<>();
            int vertexId = 0;
            int uvId = 0;
            for (Map.Entry<String, Set<Face>> facesOfMtl : faces.entrySet()) {
                obj.setActiveMaterialGroupName(facesOfMtl.getKey());
                for (Face face : facesOfMtl.getValue()) {
                    int[] vertexIndices = new int[4];
                    int[] uvIndices = new int[4];
                    for (int i=0; i<4; i++) {
                        Vertex vertex = face.vertex[i];
                        if (!vertexIdMap.containsKey(vertex)) {
                            obj.addVertex(vertex.x, vertex.y, vertex.z);
                            vertexIdMap.put(vertex, vertexId);
                            vertexId++;
                        }
                        vertexIndices[i] = vertexIdMap.get(vertex);

                        UV uv = face.uv[i];
                        if (!uvIdMap.containsKey(uv)) {
                            obj.addTexCoord(uv.u, uv.v);
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
            setUnExportedChunks(null);
        }
        sendSuccessMessage("successfully exported.");
    }

    public synchronized Set<int[]> getUnExportedChunks() {
        return chunks;
    }

    private synchronized void setUnExportedChunks(Set<int[]> s) {
        chunks = s;
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
