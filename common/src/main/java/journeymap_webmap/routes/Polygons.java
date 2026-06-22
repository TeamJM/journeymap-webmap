package journeymap_webmap.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.api.client.impl.ClientAPI;
import journeymap.api.v2.client.display.Context.UI;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.util.UIState;
import journeymap.client.cartography.color.RGB;
import journeymap.client.render.draw.DrawPolygonStep;
import journeymap.client.render.draw.OverlayDrawStep;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Polygons
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void polygonsGet(Context ctx)
    {
        Type dataType = new TypeToken<List<Map<String, String>>>()
        {
        }.getType();
        Type pointsType = new TypeToken<List<Map<String, Integer>>>()
        {
        }.getType();

        Type holesType = new TypeToken<List<List<Map<String, Integer>>>>()
        {
        }.getType();
        List<Map<String, Object>> data = new ArrayList<>();
        List<OverlayDrawStep> steps = new ArrayList<>();
        UIState fullscreenState = ClientAPI.INSTANCE.getUIState(UI.Fullscreen);
        UIState minimapState = ClientAPI.INSTANCE.getUIState(UI.Minimap);

        UIState uiState = (fullscreenState != null && !fullscreenState.active && minimapState != null && minimapState.active) ? minimapState : fullscreenState;

        ClientAPI.INSTANCE.getDrawSteps(steps, uiState);

        for (OverlayDrawStep overlayDrawStep : steps)
        {
            if (overlayDrawStep instanceof DrawPolygonStep step)
            {
                PolygonOverlay polygon = step.getOverlay();
                List<Map<String, Integer>> points = new ArrayList<>();
                String label = polygon.getLabel();
                int fontColor = polygon.getTextProperties() != null ? polygon.getTextProperties().getColor() : RGB.BLACK_RGB;

                for (BlockPos point : polygon.getOuterArea().getPoints())
                {
                    points.add(new HashMap<>()
                    {{
                        put("x", point.getX());
                        put("y", point.getY());
                        put("z", point.getZ());
                    }});
                }

                List<List<Map<String, Integer>>> holes = new ArrayList<>();

                if (polygon.getHoles() != null)
                {
                    for (MapPolygon hole : polygon.getHoles())
                    {
                        List<Map<String, Integer>> holePoints = new ArrayList<>();
                        for (BlockPos holePoint : hole.getPoints())
                        {
                            holePoints.add(new HashMap<>()
                            {{
                                put("x", holePoint.getX());
                                put("y", holePoint.getY());
                                put("z", holePoint.getZ());
                            }});
                        }
                        holes.add(holePoints);
                    }
                }

                data.add(new HashMap<>()
                {{
                    put("fillColor", RGB.toHexString(polygon.getShapeProperties().getFillColor()));
                    put("fillOpacity", polygon.getShapeProperties().getFillOpacity());
                    put("strokeColor", RGB.toHexString(polygon.getShapeProperties().getStrokeColor()));
                    put("strokeOpacity", polygon.getShapeProperties().getStrokeOpacity());
                    put("strokeWidth", polygon.getShapeProperties().getStrokeWidth());
                    put("imageLocation", step.getTextureResource() != null ? step.getTextureResource().toString() : "");
                    put("texturePositionX", polygon.getShapeProperties().getTexturePositionX());
                    put("texturePositionY", polygon.getShapeProperties().getTexturePositionY());
                    put("textureScaleX", polygon.getShapeProperties().getTextureScaleX());
                    put("textureScaleY", polygon.getShapeProperties().getTextureScaleY());
                    put("fontColor", RGB.toHexString(fontColor));
                    put("label", label);
                    put("holes", GSON.toJsonTree(holes, holesType).getAsJsonArray());
                    put("points", GSON.toJsonTree(points, pointsType).getAsJsonArray());
                }});
            }
        }

        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(GSON.toJsonTree(data, dataType).getAsJsonArray().toString());
    }
}
