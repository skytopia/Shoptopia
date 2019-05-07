package solar.rpg.shoptopia.data;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import solar.rpg.shoptopia.Main;
import solar.rpg.skyblock.util.stored.sql.Database;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Handler is responsible for storing & handling showcases.
 * Showcases are loaded in using Loader. Helpful methods are
 * used by the commands and shop listeners from this class.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.1
 */
public class Handler {

    /* Store reference of JavaPlugin + Skytopia Database implementation */
    private final Main PLUGIN;
    private final Database DB;

    /* Store reference of Loader. */
    private final Loader LOADER;

    /* Stores admin showcases and player-made showcases. */
    private Showcase[] SHOWCASES;
    private HashMap<Integer, Showcase[]> PLAYER_SHOWCASES;

    /* Instance of server economy utility. */
    private Economy ECON;

    public Handler(Main PLUGIN) {
        this.PLUGIN = PLUGIN;
        this.DB = solar.rpg.skyblock.Main.instance.main().sql().db;
        LOADER = new Loader();
    }

    /**
     * Destroys local instances of existing showcases, if any.
     * Loads in admin showcases from XML and player showcases from the database.
     */
    public boolean reload() {
        destroyShowcases();

        // Parse .xml configuration for admin showcases.
        SHOWCASES = LOADER.loadAdminShowcases(PLUGIN).toArray(new Showcase[0]);
        Main.log(Level.FINE, "Configuration scan complete! Discovered " + SHOWCASES.length + " admin showcases!");

        // Attempt to create database table if it doesn't exist.
        if (LOADER.checkTables(DB)) {
            // Parse database table for player showcases.
            PLAYER_SHOWCASES = LOADER.loadPlayerShowcases(PLUGIN, DB);
            Main.log(Level.FINE, "Database table scan complete! Discovered " + PLAYER_SHOWCASES.size() + " islands with existing showcases!");
        } else {
            Main.log(Level.SEVERE, "Unable to check database table. Skipping player showcases... ");
            return false;
        }
        return true;
    }

    /**
     * Destroy all locally-created showcase instances.
     */
    public void destroyShowcases() {
        // Destroy admin shop showcases.
        if (SHOWCASES != null)
            for (Showcase sc : SHOWCASES)
                sc.destroy();
        SHOWCASES = null;
        PLUGIN.getAdminShopListener().getClickspace().clear();

        // Destroy player shop showcases.
        if (PLAYER_SHOWCASES != null) {
            for (Showcase[] set : PLAYER_SHOWCASES.values())
                for (Showcase showcase : set)
                    if (showcase != null)
                        showcase.destroy();
            PLAYER_SHOWCASES.clear();
        }
        PLAYER_SHOWCASES = null;
        PLUGIN.getPlayerShopListener().getClickspace().clear();
    }

    /**
     * Attempts to hook into the Vault plugin, which
     * is needed for economy and permissions checking.
     *
     * @return True if hook to Vault was successful.
     */
    public boolean setupEconomy() {
        if (PLUGIN.getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = PLUGIN.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        ECON = rsp.getProvider();
        return ECON != null;
    }

    /**
     * @param loc       A location.
     * @param showcases An array of showcases.
     * @return A showcase if one exists at the location, otherwise null.
     */
    public Showcase isShowcaseAt(Location loc, Showcase... showcases) {
        if (showcases != null)
            for (Showcase sc : showcases)
                if (sc != null)
                    if (loc.equals(sc.getPosition())) return sc;
        return null;
    }

    /**
     * @param inv An inventory, entity or player.
     * @return The available item in the inventory.
     */
    public ItemStack findNextItem(Inventory inv) {
        for (ItemStack found : inv.getContents())
            if (found != null)
                if (found.getType() != Material.AIR)
                    return found;
        return null;
    }

    /**
     * Removes a player showcase from an island.
     *
     * @param islandID Player's island ID.
     * @param oldCase  The showcase to remove.
     */
    public void removePlayerShowcase(Integer islandID, Showcase oldCase) {
        if (PLAYER_SHOWCASES.containsKey(islandID)) {
            // Find index of old showcase and remove it from the island's array.
            for (int i = 0; i < PLAYER_SHOWCASES.get(islandID).length; i++)
                if (PLAYER_SHOWCASES.get(islandID)[i] != null)
                    if (PLAYER_SHOWCASES.get(islandID)[i].getPosition().equals(oldCase.getPosition()))
                        PLAYER_SHOWCASES.get(islandID)[i] = null;
        } else {
            // Create empty array for this island.
            Showcase[] showcases = new Showcase[12];
            PLAYER_SHOWCASES.put(islandID, showcases);
        }
    }

    /**
     * Adds a new player showcase into the existing PLAYER_SHOWCASES map.
     *
     * @see #handlePlayerShowcase(HashMap, Integer, Showcase)
     */
    public void addPlayerShowcase(Integer islandID, Showcase newCase) {
        handlePlayerShowcase(PLAYER_SHOWCASES, islandID, newCase);
    }

    /**
     * Inserts a showcase into an island's showcase array.
     * Creates the array if it does not exist already.
     * <em>The map of island IDs and showcase arrays must be provided.</em>
     *
     * @param islandID An island's ID.
     * @param newCase  The created showcase.
     */
    void handlePlayerShowcase(HashMap<Integer, Showcase[]> result, Integer islandID, Showcase newCase) {
        if (result.containsKey(islandID)) {
            // Adds a showcase to an island's next available index.
            for (int i = 0; i < result.get(islandID).length; i++)
                if (result.get(islandID)[i] == null) {
                    result.get(islandID)[i] = newCase;
                    return;
                }
        } else {
            // Creates the showcase array for the island, places the showcase in the first index.
            Showcase[] showcases = new Showcase[12];
            showcases[0] = newCase;
            result.put(islandID, showcases);
        }
    }

    /**
     * @param islandID An island's ID.
     * @return The amount of showcases made on the island.
     */
    public int getTotalShowcases(Integer islandID) {
        if (!PLAYER_SHOWCASES.containsKey(islandID))
            return 0;
        int total = 0;
        for (int i = 0; i < PLAYER_SHOWCASES.get(islandID).length; i++)
            if (PLAYER_SHOWCASES.get(islandID)[i] != null)
                total++;
        return total;
    }

    /**
     * @return All existing admin showcases.
     */
    public Showcase[] getShowcases() {
        return SHOWCASES;
    }

    /**
     * @return All existing player-made showcases.
     */
    public Showcase[] getPlayerShowcases() {
        Collection<Showcase[]> all = PLAYER_SHOWCASES.values();
        Set<Showcase> total = new HashSet<>();
        for (Showcase[] sca : all)
            for (Showcase sc : sca)
                if (sc != null)
                    total.add(sc);
        return total.toArray(new Showcase[0]);
    }

    /**
     * @return The map of island IDs to showcase arrays.
     */
    public HashMap<Integer, Showcase[]> getPlayerShowcasesMap() {
        return PLAYER_SHOWCASES;
    }

    /**
     * @return The Vault economy instance.
     */
    public Economy getEconomy() {
        return ECON;
    }

}
