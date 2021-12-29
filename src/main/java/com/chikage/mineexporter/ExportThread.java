package com.chikage.mineexporter;

import com.chikage.mineexporter.utils.*;
import de.javagl.obj.*;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
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

        Set<Vertex> vertices = new CopyOnWriteArraySet<>();
        Set<UV> uvs = new CopyOnWriteArraySet<>();
        Map<String, List<Face>> faces = new ConcurrentHashMap<>();

        Obj obj = Objs.create();
        CopyOnWriteArraySet<Mtl> mtls = new CopyOnWriteArraySet<>();

        Range range = new Range(pos1, pos2);

        ExportContext expCtx = new ExportContext(
                getMinecraft().getResourceManager(),
                getMinecraft().getResourcePackRepository(),
                getMinecraft().getBlockRendererDispatcher().getBlockModelShapes(),

                server.getWorld(dimensionId),

                range,

                vertices,
                uvs,
                faces
                );

//        Main.logger.info("creating ctm cache...");
//        CTMHandler ctmHandler = new CTMHandler(rpRep);
//        Main.logger.info("successfully created ctm cache.");

        IChunkProvider provider = ((WorldServer)expCtx.worldIn).getChunkProvider();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            Set<int[]> chunks = range.getChunks();
            Set<int[]> processed = new HashSet<>();

            while (!chunks.equals(processed)) {
                for (int[] chunkXZ : chunks) {
                    Chunk chunk = provider.getLoadedChunk(chunkXZ[0], chunkXZ[1]);
                    if (chunk != null && chunk.isLoaded()) {
                        executor.execute(new ExportChunk(expCtx, chunk));
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

        for (Map.Entry<String, List<Face>> facesOfMtl : faces.entrySet()) {
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
