package journeymap_webmap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class FabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WebMap.getInstance().stop());
    }
}
