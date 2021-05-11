package com.chikage.mineexporter;

import com.chikage.mineexporter.utils.Range;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

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
        for (BlockPos pos: new Range(pos1, pos2)) {
            sender.sendMessage(new TextComponentString(pos.toString()));
            IBlockState state = sender.getEntityWorld().getBlockState(pos);
            IBakedModel model = bms.getModelForState(state);

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
            for (BakedQuad quad: model.getQuads(state, null, 0)) {
                sender.sendMessage(new TextComponentString(Arrays.toString(quad.getVertexData())));
            }
        }
    }
}
