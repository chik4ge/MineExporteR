package com.chikage.mineexporter.utils;

import java.util.Arrays;

public class Face {
    public final Vertex[] vertex;
    public final UV[] uv;

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
}
