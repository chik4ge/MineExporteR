package com.chikage.mineexporter.ctm;

import com.chikage.mineexporter.ctm.method.CTMMethod;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;

//周辺ブロックとの接続情報、xyz->UVへの変換を支配
public class CTMContext {
    IBlockAccess access;
    BakedQuad quad;
    BlockPos blockPos;

    EnumFacing uFacing, vFacing, wFacing;

    public CTMContext(IBlockAccess access, BakedQuad quad, BlockPos pos){
        this.access = access;
        this.quad = quad;
        this.blockPos = pos;

        EnumFacing[] uvFacing = estimateUVFacing();
        uFacing = uvFacing[0];
        vFacing = uvFacing[1];
        wFacing = getOrthFacing(uFacing, vFacing);
    }
    
//    wはテクスチャが貼ってある面を正とする
    public boolean shouldConnectTo(CTMMethod prop, int uOffset, int vOffset, int wOffset) {
        IBlockState state = access.getBlockState(blockPos);
        IBlockState targetState = access.getBlockState(blockPos
                .offset(uFacing, uOffset)
                .offset(vFacing, vOffset)
                .offset(wFacing, wOffset)
        );
//        int meta = state.getBlock().getMetaFromState(state);
//        int targetMeta = targetState.getBlock().getMetaFromState(targetState);

//        return meta == targetMeta;

//        if (meta != targetMeta) return false;
        return Block.getStateById(Block.getStateId(state)) == Block.getStateById(Block.getStateId(targetState));
    }

    private EnumFacing[] estimateUVFacing(){
        EnumFacing[] result = new EnumFacing[2];
        int[] vData = quad.getVertexData();
        Vec3d originPos = new Vec3d(0, 0, 0);
        Vec3d maxUPos = new Vec3d(0, 0, 0);
        Vec3d maxVPos = new Vec3d(0, 0, 0);

        float minU = 1;
        float minV = 1;
        float maxU = 0;
        float maxV = 0;

        int textureWidth = quad.getSprite().getIconWidth();
        int textureHeight = quad.getSprite().getIconHeight();
        for (int i = 0; i < 4; i++) {
            int index = i * 7;

            float x = Float.intBitsToFloat(vData[index]);
            float y = Float.intBitsToFloat(vData[index + 1]);
            float z = Float.intBitsToFloat(vData[index + 2]);

            float u = (float) Math.round((quad.getSprite().getUnInterpolatedU((float) ((Float.intBitsToFloat(vData[index + 4]) - Float.intBitsToFloat(vData[(index+14)%21 + 4])*.001)/.999))/16.0)*textureWidth)/textureWidth;
            float v = 1F-(float) Math.round((quad.getSprite().getUnInterpolatedV((float) ((Float.intBitsToFloat(vData[index + 5]) - Float.intBitsToFloat(vData[(index+14)%21 + 5])*.001)/.999))/16.0)*textureHeight)/textureHeight;

            if (u<=minU && v<=minV) {
                originPos = new Vec3d(x, y, z);
                minU = u;
                minV = v;
            }
            if (u>=maxU && v<=minV) {
                maxUPos = new Vec3d(x, y, z);
                maxU = u;
            }
            if (u<=minU && v>=maxV) {
                maxVPos = new Vec3d(x, y, z);
                maxV = v;
            }
        }

        Vec3d uVec = maxUPos.subtract(originPos);
        Vec3d vVec = maxVPos.subtract(originPos);
        result[0] = EnumFacing.getFacingFromVector((float)uVec.x, (float)uVec.y, (float)uVec.z);
        result[1] = EnumFacing.getFacingFromVector((float)vVec.x, (float)vVec.y, (float)vVec.z);
        return result;
    }

//    ベクトルの外積から直行ベクトルを推定
    private EnumFacing getOrthFacing(EnumFacing facingA, EnumFacing facingB) {
        Vec3i a = facingA.getDirectionVec();
        Vec3i b = facingB.getDirectionVec();

        int resultX = a.getY()*b.getZ() - a.getZ()*b.getY();
        int resultY = a.getZ()*b.getX() - a.getX()*b.getZ();
        int resultZ = a.getX()*b.getY() - a.getY()*b.getZ();

        return EnumFacing.getFacingFromVector(resultX, resultY, resultZ);
    }

    public boolean shouldConnectTo(CTMMethod prop, int uOffset, int vOffset) {
        return shouldConnectTo(prop, uOffset, vOffset, 0);
    }

    public int[] getUVIndexes(CTMMethod prop) {
        int[] result = new int[2];

        EnumFacing.Axis uAxis = uFacing.getAxis();
        EnumFacing.Axis vAxis = vFacing.getAxis();

        switch(uAxis) {
            case X:
                result[0] = blockPos.getX();
                break;
            case Y:
                result[0] = blockPos.getY();
                break;
            case Z:
                result[0] = blockPos.getZ();
                break;
        }

        switch (vAxis) {
            case X:
                result[1] = blockPos.getX();
                break;
            case Y:
                result[1] = blockPos.getY();
                break;
            case Z:
                result[1] = blockPos.getZ();
                break;
        }

        return result;
    }
}
