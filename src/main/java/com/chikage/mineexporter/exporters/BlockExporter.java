package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.utils.ExportContext;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

public abstract class BlockExporter {
    public BlockExporter() {
    }

    protected Vec3d getOffset(IBlockAccess worldIn, IBlockState state, BlockPos pos, BlockPos origin) {
        return state.getOffset(worldIn, pos)
                .add(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
    }

    public abstract boolean export(ExportContext expCtx, IBlockState state, BlockPos pos);
}
