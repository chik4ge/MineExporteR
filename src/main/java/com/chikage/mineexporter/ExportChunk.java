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
//        Main.logger.info("start import chunk at " + chunk.x +"," + chunk.z);

        Range chunkRange = Range.toRangeFromChunk(chunk);
        Range range = chunkRange.intersect(expCtx.range);

        Map<Texture, Set<float[][][]>> faces = new HashMap<>();

        for (BlockPos pos : range) {
            IBlockState state = chunk.getBlockState(pos).getActualState(chunk.getWorld(), pos);
            try {
                if (state.toString().equals("minecraft:air")) continue;

                BlockExporter exporter = null;

                switch (state.getRenderType()) {
                    case MODEL:
                        exporter = new ModelExporter(expCtx, state, pos);
                        break;
                    case LIQUID:
                        exporter = new LiquidExporter(expCtx, state, pos);
                        break;
                    case ENTITYBLOCK_ANIMATED:
                        break;
                    default:
                        continue;
                }

                if (exporter != null) {
                    exporter.export(faces);
                }
            } catch (Throwable e) {
                ChatHandler.sendErrorMessage("Error processing block "+ state + " at " + pos + ". this block will be ignored.");
                e.printStackTrace();
            }
        }

//        As much as possible, lump objects together in chunks to avoid accessing thread-safe objects.
        for (Map.Entry<Texture, Set<float[][][]>> entry : faces.entrySet()) {
            if (expCtx.faces.containsKey(entry.getKey())) {
                expCtx.faces.get(entry.getKey()).addAll(entry.getValue());
            } else {
                expCtx.faces.put(entry.getKey(), entry.getValue());
            }
        }
        expCtx.incProcessedBlocks(range.getSize());
//        Main.logger.info("finished import chunk at " + chunk.x +"," + chunk.z);
    }
}
