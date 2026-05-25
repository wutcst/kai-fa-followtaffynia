package cn.edu.whut.sept.zuul.infra;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 存档服务。第一阶段使用 Java 序列化保存 GameState，后续可替换为 JSON。
 */
public final class SaveGameService
{
    private static final Path DEFAULT_SAVE_PATH = Paths.get("saves", "slot1.sav");

    private SaveGameService()
    {
    }

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
}
