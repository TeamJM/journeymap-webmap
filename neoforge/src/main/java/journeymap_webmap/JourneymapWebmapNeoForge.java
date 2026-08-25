package journeymap_webmap;


import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@Mod(Constants.MOD_ID)
public class JourneymapWebmapNeoForge
{
    public JourneymapWebmapNeoForge()
    {
        NeoForge.EVENT_BUS.addListener(this::onGameShuttingDown);
    }

    private void onGameShuttingDown(GameShuttingDownEvent event)
    {
        WebMap.getInstance().stop();
    }
}
