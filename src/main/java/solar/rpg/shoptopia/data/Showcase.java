package solar.rpg.shoptopia.data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import solar.rpg.shoptopia.Main;
import solar.rpg.shoptopia.player.ShopListener;

/**
 * Showcase model. Contains location, icon, and purchasing information.
 * Is able to spawn itself and interact with the listeners.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class Showcase {

    private final Boolean RESTRICTED;
    private final int OWNER_ID;
    /* Reference to JavaPlugin. */
    private Main PLUGIN;
    /* Unchanging showcase information. */
    private PurchaseData BUY_DATA;
    private PurchaseData SELL_DATA;
    private Location SHOWCASE_LOCATION;
    private ItemStack ICON;

    /* Item drop displayed on showcase. */
    private Item SHOWCASE_DROP;

    /**
     * Showcase constructor.
     *
     * @param x          X-location of showcase half-slab.
     * @param y          Y-location of showcase half-slab.
     * @param z          Z-location of showcase half-slab.
     * @param ICON       The item that the showcase will display.
     * @param BUY_DATA   The showcase's buyable item, if any.
     * @param SELL_DATA  The showcase's sellable item, if any.
     * @param RESTRICTED Whether or not this is a restricted shop.
     * @param OWNER_ID   Owning island's ID. -1 if admin shop.
     */
    public Showcase(Main PLUGIN, int x, int y, int z, ItemStack ICON, PurchaseData BUY_DATA, PurchaseData SELL_DATA, Boolean RESTRICTED, int OWNER_ID, World creationWorld) {
        this.PLUGIN = PLUGIN;
        this.ICON = ICON;
        this.BUY_DATA = BUY_DATA;
        this.SELL_DATA = SELL_DATA;
        this.RESTRICTED = RESTRICTED;
        this.OWNER_ID = OWNER_ID;
        SHOWCASE_LOCATION = new Location(creationWorld, x, y, z);
        create();
    }

    /**
     * Utility method.
     * Checks if two locations share the same Z and X axis.
     *
     * @param l1 The first location.
     * @param l2 The second location.
     * @return True if both locations share the same Z and X value.
     */
    public static boolean matchXZ(Location l1, Location l2) {
        return l1.getBlockX() == l2.getBlockX() && l1.getBlockZ() == l2.getBlockZ();
    }

    /**
     * Registers the showcase's clickspace with the listener.
     * Respawns the item drop for the showcase afterwards.
     *
     * @see ShopListener
     */
    private void create() {
        getShopListener().getClickspace().put(getPosition(), this);
        getShopListener().getClickspace().put(getPosition().clone().subtract(0, 1, 0), this);
        respawn();
    }

    /**
     * @return True if this showcase is restricted to donators.
     */
    public boolean isDonatorOnly() {
        return RESTRICTED;
    }

    public void respawn() {
        World world = SHOWCASE_LOCATION.getWorld();
        if (SHOWCASE_DROP != null) {
            if (SHOWCASE_DROP.isValid() && !SHOWCASE_DROP.isDead()) return;
            if (!SHOWCASE_LOCATION.getChunk().isLoaded()) return;
            getShopListener().getExempt().remove(SHOWCASE_DROP);
            SHOWCASE_DROP.remove();
        }
        SHOWCASE_DROP = world.dropItem(SHOWCASE_LOCATION.clone().add(0.5, 0.505, 0.5), ICON);
        SHOWCASE_DROP.setVelocity(new Vector(0, -1, 0));
        SHOWCASE_DROP.setPickupDelay(9999 * 9999);
        getShopListener().getExempt().add(SHOWCASE_DROP);
    }

    /**
     * Destroys the item drop associated with this showcase.
     * Removes information about itself to speed up GC.
     */
    public void destroy() {
        getShopListener().getExempt().remove(SHOWCASE_DROP);
        SHOWCASE_DROP.remove();
        SHOWCASE_DROP = null;
        PLUGIN = null;
        ICON = null;
        BUY_DATA = null;
        SELL_DATA = null;
        SHOWCASE_LOCATION = null;
    }

    /**
     * Showcases can belong to either the admin shop or an island.
     * There are different (but similar) listener classes for both.
     *
     * @return Appropriate shop listener for th is showcase.
     */
    private ShopListener getShopListener() {
        return isAdminShop() ? PLUGIN.getAdminShopListener() : PLUGIN.getPlayerShopListener();
    }

    /**
     * @return The showcase's item drop on display.
     */
    public Item getShowcaseDrop() {
        return SHOWCASE_DROP;
    }

    /**
     * @return True if an admin shop, false if player shop
     */
    public boolean isAdminShop() {
        return OWNER_ID == -1;
    }

    /**
     * @return The showcase's location.
     */
    public Location getPosition() {
        return SHOWCASE_LOCATION;
    }

    /**
     * @return True if you can buy from this showcase.
     */
    public boolean canBuy() {
        return BUY_DATA != null;
    }

    /**
     * @return Data related to buying from this showcase.
     */
    public PurchaseData getBuyData() {
        return BUY_DATA;
    }

    /**
     * @return True if you can sell to this showcase.
     */
    public boolean canSell() {
        return SELL_DATA != null;
    }

    /**
     * @return Data related to selling to this showcase.
     */
    public PurchaseData getSellData() {
        return SELL_DATA;
    }
}
