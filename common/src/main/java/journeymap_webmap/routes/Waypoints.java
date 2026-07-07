package journeymap_webmap.routes;

import com.mojang.blaze3d.platform.NativeImage;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.TextureCache;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.io.IOException;

public class Waypoints
{
    public static void iconGet(Context ctx)
    {
        String id = ctx.pathParam("id");

        DynamicTexture img = TextureCache.getColorizedWaypointIcon(id);

        if (img != null)
        {
            NativeImage nativeImage = img.getPixels();
            if (nativeImage != null)
            {
                try
                {
                    ctx.contentType(ContentType.IMAGE_PNG);
                    ctx.res.getOutputStream().write(nativeImage.asByteArray());
                    ctx.res.getOutputStream().flush();
                }
                catch (IOException e)
                {
                    // nothing
                }
            }
        }
    }


}
