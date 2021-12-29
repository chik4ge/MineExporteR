package com.chikage.mineexporter.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.function.Consumer;

public class Range implements Iterator<BlockPos>, Iterable<BlockPos> {
    int minX;
    int maxX;
    int minY;
    int maxY;
    int minZ;
    int maxZ;

    long size;
    BlockPos index;

    public Range(BlockPos pos1, BlockPos pos2) {
        minX = Math.min(pos1.getX(), pos2.getX());
        maxX = Math.max(pos1.getX(), pos2.getX());

        minY = Math.min(pos1.getY(), pos2.getY());
        maxY = Math.max(pos1.getY(), pos2.getY());

        minZ = Math.min(pos1.getZ(), pos2.getZ());
        maxZ = Math.max(pos1.getZ(), pos2.getZ());

        size = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        index = new BlockPos(minX, minY, minZ);
    }

    public Set<int[]> getChunks() {
        Set<int[]> res = new HashSet();
        for (int i=(minX>>4); i<=(maxX>>4); i++) {
            for (int j=(minZ>>4); j<=(maxZ>>4); j++) {
                res.add(new int[]{i,j});
            }
        }
        return res;
    }

//    TODO implement
    public Range intersect(Range inRange) {
        return null;
    }

//    TODO implement
    public static Range toRangeFromChunk(Chunk chunk) {
        return null;
    }

    @Override
    public boolean hasNext() {
        return (index.getX() <= maxX) && (index.getY() <= maxY) && (index.getZ() <= maxZ);
    }

    private BlockPos proc(BlockPos pos) {
        if (pos.getX() < maxX) {
            return pos.add(1, 0, 0);
        } else if (pos.getY() < maxY){
            return pos.add(minX-maxX, 1, 0);
        } else {
            return pos.add(minX-maxX, minY-maxY, 1);
        }
    }

    @Override
    public BlockPos next() {
        if ((index.getX() > maxX) || (index.getY() > maxY) || (index.getZ() > maxZ)) {
            throw new NoSuchElementException();
        }
        BlockPos cursor = new BlockPos(index.getX(), index.getY(), index.getZ());

        index = proc(cursor);
        return cursor;
    }

    @Override
    public void forEachRemaining(Consumer<? super BlockPos> action) {
        Iterator.super.forEachRemaining(action);
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return this;
    }

    public BlockPos getOrigin() {
        return new BlockPos(minX, minY, minZ);
    }

    public long getSize() {
        return size;
    }
}
