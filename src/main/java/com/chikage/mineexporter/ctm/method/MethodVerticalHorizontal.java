package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

public class MethodVerticalHorizontal extends CTMMethod{

    public MethodVerticalHorizontal(String path, String propertyName) {
        super(path, propertyName);
    }

    @Override
    public int getTileIndex(CTMContext ctx) {
        boolean isPUConnected = ctx.shouldConnectTo(this, 1, 0);
        boolean isNUConnected = ctx.shouldConnectTo(this, -1, 0);
        boolean isPVConnected = ctx.shouldConnectTo(this, 0, 1);
        boolean isNVConnected = ctx.shouldConnectTo(this, 0, -1);

        if (isPVConnected && !isNVConnected) return 4;
        else if (isPVConnected) return 5;
        else if (isNVConnected) return 6;

        else if (isPUConnected && !isNUConnected) return 0;
        else if (isPUConnected) return 1;
        else if (isNUConnected) return 2;

        else return 3;
    }

    @Override
    public String getMethodName() {
        return "vertical+horizontal";
    }
}
