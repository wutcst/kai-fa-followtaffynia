package cn.edu.whut.sept.zuul.engine;

/**
 * 进入王座大厅后触发的结局类型。
 */
public enum EndingType
{
    /** 持有光明宝石，Realm 重归光明 */
    LIGHT,
    /** 声望低迷，暗影笼罩 */
    SHADOW,
    /** 未集齐关键要素的中立结局 */
    NEUTRAL,
    /** 尚未触发结局 */
    NONE;

    public String getTitle()
    {
        switch (this) {
            case LIGHT:
                return "光明结局";
            case SHADOW:
                return "暗影结局";
            case NEUTRAL:
                return "中立结局";
            default:
                return "";
        }
    }

    public String getDescription()
    {
        switch (this) {
            case LIGHT:
                return "你携光明宝石步入王座大厅，Realm 的记忆被重新点亮。";
            case SHADOW:
                return "暗影吞噬了王座，编年史者在沉默中离去。";
            case NEUTRAL:
                return "Realm 依旧破碎，但编年史者留下了未完成的篇章。";
            default:
                return "";
        }
    }
}
