package journeymap_webmap.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.client.data.DataCache;
import journeymap.client.data.ImagesData;
import journeymap.client.model.entity.EntityDTO;
import journeymap.client.waypoint.ClientWaypointImpl;
import journeymap.common.Journeymap;
import journeymap_webmap.ClientThread;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data
{
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setExclusionStrategies(new EntityDTO.EntityDTOExclusionStrategy())
            .create();
    private static final Logger logger = Journeymap.getLogger("webmap/routes/data");

    private static final List<String> dataTypesRequiringSince = Arrays.asList("all", "images");

    public static void dataGet(Context ctx)
    {
        String since = ctx.queryParam("images.since") != null ? ctx.queryParam("images.since") : null;
        String type = ctx.pathParam("type");

        if (dataTypesRequiringSince.contains(type) && since == null)
        {
            logger.warn("Data type '{}' requested without 'images.since' parameter", type);
            ctx.status(400);
            ctx.result("Data type '" + type + "' requires 'images.since' parameter.");
            return;
        }
        long sinceTime = since == null ? 0 : Long.parseLong(since);

        // A DataCache load populates EntityDTO mob icons, which build 1.12.2 DynamicTextures (GL). Resolve
        // the data on the Minecraft client thread so that texture creation has an OpenGL context (Javalin
        // serves this on a Jetty worker thread). The GSON serialization below reads CPU-side fields only,
        // so it stays on the worker thread.
        Object data = ClientThread.supply(() -> resolveData(type, sinceTime, since), null);

        if (data == null)
        {
            logger.warn("Unknown data type '{}'", type);
            ctx.status(400);
            ctx.result("Unknown data type '" + type + "'");
            return;
        }

        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(GSON.toJson(data));
    }

    private static Object resolveData(String type, long sinceTime, String since)
    {
        switch (type)
        {
            case "all":
                return DataCache.INSTANCE.getAll(sinceTime);
            case "ambient":
                return DataCache.INSTANCE.getAmbientCreatures(false);
            case "animals":
                return DataCache.INSTANCE.getAnimals(false);
            case "mobs":
                return DataCache.INSTANCE.getMobs(false);
            case "images":
                return new ImagesData(Long.parseLong(since));
            case "player":
                return DataCache.INSTANCE.getPlayer(false);
            case "players":
                return DataCache.INSTANCE.getPlayers(false);
            case "world":
                return DataCache.INSTANCE.getWorld(false);
            case "villagers":
                return DataCache.INSTANCE.getVillagers(false);
            case "waypoints":
                Collection<ClientWaypointImpl> holders = DataCache.INSTANCE.getWaypoints(false);
                Map<String, Waypoint> wpMap = new HashMap<>();
                for (ClientWaypointImpl holder : holders)
                {
                    wpMap.put(holder.getId(), holder);
                }
                return wpMap;
            default:
                return null;
        }
    }
}
