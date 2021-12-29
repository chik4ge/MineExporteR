package com.chikage.mineexporter.utils;

import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.world.IBlockAccess;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExportContext {
    public final IResourceManager resMgr;
    public final ResourcePackRepository respRep;
    public final BlockModelShapes bms;

    public final IBlockAccess worldIn;

    public final Range range;

    public final Set<Vertex> vertices;
    public final Set<UV> uvs;
    public final Map<String, List<Face>> faces;

    public ExportContext(
            IResourceManager resMgr,
            ResourcePackRepository respRep,
            BlockModelShapes bms,

            IBlockAccess worldIn,

            Range range,

            Set<Vertex> vertices,
            Set<UV> uvs,
            Map<String, List<Face>> faces
            ) {

        this.resMgr = resMgr;
        this.respRep = respRep;
        this.bms = bms;

        this.worldIn = worldIn;

        this.range = range;

        this.vertices = vertices;
        this.uvs = uvs;
        this.faces = faces;
    }
}
