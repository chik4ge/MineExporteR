package com.chikage.mineexporter;

import com.chikage.mineexporter.exporters.BlockExporter;
import com.chikage.mineexporter.exporters.LiquidExporter;
import com.chikage.mineexporter.exporters.ModelExporter;
import com.chikage.mineexporter.utils.*;
import de.javagl.obj.Mtl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class ExportChunk implements Runnable{

    private final ExportContext expCtx;
    private final Chunk chunk;

    public ExportChunk(
            ExportContext expCtx,
            Chunk chunk) {
        super();

        this.expCtx = expCtx;
        this.chunk = chunk;
    }

    @Override
    public void run() {
        Main.logger.info("start import chunk at " + chunk.x +"," + chunk.z);

        Range chunkRange = Range.toRangeFromChunk(chunk);
        Range range = chunkRange.intersect(expCtx.range);

        Set<Vertex> vertices = new HashSet<>();
        Set<UV> uvs = new HashSet<>();
        Map<String, Set<Face>> faces = new HashMap<>();
        Set<Mtl> mtls = new HashSet<>();

        for (BlockPos pos : range) {
            IBlockState state = chunk.getBlockState(pos).getActualState(chunk.getWorld(), pos);
            if (state.toString().equals("minecraft:air")) continue;

            BlockExporter exporter = null;

            switch (state.getRenderType()) {
                case MODEL:
                    exporter = new ModelExporter(expCtx, state, pos);
                    break;
                case LIQUID:
                    exporter = new LiquidExporter(expCtx, state, pos);
                    break;
                case ENTITYBLOCK_ANIMATED: break;
                default: continue;
            }

            if (exporter != null) {
                exporter.export(vertices, uvs, faces, mtls);
            }
        }

//        できる限りスレッドセーフなオブジェクトにはアクセスしないようチャンクごとにまとめて処理
        expCtx.vertices.addAll(vertices);
        expCtx.uvs.addAll(uvs);
        expCtx.faces.putAll(faces);
        expCtx.mtls.addAll(mtls);
        expCtx.incProcessedBlocks(range.getSize());
        Main.logger.info("finished import chunk at " + chunk.x +"," + chunk.z);
    }
}
