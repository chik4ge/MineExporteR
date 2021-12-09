package com.chikage.mineexporter.utils;

public class Face {
    public final Vertex[] vertex;
    public final UV[] uv;

    public Face(Vertex[] v, UV[] uv) {
        this.vertex = v;
        this.uv = uv;
    }
}
