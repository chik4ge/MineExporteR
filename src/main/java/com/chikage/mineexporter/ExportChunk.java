package com.chikage.mineexporter;

import com.chikage.mineexporter.utils.Face;
import com.chikage.mineexporter.utils.UV;
import com.chikage.mineexporter.utils.Vertex;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ExportChunk implements Runnable{

    Chunk chunk;
    CopyOnWriteArraySet<Vertex> vertices;
    CopyOnWriteArraySet<UV> uvs;
    ConcurrentHashMap<String, ArrayList<Face>> faces;

    public ExportChunk(
            Chunk chunk,
            CopyOnWriteArraySet<Vertex> vertices,
            CopyOnWriteArraySet<UV> uvs,
            ConcurrentHashMap<String, ArrayList<Face>> faces) {
        super();

        this.chunk = chunk;
        this.faces = faces;
        this.uvs = uvs;
        this.vertices = vertices;
    }

    @Override
    public void run() {
        Main.logger.info("start import chunk at " + chunk.x +"," + chunk.z);
    }
}
