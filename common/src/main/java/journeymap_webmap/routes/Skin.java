package journeymap_webmap.routes;

import com.mojang.authlib.GameProfile;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.IgnSkin;
import journeymap_webmap.ClientThread;
import journeymap_webmap.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

public class Skin
{
    public static void skinGet(Context ctx)
    {
        UUID uuid = UUID.fromString(ctx.pathParam("uuid"));
        // 1.7.10 has no player-info-by-UUID lookup: NetHandlerPlayClient keys its player list by NAME and
        // GuiPlayerInfo carries no GameProfile. Synthesize a profile from the UUID exactly as JourneyMap's
        // own radar does (PlayerRadarManager) - the session service resolves the skin by UUID, so the name
        // is cosmetic. IgnSkin.getFace can build a DynamicTexture (GL) for the default/cropped face, so
        // resolve it to a CPU BufferedImage on the Minecraft client thread (this runs on a Jetty worker).
        GameProfile profile = new GameProfile(uuid, uuid.toString());
        BufferedImage img = ClientThread.supply(() -> Constants.toImage(IgnSkin.getFace(profile)), null);

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
