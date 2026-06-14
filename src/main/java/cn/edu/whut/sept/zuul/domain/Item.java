package cn.edu.whut.sept.zuul.domain;

/**
 * 可拾取物品（逻辑层）。
 */
public class Item
{
    public static final double COOKIE_WEIGHT_BOOST = 20.0;

    private final String itemId;
    private final String name;
    private final String description;
    private final double weight;
    private final String effect;

    public Item(String itemId, String name, String description, double weight, String effect)
    {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.effect = effect;
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getWeight() { return weight; }
    public String getEffect() { return effect; }
    public boolean isMagicCookie() { return "magic-cookie".equals(itemId); }
}
