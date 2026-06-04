package journeymap_webmap.routes;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.TextureCache;
import journeymap_webmap.mixin.NativeImageAccessor;

import java.io.IOException;
import java.nio.channels.Channels;

public class Waypoints
{
    public static void iconGet(Context ctx)
    {
        String id = ctx.pathParam("id");

        var img = TextureCache.getColorizedWaypointIcon(id);

        if (img != null)
        {
            var nativeImage = img.getPixels();
            if (nativeImage != null && ((NativeImageAccessor) (Object) nativeImage).getPixels() > 0)
            {
                try (var channel = Channels.newChannel(ctx.outputStream()))
                {
                    ctx.contentType(ContentType.IMAGE_PNG);
                    ((NativeImageAccessor) (Object) nativeImage).invokeWriteToChannel(channel);
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
