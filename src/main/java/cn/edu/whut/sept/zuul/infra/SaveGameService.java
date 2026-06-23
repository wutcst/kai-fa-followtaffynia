package cn.edu.whut.sept.zuul.infra;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 存档服务。使用 Java 序列化保存 GameState，支持最多 5 个存档槽位。
 * 旧的无参/路径方法保留，无参版默认映射到 1 号槽位，保证向后兼容。
 */
public final class SaveGameService
{
    /** 最大存档槽位数 */
    public static final int MAX_SLOTS = 5;

    private static final Path SAVE_DIR = Paths.get("saves");
    private static final Path DEFAULT_SAVE_PATH = slotPath(1);

    private SaveGameService()
    {
    }

    // ==================== 槽位 API ====================

    /** 返回指定槽位的存档文件路径（slot 取值 1..MAX_SLOTS）。 */
    public static Path slotPath(int slot)
    {
        if (slot < 1 || slot > MAX_SLOTS) {
            throw new IllegalArgumentException("slot must be in [1, " + MAX_SLOTS + "]: " + slot);
        }
        return SAVE_DIR.resolve("slot" + slot + ".sav");
    }

    /** 指定槽位是否存在存档文件。 */
    public static boolean hasSave(int slot)
    {
        return Files.isRegularFile(slotPath(slot));
    }

    /** 是否存在任意槽位的存档。 */
    public static boolean hasAnySave()
    {
        for (int slot = 1; slot <= MAX_SLOTS; slot++) {
            if (hasSave(slot)) {
                return true;
            }
        }
        return false;
    }

    /** 保存到指定槽位。 */
    public static void save(int slot, GameState state) throws IOException
    {
        save(slotPath(slot), state);
    }

    /** 从指定槽位读取。 */
    public static GameState load(int slot) throws IOException, ClassNotFoundException
    {
        return load(slotPath(slot));
    }

    /** 读取指定槽位的概要信息（用于槽位选择界面，永不抛异常）。 */
    public static SlotInfo describe(int slot)
    {
        Path path = slotPath(slot);
        if (!Files.isRegularFile(path)) {
            return SlotInfo.empty(slot);
        }
        long timeMillis = 0L;
        try {
            timeMillis = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            // 时间获取失败时退化为 0，仍尝试读取存档内容
        }
        try {
            GameState state = load(path);
            return SlotInfo.of(slot, timeMillis, state.getPlayerName(), state.getCurrentRoomId());
        } catch (Exception e) {
            return SlotInfo.corrupt(slot, timeMillis);
        }
    }

    /** 读取全部 5 个槽位的概要信息。 */
    public static List<SlotInfo> describeAll()
    {
        List<SlotInfo> infos = new ArrayList<>(MAX_SLOTS);
        for (int slot = 1; slot <= MAX_SLOTS; slot++) {
            infos.add(describe(slot));
        }
        return infos;
    }

    // ==================== 兼容旧版（默认 1 号槽位） ====================

    public static Path defaultSavePath()
    {
        return DEFAULT_SAVE_PATH;
    }

    public static boolean hasSave()
    {
        return Files.isRegularFile(DEFAULT_SAVE_PATH);
    }

    public static void save(GameState state) throws IOException
    {
        save(DEFAULT_SAVE_PATH, state);
    }

    public static void save(Path path, GameState state) throws IOException
    {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(state);
        }
    }

    public static GameState load() throws IOException, ClassNotFoundException
    {
        return load(DEFAULT_SAVE_PATH);
    }

    public static GameState load(Path path) throws IOException, ClassNotFoundException
    {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            return (GameState) in.readObject();
        }
    }

    // ==================== 槽位概要 ====================

    /**
     * 单个存档槽位的概要信息，用于槽位选择界面显示，不包含完整 GameState。
     */
    public static final class SlotInfo
    {
        private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");

        private final int slot;
        private final boolean exists;
        private final boolean corrupt;
        private final long timeMillis;
        private final String playerName;
        private final String roomId;

        private SlotInfo(int slot, boolean exists, boolean corrupt, long timeMillis,
            String playerName, String roomId)
        {
            this.slot = slot;
            this.exists = exists;
            this.corrupt = corrupt;
            this.timeMillis = timeMillis;
            this.playerName = playerName;
            this.roomId = roomId;
        }

        static SlotInfo empty(int slot)
        {
            return new SlotInfo(slot, false, false, 0L, null, null);
        }

        static SlotInfo corrupt(int slot, long timeMillis)
        {
            return new SlotInfo(slot, true, true, timeMillis, null, null);
        }

        static SlotInfo of(int slot, long timeMillis, String playerName, String roomId)
        {
            return new SlotInfo(slot, true, false, timeMillis, playerName, roomId);
        }

        public int getSlot()
        {
            return slot;
        }

        public boolean exists()
        {
            return exists;
        }

        public boolean isCorrupt()
        {
            return corrupt;
        }

        /** 是否为可读取的有效存档（存在且未损坏）。 */
        public boolean isLoadable()
        {
            return exists && !corrupt;
        }

        public long getTimeMillis()
        {
            return timeMillis;
        }

        public String getPlayerName()
        {
            return playerName;
        }

        public String getRoomId()
        {
            return roomId;
        }

        /** 格式化的存档时间；无时间信息时返回空串。 */
        public String formattedTime()
        {
            if (timeMillis <= 0L) {
                return "";
            }
            synchronized (TIME_FORMAT) {
                return TIME_FORMAT.format(new Date(timeMillis));
            }
        }
    }
}
