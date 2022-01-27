package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CTMMethod {
    public String directoryPath;
    public String propertyName;

    public List<String> tiles;
    public String[] matchTiles;
    public String[] matchBlocks;
    public int[] metadata;
    public int weight = 0;
    public String connect = "block";
    public String[] faces = new String[]{"all"};
    public List<String> biomes = null;
    public int minHeight = 0;
    public int maxHeight = 255;
    public String name = null;

    public CTMMethod(String path, String propertyName) {
        this.directoryPath = path;
        this.propertyName = propertyName;
    }

    public abstract int getTileIndex(CTMContext ctx);

    public abstract String getMethodName();

    public boolean isMatchMetaData(int meta) {
        return (this.metadata == null || ArrayUtils.contains(this.metadata, meta));
    }

    public boolean isMatchFace(EnumFacing facing) {
        for (String face: this.faces) {
            switch (face) {
                case "all":
                    return true;
                case "sides":
                    if(facing.getAxis().isHorizontal()) return true;
                    break;
                case "top":
                    if(facing == EnumFacing.UP) return true;
                    break;
                case "bottom":
                    if(facing == EnumFacing.DOWN) return true;
                    break;

                case "north":
                case "south":
                case "east":
                case "west":
                    if(facing.getName().equals(face)) return true;
            }
        }
        return false;
    }

    public boolean isMatchBiome(Biome biome) {
        return true;
    }

    public boolean isMatchHeight(int height) {
        return true;
    }

    public boolean isMatchTile(ResourceLocation texLocation) {
        if (this.matchTiles == null) return false;
        for (String tileName: this.matchTiles) {
            ResourceLocation rawLocation = new ResourceLocation(tileName);
            if (rawLocation == texLocation) return true;
            else {
                String pathIn;
                if (tileName.endsWith(".png")){
                    pathIn = "textures/blocks/"+ rawLocation.getPath();
                } else {
                    pathIn = "textures/blocks/"+ rawLocation.getPath()+".png";
                }
                ResourceLocation location = new ResourceLocation(rawLocation.getNamespace(), pathIn);
                if (location.equals(texLocation)) return true;
            }
        }
        return false;
    }

    public boolean isMatchBlock(IBlockState state) {
        if (this.matchBlocks == null) return false;
        for (String stateName: this.matchBlocks) {
            String[] splatted = stateName.split(":");
            if (splatted.length  <= 2) {
                Block matchBlock;
                if (stateName.matches("\\d+")) {
                    matchBlock = Block.getBlockById(Integer.parseInt(stateName));
                } else {
                    matchBlock = Block.getBlockFromName(stateName);
                }
                if(matchBlock != null && state.getBlock() == matchBlock) return true;
            } else {
                NBTTagCompound tagCompound = new NBTTagCompound();
                NBTUtil.writeBlockState(tagCompound, state);
                if (!tagCompound.getString("Name").equals(splatted[0] + ":" + splatted[1])) return false;
                NBTTagCompound properties = tagCompound.getCompoundTag("Properties");
                for (int i=3; i<splatted.length; i++) {
                    String[] stateSplatted = splatted[i].split("=");
                    if (!ArrayUtils.contains(stateSplatted[1].split(","), properties.getString(stateSplatted[0]))) return false;
                }
                return true;
            }
        }
        return false;
    }


}
