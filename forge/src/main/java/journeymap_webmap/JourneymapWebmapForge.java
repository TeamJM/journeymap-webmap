package journeymap_webmap;

import cpw.mods.fml.common.Mod;

/**
 * 1.7.10 Forge mod entry. FML lives under {@code cpw.mods.fml.*} on 1.7.10 (not {@code net.minecraftforge.fml}).
 * {@code @Mod} requires {@code modid}; name/version/dependencies come from {@code mcmod.info} via
 * {@code useMetadata}. 1.7.10's {@code @Mod} has no {@code clientSideOnly} element - the mod is effectively
 * client-only anyway (its WebMapService only starts on the client), and {@code acceptableRemoteVersions = "*"}
 * lets it connect to servers that do not have it installed.
 */
@Mod(modid = Constants.MOD_ID, useMetadata = true, acceptableRemoteVersions = "*")
public class JourneymapWebmapForge
{
}
