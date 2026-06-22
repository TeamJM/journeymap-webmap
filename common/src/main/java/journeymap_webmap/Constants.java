package journeymap_webmap;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public interface Constants
{
    String MOD_ID = "journeymap_webmap";

    static InputStream getResourceAsStream(ResourceLocation resourceLocation) throws IOException
    {
        return Minecraft.getInstance().getResourceManager().open(resourceLocation);
    }
}
