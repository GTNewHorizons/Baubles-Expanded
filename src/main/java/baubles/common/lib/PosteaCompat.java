package baubles.common.lib;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.Loader;

/// Offers the per-player baubles files to Postea's custom world data API, so versioned transformers rewrite their
/// stacks exactly once per content version change. Safe to call without Postea installed: the Postea types live in
/// the inner holder, which only loads when the mod is present.
public final class PosteaCompat {

    private static final boolean POSTEA = Loader.isModLoaded("postea");

    private PosteaCompat() {}

    /// Transforms a root read from disk in place; call before consuming it.
    public static void transform(String storageId, NBTTagCompound root) {
        if (POSTEA) Impl.transform(storageId, root);
    }

    /// Stamps a freshly built root about to be written to disk.
    public static void stamp(NBTTagCompound root) {
        if (POSTEA) Impl.stamp(root);
    }

    private static final class Impl {

        static void transform(String storageId, NBTTagCompound root) {
            com.gtnewhorizons.postea.api.CustomDataReplacementManager.transform(storageId, root);
        }

        static void stamp(NBTTagCompound root) {
            com.gtnewhorizons.postea.api.CustomDataReplacementManager.stamp(root);
        }
    }
}
