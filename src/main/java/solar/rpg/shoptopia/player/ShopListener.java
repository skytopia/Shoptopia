package solar.rpg.shoptopia.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import solar.rpg.shoptopia.Main;
import solar.rpg.shoptopia.data.Showcase;
import solar.rpg.skyblock.island.Island;
import solar.rpg.skyblock.stored.Settings;
import solar.rpg.skyblock.util.Title;

import java.text.DecimalFormat;
import java.util.*;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RED;

/**
 * This abstract class is responsible for monitoring a single set of showcases.
 * A separate shop listener is created for each individual showcase type.
 * All showcase listeners share the same behaviour when they are interacted with
 * by the player, so the only abstracted part is the set of showcases to listen to.
 *
 * @author lavuh
 * @version 1.1
 * @see #getShowcases()
 * @since 1.0
 */
public abstract class ShopListener implements Listener {

    /* Predefined messages. */
    //TODO: Extract these to an external file.
    private final String SELL = GOLD + "Sell!";
    private final String SELL_AMT_FOR = RED + "Sell x of this item for y ƒ!";
    private final String SELL_SUCCESS = RED + "Sold x of this item for y ƒ!";
    private final String NO_SELL = RED + "You cannot sell here.";
    private final String NO_ADMIN_SELL = RED + "This item cannot be sold.";
    private final String INSUFFICIENT_ITEMS = RED + "You do not have enough items to sell!";

    private final String BUY = GOLD + "Buy!";
    private final String BUY_AMT_FOR = RED + "Purchase x of this item for y ƒ!";
    private final String BUY_SUCCESS = RED + "Purchased x of this item for y ƒ!";
    private final String NO_BUY = RED + "This item cannot be purchased.";
    private final String INSUFFICIENT_MONEY = RED + "You do not have enough money to purchase this!";
    private final String INVENTORY_FULL = RED + "Some items were unable to fit into your inventory.\nWe have relocated these items to any empty storage space.";

    private final String OUT_OF_STOCK = RED + "This player shop is out of stock!";
    private final String SHOP_BROKEN = RED + "This shop is broken. Please try again later.";
    private final String DONATOR_ONLY = RED + "This shop is for donators only!";

    /* Reference to JavaPlugin. */
    private final Main PLUGIN;
    /* Decimal formatter for buy/sell prices. */
    private final DecimalFormat FORMAT;
    /* Clickspace for showcases. See #getClickspace(). */
    private final HashMap<Location, Showcase> CLICKSPACE;
    /* Set of item drops that are exempt from despawning and pickup by all players. */
    private final Set<Item> EXEMPT;

    protected ShopListener(Main PLUGIN) {
        this.PLUGIN = PLUGIN;
        PLUGIN.getServer().getPluginManager().registerEvents(this, PLUGIN);
        EXEMPT = new HashSet<>();
        FORMAT = new DecimalFormat("#.##");
        CLICKSPACE = new HashMap<>();
        antiTamperTask();
    }

    /**
     * Different abstractions of this ShopListener will look after different
     * types of showcases. As of version 1.1, these are just admin showcases
     * and player showcases.
     *
     * @return Set of relevant showcases for this individual listener.
     */
    protected abstract Showcase[] getShowcases();

    /**
     * Continually listens to showcase objects to prevent tampering.
     * If the showcase object has moved, it will be respawned.
     */
    private void antiTamperTask() {
        //If items are tampered with, teleport them back.
        new BukkitRunnable() {
            public void run() {
                // Remove random items on showcase half slabs.
                for (Item item : Bukkit.getWorld("world").getEntitiesByClass(Item.class)) {
                    for (Showcase sc : getShowcases())
                        if (Showcase.matchXZ(sc.getPosition(), item.getLocation()))
                            if (!sc.getShowcaseDrop().equals(item)) {
                                item.remove();
                                sc.respawn();
                            }
                }

                // Check that the Showcase item hasn't despawned.
                for (Showcase sc : getShowcases())
                    if (!sc.getShowcaseDrop().isValid())
                        sc.respawn();
            }
        }.runTaskTimer(PLUGIN, 0L, 100L);
    }

