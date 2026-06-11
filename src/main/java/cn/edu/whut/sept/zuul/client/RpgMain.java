package cn.edu.whut.sept.zuul.client;

import cn.edu.whut.sept.zuul.client.audio.GameAudio;
import cn.edu.whut.sept.zuul.client.screen.TitleScreen;
import cn.edu.whut.sept.zuul.client.ui.GameFonts;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class RpgMain extends Game
{
    private SpriteBatch batch;
    private GameFonts fonts;
    private GameAudio audio;

    @Override
    public void create()
    {
        batch = new SpriteBatch();
        fonts = new GameFonts();
        audio = new GameAudio();
        setScreen(new TitleScreen(this, batch));
    }

    public SpriteBatch getBatch()
    {
        return batch;
    }

    public GameFonts getFonts()
    {
        return fonts;
    }

    public GameAudio getAudio()
    {
        return audio;
    }

    @Override
    public void dispose()
    {
        if (fonts != null) {
            fonts.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (audio != null) {
            audio.dispose();
        }
        super.dispose();
    }
}
