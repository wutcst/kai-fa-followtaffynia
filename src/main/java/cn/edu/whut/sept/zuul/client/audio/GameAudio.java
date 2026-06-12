package cn.edu.whut.sept.zuul.client.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * Loads shipped audio assets first and falls back to procedural sounds when
 * assets are missing. This keeps the game playable during asset iteration.
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

    public enum Track
    {
        TITLE,
        EXPLORE,
        DUNGEON,
        COMBAT
    }

    private static final int SAMPLE_RATE = 22050;
    private static final float PROCEDURAL_VOLUME = 0.32f;
    private static final float SFX_VOLUME = 0.72f;
    private static final float MUSIC_VOLUME = 0.18f;
    private static final String SFX_DIR = "assets/audio/sfx/";
    private static final String MUSIC_DIR = "assets/audio/music/";

    private final ExecutorService executor;
    private final Map<Cue, float[]> sampleCache;
    private final Map<Cue, Long> lastPlayedAt;
    private final Map<Cue, List<Sound>> soundCache;
    private final Map<Cue, Integer> soundCursor;
    private final Map<Track, Music> musicCache;

    private volatile boolean disposed;
    private volatile boolean proceduralEnabled = true;
    private volatile boolean assetSoundsEnabled = true;
    private volatile boolean musicEnabled = true;
    private AudioDevice device;
    private Music currentMusic;
    private Track currentTrack;

    public GameAudio()
    {
        sampleCache = new EnumMap<>(Cue.class);
        lastPlayedAt = new EnumMap<>(Cue.class);
        soundCache = new EnumMap<>(Cue.class);
        soundCursor = new EnumMap<>(Cue.class);
        musicCache = new EnumMap<>(Track.class);
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

    public void playMusic(Track track)
    {
        if (!musicEnabled || disposed || track == null) {
            return;
        }
        if (track == currentTrack && currentMusic != null && currentMusic.isPlaying()) {
            return;
        }
        try {
            Music next = musicFor(track);
            if (next == null) {
                stopMusic();
                return;
            }
            if (currentMusic != null && currentMusic != next) {
                currentMusic.stop();
            }
            currentMusic = next;
            currentTrack = track;
            currentMusic.setLooping(true);
            currentMusic.setVolume(MUSIC_VOLUME);
            currentMusic.play();
        } catch (Throwable ignored) {
            musicEnabled = false;
        }
    }

    public void stopMusic()
    {
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentMusic = null;
        currentTrack = null;
    }

    private void play(final Cue cue, final float volume, long cooldownMillis)
    {
        if (disposed || cue == null) {
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

        if (playAssetSound(cue, volume)) {
            return;
        }
        playProcedural(cue, volume);
    }

    private boolean playAssetSound(Cue cue, float volume)
    {
        if (!assetSoundsEnabled || disposed) {
            return false;
        }
        try {
            List<Sound> sounds = soundsFor(cue);
            if (sounds.isEmpty()) {
                return false;
            }
            int index = nextSoundIndex(cue, sounds.size());
            sounds.get(index).play(clamp(SFX_VOLUME * volume, 0f, 1f));
            return true;
        } catch (Throwable ignored) {
            assetSoundsEnabled = false;
            return false;
        }
    }

    private synchronized List<Sound> soundsFor(Cue cue)
    {
        List<Sound> cached = soundCache.get(cue);
        if (cached != null) {
            return cached;
        }
        String[] paths = soundPathsFor(cue);
        List<Sound> sounds = new ArrayList<>();
        for (String path : paths) {
            FileHandle file = Gdx.files.internal(path);
            if (file.exists()) {
                sounds.add(Gdx.audio.newSound(file));
            }
        }
        List<Sound> result = sounds.isEmpty()
            ? Collections.<Sound>emptyList()
            : Collections.unmodifiableList(sounds);
        soundCache.put(cue, result);
        return result;
    }

    private synchronized int nextSoundIndex(Cue cue, int size)
    {
        Integer current = soundCursor.get(cue);
        int index = current == null ? 0 : current.intValue();
        soundCursor.put(cue, Integer.valueOf((index + 1) % size));
        return index;
    }

    private synchronized Music musicFor(Track track)
    {
        Music cached = musicCache.get(track);
        if (cached != null) {
            return cached;
        }
        String path = musicPathFor(track);
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists()) {
            return null;
        }
        Music music = Gdx.audio.newMusic(file);
        musicCache.put(track, music);
        return music;
    }

    private void playProcedural(final Cue cue, final float volume)
    {
        if (!proceduralEnabled || disposed) {
            return;
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
            proceduralEnabled = false;
        }
    }

    private void playNow(Cue cue, float volume)
    {
        if (disposed || !proceduralEnabled) {
            return;
        }
        try {
            AudioDevice audioDevice = getDevice();
            if (audioDevice == null) {
                return;
            }
            audioDevice.setVolume(clamp(PROCEDURAL_VOLUME * volume, 0f, 1f));
            float[] samples = samplesFor(cue);
            audioDevice.writeSamples(samples, 0, samples.length);
        } catch (Throwable ignored) {
            proceduralEnabled = false;
        }
    }

    private synchronized AudioDevice getDevice()
    {
        if (disposed || !proceduralEnabled) {
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

    private static String[] soundPathsFor(Cue cue)
    {
        switch (cue) {
            case CLICK:
                return paths("click_1.ogg", "click_2.ogg");
            case STEP:
                return paths("step_1.ogg", "step_2.ogg", "step_3.ogg");
            case PICKUP:
                return paths("pickup_1.ogg", "pickup_2.ogg");
            case DOOR:
                return paths("door_1.ogg", "door_2.ogg");
            case SAVE:
                return paths("save.ogg");
            case LOAD:
                return paths("load.ogg");
            case ERROR:
                return paths("error.ogg");
            case ATTACK:
                return paths("attack_1.ogg", "attack_2.ogg");
            case HIT:
                return paths("hit.ogg");
            case USE:
                return paths("use_1.ogg", "use_2.ogg");
            case MENU_OPEN:
                return paths("menu_open.ogg");
            case MENU_CLOSE:
                return paths("menu_close.ogg");
            case DASH:
                return paths("dash.ogg");
            default:
                return new String[0];
        }
    }

    private static String[] paths(String... fileNames)
    {
        String[] paths = new String[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            paths[i] = SFX_DIR + fileNames[i];
        }
        return paths;
    }

    private static String musicPathFor(Track track)
    {
        switch (track) {
            case TITLE:
                return MUSIC_DIR + "title.wav";
            case EXPLORE:
                return MUSIC_DIR + "explore.wav";
            case DUNGEON:
                return MUSIC_DIR + "dungeon.wav";
            case COMBAT:
                return MUSIC_DIR + "combat.wav";
            default:
                return MUSIC_DIR + "explore.wav";
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
        stopMusic();
        executor.shutdownNow();
        synchronized (this) {
            if (device != null) {
                device.dispose();
                device = null;
            }
            for (List<Sound> sounds : soundCache.values()) {
                for (Sound sound : sounds) {
                    sound.dispose();
                }
            }
            soundCache.clear();
            for (Music music : musicCache.values()) {
                music.dispose();
            }
            musicCache.clear();
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
