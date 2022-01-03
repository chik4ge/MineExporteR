package com.chikage.mineexporter.utils;

import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class Vertex {
    public float x;
    public float y;
    public float z;

    public Vertex(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex vertex = (Vertex) o;
        return Float.compare(vertex.x, x) == 0 && Float.compare(vertex.y, y) == 0 && Float.compare(vertex.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public Vec3d toVec3d() {
        return new Vec3d(x, y, z);
    }

    public void add(double dx, double dy, double dz) {
        x += dx;
        y += dy;
        z += dz;
    }
}
