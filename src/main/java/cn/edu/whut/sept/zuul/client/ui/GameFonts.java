package cn.edu.whut.sept.zuul.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

/**
 * 使用 FreeType 加载支持中文的字体。LibGDX 默认 BitmapFont 仅含 ASCII，中文会显示为方框。
 */
public class GameFonts implements Disposable
{
    private static final int FONT_SIZE = 18;
    private static final String CJK_PUNCTUATION =
        "，。！？；：“”‘’（）【】《》、·—…「」『』￥→←↑↓≥≤×☆";

    /** 界面固定文案 + 常用汉字（姓名输入等）。 */
    private static final String UI_TEXT =
        "失落Realm编年史姓名直接输入文字修改Enter开始玩家房间移动方向键切换调查拾取回退标题"
            + "无法前进已回退至进入该方向这里没有可拾取的物品拾取了失败可能超重负重编年史者"
            + "读取读档存档保存暂无成功暂停菜单继续探索背包打开关闭地面随身物品声望快捷键返回"
            + "生命体力行动负载操作状态地图菜单图标按钮像素"
            + "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能"
            + "下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加"
            + "量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关"
            + "各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立"
            + "代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接"
            + "知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造"
            + "百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教"
            + "速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土"
            + "许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委"
            + "素技备半办青省列习响约支般史感劳便团往酸历市克何除消构玉称太准精值号率族维划选标写存候毛亲快效"
            + "斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严龙飞";

    private final BitmapFont defaultFont;
    private final FreeTypeFontGenerator generator;

    public GameFonts()
    {
        FileHandle fontFile = resolveFontFile();
        generator = new FreeTypeFontGenerator(fontFile);
        defaultFont = createFont(FONT_SIZE);
        Gdx.app.log("GameFonts", "Loaded font: " + fontFile.path());
    }

    public BitmapFont getDefault()
    {
        return defaultFont;
    }

    public BitmapFont copyDefault(float scale)
    {
        return createFont(Math.max(1, Math.round(FONT_SIZE * scale)));
    }

    private BitmapFont createFont(int size)
    {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = buildInitialCharacters();
        parameter.incremental = true;
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;
        parameter.genMipMaps = false;
        return generator.generateFont(parameter);
    }

    private static String buildInitialCharacters()
    {
        String source = FreeTypeFontGenerator.DEFAULT_CHARS + CJK_PUNCTUATION + UI_TEXT;
        StringBuilder characters = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (characters.indexOf(String.valueOf(ch)) < 0) {
                characters.append(ch);
            }
        }
        return characters.toString();
    }

    private static FileHandle resolveFontFile()
    {
        FileHandle bundled = Gdx.files.internal("assets/fonts/game.ttf");
        if (bundled.exists()) {
            return bundled;
        }
        bundled = Gdx.files.internal("assets/fonts/NotoSansSC-Regular.ttf");
        if (bundled.exists()) {
            return bundled;
        }
        bundled = Gdx.files.internal("fonts/game.ttf");
        if (bundled.exists()) {
            return bundled;
        }
        bundled = Gdx.files.internal("fonts/NotoSansSC-Regular.ttf");
        if (bundled.exists()) {
            return bundled;
        }

        String[] windowsCandidates = {
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/msyhbd.ttc",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyhl.ttc"
        };
        for (String path : windowsCandidates) {
            FileHandle file = Gdx.files.absolute(path);
            if (file.exists()) {
                return file;
            }
        }

        Gdx.app.error("GameFonts", "No Chinese font found. Place assets/fonts/game.ttf or install Microsoft YaHei.");
        throw new com.badlogic.gdx.utils.GdxRuntimeException(
            "无法加载中文字体：请将 .ttf 复制到 assets/fonts/game.ttf（Windows 可用 C:/Windows/Fonts/msyh.ttc）");
    }

    @Override
    public void dispose()
    {
        defaultFont.dispose();
        generator.dispose();
    }
}
