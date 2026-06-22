package journeymap_webmap;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import static journeymap_webmap.Constants.MOD_ID;

public class WebMap
{
    public static final Logger logger = LoggerFactory.getLogger("webmap");
    private int port = 0;
    private boolean started = false;
    private Javalin app = null;
    private static WebMap INSTANCE;

    public static WebMap getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new WebMap();
        }
        return INSTANCE;
    }

    public void start()
    {
        if (!started)
        {
            findPort(true);
            initialise();
            started = true;
            logger.info("WebMap is now listening on port {}", port);
        }
    }

    private void initialise()
    {
        try
        {
            app = Javalin.create(config -> {
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
                            File dir = new File(FileHandler.getMinecraftDirectory(), Constants.WEB_DIR);
                            if (dir.exists())
                            {
                                dir.delete();
                            }
                            if (!dir.exists())
                            {
                                logger.info("Attempting to copy web content to {}", new File(Constants.JOURNEYMAP_DIR, "web"));
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
            app.start(port);
        }
        catch (Exception e)
        {
            logger.error("Failed to start server: " + e);
            stop();
        }
    }

    public void stop()
    {
        if (started)
        {
            if (app != null)
            {
                app.stop();
            }
            started = false;
            logger.info("WebMap stopped.");
        }
    }

    private void findPort(boolean tryCurrentPort)
    {
        if (port == 0)
        {
            if (JourneymapClient.getInstance() == null || JourneymapClient.getInstance().getWebMapProperties() == null)
            {
                port = 8080;
            }
            else
            {
                var configuredPort = (String) JourneymapClient.getInstance().getWebMapProperties().port.get();
                if (configuredPort == null)
                {
                    port = 0;
                }
                else
                {
                    this.port = Integer.parseInt(configuredPort);
                }
                logger.info("port found, set to " + port);
            }
        }

        if (tryCurrentPort)
        {
            try
            {
                ServerSocket socket = new ServerSocket(port);
                port = socket.getLocalPort();
                socket.close();
            }
            catch (IOException e)
            {
                logger.warn("Configured port " + port + " could not be bound: " + e);
                findPort(false);
            }

            logger.info("Configured port " + port + " is available.");
        }
        else
        {
            try
            {
                ServerSocket socket = new ServerSocket(0);
                port = socket.getLocalPort();
                socket.close();

                logger.info("New port " + port + " assigned by ServerSocket.");
            }
            catch (IOException e)
            {
                logger.error("Configured port {} could not be bound on second attempt, failing: ", port, e);
                stop();
            }
        }
    }

    public int getPort()
    {
        return this.port;
    }
}
