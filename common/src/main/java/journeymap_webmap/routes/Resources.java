package journeymap_webmap.routes;

import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.io.FileHandler;
import journeymap.client.render.draw.MobIconCache;
import journeymap.client.texture.TextureCache;
import journeymap.common.Journeymap;
import journeymap_webmap.ClientThread;
import journeymap_webmap.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.EofException;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Resources
{
    private static final Logger logger = Journeymap.getLogger("webmap/routes/resources");
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("png");

    public static void resourcesGet(Context ctx)
    {
        BufferedImage img;
        String resource = ctx.queryParam("resource");
        ResourceLocation resourceLocation = resource != null ? new ResourceLocation(resource) : null;
        String extension = resource != null ? resource.substring(resource.lastIndexOf('.') + 1) : null;

        if (Minecraft.getMinecraft().theWorld == null || !JourneymapClient.getInstance().isMapping() || resource == null || "undefined".equals(resource))
        {
            ctx.result("");
            return;
        }

        if (extension != null && extension.contains(":"))
        {
            extension = extension.split(":")[0];
        }

        if ("fake".equals(resourceLocation != null ? resourceLocation.getResourceDomain() : null))
        {
            // TextureCache.getTexture builds a 1.12.2 DynamicTexture (GL) for a "fake" resource, so resolve
            // it to a CPU BufferedImage on the Minecraft client thread (this runs on a Jetty worker).
            img = ClientThread.supply(() -> Constants.toImage(TextureCache.getTexture(resourceLocation)), null);
        }
        else
        {
            try
            {
                img = Constants.toImage(MobIconCache.getWebMapIcon(resourceLocation));
                if (img == null)
                {
                    img = ImageIO.read(Constants.getResourceAsStream(resourceLocation));
                }
            }
            catch (FileNotFoundException | NullPointerException e)
            {
                logger.warn("File at resource location not found: {}", resource);
                ctx.status(404);
                img = getDefaultImage();
            }
            catch (EofException | IIOException e)
            {
                logger.info("Connection closed while writing image response. WebMap probably reloaded.");
                ctx.result("");
                return;
            }
            catch (Exception e)
            {
                logger.error("Exception thrown while retrieving resource at location: {}", resource, e);
                ctx.status(500);
                img = getDefaultImage();
            }
        }

        ctx.contentType("image/" + extension);
        if (img != null)
        {
            try
            {
                ImageIO.write(img, "png", ctx.res.getOutputStream());
                ctx.res.getOutputStream().flush();
            }
            catch (Exception e)
            {
                logger.warn("image not found {}", resource);
            }
        }
    }

    private static BufferedImage getDefaultImage()
    {
        try
        {
            return ImageIO.read(Resources.class.getResource(FileHandler.ASSETS_JOURNEYMAP_UI + "/img/marker-dot-160.png").openStream());
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
