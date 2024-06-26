package journeymap_webmap.service.webmap.kotlin.enums

enum class WebmapStatus(val status: String)
{
    READY("ready"),
    DISABLED("disabled"),

    NO_WORLD("no_world"),
    STARTING("starting"),
}
