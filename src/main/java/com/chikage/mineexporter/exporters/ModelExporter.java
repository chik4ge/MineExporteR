package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.Main;
import com.chikage.mineexporter.TextureHandler;
import com.chikage.mineexporter.ctm.CTMContext;
import com.chikage.mineexporter.utils.ExportContext;
import com.chikage.mineexporter.utils.Face;
import com.chikage.mineexporter.utils.UV;
import com.chikage.mineexporter.utils.Vertex;
import de.javagl.obj.Mtl;
import de.javagl.obj.Mtls;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
    public ModelExporter() {
    }

    @Override
    public boolean export(ExportContext expCtx, IBlockState state, BlockPos pos) {
        IBakedModel model = expCtx.bms.getModelForState(state);
        List<Face> modelFaces = new ArrayList<>();
        for (EnumFacing facing : ArrayUtils.addAll(EnumFacing.VALUES, new EnumFacing[]{null})) {
            if (facing != null && !state.shouldSideBeRendered(expCtx.worldIn, pos, facing)) continue;

            for (BakedQuad quad : model.getQuads(state, facing, 0)) {
                Face face = new Face(new Vertex[4], new UV[4]);

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
//                    TODO 草の側面が正しく描画されない、ctmのoverlayの様子も見ながら実装
//                    普通にあとからレンダリングされてるからレイヤーっぽく見えるだけだった
//                    同じブロック内で重なる箇所があり、かつレンダリング方法がCUTOUT系ならその面についてテクスチャをまとめる処理を入れる

                if (!expCtx.mtls.stream().map(Mtl::getName).collect(Collectors.toList()).contains(texName)) {

                    try {
                        texture = texHandler.getBaseTextureImage(expCtx.rm);
                    } catch (IOException e) {
//                        noError = false;
                        Main.logger.error("failed to find texture image. block: " + state.getBlock().getRegistryName().toString());
                        e.printStackTrace();
                        continue;
                    }
                    if (texture == null) continue;

//                    if (texHandler.hasAnimation()) {
//                        texture = texHandler.getAnimatedTexture(texture);
//                    }

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
                    expCtx.mtls.add(mtl);
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

                    Vec3d offset = getOffset(expCtx.worldIn, state, pos, expCtx.range.getOrigin());

                    double d1 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[index])).toString());
                    double d2 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[index+1])).toString());
                    double d3 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[index+2])).toString());
//                    double d4 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[index+3])).toString());
                    double d5 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[index+4])).toString());
                    double d6 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[index+5])).toString());
                    double d7 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[(index+14)%21 + 4])).toString());
                    double d8 = Double.parseDouble(((Float) Float.intBitsToFloat(vData[(index+14)%21 + 5])).toString());

                    double d9  = quad.getSprite().getUnInterpolatedU((float) ((d5 - d7*.001)/.999))/16.0;
                    double d10 = quad.getSprite().getUnInterpolatedV((float) ((d6 - d8*.001)/.999))/16.0;

                    float x = ((float)Math.round((d1 + offset.x)*1000000))/1000000;
                    float y = ((float)Math.round((d2 + offset.y)*1000000))/1000000;
                    float z = ((float)Math.round((d3 + offset.z)*1000000))/1000000;

                    float u = (float) Math.round(d9*textureWidth)/textureWidth;
                    float v = 1F-(float) Math.round(d10*textureHeight)/textureHeight;

                    if (texHandler.hasAnimation()) {
                        v = texHandler.getAnimatedV(v);
                    }

//                                int nv = vData[index + 6];
//                                float nx = (byte) ((nv) & 0xFF) / 127.0F;
//                                float ny = (byte) ((nv >> 8) & 0xFF) / 127.0F;
//                                float nz = (byte) ((nv >> 16) & 0xFF) / 127.0F;
                    Vertex vertex = new Vertex(x,y,z);
                    UV uv = new UV(u, v);

                    face.vertex[i] = vertex;
                    face.uv[i] = uv;
                }

//                過去に追加したFaceと座標が重複した場合法線方向に少しずらす
                Vec3d n1 = face.getNormal();
                for (Face f: modelFaces) {
                    if (face.hasSameVertex(f)) {
                        Vec3d n2 = f.getNormal();
                        double dot = n1.dotProduct(n2);
                        if (dot > 0) {
                            face.moveTo(n1, 0.001);
                        } else {
                            face.moveTo(n1, 0.0005);
                            f.moveTo(n2, 0.0005);
                        }
                    }
                }
                modelFaces.add(face);
                Collections.addAll(expCtx.vertices, face.vertex);
                Collections.addAll(expCtx.uvs, face.uv);

                if (expCtx.faces.containsKey(texName)) {
                    expCtx.faces.get(texName).add(face);
                } else {
                    expCtx.faces.put(texName, new CopyOnWriteArraySet<>(Collections.singletonList(face)));
                }
            }
        }

        return true;
    }
}
