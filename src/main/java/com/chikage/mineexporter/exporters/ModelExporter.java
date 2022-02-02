package com.chikage.mineexporter.exporters;

import com.chikage.mineexporter.Main;
import com.chikage.mineexporter.ctm.CTMContext;
import com.chikage.mineexporter.ctm.method.CTMMethod;
import com.chikage.mineexporter.utils.ExportContext;
import com.chikage.mineexporter.utils.MathHandler;
import com.chikage.mineexporter.utils.Texture;
import de.javagl.obj.Mtl;
import de.javagl.obj.Mtls;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ModelExporter extends BlockExporter{

    public ModelExporter(ExportContext expCtx, IBlockState state, BlockPos pos) {
        super(expCtx, state, pos);
    }

    /* face: 4*(xyz + uv*)
    *  mtl : [name, ctmIndex, tint]*/
    @Override
    public boolean export(Map<Texture, Set<float[][][]>> faces) {
        IBakedModel model = expCtx.bms.getModelForState(state);
        List<float[][][]> modelFaces = new ArrayList<>();
        float[] offset = getOffset(expCtx.worldIn, state, pos, expCtx.range.getOrigin());
        for (EnumFacing facing : ArrayUtils.addAll(EnumFacing.VALUES, new EnumFacing[]{null})) {
            if (facing != null && !state.shouldSideBeRendered(expCtx.worldIn, pos, facing)) continue;

            for (BakedQuad quad : model.getQuads(state, facing, 0)) {
                float[][][] face = new float[4][2][3]; /* xyz , uv* */

                TextureAtlasSprite sprite = quad.getSprite();

                String mtlName = sprite.getIconName();

                ResourceLocation location = new ResourceLocation(mtlName);

                //CTM情報が入ったTextureを取得
                Texture tex = getTexture(location, quad);

                if (quad.hasTintIndex()) {
                    int tintRGB = getMinecraft().getBlockColors().colorMultiplier(state, expCtx.worldIn, pos, quad.getTintIndex());
                    tex.setTintColor(tintRGB);
                }

                int textureWidth = sprite.getIconWidth();
                int textureHeight = sprite.getIconHeight();
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

                    float x = MathHandler.round(Float.intBitsToFloat(vData[index    ]), 1000000) + offset[0];
                    float y = MathHandler.round(Float.intBitsToFloat(vData[index + 1]), 1000000) + offset[1];
                    float z = MathHandler.round(Float.intBitsToFloat(vData[index + 2]), 1000000) + offset[2];

                    float u =    MathHandler.round(quad.getSprite().getUnInterpolatedU(Float.intBitsToFloat(vData[index + 4]))/16, textureWidth);
                    float v = 1F-MathHandler.round(quad.getSprite().getUnInterpolatedV(Float.intBitsToFloat(vData[index + 5]))/16, textureHeight);

                    if (sprite.hasAnimationMetadata()) {
                        int animationIndex = 0;
                        int frameCount = sprite.getFrameCount();
                        v = MathHandler.round(v * ((animationIndex%frameCount)+1)/frameCount, frameCount*textureHeight);
                    }

                    face[i][0][0] = x;
                    face[i][0][1] = y;
                    face[i][0][2] = z;
                    face[i][1][0] = u;
                    face[i][1][1] = v;
                }

//                過去に追加したFaceと座標が重複した場合法線方向に少しずらす
                float[] n1 = MathHandler.calcNormal(face);
                for (float[][][] f: modelFaces) {
                    if (MathHandler.hasSameVertex(f, face)) {
                        float[] n2 = MathHandler.calcNormal(f);
                        float dot = MathHandler.dotProduct(n1, n2);
                        if (dot > 0) {
                            MathHandler.moveFaceTo(face, n1, 0.001f);
                        } else {
                            MathHandler.moveFaceTo(face, n1, 0.0005f);
                            MathHandler.moveFaceTo(f, n2, 0.0005f);
                        }
                    }
                }
                modelFaces.add(face);

                if (faces.containsKey(tex)) {
                    faces.get(tex).add(face);
                } else {
                    faces.put(tex, new CopyOnWriteArraySet<>(Collections.singletonList(face)));
                }
            }
        }

        return true;
    }

    private Texture getTexture(ResourceLocation location, BakedQuad quad) {
        ResourceLocation realLocation = new ResourceLocation(location.getNamespace(), "textures/"+ location.getPath()+".png");

        CTMContext ctmCtx = new CTMContext(expCtx.worldIn, quad, pos);
        CTMMethod method = expCtx.ctmHandler.getMethod(ctmCtx, realLocation);

        if (method != null) {
            return expCtx.ctmHandler.getCTMTexture(location, ctmCtx, method);
        } else {
            return new Texture(location);
        }
    }
}
