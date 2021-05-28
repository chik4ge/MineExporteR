package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

public class MethodRepeat extends CTMMethod{

    public int width;
    public int height;

    public MethodRepeat(String path, String propertyName) {
        super(path, propertyName);
    }

    @Override
    public int getTileIndex(CTMContext ctx) {
        int[] uvIndices = ctx.getUVIndexes(this);
        int uIndex = mod(uvIndices[0], width);
        int vIndex = mod(uvIndices[1], height);

        return uIndex + vIndex*height;
    }

    @Override
    public String getMethodName() {
        return "repeat";
    }

    private int mod(int n, int m) {
        int result = n%m;
        if (result < 0) result += m;
        return result;
    }
}
