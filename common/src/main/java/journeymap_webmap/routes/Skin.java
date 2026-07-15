package journeymap_webmap.routes;

import com.mojang.authlib.GameProfile;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.IgnSkin;
import journeymap_webmap.ClientThread;
import journeymap_webmap.Constants;
import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

public class Skin
{
    public static void skinGet(Context ctx)
    {
        UUID uuid = UUID.fromString(ctx.pathParam("uuid"));
        GameProfile profile = Minecraft.getMinecraft().getConnection().getPlayerInfo(uuid).getGameProfile();
        BufferedImage img;
        if (profile == null)
        {
            img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        }
        else
        {
            // IgnSkin.getFace can build a DynamicTexture (GL) for the default/cropped face, so resolve it
            // to a CPU BufferedImage on the Minecraft client thread (this runs on a Jetty worker).
            img = ClientThread.supply(() -> Constants.toImage(IgnSkin.getFace(profile)), null);
        }

        if (img != null)
        {
            try
            {
                ctx.contentType(ContentType.IMAGE_PNG);
                ImageIO.write(img, "png", ctx.res.getOutputStream());
                ctx.res.getOutputStream().flush();
            }
            catch (IOException e)
            {
                // nothing
            }
        }
    }
}
