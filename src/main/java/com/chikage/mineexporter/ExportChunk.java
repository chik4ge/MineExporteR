package com.chikage.mineexporter;

import com.chikage.mineexporter.exporters.BlockExporter;
import com.chikage.mineexporter.exporters.LiquidExporter;
import com.chikage.mineexporter.exporters.ModelExporter;
import com.chikage.mineexporter.utils.ExportContext;
import com.chikage.mineexporter.utils.Range;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

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
                case ENTITYBLOCK_ANIMATED:
                    if (state.getBlock() instanceof BlockBed) {

                    } else if (state.getBlock() instanceof BlockChest) {

                    } else if (state.getBlock() instanceof BlockEnderChest) {

                    } else if (state.getBlock() instanceof BlockShulkerBox) {

                    }
                    break;
                default: continue;
            }

            if (exporter != null) {
                exporter.export(expCtx, state, pos);
            }
        }

        Vec3i dim = range.calcDimension();
        AxisAlignedBB aabb = new AxisAlignedBB(
                range.getOrigin().getX(),
                range.getOrigin().getY(),
                range.getOrigin().getZ(),
                range.getOrigin().getX() + dim.getX(),
                range.getOrigin().getY() + dim.getY(),
                range.getOrigin().getZ() + dim.getZ()
        );
        List<Entity> entities = ((World) expCtx.worldIn).getEntitiesWithinAABB(Entity.class, aabb);
        for (Entity entity : entities) {
            if (entity instanceof EntityPlayer) continue;
//            if (entity instanceof ...) {}
        }
        Main.logger.info("finished import chunk at " + chunk.x +"," + chunk.z);
    }
}
