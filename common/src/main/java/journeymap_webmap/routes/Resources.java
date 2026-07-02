package journeymap_webmap.routes;

import com.mojang.blaze3d.platform.NativeImage;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.io.FileHandler;
import journeymap.client.render.draw.MobIconCache;
import journeymap.client.texture.TextureCache;
import journeymap.common.Journeymap;
import journeymap_webmap.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.EofException;

import javax.imageio.IIOException;
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
        NativeImage img;
        String resource = ctx.queryParam("resource");
        ResourceLocation resourceLocation = resource != null ? new ResourceLocation(resource) : null;
        boolean close = false;
        String extension = resource != null ? resource.substring(resource.lastIndexOf('.') + 1) : null;

        if (Minecraft.getInstance().level == null || !JourneymapClient.getInstance().isMapping() || resource == null || "undefined".equals(resource))
        {
            ctx.result("");
            return;
        }

        if (extension != null && extension.contains(":"))
        {
            extension = extension.split(":")[0];
        }

        if ("fake".equals(resourceLocation != null ? resourceLocation.getNamespace() : null))
        {
            img = TextureCache.getTexture(resourceLocation).getPixels();
        }
        else
        {
            try
            {
                img = MobIconCache.getWebMapIcon(resourceLocation).getPixels();
                if (img == null)
                {
                    close = true;
                    img = NativeImage.read(Constants.getResourceAsStream(resourceLocation));
                }
            }
            catch (FileNotFoundException | NullPointerException e)
            {
                logger.warn("File at resource location not found: {}", resource);
                ctx.status(404);
                close = true;
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
                close = true;
                img = getDefaultImage();
            }
        }

        ctx.contentType("image/" + extension);
        if (img != null)
        {
            try
            {
                ctx.outputStream().write(img.asByteArray());
                ctx.outputStream().flush();
            }
            catch (Exception e)
            {
                logger.warn("image not found {}", resource);
            }
            if (close)
            {
                img.close();
            }
        }
    }

    private static NativeImage getDefaultImage()
    {
        try
        {
            NativeImage img;
            img = NativeImage.read(Resources.class.getResource(FileHandler.ASSETS_JOURNEYMAP_UI + "/img/marker-dot-160.png").openStream());
            return img;
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
