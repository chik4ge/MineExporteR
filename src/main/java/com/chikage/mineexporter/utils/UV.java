package com.chikage.mineexporter.utils;

import java.util.Objects;

public class UV {
    public final float u;
    public final float v;

    public UV(float u, float v) {
        this.u = u;
        this.v = v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UV uv = (UV) o;
        return Float.compare(uv.u, u) == 0 && Float.compare(uv.v, v) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, v);
    }
}
