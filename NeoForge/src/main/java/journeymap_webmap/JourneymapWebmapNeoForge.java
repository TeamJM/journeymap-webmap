package journeymap_webmap;


import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import journeymap.client.io.FileHandler;
import journeymap_webmap.routes.Data;
import journeymap_webmap.routes.Log;
import journeymap_webmap.routes.Polygons;
import journeymap_webmap.routes.Resources;
import journeymap_webmap.routes.Skin;
import journeymap_webmap.routes.Status;
import journeymap_webmap.routes.Tiles;
import journeymap_webmap.routes.Waypoints;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static journeymap.common.Journeymap.MOD_ID;

@Mod(Constants.MOD_ID)
public class JourneymapWebmapNeoForge
{
    public static final Logger logger = LoggerFactory.getLogger("webmap");

    public JourneymapWebmapNeoForge(IEventBus eventBus)
    {
        eventBus.addListener(this::clientSetupEvent);
    }

    private void clientSetupEvent(FMLClientSetupEvent event)
    {
        WebMap.getInstance(create());
    }

    private Javalin create()
    {
        return Javalin.create(config -> {
                    String assetsRootProperty = System.getProperty("journeymap.webmap.assets_root", null);
                    File testFile = new File("../src/main/resources" + FileHandler.ASSETS_WEBMAP);

                    if (assetsRootProperty != null)
                    {
                        logger.info("Detected 'journeymap.webmap.assets_root' property, serving static files from: " + assetsRootProperty);
                        config.staticFiles.add(assetsRootProperty, Location.EXTERNAL);
                    }
                    else if (testFile.exists())
                    {
                        try
                        {
                            String assets = testFile.getCanonicalPath();
                            logger.info("Development environment detected, serving static files from the filesystem.: " + assets);
                            config.staticFiles.add(testFile.getCanonicalPath(), Location.EXTERNAL);
                        }
                        catch (IOException e)
                        {
                            logger.error("WebMap error finding local assets path", e);
                        }
                    }
                    else
                    {
                        File dir = new File(FileHandler.getMinecraftDirectory(), journeymap.client.Constants.WEB_DIR);
                        if (dir.exists())
                        {
                            dir.delete();
                        }
                        if (!dir.exists())
                        {
                            logger.info("Attempting to copy web content to {}", new File(journeymap.client.Constants.JOURNEYMAP_DIR, "web"));
                            boolean created = FileHandler.copyResources(dir, ResourceLocation.fromNamespaceAndPath(MOD_ID, "web"), "", false);
                            logger.info("Web content copied successfully: {}", created);
                        }

                        if (dir.exists())
                        {
                            logger.info("Loading web content from local: {}", dir.getPath());
                            config.staticFiles.add(dir.getPath(), Location.EXTERNAL);
                        }
                        else
                        {
                            logger.info("Loading web content from jar: {}", FileHandler.ASSETS_WEBMAP);
                            config.staticFiles.add(FileHandler.ASSETS_WEBMAP, Location.CLASSPATH);
                        }
                    }
                })
                .before(ctx -> {
                    ctx.header("Access-Control-Allow-Origin", "*");
                    ctx.header("Cache-Control", "no-cache");
                })
                .get("/waypoint/{id}/icon", Waypoints::iconGet)
                .get("/data/{type}", Data::dataGet)
                .get("/logs", Log::logGet)
                .get("/polygons", Polygons::polygonsGet)
                .get("/resources", Resources::resourcesGet)
                .get("/skin/{uuid}", Skin::skinGet)
                .get("/status", Status::statusGet)
                .get("/tiles/tile.png", Tiles::tilesGet);
    }
}
