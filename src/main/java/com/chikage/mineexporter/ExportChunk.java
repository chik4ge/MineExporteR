package com.chikage.mineexporter;

import com.chikage.mineexporter.exporters.BlockExporter;
import com.chikage.mineexporter.exporters.LiquidExporter;
import com.chikage.mineexporter.exporters.ModelExporter;
import com.chikage.mineexporter.utils.ExportContext;
import com.chikage.mineexporter.utils.Range;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

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

        for (BlockPos pos : range) {
            expCtx.incProcessedBlocks();
            IBlockState state = chunk.getBlockState(pos).getActualState(chunk.getWorld(), pos);
            if (state.toString().equals("minecraft:air")) continue;

            BlockExporter exporter = null;

            switch (state.getRenderType()) {
                case MODEL:
                    exporter = new ModelExporter();
                    break;
                case LIQUID:
                    exporter = new LiquidExporter();
                    break;
                case ENTITYBLOCK_ANIMATED: break;
                default: continue;
            }

            if (exporter != null) {
                exporter.export(expCtx, state, pos);
            }
        }
        Main.logger.info("finished import chunk at " + chunk.x +"," + chunk.z);
    }
}
