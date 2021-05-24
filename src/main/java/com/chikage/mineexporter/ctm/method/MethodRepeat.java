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
        int[] uvIndecies = ctx.getUVIndexes(this);
        int uIndex = uvIndecies[0] % width;
        int vIndex = uvIndecies[1] % height;

        return uIndex + vIndex*height;
    }

    @Override
    public String getMethodName() {
        return "repeat";
    }
}
