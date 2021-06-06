package com.chikage.mineexporter.ctm;

import com.chikage.mineexporter.Main;
import com.chikage.mineexporter.ctm.method.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

//CTM処理の大元
//各処理への指示、データの総括、ExportThreadとのやり取りはここを通じて行う
public class CTMHandler {
    IResourceManager resourceManager;
    ResourcePackRepository rpRep;

//    Map<String, CTMMethod> locationCache = new HashMap<>();
    ArrayList<CTMMethod> methods = new ArrayList<>();

    String ctmDir = "assets/minecraft/mcpatcher/ctm/";

    public CTMHandler(IResourceManager resourceManager, ResourcePackRepository rpRep) {
        this.resourceManager = resourceManager;
        this.rpRep = rpRep;

        createLocationCache();
    }

    private void createLocationCache() {
        for (ResourcePackRepository.Entry pack: rpRep.getRepositoryEntries()) {
            String packName = pack.getResourcePackName();
            Path filePath = Paths.get(".\\resourcepacks\\" + packName);
            File file = filePath.toFile();
            if (packName.endsWith(".zip")) {
                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(filePath)))) {
                    ZipFile zipfile = new ZipFile(file);
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory() || !entry.getName().endsWith(".properties") || !entry.getName().startsWith(ctmDir)) {
                            continue;
                        }
//                        remove ".properties"
                        String name = entry.getName().substring(0, entry.getName().length()-11);

                        String directoryPath = String.join("/", Arrays.asList(name.split("/")).subList(2, name.split("/").length-1));
                        String propertyName = name.split("/")[name.split("/").length-1];

                        InputStream entryInputStream = zipfile.getInputStream(entry);

