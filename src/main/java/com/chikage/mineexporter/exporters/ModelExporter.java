package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.Main;
import com.chikage.mineexporter.TextureHandler;
import com.chikage.mineexporter.ctm.CTMContext;
import com.chikage.mineexporter.utils.ExportContext;
import de.javagl.obj.Mtl;
import de.javagl.obj.Mtls;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ModelExporter extends BlockExporter{

    public ModelExporter(ExportContext expCtx, IBlockState state, BlockPos pos) {
        super(expCtx, state, pos);
    }

    @Override
    public boolean export(Map<String, Set<float[][][]>> faces, Set<Mtl> mtls) {
        IBakedModel model = expCtx.bms.getModelForState(state);
        List<float[][][]> modelFaces = new ArrayList<>();
        float[] offset = getOffset(expCtx.worldIn, state, pos, expCtx.range.getOrigin());
        for (EnumFacing facing : ArrayUtils.addAll(EnumFacing.VALUES, new EnumFacing[]{null})) {
            if (facing != null && !state.shouldSideBeRendered(expCtx.worldIn, pos, facing)) continue;

            for (BakedQuad quad : model.getQuads(state, facing, 0)) {
                float[][][] face = new float[4][2][3]; /* xyz , uv* */

                CTMContext ctx = new CTMContext(expCtx.worldIn, quad, pos);
                TextureHandler texHandler = new TextureHandler(quad.getSprite(), expCtx.ctmHandler, ctx);

                String texName = texHandler.getTextureName();

                BufferedImage texture;
                int tintRGB = -1;

                String ctmName = texHandler.getCTMName();
                if (!ctmName.equals("none")) texName += "-" + ctmName;

//                        TODO colormap実装
//                    Biome biome = sender.getEntityWorld().getBiome(pos);
//                    float temperature = biome.getTemperature(pos);
//                    float rainfall = biome.getRainfall();
//
//                    float adjTemp = Math.max(Math.min(temperature, 1F), 0F);
//                    float adjRainfall = Math.max(Math.min(rainfall, 1F), 0F)*adjTemp;

                if (quad.hasTintIndex()) {
                    tintRGB = getMinecraft().getBlockColors().colorMultiplier(state, expCtx.worldIn, pos, quad.getTintIndex());
                    texName += "-" + Integer.toHexString(tintRGB);
                }

                List<String> mtlNames = mtls.stream().map(Mtl::getName).collect(Collectors.toList());
                if (!mtlNames.contains(texName)) {
                    try {
                        texture = texHandler.getBaseTextureImage(expCtx.rm);
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + state.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        continue;
                    }
                    if (texture == null) continue;

                    try {
                        if (!ctmName.equals("none")) texHandler.setConnectedImage(expCtx.rm, texture, expCtx.ctmHandler);
                    } catch (IOException | ArrayIndexOutOfBoundsException e) {
//                        noError = false;
                        Main.logger.error("failed to find ctm image. block: " + state.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                    }

                    if (tintRGB != -1) {
                        texHandler.setColormapToImage(texture, tintRGB);
                    }

                    String texLocation = "textures/" + texName + ".png";
                    try {
                        texHandler.save(texture, Paths.get("MineExporteR/" + texLocation));
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error(TextFormatting.RED + "failed to save texture image: " + texLocation);
                        e.printStackTrace();
                    }

                    Mtl mtl = Mtls.create(texName);
                    mtl.setMapKd(texLocation);
                    mtls.add(mtl);
                }

                int textureWidth = texHandler.getTextureWidth();
                int textureHeight = texHandler.getTextureHeight();
                int[] vData = quad.getVertexData();

                //            VertexDataの構造覚え書き
                //            基本的に28要素のint配列で保存
                //            1頂点あたり7要素(全部で7*32bitの情報を保持)し、それが4つある
                //            以下インデックスごとの要素
                //            0...x座標(Float)
                //            1...y座標(Float)
                //            2...z座標(Float)
                //            3...頂点色(rgba)(Byte*4)
                //            4...U座標(Float)
                //            5...V座標(Float)
                //            6...法線ベクトル(Byte*3) + あまり8bit

                for (int i = 0; i < 4; i++) {
                    int index = i * 7;

                    float x = round(Float.intBitsToFloat(vData[index    ]), 1000000) + offset[0];
                    float y = round(Float.intBitsToFloat(vData[index + 1]), 1000000) + offset[1];
                    float z = round(Float.intBitsToFloat(vData[index + 2]), 1000000) + offset[2];

                    float u =    round(quad.getSprite().getUnInterpolatedU(Float.intBitsToFloat(vData[index + 4]))/16, textureWidth);
                    float v = 1F-round(quad.getSprite().getUnInterpolatedV(Float.intBitsToFloat(vData[index + 5]))/16, textureHeight);

                    if (texHandler.hasAnimation()) {
                        v = texHandler.getAnimatedV(v);
                    }

//                                int nv = vData[index + 6];
//                                float nx = (byte) ((nv) & 0xFF) / 127.0F;
//                                float ny = (byte) ((nv >> 8) & 0xFF) / 127.0F;
//                                float nz = (byte) ((nv >> 16) & 0xFF) / 127.0F;

                    face[i][0][0] = x;
                    face[i][0][1] = y;
                    face[i][0][2] = z;
                    face[i][1][0] = u;
                    face[i][1][1] = v;
                }

//                過去に追加したFaceと座標が重複した場合法線方向に少しずらす
                float[] n1 = calcNormal(face);
                for (float[][][] f: modelFaces) {
                    if (hasSameVertex(f, face)) {
                        float[] n2 = calcNormal(f);
                        float dot = dotProduct(n1, n2);
                        if (dot > 0) {
                            moveFaceTo(face, n1, 0.001f);
                        } else {
                            moveFaceTo(face, n1, 0.0005f);
                            moveFaceTo(f, n2, 0.0005f);
                        }
                    }
                }
                modelFaces.add(face);

                if (faces.containsKey(texName)) {
                    faces.get(texName).add(face);
                } else {
                    faces.put(texName, new CopyOnWriteArraySet<>(Collections.singletonList(face)));
                }
            }
        }

        return true;
    }

    private float round(float i, int base) {
        float x = i*base;
        int n = (x + 0.5) > 0 ? (int) (x + 0.5) : (int) (x - 0.49999999999999D);
        return (float) n / base;
    }

    private float[] calcNormal(float[][][] face) {
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

    private float[] crossProduct(float[] v1, float[] v2) {
        return normalize(new float[]{
                v1[1] * v2[2] - v1[2] * v2[1], 
                v1[2] * v2[0] - v1[0] * v2[2], 
                v1[0] * v2[1] - v1[1] * v2[0]
        });
    }

    private float dotProduct(float[] v1, float[] v2) {
        return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2];
    }

    private boolean hasSameVertex(float[][][] f1, float[][][] f2) {
        Set<float[]> v1 = new HashSet<>(Arrays.asList(f1[0][0], f1[1][0], f1[2][0], f1[3][0]));
        Set<float[]> v2 = new HashSet<>(Arrays.asList(f2[0][0], f2[1][0], f2[2][0], f2[3][0]));

        return v1.equals(v2);
    }

    private float[] normalize(float[] v) {
        float d0 = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return d0 < 1.0E-4D ? new float[]{0, 0, 0} : new float[]{v[0] / d0, v[1] / d0, v[2] / d0};
    }

//    vec must be normalized
    private void moveFaceTo(float[][][] face, float[] vec, float amount){
        for (int i=0;i<4; i++) {
            for (int j=0; j<3; j++) {
                face[i][0][j] += amount*vec[j];
            }
        }
    }
}
