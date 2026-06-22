package journeymap_webmap;

import journeymap.api.services.WebMapService;

public class WebMapServiceProvider implements WebMapService
{
    @Override
    public void start()
    {
        WebMap.getInstance().start();
    }

    @Override
    public void stop()
    {
        WebMap.getInstance().stop();
    }

    @Override
    public int getPort()
    {
        return WebMap.getInstance().getPort();
    }

    @Override
    public String getVersion()
    {
        return "1.0.8";
    }
}
