package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

import java.util.Random;

public class MethodRandom extends CTMMethod{
    public MethodRandom(String path, String propertyName) {
        super(path, propertyName);
    }

    @Override
    public int getTileIndex(CTMContext ctx) {
        Random rand = new Random();
        return rand.nextInt(tiles.size());
    }

    @Override
    public String getMethodName() {
        return "random";
    }
}
