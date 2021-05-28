package com.chikage.mineexporter;

import com.chikage.mineexporter.ctm.CTMContext;
import com.chikage.mineexporter.ctm.CTMHandler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
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

    public String getConnectedImage(IResourceManager rm, BufferedImage image, CTMHandler handler, CTMContext ctx) throws IOException {
        if (!handler.hasCTMProperty(baseName)) return "none";

        String methodName = handler.getMethodName(baseName);
        int index = handler.getTileIndex(baseName, ctx);
        if (methodName.equals("ctm_compact")){
            BufferedImage[] images = new BufferedImage[5];
            InputStream texIS0 = rm.getResource(new ResourceLocation(handler.getTilePath(baseName, 0))).getInputStream();
            InputStream texIS1 = rm.getResource(new ResourceLocation(handler.getTilePath(baseName, 1))).getInputStream();
            InputStream texIS2 = rm.getResource(new ResourceLocation(handler.getTilePath(baseName, 2))).getInputStream();
            InputStream texIS3 = rm.getResource(new ResourceLocation(handler.getTilePath(baseName, 3))).getInputStream();
            InputStream texIS4 = rm.getResource(new ResourceLocation(handler.getTilePath(baseName, 4))).getInputStream();
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

            int[] indices = handler.getCompactTileIndices(baseName, ctx);

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int i = indices[Math.round((float)x/image.getWidth())*2+Math.round((float)y/image.getHeight())];
                    image.setRGB(x, y, images[i].getRGB(x, y));
                }
            }

        } else {
            ResourceLocation location = new ResourceLocation(handler.getTilePath(baseName, index));
            InputStream texInputStream = rm.getResource(location).getInputStream();
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
