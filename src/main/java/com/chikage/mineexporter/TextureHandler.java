package com.chikage.mineexporter;

import com.chikage.mineexporter.ctm.CTMHandler;
import com.chikage.mineexporter.ctm.method.CTMMethod;
import com.chikage.mineexporter.ctm.method.MethodCTMCompact;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TextureHandler {

    private static final Map<ResourceLocation, BufferedImage> texCache = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> nullTexCache = new HashSet<>();

    public static BufferedImage setConnectedImage(BufferedImage defaultImage, IResourceManager rm, CTMHandler handler, String methodName, int index) throws IOException{
        CTMMethod method = handler.getMethod(methodName);
        if (method == null) throw new IOException("specified method name is not exist: " + methodName);

        if (method instanceof MethodCTMCompact) {
            BufferedImage[] images = new BufferedImage[5];
            images[0] = handler.getTileBufferedImage(rm, method, 0);
            images[1] = handler.getTileBufferedImage(rm, method, 1);
            images[2] = handler.getTileBufferedImage(rm, method, 2);
            images[3] = handler.getTileBufferedImage(rm, method, 3);
            images[4] = handler.getTileBufferedImage(rm, method, 4);

            //サイズが異なっていたら揃える
            int maxWidth = Arrays.stream(images)
                    .max(Comparator.comparing(BufferedImage::getWidth))
                    .orElseThrow(NoSuchElementException::new)
                    .getWidth();
            int maxHeight = Arrays.stream(images)
                    .max(Comparator.comparing(BufferedImage::getHeight))
                    .orElseThrow(NoSuchElementException::new)
                    .getWidth();
            for (int i=0; i<5; i++) {
                BufferedImage tile = images[i];
                if (tile.getWidth() != maxWidth || tile.getHeight() != maxHeight) {
                    images[i] = Scalr.resize(tile, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, maxWidth, maxHeight);
                }
            }
            if (Arrays.asList(images).contains(null)) return defaultImage;

            int[] indices = handler.getCompactTileIndices(index);

            BufferedImage result = new BufferedImage(maxWidth, maxHeight, defaultImage.getType());

            for (int x = 0; x < maxWidth; x++) {
                for (int y = 0; y < maxHeight; y++) {
                    int i = indices[Math.round((float)x/maxWidth)*2+Math.round((float)y/maxHeight)];
                    result.setRGB(x, y, images[i].getRGB(x, y));
                }
            }

            return result;

        } else {
            BufferedImage result = handler.getTileBufferedImage(rm, method, index);
            if (result == null) return defaultImage;
            return result;

            //サイズが異なっていたら揃える
//            if (newImage.getWidth() != image.getWidth() || newImage.getHeight() != image.getHeight()) {
//                newImage = Scalr.resize(newImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, image.getWidth(), image.getHeight());
//            }
//            for (int x = 0; x < image.getWidth(); x++) {
//                for (int y = 0; y < image.getHeight(); y++) {
//                    image.setRGB(x, y, newImage.getRGB(x, y));
//                }
//            }
        }
    }

    public static void setColormapToImage(BufferedImage image, int tintRGB) {
        int tintR = tintRGB>>>16 & 0xFF;
        int tintG = tintRGB>>>8 & 0xFF;
        int tintB = tintRGB & 0xFF;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                int argb = image.getRGB(x,y);

                int mR = tintR * (argb>>>16 & 0xFF) / 255;
                int mG = tintG * (argb>>>8 & 0xFF) / 255;
                int mB = tintB * (argb & 0xFF) / 255;

                int multiplied = argb&0xFF000000 | mR<<16 | mG<<8 | mB;
                image.setRGB(x, y, multiplied);
            }
        }
    }

    public void setOverlayImage(BufferedImage baseImage, BufferedImage overlayImage) {

    }

    public static void save(BufferedImage image, Path output) throws IOException {
        if (image != null && !Files.exists(output)) {
            if (!Files.exists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }
            ImageIO.write(image, "png", output.toFile());
        }
    }

    private String getSplitLast(String s, String regex) {
        String[] splatted = s.split(regex);
        return splatted[splatted.length-1];
    }

    public static BufferedImage fetchImageCopy(IResourceManager rm, ResourceLocation location) throws IOException {
        if (texCache.containsKey(location)) return copyImage(texCache.get(location));
        else if (nullTexCache.contains(location)) return null;
        else {
            try {
                InputStream texInputStream = rm.getResource(location).getInputStream();
                BufferedImage image = ImageIO.read(texInputStream);
                texCache.put(location, image);
                texInputStream.close();
                return copyImage(image);
            } catch (IOException e) {
                nullTexCache.add(location);
                throw e;
            }
        }
    }

//    quote from https://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
    public static BufferedImage copyImage(BufferedImage source){
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        pasteImage(0, 0, source, image);

        return image;
    }

    public static void pasteImage(int xIn, int yIn, BufferedImage fromImage, BufferedImage toImage) {
        for (int x = 0; x < fromImage.getWidth(); x++) {
            for (int y = 0; y < fromImage.getHeight(); y++) {

                int argb = fromImage.getRGB(x,y);

                toImage.setRGB(xIn + x, yIn + y, argb);
            }
        }
    }
}
