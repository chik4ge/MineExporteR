package com.chikage.mineexporter.ctm.method;

import com.chikage.mineexporter.ctm.CTMContext;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CTMMethod {
    public String directoryPath;

    public List<String> tiles;
    public List<String> matchTiles;
    public List<String> matchBlocks;
    public int weight = 0;
    public String connect = "block";
    public List<String> faces = new ArrayList<>(Arrays.asList("all"));
    public List<String> biomes = null;
    public int minHeight = 0;
    public int maxHeight = 255;
    public String name = null;

    public CTMMethod(String path) {
        this.directoryPath = path;
    }

    public abstract String getTile(CTMContext ctx);
}
