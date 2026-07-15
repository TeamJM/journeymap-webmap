package journeymap_webmap;

import journeymap.client.texture.TextureAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public interface Constants
{
    String MOD_ID = "journeymap_webmap";

    static InputStream getResourceAsStream(ResourceLocation resourceLocation) throws IOException
    {
        return Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation).getInputStream();
    }

    /**
     * Rebuilds a CPU-side ARGB {@link BufferedImage} from a 1.12.2 {@link DynamicTexture}. 1.12.2 has no
     * {@code com.mojang.blaze3d.platform.NativeImage}: a DynamicTexture exposes its pixels as an int[] via
     * {@code getTextureData()}, and JourneyMap's DynamicTextureMixin implements {@link TextureAccess} on
     * every DynamicTexture at runtime to expose the display width/height. This is the webmap-side twin of
     * {@code journeymap.client.render.draw.MobIconCache#toImage}. Returns null when the texture is missing
     * or its backing image has been released, so callers can skip the response cleanly.
     */
    static BufferedImage toImage(DynamicTexture texture)
    {
        if (texture == null || !((TextureAccess) texture).journeymap$hasImage())
        {
            return null;
        }
        int width = ((TextureAccess) texture).journeymap$getWidth();
        int height = ((TextureAccess) texture).journeymap$getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, texture.getTextureData(), 0, width);
        return image;
    }
}
