
package baubles.common;

import baubles.common.container.ContainerPlayerExpanded;
import baubles.common.event.KeyHandler;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;



public class CommonProxy implements IGuiHandler {

	public KeyHandler keyHandler;

	public void registerHandlers() {}


	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if(ID == Baubles.GUI) {
            return new ContainerPlayerExpanded(player.inventory, !world.isRemote, player);
        }
		return null;
	}

	public World getClientWorld() {
		return null;
	}


	public void registerKeyBindings() {}

}
