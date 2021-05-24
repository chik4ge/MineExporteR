package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;

public class MethodHorizontal extends CTMMethod {

    public MethodHorizontal(String path, String propertyName) {
        super(path, propertyName);
    }

    @Override
    public int getTileIndex(CTMContext ctx) {
        boolean isPUConnected = ctx.shouldConnectTo(this, 1, 0);
        boolean isNUConnected = ctx.shouldConnectTo(this, -1, 0);

        if (isPUConnected && !isNUConnected) return 0;
        else if (isPUConnected) return 1;
        else if (isNUConnected) return 2;
        else return 3;
    }

    @Override
    public String getMethodName() {
        return "horizontal";
    }
}
