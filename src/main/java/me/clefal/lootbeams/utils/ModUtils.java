package me.clefal.lootbeams.utils;

import java.util.ArrayList;
import java.util.List;

//? if fabric {
/*import net.fabricmc.loader.api.FabricLoader;
*///?} elif neoforge {
import net.neoforged.fml.ModList;
//?} else {
/*import net.minecraftforge.fml.ModList;
*///?}

/**
 * Loader-agnostic mod lookups.
 * <p>
 * Replaces NirvanaLib's {@code ModUtils} so that targets which vendor their libraries need no
 * library mod at runtime. Forge and NeoForge expose the same {@code ModList} API, so only the
 * import differs between them.
 */
public final class ModUtils {

    private ModUtils() {
    }

    public static boolean isModLoaded(String modId) {
        //? if fabric {
        /*return FabricLoader.getInstance().isModLoaded(modId);
        *///?} else {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
        //?}
    }

    public static List<String> getModList() {
        List<String> ids = new ArrayList<>();
        //? if fabric {
        /*FabricLoader.getInstance().getAllMods().forEach(mod -> ids.add(mod.getMetadata().getId()));
        *///?} else {
        ModList modList = ModList.get();
        if (modList != null) {
            modList.getMods().forEach(mod -> ids.add(mod.getModId()));
        }
        //?}
        return ids;
    }
}
