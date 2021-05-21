package com.chikage.mineexporter.ctm;

import java.nio.file.Path;

public class PropertyHorizontal extends CTMProperty{
    public PropertyHorizontal(String path) {
        super(path);
    }

    @Override
    String getTile(CTMContext ctx) {
        boolean isPUConnected = ctx.shouldConnectTo(this, 1, 0);
        boolean isNUConnected = ctx.shouldConnectTo(this, -1, 0);

        if (isPUConnected && !isNUConnected) return tiles.get(0);
        else if (isPUConnected) return tiles.get(1);
        else if (isNUConnected) return tiles.get(2);
        else return tiles.get(3);
    }
}
