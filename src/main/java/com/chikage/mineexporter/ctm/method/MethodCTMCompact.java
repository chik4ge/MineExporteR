package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;
import org.apache.commons.lang3.BooleanUtils;

public class MethodCTMCompact extends MethodCTM{
    public MethodCTMCompact(String path, String propertyName) {
        super(path, propertyName);
    }

    private final int[] compactIndices = new int[]{0, 3, 2, 4, 0, 3, 2, 1};

//    size:4 NP NN PP PN
    public int[] getCompactTileIndices(CTMContext ctx) {
        int[] result = new int[4];

        int isPUConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 1, 0));
        int isNUConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, -1, 0));
        int isPVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 0, 1));
        int isNVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 0, -1));
        int isPUPVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 1, 1));
        int isPUNVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 1, -1));
        int isNUPVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, -1, 1));
        int isNUNVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, -1, -1));

        result[0] = compactIndices[isNUConnected | isPVConnected<<1 | isNUPVConnected<<2];
        result[1] = compactIndices[isNUConnected | isNVConnected<<1 | isNUNVConnected<<2];
        result[2] = compactIndices[isPUConnected | isPVConnected<<1 | isPUPVConnected<<2];
        result[3] = compactIndices[isPUConnected | isNVConnected<<1 | isPUNVConnected<<2];

        return result;
    }

    @Override
    public String getMethodName() {
        return "ctm_compact";
    }
}
