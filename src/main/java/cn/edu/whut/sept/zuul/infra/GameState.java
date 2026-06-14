package cn.edu.whut.sept.zuul.infra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 游戏动态状态快照，用于 save/load。
 */
public class GameState implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String playerName;
    private String currentRoomId;
    private float playerX;
    private float playerY;
    private String facing;
    private String entryDirection;
    private int hp;
    private int maxHp;
    private double maxWeight;
    private int reputation;
    private List<String> inventory;
    private Set<String> unlockedLocks;
    private Map<String, String> questStates;
    private Set<String> defeatedNpcs;
    private Set<String> exploredRoomIds;
    private List<String> roomHistory;
    private Map<String, List<String>> roomItems;

    public GameState()
    {
        inventory = new ArrayList<>();
        unlockedLocks = new HashSet<>();
        questStates = new HashMap<>();
        defeatedNpcs = new HashSet<>();
        exploredRoomIds = new HashSet<>();
        roomHistory = new ArrayList<>();
        roomItems = new HashMap<>();
        facing = "down";
        entryDirection = "default";
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }

    public String getCurrentRoomId()
    {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId)
    {
        this.currentRoomId = currentRoomId;
    }

    public float getPlayerX()
    {
        return playerX;
    }

    public void setPlayerX(float playerX)
    {
        this.playerX = playerX;
    }

    public float getPlayerY()
    {
        return playerY;
    }

    public void setPlayerY(float playerY)
    {
        this.playerY = playerY;
    }

    public String getFacing()
    {
        return facing;
    }

    public void setFacing(String facing)
    {
        this.facing = facing;
    }

    public String getEntryDirection()
    {
        return entryDirection;
    }

    public void setEntryDirection(String entryDirection)
    {
        this.entryDirection = entryDirection;
    }

    public int getHp()
    {
        return hp;
    }

    public void setHp(int hp)
    {
        this.hp = hp;
    }

    public int getMaxHp()
    {
        return maxHp;
    }

    public void setMaxHp(int maxHp)
    {
        this.maxHp = maxHp;
    }

    public double getMaxWeight()
    {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight)
    {
        this.maxWeight = maxWeight;
    }

    public int getReputation()
    {
        return reputation;
    }

    public void setReputation(int reputation)
    {
        this.reputation = reputation;
    }

    public List<String> getInventory()
    {
        return inventory;
    }

    public Set<String> getUnlockedLocks()
    {
        return unlockedLocks;
    }

    public Map<String, String> getQuestStates()
    {
        return questStates;
    }

    public Set<String> getDefeatedNpcs()
    {
        return defeatedNpcs;
    }

    public Set<String> getExploredRoomIds()
    {
        return exploredRoomIds;
    }

    public List<String> getRoomHistory()
    {
        return roomHistory;
    }

    public Map<String, List<String>> getRoomItems()
    {
        if (roomItems == null) {
            roomItems = new HashMap<>();
        }
        return roomItems;
    }
}
