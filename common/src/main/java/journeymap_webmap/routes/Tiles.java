package journeymap_webmap.routes;

import com.mojang.blaze3d.platform.NativeImage;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.data.WorldData;
import journeymap.client.io.FileHandler;
import journeymap.client.io.RegionImageHandler;
import journeymap.client.model.map.MapType;
import journeymap.client.render.map.RegionTile;
import journeymap.common.helper.DimensionHelper;
import journeymap_webmap.WebMap;
import journeymap_webmap.mixin.NativeImageAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.eclipse.jetty.io.EofException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;

public class Tiles
{
    public static void tilesGet(Context ctx)
    {
        int x = ctx.queryParam("x") != null ? Integer.parseInt(ctx.queryParam("x")) : 0;
        Integer y = ctx.queryParam("y") != null ? Integer.parseInt(ctx.queryParam("y")) : 0;
        int z = ctx.queryParam("z") != null ? Integer.parseInt(ctx.queryParam("z")) : 0;

        String dimension = ctx.queryParam("dimension") != null ? ctx.queryParam("dimension") : "minecraft:overworld";
        String mapTypeString = ctx.queryParam("mapTypeString") != null ? ctx.queryParam("mapTypeString") : MapType.Name.day.name();
        int zoom = ctx.queryParam("zoom") != null ? Integer.parseInt(ctx.queryParam("zoom")) : 0;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null)
        {
            WebMap.logger.warn("Tiles requested before world loaded");
            ctx.status(400);
            ctx.result("World not loaded");
            return;
        }

        if (!JourneymapClient.getInstance().isMapping())
        {
            WebMap.logger.warn("Tiles requested before JourneyMap started");
            ctx.status(400);
            ctx.result("JourneyMap is still starting");
            return;
        }

        File worldDir = FileHandler.getJMWorldDir(minecraft);

        try
        {
            if (!worldDir.exists() || !worldDir.isDirectory())
            {
                WebMap.logger.warn("JM world directory not found");
                ctx.status(404);
                ctx.result("World not found");
                return;
            }
        }
        catch (NullPointerException e)
        {
            WebMap.logger.warn("NPE occurred while locating JM world directory");
            ctx.status(404);
            ctx.result("World not found");
            return;
        }

        MapType.Name mapTypeName;

        try
        {
            mapTypeName = MapType.Name.valueOf(mapTypeString);
        }
        catch (IllegalArgumentException e)
        {
            WebMap.logger.warn("Invalid map type supplied during tiles request: " + mapTypeString);
            ctx.status(400);
            ctx.result("Invalid map type: " + mapTypeString);
            return;
        }

        if (mapTypeName != MapType.Name.underground)
        {
            y = null;  // Only underground maps have elevation
        }

        if (mapTypeName == MapType.Name.underground && WorldData.isHardcoreAndMultiplayer())
        {
            WebMap.logger.debug("Blank tile returned for underground view on a hardcore server");
            OutputStream output = ctx.outputStream();

            ctx.contentType(ContentType.IMAGE_PNG);
            try
            {
                output.write(Files.readAllBytes(RegionImageHandler.getBlank512x512ImageFile().toPath()));
                output.flush();
            }
            catch (IOException e)
            {
                WebMap.logger.error("Error reading blank file from bytes: ", e);
            }
        }

        int scale = (int) Math.pow(2, zoom);
        int distance = 32 / scale;

        int minChunkX = x * distance;
        int minChunkY = z * distance;

        int maxChunkX = minChunkX + distance - 1;
        int maxChunkY = minChunkY + distance - 1;

        ChunkPos startCoord = new ChunkPos(minChunkX, minChunkY);
        ChunkPos endCoord = new ChunkPos(maxChunkX, maxChunkY);

        boolean showGrid = JourneymapClient.getInstance().getFullMapProperties().showGrid.get();
        MapType mapType = new MapType(mapTypeName, y, DimensionHelper.getWorldKeyForName(dimension));

        NativeImage img = RegionImageHandler.getMergedChunks(
                worldDir, startCoord, endCoord, mapType, true, null,
                RegionTile.TILE_SIZE, RegionTile.TILE_SIZE, false, showGrid
        );

        OutputStream output = ctx.outputStream();

        try
        {
            ctx.contentType(ContentType.IMAGE_PNG);
            ((NativeImageAccessor) (Object) img).invokeWriteToChannel(Channels.newChannel(output));
            output.flush();
        }
        catch (EofException e)
        {
            WebMap.logger.info("Connection closed while writing image response. WebMap probably reloaded.");
            ctx.status(404);
        }
        catch (IOException e)
        {
            WebMap.logger.info("Connection closed while writing image response. WebMap probably reloaded.");
            ctx.status(404);
        }
        img.close();
    }
}
