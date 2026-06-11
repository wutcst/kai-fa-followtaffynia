package cn.edu.whut.sept.zuul.client.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * Tiny procedural sound effects. This avoids shipping extra binary assets while
 * still giving the client immediate feedback for common actions.
 */
public final class GameAudio implements Disposable
{
    public enum Cue
    {
        CLICK,
        STEP,
        PICKUP,
        DOOR,
        SAVE,
        LOAD,
        ERROR,
        ATTACK,
        HIT,
        USE,
        MENU_OPEN,
        MENU_CLOSE,
        DASH
    }

    private static final int SAMPLE_RATE = 22050;
    private static final float MASTER_VOLUME = 0.32f;

    private final ExecutorService executor;
    private final Map<Cue, float[]> sampleCache;
    private final Map<Cue, Long> lastPlayedAt;

    private volatile boolean disposed;
    private volatile boolean enabled = true;
    private AudioDevice device;

    public GameAudio()
    {
        sampleCache = new EnumMap<>(Cue.class);
        lastPlayedAt = new EnumMap<>(Cue.class);
        executor = Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable runnable)
            {
                Thread thread = new Thread(runnable, "zuul-sfx");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public void play(Cue cue)
    {
        play(cue, 1f, 0L);
    }

    public void playThrottled(Cue cue, long cooldownMillis)
    {
        play(cue, 1f, cooldownMillis);
    }

    private void play(final Cue cue, final float volume, long cooldownMillis)
    {
        if (!enabled || disposed || cue == null) {
            return;
        }

        long now = TimeUtils.millis();
        synchronized (lastPlayedAt) {
            Long last = lastPlayedAt.get(cue);
            if (last != null && now - last < cooldownMillis) {
                return;
            }
            lastPlayedAt.put(cue, now);
        }

        try {
            executor.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    playNow(cue, volume);
                }
            });
        } catch (RejectedExecutionException ignored) {
            enabled = false;
        }
    }

    private void playNow(Cue cue, float volume)
    {
        if (disposed || !enabled) {
            return;
        }
        try {
            AudioDevice audioDevice = getDevice();
            if (audioDevice == null) {
                return;
            }
            audioDevice.setVolume(clamp(MASTER_VOLUME * volume, 0f, 1f));
            float[] samples = samplesFor(cue);
            audioDevice.writeSamples(samples, 0, samples.length);
        } catch (Throwable ignored) {
            enabled = false;
        }
    }

    private synchronized AudioDevice getDevice()
    {
        if (disposed || !enabled) {
            return null;
        }
        if (device == null) {
            device = Gdx.audio.newAudioDevice(SAMPLE_RATE, true);
        }
        return device;
    }

    private float[] samplesFor(Cue cue)
    {
        synchronized (sampleCache) {
            float[] cached = sampleCache.get(cue);
            if (cached != null) {
                return cached;
            }
            float[] generated = createCue(cue);
            sampleCache.put(cue, generated);
            return generated;
        }
    }

    private float[] createCue(Cue cue)
    {
        switch (cue) {
            case CLICK:
                return concat(sweep(36, 760f, 1120f, 0.55f, Wave.SQUARE),
                    sweep(32, 1120f, 820f, 0.35f, Wave.SQUARE));
            case STEP:
                return concat(sweep(24, 110f, 86f, 0.18f, Wave.NOISE),
                    sweep(34, 95f, 70f, 0.20f, Wave.TRIANGLE));
            case PICKUP:
                return concat(sweep(48, 620f, 920f, 0.44f, Wave.SQUARE),
                    sweep(58, 920f, 1320f, 0.36f, Wave.SQUARE),
                    sweep(46, 1320f, 1680f, 0.24f, Wave.SINE));
            case DOOR:
                return concat(sweep(90, 170f, 120f, 0.38f, Wave.TRIANGLE),
                    sweep(95, 118f, 92f, 0.30f, Wave.NOISE));
            case SAVE:
                return concat(sweep(54, 523f, 523f, 0.38f, Wave.SQUARE),
                    sweep(54, 659f, 659f, 0.38f, Wave.SQUARE),
                    sweep(72, 784f, 880f, 0.30f, Wave.SQUARE));
            case LOAD:
                return concat(sweep(54, 784f, 784f, 0.34f, Wave.SQUARE),
                    sweep(54, 659f, 659f, 0.32f, Wave.SQUARE),
                    sweep(72, 523f, 440f, 0.28f, Wave.SQUARE));
            case ERROR:
                return concat(sweep(72, 240f, 190f, 0.42f, Wave.SQUARE),
                    sweep(90, 190f, 140f, 0.36f, Wave.SQUARE));
            case ATTACK:
                return concat(sweep(44, 240f, 780f, 0.42f, Wave.NOISE),
                    sweep(58, 780f, 440f, 0.30f, Wave.SQUARE));
            case HIT:
                return concat(sweep(48, 170f, 96f, 0.42f, Wave.NOISE),
                    sweep(92, 96f, 62f, 0.32f, Wave.TRIANGLE));
            case USE:
                return concat(sweep(55, 420f, 720f, 0.36f, Wave.SINE),
                    sweep(70, 720f, 980f, 0.30f, Wave.SQUARE));
            case MENU_OPEN:
                return concat(sweep(42, 520f, 760f, 0.30f, Wave.SQUARE),
                    sweep(48, 760f, 980f, 0.24f, Wave.SQUARE));
            case MENU_CLOSE:
                return concat(sweep(42, 760f, 520f, 0.30f, Wave.SQUARE),
                    sweep(48, 520f, 360f, 0.22f, Wave.SQUARE));
            case DASH:
                return concat(sweep(34, 260f, 620f, 0.30f, Wave.NOISE),
                    sweep(46, 620f, 320f, 0.24f, Wave.SQUARE));
            default:
                return sweep(50, 440f, 440f, 0.25f, Wave.SQUARE);
        }
    }

    private static float[] sweep(int millis, float startHz, float endHz, float volume, Wave wave)
    {
        int count = Math.max(1, SAMPLE_RATE * millis / 1000);
        float[] samples = new float[count];
        double phase = 0.0;
        long noise = 0x13579BDFL + millis + (long) startHz * 31L + (long) endHz * 17L;
        for (int i = 0; i < count; i++) {
            float t = count <= 1 ? 1f : (float) i / (float) (count - 1);
            float hz = startHz + (endHz - startHz) * t;
            phase += Math.PI * 2.0 * hz / SAMPLE_RATE;
            float value;
            if (wave == Wave.NOISE) {
                noise = noise * 1664525L + 1013904223L;
                value = (((noise >>> 16) & 0xffff) / 32768f) - 1f;
            } else if (wave == Wave.SQUARE) {
                value = Math.sin(phase) >= 0 ? 1f : -1f;
            } else if (wave == Wave.TRIANGLE) {
                value = (float) (2.0 / Math.PI * Math.asin(Math.sin(phase)));
            } else {
                value = (float) Math.sin(phase);
            }
            samples[i] = value * volume * envelope(t);
        }
        return samples;
    }

    private static float envelope(float t)
    {
        float attack = t < 0.08f ? t / 0.08f : 1f;
        float release = t > 0.72f ? Math.max(0f, (1f - t) / 0.28f) : 1f;
        return attack * release;
    }

    private static float[] concat(float[]... chunks)
    {
        int length = 0;
        for (float[] chunk : chunks) {
            length += chunk.length;
        }
        float[] out = new float[length];
        int offset = 0;
        for (float[] chunk : chunks) {
            System.arraycopy(chunk, 0, out, offset, chunk.length);
            offset += chunk.length;
        }
        return out;
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void dispose()
    {
        disposed = true;
        executor.shutdownNow();
        synchronized (this) {
            if (device != null) {
                device.dispose();
                device = null;
            }
        }
    }

    private enum Wave
    {
        SINE,
        SQUARE,
        TRIANGLE,
        NOISE
    }
}
