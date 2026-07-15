package journeymap_webmap;

import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Runs webmap route work that touches OpenGL/render state on the Minecraft client thread. Javalin serves
 * each request on a Jetty worker thread, but 1.12.2's {@code DynamicTexture} constructor uploads to GL
 * eagerly ({@code glGenTextures}), which throws "No OpenGL context found in the current thread" off the
 * render thread. Any JourneyMap call that may build a texture - mob icons (via a DataCache load that
 * populates EntityDTO icons), colorized waypoint icons, {@code TextureCache.getTexture} - must therefore be
 * marshalled here. On 1.16.5 this was unnecessary because DynamicTexture#getPixels reads the CPU-side
 * NativeImage and GL upload is deferred; 1.12.2 uploads on construction.
 */
public final class ClientThread
{
    private ClientThread()
    {
    }

    /**
     * Executes {@code supplier} on the Minecraft client thread and returns its result, blocking the caller
     * (a Jetty worker) until the render thread runs it. Returns {@code fallback} when already timed out, the
     * client thread does not complete within the timeout (e.g. the game is paused mid-frame), or the
     * supplier throws - so a route never hangs a Jetty worker indefinitely. When already on the client
     * thread the supplier runs inline.
     */
    public static <T> T supply(Supplier<T> supplier, T fallback)
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.isCallingFromMinecraftThread())
        {
            return supplier.get();
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        minecraft.addScheduledTask(() ->
        {
            try
            {
                future.complete(supplier.get());
            }
            catch (Throwable t)
            {
                future.completeExceptionally(t);
            }
        });

        try
        {
            return future.get(5, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            return fallback;
        }
    }
}
