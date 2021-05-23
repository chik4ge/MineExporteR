package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

public class MethodHorizontalVertical extends CTMMethod{
    public MethodHorizontalVertical(String path) {
        super(path);
    }

    @Override
    public String getTile(CTMContext ctx) {
        boolean isPUConnected = ctx.shouldConnectTo(this, 1, 0);
        boolean isNUConnected = ctx.shouldConnectTo(this, -1, 0);
        boolean isPVConnected = ctx.shouldConnectTo(this, 0, 1);
        boolean isNVConnected = ctx.shouldConnectTo(this, 0, -1);

        if (isPUConnected && !isNUConnected) return tiles.get(0);
        else if (isPUConnected) return tiles.get(1);
        else if (isNUConnected) return tiles.get(2);

        else if (isPVConnected && !isNVConnected) return tiles.get(4);
        else if (isPVConnected) return tiles.get(5);
        else if (isNVConnected) return tiles.get(6);

        else return tiles.get(3);
    }
}
