package journeymap_webmap.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface NativeImageAccessor
{
    @Accessor("pixels")
    long getPixels();

    @Invoker("writeToChannel")
    boolean invokeWriteToChannel(WritableByteChannel channel);
}
