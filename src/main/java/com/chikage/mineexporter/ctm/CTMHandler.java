package com.chikage.mineexporter.ctm;

import com.chikage.mineexporter.Main;
import com.chikage.mineexporter.TextureHandler;
import com.chikage.mineexporter.ctm.method.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

//CTM処理の大元
//各処理への指示、データの総括、ExportThreadとのやり取りはここを通じて行う
// TODO ブロックごとにインスタンスを生成するように
// TODO リソパ変更時にmethodsの変更を行う
public class CTMHandler {
    ResourcePackRepository rpRep;

//    Map<String, CTMMethod> locationCache = new HashMap<>();
//    ArrayList<CTMMethod> methods = new ArrayList<>();
    Map<ResourceLocation, Set<CTMMethod>> tileMatches = new ConcurrentHashMap<>();
    Map<Block, Set<CTMMethod>> blockMatches = new ConcurrentHashMap<>();

    String ctmDir = "assets/minecraft/mcpatcher/ctm/";

    public CTMHandler(ResourcePackRepository rpRep) {
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
//                        if (method != null) methods.add(method);
                        putMethod(method);
                        entryInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (file.isDirectory()) {
//                TODO ディレクトリのリソパの実装
            }
        }

//        Collections.sort(methods, Comparator.comparing(o -> o.propertyName));
    }

    private void putMethod(CTMMethod method) {
        if (method == null) return;

        if (method.matchTiles != null) {
            for (String tileName: method.matchTiles) {
                ResourceLocation rawLocation = new ResourceLocation(tileName);
                putIfAbsent(tileMatches, rawLocation, method);

                String pathIn;
                if (tileName.endsWith(".png")){
                    pathIn = "textures/blocks/"+ rawLocation.getPath();
                } else {
                    pathIn = "textures/blocks/"+ rawLocation.getPath()+".png";
                }
                ResourceLocation location = new ResourceLocation(rawLocation.getNamespace(), pathIn);
                putIfAbsent(tileMatches, location, method);
            }
        }

        if (method.matchBlocks != null) {
            for(String stateName: method.matchBlocks) {
                String blockName;
                String[] splatted = stateName.split(":");
                if (splatted.length >= 2) {
                    blockName = splatted[0] + ":" + splatted[1];
                } else {
                    blockName = stateName;
                }
                Block matchBlock = Block.getBlockFromName(blockName);
                putIfAbsent(blockMatches, matchBlock, method);
            }
        }
    }

    private <K, V> void putIfAbsent(Map<K, Set<V>> ma, K key, V me) {
        if (ma.containsKey(key)) {
            ma.get(key).add(me);
        } else {
            ma.put(key, new HashSet<V>(Arrays.asList(me)));
        }
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
            result.faces = faces.split(" ");
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
            } else {
                result.add(Integer.parseInt(element));
            }
        }

        return ArrayUtils.toPrimitive(result.toArray(new Integer[result.size()]));
    }

    public BufferedImage getTileBufferedImage(IResourceManager rm, CTMMethod method, int index) throws ArrayIndexOutOfBoundsException, IOException {
        if (index < 0 || index >= method.tiles.size()) throw new ArrayIndexOutOfBoundsException("index must be in 0 to " + (method.tiles.size()-1) + ": " + index);
        String path = method.tiles.get(index);
        BufferedImage result = null;

        try {
            result = TextureHandler.getImage(rm, new ResourceLocation("minecraft:" + method.directoryPath + "/" + path + ".png"));
        } catch (IOException ignored) {
        }

//        full path
        if(result == null) {
            if (path.startsWith("assets/")) {
                String[] paths = path.split("/");
                ResourceLocation location = new ResourceLocation(paths[1], String.join("/", Arrays.copyOfRange(paths, 2, paths.length)) + ".png");
                result = TextureHandler.getImage(rm, location);
            } else {
                throw new IOException("An error occurred while parsing the path. Path: " + path);
            }
        }

        return result;
    }

    public CTMMethod getMethod(CTMContext ctx, ResourceLocation texLocation) {
        IBlockState state = ctx.getBlockState();
        EnumFacing face = ctx.getFacing();
        int metadata = state.getBlock().getMetaFromState(state);

        if (tileMatches.containsKey(texLocation)) {
            CTMMethod result = null;
            for(CTMMethod method: tileMatches.get(texLocation)) {
                if (!isMatchMetaData(method, metadata)) continue;
                if (!isMatchFace(method, face)) continue;
                
                if(result == null) result = method;
                else if (result.propertyName.compareTo(method.propertyName) > 0) {
                    result = method;
                }
            }
            if (result != null) return result;
        }

        if (blockMatches.containsKey(state.getBlock())) {
            CTMMethod result = null;
            for(CTMMethod method: blockMatches.get(state.getBlock())) {
                if (!isMatchMetaData(method, metadata)) continue;
                if (!isMatchFace(method, face)) continue;
                if (!isMatchBlock(method, state)) continue;

                if(result == null) result = method;
                else if (result.propertyName.compareTo(method.propertyName) > 0) {
                    result = method;
                }
            }
            return result;
        }
        return null;
    }

    private boolean isMatchMetaData(CTMMethod method, int meta) {
        return (method.metadata == null || ArrayUtils.contains(method.metadata, meta));
    }

    private boolean isMatchFace(CTMMethod method, EnumFacing facing) {
        for (String face: method.faces) {
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

    private boolean isMatchBiome(CTMMethod method, Biome biome) {
        return true;
    }

    private boolean isMatchHeight(CTMMethod method, int height) {
        return true;
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

    public int getTileIndex(CTMMethod method, CTMContext ctx) {
        return method.getTileIndex(ctx);
    }

    public int[] getCompactTileIndices(int index) {
        switch(index){
            case 0: return new int[]{0, 0, 0, 0};
            case 1: return new int[]{0, 0, 3, 3};
            case 2: return new int[]{3, 3, 3, 3};
            case 3: return new int[]{3, 3, 0, 0};
            case 4: return new int[]{0, 2, 3, 4};
            case 5: return new int[]{3, 4, 0, 2};
            case 6: return new int[]{2, 2, 4, 4};
            case 7: return new int[]{3, 4, 3, 4};
            case 8: return new int[]{4, 4, 1, 4};
            case 9: return new int[]{4, 4, 4, 1};
            case 10: return new int[]{1, 4, 1, 1};
            case 11: return new int[]{1, 4, 1, 4};
            case 12: return new int[]{0, 2, 0, 2};
            case 13: return new int[]{0, 2, 3, 1};
            case 14: return new int[]{3, 1, 3, 1};
            case 15: return new int[]{3, 1, 0, 2};
            case 16: return new int[]{2, 0, 4, 3};
            case 17: return new int[]{4, 3, 2, 0};
            case 18: return new int[]{4, 3, 4, 3};
            case 19: return new int[]{4, 4, 2, 2};
            case 20: return new int[]{1, 4, 4, 4};
            case 21: return new int[]{4, 1, 4, 4};
            case 22: return new int[]{4, 1, 4, 1};
            case 23: return new int[]{4, 4, 1, 1};
            case 24: return new int[]{2, 2, 2, 2};
            case 25: return new int[]{2, 2, 1, 1};
            case 26: return new int[]{1, 1, 1, 1};
            case 27: return new int[]{1, 1, 2, 2};
            case 28: return new int[]{2, 2, 4, 1};
            case 29: return new int[]{3, 1, 3, 4};
            case 30: return new int[]{2, 2, 1, 4};
            case 31: return new int[]{3, 4, 3, 1};
            case 32: return new int[]{1, 1, 1, 4};
            case 33: return new int[]{1, 4, 1, 1};
            case 34: return new int[]{4, 1, 1, 4};
            case 35: return new int[]{1, 4, 4, 1};
            case 36: return new int[]{2, 0, 2, 0};
            case 37: return new int[]{2, 0, 1, 3};
            case 38: return new int[]{1, 3, 1, 3};
            case 39: return new int[]{1, 3, 2, 0};
            case 40: return new int[]{4, 3, 1, 3};
            case 41: return new int[]{1, 4, 2, 2};
            case 42: return new int[]{1, 3, 4, 3};
            case 43: return new int[]{4, 1, 2, 2};
            case 44: return new int[]{1, 1, 4, 1};
            case 45: return new int[]{4, 1, 1, 1};
            case 46: return new int[]{4, 4, 4, 4};
            default:throw new IndexOutOfBoundsException("compact index is must be in 0 to 46");
        }
    }

    public String getMethodName(CTMMethod method) {
        return method.getMethodName();
    }


}
