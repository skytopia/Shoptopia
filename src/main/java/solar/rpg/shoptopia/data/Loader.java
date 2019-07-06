package solar.rpg.shoptopia.data;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import solar.rpg.shoptopia.Main;
import solar.rpg.skyblock.stored.Database;
import solar.rpg.skyblock.stored.Settings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Loader is responsible for scanning the .XML config and database table.
 * These values are returned to the Handler where they are then stored.
 *
 * @author lavuh
 * @version 1.1
 * @see Handler
 * @since 1.1
 */
class Loader {

    Loader() {
    }

    /**
     * Checks if the player showcases database table exists.
     * If it doesn't, it will attempt to create it.
     *
     * @param db Skytopia database implementation.
     * @return True if table exists, false if table was unable to be made.
     */
    boolean checkTables(Database db) {
        try {
            db.regenerateTable("Shops",
                    "CREATE TABLE `Shops` (" +
                            "`owner_id` SMALLINT UNSIGNED NOT NULL," +
                            "`amount` SMALLINT UNSIGNED NOT NULL," +
                            "`price` INT UNSIGNED NOT NULL," +
                            "`item` TINYTEXT NOT NULL," +
                            "`xyz` VARCHAR(255) NOT NULL," +
                            "PRIMARY KEY (`owner_id`, `xyz`)," +
                            "FOREIGN KEY (`owner_id`) REFERENCES Island(`island_id`));");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @param main Instance of JavaPlugin for file I/O purposes.
     * @return Parsed configuration file admin showcases.
     */
    Set<Showcase> loadAdminShowcases(Main main) {
        Set<Showcase> result = new HashSet<>();
        Main.log(Level.FINE, "Attempting to scan configuration!");

        // Attempt to make plugin data folder, if unsuccessful, return empty set.
        if (!main.getDataFolder().exists())
            if (!main.getDataFolder().mkdir())
                return result;

        // Check for XML config file.
        File config = new File(main.getDataFolder() + File.separator + "shops.xml");
        if (!config.exists())
            Main.log(Level.WARNING, "Configuration file does not exist!");
        else {
            try {
                // Load in XML config file to DOM parser.
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(config);
                doc.normalize();

                // Look through each individual <showcase/> tag.
                Element showcaseRoot = doc.getDocumentElement();
                NodeList showcases = showcaseRoot.getElementsByTagName("showcase");
                for (int i = 0; i < showcases.getLength(); i++) {
                    Element showcase = (Element) showcases.item(i);

                    // Retrieve x,y,z coordinate for showcase item.
                    int x = Integer.parseInt(showcase.getAttribute("x"));
                    int y = Integer.parseInt(showcase.getAttribute("y"));
                    int z = Integer.parseInt(showcase.getAttribute("z"));

                    // Is this showcase restricted to donators only?
                    boolean restricted = showcase.getElementsByTagName("donator").getLength() != 0;

                    Element icon = (Element) showcase.getElementsByTagName("icon").item(0);
                    ItemStack iconStack = parseItemStack(icon.getAttribute("item"));
                    PurchaseData buyData;
                    PurchaseData sellData;

                    // If there is data to buy from this showcase, parse it.
                    if (showcase.getElementsByTagName("buy").getLength() == 1) {
                        Element buy = (Element) showcase.getElementsByTagName("buy").item(0);
                        buyData = new PurchaseData(parseItemStack(buy.getAttribute("item"), Integer.parseInt(buy.getAttribute("amount"))), Double.parseDouble(buy.getAttribute("price")));
                    } else buyData = null;

                    // If there is data to sell from this showcase, parse it.
                    if (showcase.getElementsByTagName("sell").getLength() == 1) {
                        Element sell = (Element) showcase.getElementsByTagName("sell").item(0);
                        sellData = new PurchaseData(parseItemStack(sell.getAttribute("item"), Integer.parseInt(sell.getAttribute("amount"))), Double.parseDouble(sell.getAttribute("price")));
                    } else sellData = null;

                    // Create the showcase, add it to the set, continue.
                    result.add(new Showcase(main, x, y, z, iconStack, buyData, sellData, restricted, -1, Bukkit.getWorld("world")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * @param main Instance of JavaPlugin for showcase purposes.
     * @param db   Skytopia database implementation.
     * @return Parsed database table for player showcases.
     */
    HashMap<Integer, Showcase[]> loadPlayerShowcases(Main main, Database db) {
        HashMap<Integer, Showcase[]> result = new HashMap<>();
        Main.log(Level.FINE, "Attempting to scan database!");

        try {
            ResultSet check = db.prepare("SELECT * FROM `Shops`").executeQuery();

            // Go through every player shop row, attempt to add it in.
            while (check.next()) {
                // Retrieve x,y,z coordinates for location item.
                String[] xyz = check.getString("xyz").split(",");
                int x = Integer.parseInt(xyz[0]);
                int y = Integer.parseInt(xyz[1]);
                int z = Integer.parseInt(xyz[2]);

                // Parse item stack for this player showcase.
                ItemStack iconStack = parseItemStack(check.getString("item"), check.getInt("amount"));

                // Player showcases are always buy-only, so we can safely add in the buy data.
                PurchaseData buyData = new PurchaseData(iconStack, (double) check.getInt("price"));

                // Places the new showcase into the proper array index for the owning island.
                main.getHandler().handlePlayerShowcase(result, check.getInt("owner_id"), new Showcase(main, x, y, z, iconStack, buyData, null, false, check.getInt("owner_id"), Bukkit.getWorld(Settings.ADMIN_WORLD_ID)));
            }
            check.close();
        } catch (SQLException ex) {
            // Something went wrong, just print the stack trace and move on.
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Attempts to parse a String into an ItemStack.
     * The format is specific to Shoptopia.
     *
     * @param item   The item to parse.
     * @param amount Amount of the item: ignore varargs for a default of 1.
     * @return The parsed item stack.
     */
    private ItemStack parseItemStack(String item, int... amount) {
        int amt = amount.length > 0 ? amount[0] : 1;
        String[] reg = item.split(":");
        if (reg[0].startsWith("PLAYER"))
            return playerHead(reg[1], amt);
        else if (reg.length == 1)
            return new ItemStack(Material.valueOf(reg[0]), amt);
        else
            throw new IllegalArgumentException("Unexpected data: " + reg[1]);
    }

    /**
     * Parses a player head item stack's information.
     *
     * @param player The player's username.
     * @param amt    Amount of the item.
     * @return The resulting player head item stack..
     */
    private ItemStack playerHead(String player, int amt) {
        ItemStack SKULL = new ItemStack(Material.PLAYER_HEAD, amt);
        SkullMeta meta = (SkullMeta) SKULL.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(player));
        SKULL.setItemMeta(meta);
        return SKULL;
    }
}