                        CTMMethod method = createCTMProperty(entryInputStream, directoryPath, propertyName);

//                        locationCache.put(name.split("/")[name.split("/").length-1], method);
                        if (method != null) methods.add(method);
                        entryInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (file.isDirectory()) {
//                TODO ディレクトリのリソパの実装
            }
        }

        Collections.sort(methods, Comparator.comparing(o -> o.propertyName));
    }

    private CTMMethod createCTMProperty(InputStream stream, String path, String propertyName) throws IOException {
        CTMMethod result = null;

        Properties properties = new Properties();
        properties.load(stream);

        String method = properties.getProperty("method");

        if (method == null) {
            Main.logger.error("unexpected null method: " + path + "/" + propertyName);
            return null;
        }

        switch(method) {
            case "ctm":
                result = new MethodCTM(path, propertyName);
                break;

            case "ctm_compact":
                result = new MethodCTMCompact(path, propertyName);
                break;

            case "horizontal":
                result = new MethodHorizontal(path, propertyName);
                break;

            case "vertical":
                result = new MethodVertical(path, propertyName);
                break;

            case "horizontal+vertical":
                result = new MethodHorizontalVertical(path, propertyName);
                break;

            case "vertical+horizontal":
                result = new MethodVerticalHorizontal(path, propertyName);
                break;

            case "top":
                break;

            case "random":
                result = new MethodRandom(path, propertyName);
                break;

            case "repeat":
                result = new MethodRepeat(path, propertyName);

                int width = Integer.parseInt(properties.getProperty("width"));
                int height = Integer.parseInt(properties.getProperty("height"));

                ((MethodRepeat) result).width = width;
                ((MethodRepeat) result).height = height;
                break;

            case "fixed":
                break;

            case "overlay":
                break;

            case "overlay_ctm":
                break;

            case "overlay_random":
                break;

            case "overlay_repeat":
                break;

            case "overlay_fixed":
                break;

            default: {
                Main.logger.error("unknown ctm type: " + properties.getProperty("method"));
            }
        }

        if (result == null) return null;

        String tiles = properties.getProperty("tiles");
        if (tiles != null) {
            result.tiles = getTiles(tiles);
        }

        String matchBlocks = properties.getProperty("matchBlocks");
        if (matchBlocks != null) {
            result.matchBlocks = matchBlocks.split(" ");
        } else if (propertyName.startsWith("block_")) {
            result.matchBlocks = new String[]{propertyName.substring(6)};
        }

        String matchTiles = properties.getProperty("matchTiles");
        if (matchTiles != null) {
            result.matchTiles = matchTiles.split(" ");
        } else if (matchBlocks == null) {
            result.matchTiles = new String[]{propertyName};
        }

        String metadata = properties.getProperty("metadata");
        if (metadata != null) {
            result.metadata = getMetadata(metadata);
        }

//        TODO connect

//        TODO weight
        /*String weight = properties.getProperty("weight");
        if (weight != null) {
            result.weight = Integer.parseInt(weight);
        }*/

        String faces = properties.getProperty("faces");
        if (faces != null) {
            result.faces = Arrays.asList(faces.split(" "));
        }

//        TODO biomes
        /*String biomes = properties.getProperty("biomes");
        if (biomes != null) {
            result.biomes = biomes.split(" ");
        }*/

//        TODO heights
//        TODO name

        return result;
    }

    private List<String> getTiles(String s) {
        List<String> result = new ArrayList<>();

        String[] spaced = s.split(" ");
        for (String element: spaced) {
            List<String> hyphened = Arrays.asList(element.split("-"));
            if (hyphened.size() == 2) {
                try {
                    int start = Integer.parseInt(hyphened.get(0));
                    int end = Integer.parseInt(hyphened.get(1));

                    for (int i=start; i<=end; i++) {
                        result.add(String.valueOf(i));
                    }
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }

            if (element.equals("<skip>") || element.equals("<default>")) {
                result.add(null);
                continue;
            }
            if (element.endsWith(".png")){
                result.add(element.substring(0, element.length()-4));
                continue;
            }
            result.add(element);
        }

        return result;
    }

    private int[] getMetadata(String s) {
        List<Integer> result = new ArrayList<>();

        String[] spaced = s.split(" ");
        for (String element: spaced) {
            List<String> hyphened = Arrays.asList(element.split("-"));
            if (hyphened.size() == 2) {
                int start = Integer.parseInt(hyphened.get(0));
                int end = Integer.parseInt(hyphened.get(1));

                for (int i=start; i<=end; i++) {
                    result.add(i);
                }
            }
        }

        return ArrayUtils.toPrimitive(result.toArray(new Integer[result.size()]));
    }

    public InputStream getTileInputStream(IResourceManager rm, CTMMethod method, int index) throws ArrayIndexOutOfBoundsException, IOException {
        if (index < 0 || index >= method.tiles.size()) throw new ArrayIndexOutOfBoundsException("index must be in 0 to " + (method.tiles.size()-1) + ": " + index);
        String path = method.tiles.get(index);
        InputStream result = null;

        try {
            result =  rm.getResource(new ResourceLocation("minecraft:" + method.directoryPath + "/" + path + ".png")).getInputStream();
        } catch (IOException ignored) {
        }

//        full path
        if(result == null) {
            if (path.startsWith("assets/")) {
                String[] paths = path.split("/");
                ResourceLocation location = new ResourceLocation(paths[1], String.join("/", Arrays.copyOfRange(paths, 2, paths.length)) + ".png");
                result = rm.getResource(location).getInputStream();
            } else {
                throw new IOException("An error occurred while parsing the path. Path: " + path);
            }
        }

        return result;
    }

    public CTMMethod getMethod(IBlockState state, ResourceLocation texLocation) {
        CTMMethod matchedByBlock = null;
        boolean isBlockMatched = false;
        for (CTMMethod method: methods) {
            if (isMatchTile(method, texLocation)) return method;
            if (!isBlockMatched && isMatchBlock(method, state)){
                matchedByBlock = method;
                isBlockMatched = true;
            }
        }
        return matchedByBlock;
    }

    private boolean isMatchTile(CTMMethod method, ResourceLocation texLocation) {
        if (method.matchTiles == null) return false;
        for (String tileName: method.matchTiles) {
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

//    TODO このスパゲッティなんとかする
    private boolean isMatchBlock(CTMMethod method, IBlockState state) {
        if (method.matchBlocks == null) return false;
        for (String stateName: method.matchBlocks) {
            String[] splatted = stateName.split(":");
            if (splatted.length  <= 2) {
                Block matchBlock;
                if (stateName.matches("\\d+")) {
                    matchBlock = Block.getBlockById(Integer.parseInt(stateName));
                } else {
                    matchBlock = Block.getBlockFromName(stateName);
                }
                if(matchBlock != null && state.getBlock() == matchBlock && (method.metadata == null || ArrayUtils.contains(method.metadata, matchBlock.getMetaFromState(state)))) return true;
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

    public int getTileIndex(CTMMethod method, CTMContext ctx) {
        return method.getTileIndex(ctx);
    }

    public int[] getCompactTileIndices(CTMMethod method, CTMContext ctx) {
        return ((MethodCTMCompact)method).getCompactTileIndices(ctx);
    }

    public String getMethodName(CTMMethod method) {
        return method.getMethodName();
    }


}
