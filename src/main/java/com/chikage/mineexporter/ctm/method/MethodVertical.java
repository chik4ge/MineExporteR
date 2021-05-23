package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

public class MethodVertical extends CTMMethod{
    public MethodVertical(String path) {
        super(path);
    }

    @Override
    public String getTile(CTMContext ctx) {
        boolean isPVConnected = ctx.shouldConnectTo(this, 0, 1);
        boolean isNVConnected = ctx.shouldConnectTo(this, 0, -1);

        if (isPVConnected && !isNVConnected) return tiles.get(0);
        else if (isPVConnected) return tiles.get(1);
        else if (isNVConnected) return tiles.get(2);
        else return tiles.get(3);
    }
}
