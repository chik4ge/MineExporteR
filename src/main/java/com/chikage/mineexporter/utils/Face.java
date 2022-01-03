package com.chikage.mineexporter.utils;

import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Face {
    public Vertex[] vertex;
    public UV[] uv;

    public Face(Vertex[] v, UV[] uv) {
        this.vertex = v;
        this.uv = uv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Face face = (Face) o;
        return Arrays.equals(vertex, face.vertex) && Arrays.equals(uv, face.uv);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(vertex);
        result = 31 * result + Arrays.hashCode(uv);
        return result;
    }

    public boolean hasSameVertex(Face other) {
        Set<Vertex> v1 = new HashSet<>(Arrays.asList(this.vertex));
        Set<Vertex> v2 = new HashSet<>(Arrays.asList(other.vertex));

        return v1.equals(v2);
    }

    public Vec3d getNormal() {
        Vec3d v1 = vertex[1].toVec3d().subtract(vertex[0].toVec3d());
        Vec3d v2 = vertex[3].toVec3d().subtract(vertex[0].toVec3d());

        return v1.crossProduct(v2).normalize();
    }

    public void moveTo(Vec3d vec, double len) {
        for (Vertex v : vertex) {
            v.add(vec.x*len, vec.y*len, vec.z*len);
        }
    }
}
