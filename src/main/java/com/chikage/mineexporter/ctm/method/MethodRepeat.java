package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

public class MethodRepeat extends CTMMethod{
    public MethodRepeat(String path) {
        super(path);
    }

    public int width;
    public int height;

    @Override
    public String getTile(CTMContext ctx) {
        int[] uvIndecies = ctx.getUVIndexes(this);
        int uIndex = uvIndecies[0] % width;
        int vIndex = uvIndecies[1] % height;

        return tiles.get(uIndex + vIndex*height);
    }
}
