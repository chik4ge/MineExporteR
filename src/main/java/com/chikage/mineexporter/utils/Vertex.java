package com.chikage.mineexporter.utils;

import java.util.Objects;

public class Vertex {
    public final float x;
    public final float y;
    public final float z;

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
}
