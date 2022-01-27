package com.chikage.mineexporter.utils;

import java.util.Arrays;

public class MathHandler {
    public static float round(float i, int base) {
        float x = i*base;
        int n = (x + 0.5) > 0 ? (int) (x + 0.5) : (int) (x - 0.49999999999999D);
        return (float) n / base;
    }

    public static float[] calcNormal(float[][][] face) {
        float[] v1 = new float[] {
                face[1][0][0] - face[0][0][0], /* face[1*5 + 0] - face[0*5+ 0] */
                face[1][0][1] - face[0][0][1], /* face[1*5 + 1] - face[0*5+ 1] */
                face[1][0][2] - face[0][0][2], /* face[1*5 + 2] - face[0*5+ 2] */
        };
        float[] v2 = new float[] {
                face[3][0][0] - face[0][0][0], /* face[3*5 + 0] - face[0*5+ 0] */
                face[3][0][1] - face[0][0][1], /* face[3*5 + 1] - face[0*5+ 1] */
                face[3][0][2] - face[0][0][2], /* face[3*5 + 2] - face[0*5+ 2] */
        };

        return crossProduct(v1, v2);
    }

    private static float[] crossProduct(float[] v1, float[] v2) {
        return normalize(new float[]{
                v1[1] * v2[2] - v1[2] * v2[1],
                v1[2] * v2[0] - v1[0] * v2[2],
                v1[0] * v2[1] - v1[1] * v2[0]
        });
    }

    public static float dotProduct(float[] v1, float[] v2) {
        return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2];
    }

    public static boolean hasSameVertex(float[][][] f1, float[][][] f2) {

        float[][] v1 = new float[][]{f1[0][0], f1[1][0], f1[2][0], f1[3][0]};
        float[][] v2 = new float[][]{f2[0][0], f2[1][0], f2[2][0], f2[3][0]};

        Arrays.sort(v1, (a, b) -> compareVec(a, b, 0));
        Arrays.sort(v2, (a, b) -> compareVec(a, b, 0));

        return Arrays.deepEquals(v1, v2);
    }

    private static int compareVec(float[] v1, float[] v2, int i) {
        int c = Float.compare(v1[i], v2[i]);
        if (c == 0 && i <= 1) return compareVec(v1, v2, i+1);
        else return c;
    }

    private static float[] normalize(float[] v) {
        float d0 = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return d0 < 1.0E-4D ? new float[]{0, 0, 0} : new float[]{v[0] / d0, v[1] / d0, v[2] / d0};
    }

    //    vec must be normalized
    public static void moveFaceTo(float[][][] face, float[] vec, float amount){
        for (int i=0;i<4; i++) {
            for (int j=0; j<3; j++) {
                face[i][0][j] += amount*vec[j];
            }
        }
    }
}
