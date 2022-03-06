package com.chikage.mineexporter.utils;

import java.util.Arrays;

public class FloatArrayWrapper {
    private float[] data;

    public FloatArrayWrapper(float[] data) {
        this.data = data.clone();
    }

    public boolean equals(Object other) {
        if (other instanceof FloatArrayWrapper) {
            return Arrays.equals(data, ((FloatArrayWrapper) other).data);
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