    /**
     * Clickspace is a set of block locations where a click
     * from a player can activate a showcase action.
     *
     * @return Clickspace for this listener.
     */
    public HashMap<Location, Showcase> getClickspace() {
        return CLICKSPACE;
    }

    /**
     * Removes a showcase from the clickspace map.
     *
     * @param toRemove The showcase to remove.
     */
    void removeClickspace(Showcase toRemove) {
        Set<Location> set = new HashSet<>();
        // Go through entry set and identify keys that belong to the showcase.
        for (Map.Entry<Location, Showcase> entry : CLICKSPACE.entrySet())
            if (entry.getValue().getPosition().equals(toRemove.getPosition()))
                set.add(entry.getKey());

        // Remove these keys from the clickspace.
        for (Location remove : set)
            CLICKSPACE.remove(remove);
        set.clear();
    }

    /**
     * @return Set of item drops (from showcases) that shouldn't be despawned or picked up.
     */
    public Set<Item> getExempt() {
        return EXEMPT;
    }

    /**
     * Shows a preview of a showcase's buying price for a player.
     *
     * @param sc     The showcase.
     * @param target The player to show the preview.
     */
    private void previewBuy(Showcase sc, Player target) {
        if (sc.canBuy()) {
            target.playSound(target.getLocation(), Sound.ENTITY_CAT_HURT, 2F, 2F);
            Title.showTitle(target, BUY, BUY_AMT_FOR.replace("x", sc.getBuyData().getAmount() + "").replace("y", FORMAT.format(sc.getBuyData().getPrice())), 20, 100, 20);
        } else {
            target.playSound(target.getLocation(), Sound.ENTITY_CAT_HISS, 2F, 2F);
            Title.showTitle(target, "", NO_BUY, 20, 100, 20);
        }
    }

    /**
     * Preview the sell price for this item.
     * Requires a dependency on Floating Anvil's Title class.
     *
     * @param sc     The showcase.
     * @param target The player who clicked.
     */
    private void previewSell(Showcase sc, Player target) {
        if (sc.canSell()) {
            target.playSound(target.getLocation(), Sound.ENTITY_CAT_HURT, 2F, 2F);
            Title.showTitle(target, SELL, SELL_AMT_FOR.replace("x", sc.getSellData().getAmount() + "").replace("y", FORMAT.format(sc.getSellData().getPrice())), 20, 100, 20);
        } else {
            target.playSound(target.getLocation(), Sound.ENTITY_CAT_HISS, 2F, 2F);
            Title.showTitle(target, "", sc.isAdminShop() ? NO_ADMIN_SELL : NO_SELL, 20, 100, 20);
        }
    }

