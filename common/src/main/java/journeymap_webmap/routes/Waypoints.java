package journeymap_webmap.routes;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.TextureCache;
import journeymap_webmap.ClientThread;
import journeymap_webmap.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Waypoints
{
    public static void iconGet(Context ctx)
    {
        String id = ctx.pathParam("id");

        // getColorizedWaypointIcon builds a 1.12.2 DynamicTexture (GL) on first request, so resolve the
        // icon to a CPU BufferedImage on the Minecraft client thread; the PNG is encoded below on the
        // Jetty worker thread.
        BufferedImage image = ClientThread.supply(() -> Constants.toImage(TextureCache.getColorizedWaypointIcon(id)), null);

        if (image != null)
        {
            try
            {
                ctx.contentType(ContentType.IMAGE_PNG);
                ImageIO.write(image, "png", ctx.res.getOutputStream());
                ctx.res.getOutputStream().flush();
            }
            catch (IOException e)
            {
                // nothing
            }
        }
    }


}
