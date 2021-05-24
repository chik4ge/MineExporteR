package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

public class MethodVertical extends CTMMethod{

    public MethodVertical(String path, String propertyName) {
        super(path, propertyName);
    }

    @Override
    public int getTileIndex(CTMContext ctx) {
        boolean isPVConnected = ctx.shouldConnectTo(this, 0, 1);
        boolean isNVConnected = ctx.shouldConnectTo(this, 0, -1);

        if (isPVConnected && !isNVConnected) return 0;
        else if (isPVConnected) return 1;
        else if (isNVConnected) return 2;
        else return 3;
    }

    @Override
    public String getMethodName() {
        return "vertical";
    }
}
