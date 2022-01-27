package com.chikage.mineexporter.utils;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public class Texture {
    private ResourceLocation baseTexLocation;
    private String ctmName;
    private final TextureType textureType;
    private int CTMIndex = -1;
    private int tintColor = -1;

    public Texture(ResourceLocation baseTexLocation, String ctmName) {
        this.baseTexLocation = baseTexLocation;
        this.ctmName = ctmName;
        this.textureType = TextureType.CTM;
    }

    public Texture(ResourceLocation baseTexLocation) {
        this.baseTexLocation = baseTexLocation;
        this.textureType = TextureType.NORMAL;
    }

    public void setCTMIndex(int index) {
        this.CTMIndex = index;
    }

    public void setTintColor(int tint) {
        this.tintColor = tint;
    }

    public String getId() {
        if (textureType == TextureType.CTM) {
            return this.ctmName;
        }
        return this.baseTexLocation.toString();
    }

    public ResourceLocation getBaseTexLocation() {
        return baseTexLocation;
    }

    public TextureType getTextureType() {
        return textureType;
    }

    public int getCTMIndex() {
        return CTMIndex;
    }

    public int getTintColor() {
        return tintColor;
    }

    public double getTintLuminance() {
        int r = tintColor>>>16 & 0xFF;
        int g = tintColor>>>8 & 0xFF;
        int b = tintColor & 0xFF;

        return ( r*299 + g*587 + b*114 ) / 2550.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Texture texture = (Texture) o;
        return CTMIndex == texture.CTMIndex &&
                tintColor == texture.tintColor &&
                Objects.equals(baseTexLocation, texture.baseTexLocation) &&
                Objects.equals(ctmName, texture.ctmName) &&
                textureType == texture.textureType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseTexLocation, ctmName, textureType, CTMIndex, tintColor);
    }

    public enum TextureType {
        CTM,
        NORMAL
    }
}
