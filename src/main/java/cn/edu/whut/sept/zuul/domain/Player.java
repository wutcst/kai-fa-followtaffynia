package cn.edu.whut.sept.zuul.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Player
{
    public static final double DEFAULT_MAX_WEIGHT = 100.0;
    public static final int DEFAULT_MAX_HP = 100;

    private final String name;
    private int hp;
    private int maxHp;
    private double maxWeight;
    private int reputation;
    private final List<Item> inventory;

    public Player(String name)
    {
        this.name = name == null || name.trim().isEmpty() ? "编年史者" : name.trim();
        this.hp = DEFAULT_MAX_HP;
        this.maxHp = DEFAULT_MAX_HP;
        this.maxWeight = DEFAULT_MAX_WEIGHT;
        this.reputation = 0;
        this.inventory = new ArrayList<>();
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getMaxHp() { return maxHp; }
    public double getMaxWeight() { return maxWeight; }
    public void setMaxWeight(double maxWeight) { this.maxWeight = maxWeight; }
    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }
    public List<Item> getInventory() { return inventory; }

    public double totalWeight()
    {
        double total = 0;
        for (Item item : inventory) {
            total += item.getWeight();
        }
        return total;
    }

    public void addItem(Item item) { inventory.add(item); }

    public Item removeItem(String itemId)
    {
        Iterator<Item> it = inventory.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            if (item.getItemId().equals(itemId)) {
                it.remove();
                return item;
            }
        }
        return null;
    }

    public void clearInventory() { inventory.clear(); }
    public boolean isDead() { return hp <= 0; }
}
