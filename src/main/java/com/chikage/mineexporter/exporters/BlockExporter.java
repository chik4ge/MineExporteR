package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.utils.Face;
import com.chikage.mineexporter.utils.UV;
import com.chikage.mineexporter.utils.Vertex;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class BlockExporter {
    private IBlockState state;
    private Vec3d offset;

    public BlockExporter(IBlockState state, Vec3d offset) {
        this.state = state;
        this.offset = offset;
    }

    public abstract boolean export(
            CopyOnWriteArraySet<Vertex> vertices,
            CopyOnWriteArraySet<UV> uvs,
            ConcurrentHashMap<String, ArrayList<Face>> faces);
}
