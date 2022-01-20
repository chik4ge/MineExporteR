package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.utils.ExportContext;
import de.javagl.obj.Mtl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

import java.util.Map;
import java.util.Set;

public abstract class BlockExporter {
    protected ExportContext expCtx;
    protected IBlockState state;
    protected BlockPos pos;
    public BlockExporter(ExportContext expCtx, IBlockState state, BlockPos pos) {
        this.expCtx = expCtx;
        this.state = state;
        this.pos = pos;
    }

    protected float[] getOffset(IBlockAccess worldIn, IBlockState state, BlockPos pos, BlockPos origin) {
        Vec3d offset = state.getOffset(worldIn, pos)
                .add(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());

        return new float[]{(float)offset.x, (float)offset.y, (float)offset.z};
    }

    public abstract boolean export(Map<String, Set<float[][][]>> faces, Set<Mtl> mtls);
}
