package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CTMMethod {
    public String directoryPath;
    public String propertyName;

    public List<String> tiles;
    public List<String> matchTiles;
    public List<String> matchBlocks;
    public int weight = 0;
    public String connect = "block";
    public List<String> faces = new ArrayList<>(Arrays.asList("all"));
    public List<String> biomes = null;
    public int minHeight = 0;
    public int maxHeight = 255;
    public String name = null;

    public CTMMethod(String path, String propertyName) {
        this.directoryPath = path;
        this.propertyName = propertyName;
    }

    public abstract int getTileIndex(CTMContext ctx);

    public abstract String getMethodName();
}