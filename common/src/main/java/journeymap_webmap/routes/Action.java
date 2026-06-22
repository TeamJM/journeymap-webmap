package journeymap_webmap.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.io.FileHandler;
import journeymap.client.io.MapSaver;
import journeymap.client.model.map.MapType;
import journeymap.client.task.multi.MapRegionTask;
import journeymap.client.task.multi.SaveMapTask;
import journeymap.common.Journeymap;
import journeymap.common.helper.DimensionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Action
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = Journeymap.getLogger("webmap/routes/action");

    public void actionGet(Context ctx)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null)
        {
            logger.warn("Action requested before world loaded");
            ctx.status(400);
            ctx.result("World not loaded"); // TODO: Handle world being unloaded
            return;
        }

        if (!JourneymapClient.getInstance().isMapping())
        {
            logger.warn("Action requested before Journeymap started");
            ctx.status(400);
            ctx.result("JourneyMap is still starting"); // TODO: Handle JM not being started
            return;
        }

        String type = ctx.pathParam("type");

        switch (type)
        {
            case "automap":
                autoMap(ctx, minecraft, level);
                break;
            case "savemap":
                saveMap(ctx, minecraft, level);
                break;
            default:
                logger.warn("Unknown action type '" + type + "'");
                ctx.status(400);
                ctx.result("Unknown action type '" + type + "'");
                return;
        }
    }

    public void saveMap(Context ctx, Minecraft minecraft, Level level)
    {
        File worldDir = FileHandler.getJMWorldDir(minecraft);

        if (!worldDir.exists() || !worldDir.isDirectory())
        {
            logger.warn("JM world directory not found");
            ctx.status(500);
            ctx.result("Unable to find JourneyMap world directory");
            return;
        }

        String dimension = ctx.queryParam("dim") == null ? "minecraft:overworld" : ctx.queryParam("dim");
        String mapTypeString = ctx.queryParam("mapType") == null ? MapType.Name.day.name() : ctx.queryParam("mapType");

        Integer vSlice = ctx.queryParamAsClass("depth", Integer.class).getOrDefault(0);
        MapType.Name mapTypeName;

        try
        {
            mapTypeName = MapType.Name.valueOf(mapTypeString);
        }
        catch (IllegalArgumentException e)
        {
            logger.warn("Invalid map type '{}'", mapTypeString);
            ctx.status(400);
            ctx.result("Invalid map type '" + mapTypeString + "'");
            return;
        }

        if (mapTypeName != MapType.Name.underground)
        {
            vSlice = null;
        }

        boolean hardcore = level.getLevelData().isHardcore();
        MapType mapType = MapType.from(mapTypeName, vSlice, DimensionHelper.getWorldKeyForName(dimension));

        if (mapType.isUnderground() && hardcore)
        {
            logger.warn("Cave mapping is not allowed on hardcore servers");
            ctx.status(400);
            ctx.result("Cave mapping is not allowed on hardcore servers");
            return;
        }

        MapSaver mapSaver = new MapSaver(worldDir, mapType);

        if (!mapSaver.isValid())
        {
            logger.info("No image files to save");
            ctx.status(400);
            ctx.result("No image files to save");
            return;
        }

        JourneymapClient.getInstance().toggleTask(SaveMapTask.Manager.class, true, mapSaver);

        Map<String, Object> data = new HashMap<>();
        data.put("filename", mapSaver.getSaveFileName());

        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(GSON.toJson(data));
    }

    public void autoMap(Context ctx, Minecraft minecraft, Level level)
    {
        Map<String, Object> data = new HashMap<>();
        boolean enabled = JourneymapClient.getInstance().isTaskManagerEnabled(MapRegionTask.Manager.class);
        String scope = ctx.queryParam("scope") != null ? ctx.queryParam("scope") : "stop";

        if (scope.equals("stop") && enabled)
        {
            JourneymapClient.getInstance().toggleTask(MapRegionTask.Manager.class, false, false);
            data.put("message", "automap_complete");
        }
        else if (!enabled)
        {
            boolean doAll = scope.equals("all");
            JourneymapClient.getInstance().toggleTask(MapRegionTask.Manager.class, true, doAll);
            data.put("message", "automap_started");
        }
        else
        {
            data.put("message", "automap_already_started");
        }

        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(GSON.toJson(data));
    }
}
