package journeymap_webmap.routes;

import io.javalin.http.Context;
import journeymap.client.log.JMLogger;
import journeymap_webmap.WebMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Log
{
    public static void logGet(Context ctx)
    {
        File file = JMLogger.getLogFile();

        if (file.exists())
        {
            try
            {
                ctx.res().addHeader("Content-Disposition", "inline; filename=\"journeymap.log\"");
                ctx.res().getOutputStream().write(Files.readAllBytes(file.toPath()));
                ctx.res().getOutputStream().flush();
            }
            catch (IOException e)
            {
                WebMap.logger.error("Failed to read log file: ", e);
            }

        }
        else
        {
            WebMap.logger.warn("Unable to find JourneyMap logfile");
            ctx.status(404);
            ctx.result("Not found:" + file.getPath());
        }
    }
}
