package journeymap_webmap;

import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class JourneymapWebmapForge
{
    public JourneymapWebmapForge()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onGameShuttingDown);
    }

    private void onGameShuttingDown(GameShuttingDownEvent event)
    {
        WebMap.getInstance().stop();
    }
}
