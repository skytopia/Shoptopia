package solar.rpg.shoptopia.data;

import org.bukkit.inventory.ItemStack;

/**
 * Contains information on what is available in a showcase.
 * This consists of an item stack, and how much it is worth.
 * <em>This can be treated as either selling or buying data.</em>
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class PurchaseData {

    private final ItemStack STOCK;
    private final Double PRICE;

    /**
     * Purchase data constructor.
     *
     * @param STOCK The stock that is purchasable.
     * @param PRICE The price of the stock.
     */
    public PurchaseData(ItemStack STOCK, Double PRICE) {
        this.STOCK = STOCK;
        this.PRICE = PRICE;
    }

    /**
     * Returns the purchase data's stock.
     *
     * @return The stock.
     */
    public ItemStack getStock() {
        return STOCK.clone();
    }

    /**
     * Returns the amount of stock in consideration.
     *
     * @return The amount.
     * @see ItemStack
     */
    public Integer getAmount() {
        return getStock().getAmount();
    }

    /**
     * Return the price of the stock.
     *
     * @return The price.
     */
    public Double getPrice() {
        return PRICE;
    }
}
