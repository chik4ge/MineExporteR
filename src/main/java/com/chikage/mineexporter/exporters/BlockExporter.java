package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.utils.ExportContext;
import com.chikage.mineexporter.utils.Face;
import com.chikage.mineexporter.utils.UV;
import com.chikage.mineexporter.utils.Vertex;
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

    protected Vec3d getOffset(IBlockAccess worldIn, IBlockState state, BlockPos pos, BlockPos origin) {
        return state.getOffset(worldIn, pos)
                .add(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
    }

    public abstract boolean export(Set<Vertex> vertices, Set<UV> uvs, Map<String, Set<Face>> faces, Set<Mtl> mtls);
}
