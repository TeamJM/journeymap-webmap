package journeymap_webmap.routes;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.texture.IgnSkin;
import journeymap_webmap.mixin.NativeImageAccessor;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.channels.Channels;
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

        if (img != null && ((NativeImageAccessor) (Object) img).getPixels() > 0)
        {
            try (var channel = Channels.newChannel(ctx.outputStream()))
            {
                ctx.contentType(ContentType.IMAGE_PNG);
                ((NativeImageAccessor) (Object) img).invokeWriteToChannel(channel);
                ctx.outputStream().flush();
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
