package com.chikage.mineexporter;

import com.chikage.mineexporter.exporters.BlockExporter;
import com.chikage.mineexporter.exporters.LiquidExporter;
import com.chikage.mineexporter.exporters.ModelExporter;
import com.chikage.mineexporter.utils.Face;
import com.chikage.mineexporter.utils.Range;
import com.chikage.mineexporter.utils.UV;
import com.chikage.mineexporter.utils.Vertex;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ExportChunk implements Runnable{

    Range range;
    Chunk chunk;
    CopyOnWriteArraySet<Vertex> vertices;
    CopyOnWriteArraySet<UV> uvs;
    ConcurrentHashMap<String, ArrayList<Face>> faces;

    public ExportChunk(
            Range range,
            Chunk chunk,
            CopyOnWriteArraySet<Vertex> vertices,
            CopyOnWriteArraySet<UV> uvs,
            ConcurrentHashMap<String, ArrayList<Face>> faces) {
        super();

        this.range = range;
        this.chunk = chunk;
        this.faces = faces;
        this.uvs = uvs;
        this.vertices = vertices;
    }

    @Override
    public void run() {
        Main.logger.info("start import chunk at " + chunk.x +"," + chunk.z);

        for (int x=Math.max(chunk.x, range.getMinX()); x<=Math.min(chunk.x, range.getMaxX()); x++) {
            for (int z=Math.max(chunk.z, range.getMinZ()); z<=Math.min(chunk.z, range.getMaxZ()); z++) {
                for (int y=range.getMinY(); y<=range.getMaxY(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    IBlockState state = chunk.getBlockState(pos).getActualState(chunk.getWorld(), pos);
                    if (state.toString().equals("minecraft:air")) continue;

                    Vec3d offset = state.getOffset(chunk.getWorld(), pos)
                            .add(x - range.getMinX(), y - range.getMinY(), z - range.getMinZ());

                    BlockExporter exporter = null;

                    switch (state.getRenderType()) {
                        case MODEL:
                            exporter = new ModelExporter(state, offset);
                            break;
                        case LIQUID:
                            exporter = new LiquidExporter(state, offset);
                            break;
                        case ENTITYBLOCK_ANIMATED: break;
                        default: continue;
                    }

                    if (exporter != null) {
                        exporter.export(vertices, uvs, faces);
                    }
                }
            }
        }
    }
}
