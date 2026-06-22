package journeymap_webmap.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.model.map.MapState;
import journeymap.client.ui.minimap.MiniMap;
import journeymap_webmap.WebmapStatus;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public class Status
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void statusGet(Context ctx)
    {
        Map<String, Object> data = new HashMap<>();

        WebmapStatus status;
        if (Minecraft.getInstance().level == null)
        {
            status = WebmapStatus.NO_WORLD;
        }
        else if (!JourneymapClient.getInstance().isMapping())
        {
            status = WebmapStatus.STARTING;
        }
        else
        {
            status = WebmapStatus.READY;
        }

        if (status == WebmapStatus.READY)
        {
            MapState mapState = MiniMap.state();

            data.put("mapType", mapState.getMapType().name());

            Map<String, Boolean> allowedMapTypes = new HashMap<>();
            allowedMapTypes.put("cave", mapState.isCaveMappingAllowed() && mapState.isCaveMappingEnabled());
            allowedMapTypes.put("surface", mapState.isSurfaceMappingAllowed());
            allowedMapTypes.put("topo", mapState.isTopoMappingAllowed());

            if (allowedMapTypes.values().stream().noneMatch(Boolean::booleanValue))
            {
                status = WebmapStatus.DISABLED;
            }

            data.put("allowedMapTypes", allowedMapTypes);
        }

        data.put("status", status.getStatus());

        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(GSON.toJson(data));
    }
}