    /**
     * Attempts to buy the showcase contents for a player.
     *
     * @param sc     The target showcase to buy from.
     * @param target The player who is purchasing.
     */
    private void tryBuy(Showcase sc, Player target) {
        // Get the (supposed) chest block for player shop purposes.
        Block block = sc.getPosition().getBlock().getRelative(BlockFace.DOWN);

        if (!sc.isAdminShop()) {
            // If it is a player shop, make sure the storage chest exists below the showcase block.
            if (block.getType() != Material.CHEST) {
                target.sendMessage(SHOP_BROKEN);
                return;
            }

            // Check that the player shop is in stock too.
            if (!((Chest) block.getState()).getBlockInventory().containsAtLeast(sc.getBuyData().getStock(), sc.getBuyData().getAmount())) {
                target.sendMessage(OUT_OF_STOCK);
                return;
            }
        }

        // Check if the showcase is donator only.
        if (sc.isDonatorOnly())
            if (!target.hasPermission("skytopia.donator")) {
                target.sendMessage(DONATOR_ONLY);
                return;
            }

        if (sc.canBuy()) {
            // Check that the player has sufficient money to purchase from this showcase.
            if (PLUGIN.getHandler().getEconomy().has(target, sc.getBuyData().getPrice())) {
                // Check if the player's inventory was too full to put the purchased items in.
                Collection<ItemStack> leftover = target.getInventory().addItem(sc.getBuyData().getStock()).values();
                Island found = solar.rpg.skyblock.Main.instance.main().islands().getIsland(target.getUniqueId());
                if (leftover.size() > 0) {
                    // Move any leftover items to their Island storage.
                    for (ItemStack item : leftover)
                        found.inv().getStorage().addItem(item);

                    // Notify the island members that a purchased item was put in storage.
                    found.actions().messageAll(INVENTORY_FULL);
                }

                // Remove stock if it is a player shop. Credit the island owner's balance.
                if (!sc.isAdminShop()) {
                    ((Chest) block.getState()).getBlockInventory().removeItem(sc.getBuyData().getStock());
                    PLUGIN.getHandler().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(found.members().getOwner()), sc.getBuyData().getPrice());
                }

                // Withdraw the money, notify the player.
                PLUGIN.getHandler().getEconomy().withdrawPlayer(target, sc.getBuyData().getPrice());
                target.playSound(target.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 0.7F, 0.5F);
                Title.showTitle(target, BUY, BUY_SUCCESS.replace("x", sc.getBuyData().getAmount() + "").replace("y", FORMAT.format(sc.getBuyData().getPrice())), 20, 100, 20);
            } else {
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
                target.sendMessage(INSUFFICIENT_MONEY);
            }
        } else {
            // You cannot buy from this showcase.
            target.playSound(target.getLocation(), Sound.ENTITY_CAT_HISS, 2F, 2F);
            Title.showTitle(target, "", NO_BUY, 20, 100, 20);
        }
    }

    /**
     * Try and buy this item.
     * Requires a dependency on Floating Anvil's Title class.
     * Requires a dependency on Floating Anvil's Vault hook.
     *
     * @param sc     The target showcase.
     * @param target The clicker.
     */
    private void trySell(Showcase sc, Player target) {
        // Check if the showcase is donator only.
        if (sc.isDonatorOnly())
            if (!target.hasPermission("skytopia.donator")) {
                target.sendMessage(DONATOR_ONLY);
                return;
            }

        if (sc.canSell()) {
            // Check that the player has sufficient items for this sale.
            if (target.getInventory().containsAtLeast(sc.getSellData().getStock(), sc.getSellData().getAmount())) {
                // Remove items from inventory. Credit player's balance.
                target.getInventory().removeItem(sc.getSellData().getStock());
                PLUGIN.getHandler().getEconomy().depositPlayer(target, sc.getSellData().getPrice());
                target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 2F, 0.5F);
                Title.showTitle(target, SELL, SELL_SUCCESS.replace("x", sc.getSellData().getAmount() + "").replace("y", FORMAT.format(sc.getSellData().getPrice())), 20, 100, 20);
            } else {
                // Notify player that they do not have sufficient items to sell.
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
                target.sendMessage(INSUFFICIENT_ITEMS);
            }
        } else {
            // You cannot sell from this showcase.
            target.playSound(target.getLocation(), Sound.ENTITY_CAT_HISS, 2F, 2F);
            Title.showTitle(target, "", sc.isAdminShop() ? NO_ADMIN_SELL : NO_SELL, 20, 100, 20);
        }
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        // Prevents showcase drops from despawning.
        if (EXEMPT.contains(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        // Prevents players from breaking existing showcase blocks in the island world.
        if (!event.getBlock().getWorld().getName().equals(Settings.ADMIN_WORLD_ID)) return;
        if (!CLICKSPACE.containsKey(event.getBlock().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        // Check for an instance of a valid click on a showcase.
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;
        if (!CLICKSPACE.containsKey(event.getClickedBlock().getLocation())) return;

        // Ignore if this is a player-based showcase that they own.
        Showcase found = CLICKSPACE.get(event.getClickedBlock().getLocation());
        if (!found.isAdminShop()) {
            Island at = solar.rpg.skyblock.Main.instance.main().islands().getIslandAt(event.getClickedBlock().getLocation());
            if (at.members().isMember(event.getPlayer().getUniqueId())) return;
        }

        // Cancel the event and process the showcase interaction.
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Either buy or preview the buy price.
            if (event.getPlayer().isSneaking())
                previewBuy(found, event.getPlayer());
            else tryBuy(found, event.getPlayer());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Either sell or preview the sell price.
            if (event.getPlayer().isSneaking())
                previewSell(found, event.getPlayer());
            else trySell(found, event.getPlayer());
        }
    }
}
