package com.chikage.mineexporter;

import com.chikage.mineexporter.utils.Range;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjWriter;
import de.javagl.obj.Objs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ExportThread extends Thread {
    private MinecraftServer server;
    private ICommandSender sender;
    private BlockPos pos1;
    private BlockPos pos2;

    public ExportThread(MinecraftServer server, ICommandSender sender, BlockPos pos1, BlockPos pos2) {
        this.sender = sender;
        this.server = server;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public void run() {
        BlockModelShapes bms = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
        Obj obj = Objs.create();
        int faceindex = 0;

        Range range = new Range(pos1, pos2);

        for (BlockPos pos: range) {
            int xOffset = pos.getX() - range.getMinX();
            int yOffset = pos.getY() - range.getMinY();
            int zOffset = pos.getZ() - range.getMinZ();

            IBlockState state = sender.getEntityWorld().getBlockState(pos);
            IBlockState aState = state.getActualState(sender.getEntityWorld(), pos);
            IBakedModel model = bms.getModelForState(aState);

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

//            TODO TileEntityは正常に描画されない
            for (EnumFacing facing : ArrayUtils.addAll(EnumFacing.VALUES, new EnumFacing[]{null})) {
                for (BakedQuad quad : model.getQuads(aState, facing, 0)) {
                    int[] vData = quad.getVertexData();
                    for (int i = 0; i < 4; i++) { //objで三角タイプもあるかも
                        int index = i * 7;

                        float x = Float.intBitsToFloat(vData[index]) + xOffset;
                        float y = Float.intBitsToFloat(vData[index + 1]) + yOffset;
                        float z = Float.intBitsToFloat(vData[index + 2]) + zOffset;

                        float u = Float.intBitsToFloat(vData[index + 4]);
                        float v = Float.intBitsToFloat(vData[index + 5]);

                        int nv = vData[index + 6];
                        float nx = (byte) ((nv) & 0xFF) / 127.0F;
                        float ny = (byte) ((nv >> 8) & 0xFF) / 127.0F;
                        float nz = (byte) ((nv >> 16) & 0xFF) / 127.0F;

                        obj.addVertex(x, y, z);
                        obj.addNormal(nx, ny, nz);
                    }
                    obj.addFaceWithNormals(4 * faceindex, 4 * faceindex + 1, 4 * faceindex + 2, 4 * faceindex + 3);
                    faceindex += 1;
                }
            }
        }

        File file = new File("MineExporteR/export.obj");
        try {
            OutputStream output = new FileOutputStream(file);
            ObjWriter.write(obj, output);
    //
    //                byte sbyte[] = "Java".getBytes(StandardCharsets.UTF_8);
    //
    //                for(int i = 0; i < sbyte.length; i++){
    //                    output.write(sbyte[i]);
    //                }

            output.close();
            sender.sendMessage(new TextComponentString("successfully exported."));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
