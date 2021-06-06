package com.chikage.mineexporter;

import com.chikage.mineexporter.ctm.CTMContext;
import com.chikage.mineexporter.ctm.CTMHandler;
import com.chikage.mineexporter.ctm.method.CTMMethod;
import com.chikage.mineexporter.ctm.method.MethodCTMCompact;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TextureHandler {

    private int width;
    private int height;
    private String baseName;
    private ResourceLocation baseTexLocation;

    public TextureHandler(TextureAtlasSprite sprite) {
        this.width = sprite.getIconWidth();
        this.height = sprite.getIconHeight();

        String iconName = sprite.getIconName();
        this.baseName = getSplitLast(iconName, "/");

        ResourceLocation rawLocation = new ResourceLocation(iconName);
        this.baseTexLocation = new ResourceLocation(rawLocation.getNamespace(), "textures/"+ rawLocation.getPath()+".png");
    }

    public String getConnectedImage(IResourceManager rm, BufferedImage image, CTMHandler handler, CTMContext ctx) throws IOException{
        CTMMethod method = handler.getMethod(ctx.getBlockState(), baseTexLocation);
        if (method == null) return "none";

        String methodName = handler.getMethodName(method);
        int index = handler.getTileIndex(method, ctx);
        if (method instanceof MethodCTMCompact){
            BufferedImage[] images = new BufferedImage[5];
            InputStream texIS0 = handler.getTileInputStream(rm, method, 0);
            InputStream texIS1 = handler.getTileInputStream(rm, method, 1);
            InputStream texIS2 = handler.getTileInputStream(rm, method, 2);
            InputStream texIS3 = handler.getTileInputStream(rm, method, 3);
            InputStream texIS4 = handler.getTileInputStream(rm, method, 4);
            images[0] = ImageIO.read(texIS0);
            images[1] = ImageIO.read(texIS1);
            images[2] = ImageIO.read(texIS2);
            images[3] = ImageIO.read(texIS3);
            images[4] = ImageIO.read(texIS4);
            texIS0.close();
            texIS1.close();
            texIS2.close();
            texIS3.close();
            texIS4.close();

            int[] indices = handler.getCompactTileIndices(method, ctx);

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int i = indices[Math.round((float)x/image.getWidth())*2+Math.round((float)y/image.getHeight())];
                    image.setRGB(x, y, images[i].getRGB(x, y));
                }
            }

        } else {
            InputStream texInputStream = handler.getTileInputStream(rm, method, index);
            BufferedImage newImage = ImageIO.read(texInputStream);
            texInputStream.close();

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    image.setRGB(x, y, newImage.getRGB(x, y));
                }
            }
        }

        return methodName + index;
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
        InputStream texInputStream = rm.getResource(baseTexLocation).getInputStream();
        return ImageIO.read(texInputStream);
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
}
