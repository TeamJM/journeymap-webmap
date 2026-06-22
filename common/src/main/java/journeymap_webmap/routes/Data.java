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
import org.apache.logging.log4j.Logger;

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

    private static final List<String> dataTypesRequiringSince = List.of("all", "images");

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
        Object data = null;
        switch (type)
        {
            case "all":
                data = DataCache.INSTANCE.getAll(sinceTime);
                break;
            case "ambient":
                data = DataCache.INSTANCE.getAmbientCreatures(false);
                break;
            case "animals":
                data = DataCache.INSTANCE.getAnimals(false);
                break;
            case "mobs":
                data = DataCache.INSTANCE.getMobs(false);
                break;
            case "images":
                data = new ImagesData(Long.parseLong(since));
                break;
            case "player":
                data = DataCache.INSTANCE.getPlayer(false);
                break;
            case "players":
                data = DataCache.INSTANCE.getPlayers(false);
                break;
            case "world":
                data = DataCache.INSTANCE.getWorld(false);
                break;
            case "villagers":
                data = DataCache.INSTANCE.getVillagers(false);
                break;
            case "waypoints":
                Collection<ClientWaypointImpl> holders = DataCache.INSTANCE.getWaypoints(false);
                Map<String, Waypoint> wpMap = new HashMap<>();
                for (ClientWaypointImpl holder : holders)
                {
                    wpMap.put(holder.getId(), holder);
                }
                data = wpMap;
                break;
            default:
                logger.warn("Unknown data type '{}'", type);
                ctx.status(400);
                ctx.result("Unknown data type '" + type + "'");
                return;
        }

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
}
