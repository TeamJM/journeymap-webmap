package journeymap_webmap.routes;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.IgnSkin;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.UUID;

public class Skin
{
    public static void skinGet(Context ctx)
    {
        UUID uuid = UUID.fromString(ctx.pathParam("uuid"));
        GameProfile profile = Minecraft.getInstance().getConnection().getPlayerInfo(uuid).getProfile();
        boolean close = false;
        NativeImage img;
        if (profile == null)
        {
            img = new NativeImage(24, 24, false);
            close = true;
        }
        else
        {
            img = IgnSkin.getFace(profile).getPixels();
        }

        if (img != null)
        {
            try
            {
                ctx.contentType(ContentType.IMAGE_PNG);
                ctx.res.getOutputStream().write(img.asByteArray());
                ctx.res.getOutputStream().flush();
                if (close)
                {
                    img.close();
                }
            }
            catch (IOException e)
            {
                // nothing
            }
        }
    }
}
