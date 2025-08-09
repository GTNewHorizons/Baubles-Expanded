package baubles.nuker;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * The purpose of this plugin is to neutralize Thaumcraft's IFMLLoadingPlugin,
 * it doesn't prevent the IFMLLoadingPlugin from being constructed, but it does
 * prevent the methods from running.
 * We are doing that because Thaumcraft's plugin is downloading the Baubles mod
 * if it is not present and that will cause issues with this mod.
 * This plugin needs to be sorted index below 0 to run before Thaumcraft's plugin.
 * It neutralizes Thaumcraft's plugin by finding its corresponding CoreModManager$FMLPluginWrapper
 * in the ITweakers list, and it replaces the reference to the IFMLLoadingPlugin instance with "this".
 * As a result the methods from Thaumcraft's plugin will run 0 time and the methods
 * from this plugin will run twice.
 */
@SuppressWarnings("unused")
@IFMLLoadingPlugin.SortingIndex(-1)
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class ThaumcraftDepLoaderNuker implements IFMLLoadingPlugin {

    private boolean flag;

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void injectData(Map<String, Object> data) {
        if (flag) return;
        flag = true;
        List<ITweaker> tweakers = (List<ITweaker>) Launch.blackboard.get("Tweaks");
        Field coreModInstance = null;
        for (ITweaker tweaker : tweakers) {
            if ("cpw.mods.fml.relauncher.CoreModManager$FMLPluginWrapper".equals(tweaker.getClass().getName())) {
                try {
                    if (coreModInstance == null) {
                        coreModInstance = tweaker.getClass().getDeclaredField("coreModInstance");
                        coreModInstance.setAccessible(true);
                    }
                    final Object plugin = coreModInstance.get(tweaker);
                    final String name = plugin.getClass().getName();
                    if ("thaumcraft.codechicken.core.launch.DepLoader".equals(name)) {
                        coreModInstance.set(tweaker, this);
                        FMLRelaunchLog.fine("[ThaumcraftDepLoaderNuker] Redirected %s FMLPluginWrapper to call our FMLPlugin %s instead", name, this.getClass().getName());
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    FMLRelaunchLog.log(Level.ERROR, e, "[ThaumcraftDepLoaderNuker] An error occurred trying to read the Tweakers");
                }
            }
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
