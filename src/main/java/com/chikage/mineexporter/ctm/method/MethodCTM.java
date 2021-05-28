package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;
import org.apache.commons.lang3.BooleanUtils;

public class MethodCTM extends CTMMethod {
    public MethodCTM(String path, String propertyName) {
        super(path, propertyName);
    }

    private final int[] indices = new int[]{
             0,  3,  0,  3, 12,  5, 12, 15,  0,  3,  0,  3, 12,  5, 12, 15,
             1,  2,  1,  2,  4,  7,  4, 29,  1,  2,  1,  2, 13, 31, 13, 14,
             0,  3,  0,  3, 12,  5, 12, 15,  0,  3,  0,  3, 12,  5, 12, 15,
             1,  2,  1,  2,  4,  7,  4, 29,  1,  2,  1,  2, 13, 31, 13, 14,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            16, 18, 16, 18,  6, 46,  6, 21, 16, 18, 16, 18, 28,  9, 28, 22,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            37, 40, 37, 40, 30,  8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
             0,  3,  0,  3, 12,  5, 12, 15,  0,  3,  0,  3, 12,  5, 12, 15,
             1,  2,  1,  2,  4,  7,  4, 29,  1,  2,  1,  2, 13, 31, 13, 14,
             0,  3,  0,  3, 12,  5, 12, 15,  0,  3,  0,  3, 12,  5, 12, 15,
             1,  2,  1,  2,  4,  7,  4, 29,  1,  2,  1,  2, 13, 31, 13, 14,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            16, 42, 16, 42,  6, 20,  6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26
    };


//    46 -> 0101 0101
//    8 -> 0111 0101
//    9 -> 0101 1101
//    20 -> 1101 0101
//    21 -> 0101 0111
//    1 -> 0001 0000
//    3 -> 0000 0001
//    12 -> 0000 0100
//    36 -> 0100 0000

    @Override
    public int getTileIndex(CTMContext ctx) {
        int isPUConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 1, 0));
        int isNUConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, -1, 0));
        int isPVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 0, 1));
        int isNVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 0, -1));
        int isPUPVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 1, 1));
        int isPUNVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, 1, -1));
        int isNUPVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, -1, 1));
        int isNUNVConnected = BooleanUtils.toInteger(ctx.shouldConnectTo(this, -1, -1));

//      8 | 7 | 6
//      1 | x | 5
//      2 | 3 | 4

        int flag = isPUConnected<<4 |
                isNUConnected |
                isPVConnected<<6 |
                isNVConnected<<2 |
                isPUPVConnected<<5 |
                isPUNVConnected<<3 |
                isNUPVConnected<<7 |
                isNUNVConnected<<1;
        return indices[flag];
    }

    @Override
    public String getMethodName() {
        return "ctm";
    }
}
