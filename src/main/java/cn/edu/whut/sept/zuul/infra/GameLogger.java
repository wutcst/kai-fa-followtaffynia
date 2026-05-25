package cn.edu.whut.sept.zuul.infra;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 游戏日志工具，输出到 ./game.log 文件，方便运行后检查游戏状态。
 */
public final class GameLogger
{
    private static final Logger LOG = Logger.getLogger("Game");
    private static boolean initialized;

    private GameLogger()
    {
    }

    /** 简洁的 UTF-8 日志格式：[HH:mm:ss] message */
    private static final class CompactFormatter extends Formatter
    {
        @Override
        public String format(LogRecord record)
        {
            String ts = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(ts).append("] ");
            sb.append(record.getMessage());
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append("\n").append(sw);
            }
            sb.append("\n");
            return sb.toString();
        }
    }

    public static void init()
    {
        if (initialized) {
            return;
        }
        try {
            FileHandler fh = new FileHandler("game.log", false);
            fh.setFormatter(new CompactFormatter());
            fh.setEncoding("UTF-8");
            fh.setLevel(Level.ALL);
            LOG.addHandler(fh);
            LOG.setLevel(Level.ALL);
            LOG.setUseParentHandlers(false);
            initialized = true;
        } catch (IOException e) {
            System.err.println("[GameLogger] Failed to init file handler: " + e.getMessage());
        }
    }

    public static Logger get()
    {
        if (!initialized) {
            init();
        }
        return LOG;
    }

    public static String timestamp()
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /** short cut */
    public static void info(String msg)
    {
        if (!initialized) init();
        LOG.info(msg);
    }

    public static void warn(String msg)
    {
        if (!initialized) init();
        LOG.warning(msg);
    }
}
