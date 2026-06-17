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
    /** 拒绝光暗、清醒旁观的中立之路 */
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
                return "你触碰了王座，却知晓它不属于你。\n"
                    + "你已走过光明与暗影的两端，了解 Realm 的一切。\n"
                    + "你选择等待——等待那最终之人成为永恒的王，\n"
                    + "而你，将以编年史者之名，辅佐左右。";
            default:
                return "";
        }
    }
}
