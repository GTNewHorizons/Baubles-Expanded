package baubles.common.command;

import java.util.ArrayList;
import java.util.List;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleSlotInventory;
import baubles.common.container.BaubleSlotState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

public class CommandBaubles extends CommandBase {

    public static final String MODIFIER_SOURCE = "baubles:command";

    @Override
    public String getCommandName() {
        return "baubles";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.baubles.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) throw new WrongUsageException(getCommandUsage(sender));
        String operation = args[0];
        if ("list".equals(operation)) {
            if (args.length > 2) throw new WrongUsageException(getCommandUsage(sender));
            IBaubleSlotInventory inventory = args.length == 2 ? inventory(getPlayer(sender, args[1])) : null;
            for (String type : BaubleExpandedSlots.getCurrentlyRegisteredTypes()) {
                if (BaubleExpandedSlots.unknownType.equals(type)) continue;
                int count = inventory == null ? BaubleExpandedSlots.totalCurrentlyAssignedSlotsOfType(type)
                        : inventory.getSlotCount(type);
                sender.addChatMessage(new ChatComponentText(type + ": " + count));
            }
            return;
        }
        if ("reset".equals(operation)) {
            if (args.length < 2 || args.length > 3) throw new WrongUsageException(getCommandUsage(sender));
            EntityPlayerMP player = getPlayer(sender, args[1]);
            IBaubleSlotInventory inventory = inventory(player);
            try {
                if (args.length == 3) {
                    validateType(args[2]);
                    inventory.setSlotModifier(args[2], MODIFIER_SOURCE, 0);
                } else inventory.resetSlotModifiers(MODIFIER_SOURCE);
            } catch (IllegalArgumentException e) {
                throw new CommandException("commands.baubles.invalid", e.getMessage());
            }
            func_152373_a(sender, this, "commands.baubles.reset", player.getCommandSenderName());
            return;
        }
        if (!("set".equals(operation) || "add".equals(operation) || "remove".equals(operation))
                || args.length < 3 || args.length > 4) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        String type = args[1];
        validateType(type);
        EntityPlayerMP player = getPlayer(sender, args[2]);
        int amount = args.length == 4 ? parseIntBounded(sender, args[3], 0, BaubleSlotState.MAX_SLOTS) : 1;
        IBaubleSlotInventory inventory = inventory(player);
        try {
            if ("set".equals(operation)) {
                inventory.setSlotCount(type, MODIFIER_SOURCE, amount);
            } else {
                long contribution = inventory.getSlotModifiers(type).getOrDefault(MODIFIER_SOURCE, 0);
                contribution += "add".equals(operation) ? amount : -amount;
                if (contribution < Integer.MIN_VALUE || contribution > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Slot modifier overflow");
                }
                inventory.setSlotModifier(type, MODIFIER_SOURCE, (int) contribution);
            }
        } catch (IllegalArgumentException e) {
            throw new CommandException("commands.baubles.invalid", e.getMessage());
        }
        func_152373_a(sender, this, "commands.baubles.changed", player.getCommandSenderName(), type, inventory.getSlotCount(type));
    }

    private IBaubleSlotInventory inventory(EntityPlayerMP player) {
        IBaubleSlotInventory inventory = BaublesApi.getBaubleSlots(player);
        if (inventory == null) throw new CommandException("commands.baubles.unavailable");
        return inventory;
    }

    private void validateType(String type) {
        if (!BaubleExpandedSlots.isTypeRegistered(type) || BaubleExpandedSlots.unknownType.equals(type)) {
            throw new CommandException("commands.baubles.type", type);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "list", "set", "add", "remove", "reset");
        if (isUsernameIndex(args, args.length - 1)) {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }
        if ((args.length == 2 && isCapacityCommand(args[0])) || (args.length == 3 && "reset".equals(args[0]))) {
            List<String> types = new ArrayList<>(BaubleExpandedSlots.getCurrentlyRegisteredTypes());
            types.remove(BaubleExpandedSlots.unknownType);
            return getListOfStringsFromIterableMatchingLastWord(args, types);
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return args.length > 0 && (index == 1 && ("list".equals(args[0]) || "reset".equals(args[0]))
                || index == 2 && isCapacityCommand(args[0]));
    }

    private boolean isCapacityCommand(String operation) {
        return "set".equals(operation) || "add".equals(operation) || "remove".equals(operation);
    }
}
