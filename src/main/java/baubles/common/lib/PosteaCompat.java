package baubles.common.lib;

import com.gtnewhorizons.postea.api.IVersionedTransformer;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.CustomDataReplacementManager;

import cpw.mods.fml.common.Loader;

/// Used to provide custom save data (like bauble files) to Postea's [IVersionedTransformer]s.
///
/// These transformers allow other mods to change saved data based on their own version stamps. This compat flags,
/// to those transformers, the times they should fire. [#transform] is used before consuming data, so the transformer
/// can run, and [#stamp] is used for fresh data to ensure the transformer can add a stamp of its current version.
public final class PosteaCompat {

    private static final boolean POSTEA = Loader.isModLoaded("postea");

    private PosteaCompat() {}

    /// Transforms a root read from disk in place; call before consuming it.
    public static void transform(String storageId, NBTTagCompound root) {
        if (POSTEA) Impl.transform(storageId, root);
    }

    /// Stamps a freshly built root about to be written to disk, keeping the stamps in `carried` (from [#savedStamps])
    /// for transformers not registered now. `carried` may be null.
    public static void stamp(NBTTagCompound root, NBTTagCompound carried) {
        if (POSTEA) Impl.stamp(root, carried);
    }

    /// A copy of the stamps on a root read from disk, or null when it carries none or Postea is absent. Call after
    /// [#transform].
    public static NBTTagCompound savedStamps(NBTTagCompound root) {
        return POSTEA ? Impl.savedStamps(root) : null;
    }

    private static final class Impl {

        static void transform(String storageId, NBTTagCompound root) {
            CustomDataReplacementManager.transform(storageId, root);
        }

        static void stamp(NBTTagCompound root, NBTTagCompound carried) {
            CustomDataReplacementManager.stamp(root, carried);
        }

        static NBTTagCompound savedStamps(NBTTagCompound root) {
            return CustomDataReplacementManager.savedStamps(root);
        }
    }
}
