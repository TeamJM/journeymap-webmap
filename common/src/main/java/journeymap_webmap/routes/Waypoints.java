package journeymap_webmap.routes;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.TextureCache;

import java.io.IOException;

public class Waypoints
{
    public static void iconGet(Context ctx)
    {
        String id = ctx.pathParam("id");

        var img = TextureCache.getColorizedWaypointIcon(id);

        if (img != null)
        {
            var nativeImage = img.getPixels();
            if (nativeImage != null)
            {
                try
                {
                    ctx.contentType(ContentType.IMAGE_PNG);
                    ctx.outputStream().write(nativeImage.asByteArray());
                    ctx.outputStream().flush();
                }
                catch (IOException e)
                {
                    // nothing
                }
            }
        }
    }


}
