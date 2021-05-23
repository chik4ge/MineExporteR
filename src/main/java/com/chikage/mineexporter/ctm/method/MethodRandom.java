package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

import java.util.Random;

public class MethodRandom extends CTMMethod{
    public MethodRandom(String path) {
        super(path);
    }

    @Override
    public String getTile(CTMContext ctx) {
        Random rand = new Random();
        return tiles.get(rand.nextInt(tiles.size()));
    }
}
