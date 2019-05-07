package solar.rpg.shoptopia;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import solar.rpg.shoptopia.data.Handler;
import solar.rpg.shoptopia.data.Showcase;
import solar.rpg.shoptopia.player.Commands;
import solar.rpg.shoptopia.player.ShopListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main plugin class. Acts as entry point and central point of plugin.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class Main extends JavaPlugin {

    /* Keep a static instance of the logger after enabling so all classes can log. */
    private static Logger logger;

    /* Showcase event listeners. */
    private ShopListener ADMIN_SHOP_LISTENER, PLAYER_SHOP_LISTENER;

    /* Showcase data handler. */
    private Handler HANDLER;

    /* Commands handler. */
    private Commands COMMANDS;

    /**
     * Global logging method. Prints out to console with Shoptopia prefix.
     *
     * @param level Logging level.
     * @param msg   Message to log.
     */
    public static void log(Level level, String msg) {
        logger.log(level, String.format("[Shoptopia] %s", msg));
    }

    public void onEnable() {
        logger = getLogger();
        log(Level.FINE, String.format("Enabling Shoptopia v%s!", getDescription().getVersion()));

        // Setup showcase handler.
        HANDLER = new Handler(this);

        // Create anonymous shop listener classes for both kinds of showcases.
        ADMIN_SHOP_LISTENER = new ShopListener(this) {
            protected Showcase[] getShowcases() {
                return getHandler().getShowcases();
            }
        };
        PLAYER_SHOP_LISTENER = new ShopListener(this) {
            protected Showcase[] getShowcases() {
                return getHandler().getPlayerShowcases();
            }
        };

        // Setup commands handler.
        COMMANDS = new Commands(this);

        if (!getHandler().setupEconomy()) {
            log(Level.SEVERE, "Unable to hook into economy plugin, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else
            log(Level.FINE, String.format("Hooked into economy plugin! Detected: %s", getHandler().getEconomy().getName()));

        // Load in showcase data from configuration file and database.
        getHandler().reload();
        log(Level.FINE, "Plugin successfully loaded with no issues. Hello!");
    }

    public void onDisable() {
        HANDLER.destroyShowcases();
        log(Level.INFO, "Showcases have been destroyed. Goodbye!");
    }

    /**
     * @return Instance of the admin shop listener.
     */
    public ShopListener getAdminShopListener() {
        return ADMIN_SHOP_LISTENER;
    }

    /**
     * @return Instance of the player shop lsitener.
     */
    public ShopListener getPlayerShopListener() {
        return PLAYER_SHOP_LISTENER;
    }

    /**
     * @return Instance of showcase handler.
     */
    public Handler getHandler() {
        return HANDLER;
    }

    /**
     * Passes off command handling to the Commands handler.
     *
     * @see Commands#onCommand(CommandSender, Command, String[])
     */
    public boolean onCommand(CommandSender sender, Command cmd, String label, final String[] args) {
        return COMMANDS.onCommand(sender, cmd, args);
    }
}

