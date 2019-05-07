package solar.rpg.shoptopia.player;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import solar.rpg.shoptopia.Main;
import solar.rpg.shoptopia.data.PurchaseData;
import solar.rpg.shoptopia.data.Showcase;
import solar.rpg.skyblock.island.Island;
import solar.rpg.skyblock.util.stored.settings.Settings;

import static org.bukkit.ChatColor.*;

/**
 * Commands is responsible for interpreting player commands.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
//TODO: Add /shop update <amount> <price>
public class Commands {

    /* Predefined messages. */
    //TODO: Extract these to an external file.
    private final String ARGUMENT_ERROR = RED + "Invalid usage. Please use /shop <info/reload/create>.";
    private final String ARGUMENT_CREATE_ERROR = RED + "Invalid usage. Please use /shop create <amount> <price>";
    private final String ARGUMENT_UPDATE_ERROR = RED + "Invalid usage. Please use /shop update <amount> <price>";
    private final String ARGUMENT_INFO = GOLD + "Shoptopia plugin by lavuh. https://github.com/skytopia/Shoptopia/";
    private final String SHOP_INVALID_LOCATION = RED + "You are not allowed to do that here!";
    private final String SHOP_TOO_MANY = RED + "You cannot create more than 12 shops at once!";
    private final String SHOP_NO_ITEM = RED + "There are no items in your designated chest!";
    private final String SHOP_OBSTRUCTED = RED + "The chest is being obstructed. Please remove any blocks above it!";
    private final String SHOP_REMOVED = RED + "You have successfully removed this player shop!";
    private final String SHOP_CREATED = GOLD + "You have successfully created a player shop!";
    private final String SHOP_INVALID_BLOCK = RED + "You cannot create a shop here!";
    private final String SHOP_NO_SHOP = RED + "There is currently no shop set up here!";
    private final String CHEST_NOT_FOUND = RED + "You are not looking at a chest block!";
    private final String RELOAD_SUCCESS = GREEN + "Showcases successfully reloaded.";
    private final String RELOAD_FAILURE = RED + "Player showcases were not able to be loaded. Some may be missing...";
    private final String NO_PERMISSION = RED + "I admire your curiosity, but this command isn't for you.";

    /* Reference to JavaPlugin. */
    private final Main PLUGIN;

    public Commands(Main PLUGIN) {
        this.PLUGIN = PLUGIN;
    }

    /**
     * There is no need for a framework, so just use
     * a simple CommandExecutor method to handle /shop.
     *
     * @param sender The player who sent the command.
     * @param cmd    Bukkit's instance of what command was typed.
     * @param args   The arguments, if any.
     * @return Always returns true.
     */
    @SuppressWarnings("SameReturnValue")
    public boolean onCommand(CommandSender sender, Command cmd, final String[] args) {
        if (cmd.getName().equalsIgnoreCase("shop")) {
            if (args.length == 0)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + sender.getName() + " shop");
            else
                switch (args[0].toLowerCase()) {
                    case "info":
                        // Show basic plugin information.
                        sender.sendMessage(ARGUMENT_INFO);
                        break;
                    case "reload":
                        // Attempt to reload existing showcases again.
                        if (sender.hasPermission("skytopia.staff"))
                            if (PLUGIN.getHandler().reload())
                                sender.sendMessage(RELOAD_SUCCESS);
                            else sender.sendMessage(RELOAD_FAILURE);
                        else
                            sender.sendMessage(NO_PERMISSION);
                        break;
                    case "create":
                        try {
                            // Create requires 3 arguments.
                            if (args.length != 3) {
                                sender.sendMessage(ARGUMENT_CREATE_ERROR);
                                return true;
                            }

                            // Parse number arguments into primitives.
                            int amount = Integer.parseInt(args[1]);
                            int price = Integer.parseInt(args[2]);

                            // Player showcases can only be created in the islands world.
                            if (!((Player) sender).getWorld().getName().equals(Settings.ADMIN_WORLD_ID)) {
                                sender.sendMessage(SHOP_INVALID_LOCATION);
                                return true;
                            }

                            // Check if the block the player is looking at is an eligible chest.
                            Block targetBlock = ((Player) sender).getTargetBlock(null, 5);
                            if (targetBlock.getType() != Material.CHEST) {
                                sender.sendMessage(CHEST_NOT_FOUND);
                                return true;
                            } else if (((Chest) targetBlock.getState()).getBlockInventory().getSize() == 54) {
                                //Only allow single chests.
                                sender.sendMessage(SHOP_INVALID_BLOCK);
                                return true;
                            }

                            // Check if they are within their island boundaries.
                            Chest chest = (Chest) targetBlock.getState();
                            Island found = solar.rpg.skyblock.Main.instance.main().islands().getIslandAt(targetBlock.getLocation());
                            if (!found.members().isMember(((Player) sender).getUniqueId())) {
                                sender.sendMessage(SHOP_INVALID_LOCATION);
                                return true;
                            }

                            // Check if the maximum number of showcases will not be exceeded.
                            if (PLUGIN.getHandler().getTotalShowcases(found.getID()) >= 12) {
                                sender.sendMessage(SHOP_TOO_MANY);
                                return true;
                            }

                            // Check if there is not an existing showcase on this block already.
                            if (PLUGIN.getHandler().isShowcaseAt(targetBlock.getLocation(), PLUGIN.getHandler().getPlayerShowcasesMap().get(found.getID())) != null) {
                                sender.sendMessage(SHOP_INVALID_BLOCK);
                                return true;
                            }

                            // Check if there is an immediate free space above the chest.
                            if (targetBlock.getRelative(BlockFace.UP).getType() != Material.AIR) {
                                sender.sendMessage(SHOP_OBSTRUCTED);
                                return true;
                            }

                            // Check if there is stock inside the chest to create purchase data with.
                            // Create a singular stack clone of the stock if there is.
                            ItemStack toSell = PLUGIN.getHandler().findNextItem(chest.getBlockInventory());
                            if (toSell == null) {
                                sender.sendMessage(SHOP_NO_ITEM);
                                return true;
                            }
                            toSell = toSell.clone();
                            ItemStack showcaseProduct = toSell.clone();
                            showcaseProduct.setAmount(amount);
                            toSell.setAmount(1);

                            // Create purchase data for this player showcase.
                            PurchaseData buyData = new PurchaseData(showcaseProduct, (double) price);

                            // Create a half slab to put on top of the chest.
                            targetBlock.getRelative(BlockFace.UP).setType(Material.WOOD_STEP);
                            targetBlock.getRelative(BlockFace.UP).setData((byte) 1);

                            // Create the Showcase object and place it in the player showcases map.
                            Showcase created = new Showcase(PLUGIN, chest.getX(), chest.getY() + 1, chest.getZ(), toSell, buyData, null, false, found.getID(), chest.getWorld());
                            PLUGIN.getHandler().addPlayerShowcase(found.getID(), created);
                            sender.sendMessage(SHOP_CREATED);

                            // Reflect the creation of this new player shop in the database table.
                            solar.rpg.skyblock.Main.instance.main().sql().queue("INSERT INTO `shops`(`owner_id`, `amount`, `price`, `item`, `xyz`) VALUES (?,?,?,?,?)",
                                    found.getID(), amount, price, showcaseProduct.getType() + ":" + showcaseProduct.getData().getData(), chest.getX() + "," + (chest.getY() + 1) + "," + chest.getZ());
                            break;
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(ARGUMENT_CREATE_ERROR);
                        }
                    case "remove":
                        // Check if the player is in the island world, where player shops are located.
                        if (!((Player) sender).getWorld().getName().equals(Settings.ADMIN_WORLD_ID)) {
                            sender.sendMessage(SHOP_INVALID_LOCATION);
                            return true;
                        }

                        // Check that the player is looking at a chest block.
                        Block targetBlock = ((Player) sender).getTargetBlock(null, 5);
                        if (targetBlock.getType() != Material.CHEST) {
                            sender.sendMessage(CHEST_NOT_FOUND);
                            return true;
                        }

                        // Check if they are within their island boundaries.
                        Island found = solar.rpg.skyblock.Main.instance.main().islands().getIslandAt(targetBlock.getLocation());
                        if (!found.members().isMember(((Player) sender).getUniqueId())) {
                            sender.sendMessage(SHOP_INVALID_LOCATION);
                            return true;
                        }

                        // Get the showcase object of the showcase located at this chest block, if any.
                        Showcase foundCase = PLUGIN.getHandler().isShowcaseAt(targetBlock.getLocation().clone().add(0, 1, 0),
                                PLUGIN.getHandler().getPlayerShowcasesMap().get(found.getID()));

                        // Check that there is indeed a showcase set up on this chest block.
                        if (foundCase == null) {
                            sender.sendMessage(SHOP_NO_SHOP);
                            return true;
                        }

                        // Remove it from the player shop listener's clickspace. Also remove it from the map.
                        PLUGIN.getPlayerShopListener().removeClickspace(foundCase);
                        PLUGIN.getHandler().removePlayerShowcase(found.getID(), foundCase);

                        // Destroy the showcase object and the item drop associated with it.
                        foundCase.destroy();
                        targetBlock.getRelative(BlockFace.UP).setType(Material.AIR);
                        sender.sendMessage(SHOP_REMOVED);

                        // Reflect the removal of this player shop in the database table.
                        solar.rpg.skyblock.Main.instance.main().sql().queue(
                                "DELETE FROM `shops` WHERE (`owner_id`, `xyz`) = (?,?)",
                                found.getID(), targetBlock.getX() + "," + (targetBlock.getY() + 1) + "," + targetBlock.getZ());
                        break;
                    default:
                        sender.sendMessage(ARGUMENT_ERROR);
                        break;
                }
        }
        return true;
    }
}
