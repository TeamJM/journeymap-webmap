package journeymap_webmap;

import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class JourneymapWebmapForge
{
    public JourneymapWebmapForge()
    {
        GameShuttingDownEvent.BUS.addListener(this::onGameShuttingDown);
    }

    private void onGameShuttingDown(GameShuttingDownEvent event)
    {
        WebMap.getInstance().stop();
    }
}
