package com.chikage.mineexporter;

import com.chikage.mineexporter.ctm.CTMContext;
import com.chikage.mineexporter.ctm.CTMHandler;
import com.chikage.mineexporter.ctm.method.CTMMethod;
import com.chikage.mineexporter.ctm.method.MethodCTMCompact;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TextureHandler {

    private static final Map<ResourceLocation, BufferedImage> texCache = new HashMap<>();

    private int width;
    private int height;
    private String baseName;
    private ResourceLocation baseTexLocation;
    private CTMMethod ctmMethod;
    private int ctmIndex = -1;

    public TextureHandler(TextureAtlasSprite sprite) {
        this.width = sprite.getIconWidth();
        this.height = sprite.getIconHeight();

        String iconName = sprite.getIconName();
        this.baseName = getSplitLast(iconName, "/");

        ResourceLocation rawLocation = new ResourceLocation(iconName);
        this.baseTexLocation = new ResourceLocation(rawLocation.getNamespace(), "textures/"+ rawLocation.getPath()+".png");
    }

    public TextureHandler(TextureAtlasSprite sprite, CTMHandler ctmHandler, CTMContext ctx) {
        this(sprite);
        ctmMethod = ctmHandler.getMethod(ctx, baseTexLocation);
        if (ctmMethod != null) {
            ctmIndex = ctmHandler.getTileIndex(ctmMethod, ctx);
        }
    }

    public String getCTMName() {
        if (ctmMethod == null) return "none";
        return ctmMethod.getMethodName() + ctmIndex;
    }

    public void setConnectedImage(IResourceManager rm, BufferedImage image, CTMHandler handler) throws IOException{
        if (ctmMethod instanceof MethodCTMCompact) {
            BufferedImage[] images = new BufferedImage[5];
            images[0] = handler.getTileBufferedImage(rm, ctmMethod, 0);
            images[1] = handler.getTileBufferedImage(rm, ctmMethod, 1);
            images[2] = handler.getTileBufferedImage(rm, ctmMethod, 2);
            images[3] = handler.getTileBufferedImage(rm, ctmMethod, 3);
            images[4] = handler.getTileBufferedImage(rm, ctmMethod, 4);

            int[] indices = handler.getCompactTileIndices(ctmIndex);

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int i = indices[Math.round((float)x/image.getWidth())*2+Math.round((float)y/image.getHeight())];
                    image.setRGB(x, y, images[i].getRGB(x, y));
                }
            }

        } else {
            BufferedImage newImage = handler.getTileBufferedImage(rm, ctmMethod, ctmIndex);

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    image.setRGB(x, y, newImage.getRGB(x, y));
                }
            }
        }
    }

    public void setColormapToImage(BufferedImage image, int tintRGB) {
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

    public BufferedImage getBaseTextureImage(IResourceManager rm) throws IOException {
        return getImage(rm, baseTexLocation);
    }

    public void save(BufferedImage image, Path output) throws IOException {
        if (!Files.exists(output)) {
            if (!Files.exists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }
            ImageIO.write(image, "png", output.toFile());
        }
    }

    public int getTextureWidth() {return width;}
    public int getTextureHeight() {return height;}
    public String getTextureName() {return baseName;}

    private String getSplitLast(String s, String regex) {
        String[] splatted = s.split(regex);
        return splatted[splatted.length-1];
    }

    public static BufferedImage getImage(IResourceManager rm, ResourceLocation location) throws IOException {
        if (texCache.containsKey(location)) return copyImage(texCache.get(location));
        else {
            try {
                InputStream texInputStream = rm.getResource(location).getInputStream();
                BufferedImage image = ImageIO.read(texInputStream);
                texCache.put(location, image);
                texInputStream.close();
                return copyImage(image);
            } catch (IOException e) {
                texCache.put(location, null);
                throw e;
            }
        }
    }

//    quote from https://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
//    上2つはテクスチャがおかしくなるのでボツ
//    原因はわからない
    private static BufferedImage copyImage(BufferedImage source){
        if (source == null) return null;
//        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
//        Graphics g = b.createGraphics();
//        g.drawImage(source, 0, 0, null);
//        g.dispose();
//        return b;
//        BufferedImage bi = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
//        byte[] sourceData = ((DataBufferByte)source.getRaster().getDataBuffer()).getData();
//        byte[] biData = ((DataBufferByte)bi.getRaster().getDataBuffer()).getData();
//        System.arraycopy(sourceData, 0, biData, 0, sourceData.length);
//        return bi;
        ColorModel cm = source.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = source.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
}
