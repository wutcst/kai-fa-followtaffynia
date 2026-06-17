package cn.edu.whut.sept.zuul.engine;

/**
 * 进入王座大厅后触碰王座触发的结局类型。
 *
 * <h3>四条结局</h3>
 * <pre>
 * LIGHT:  光明印记 + 光之宝石 + 守卫勋章 + 声望>=0 + 无人死亡
 * SHADOW: 暗影之契 + (杀守卫 OR 声望<0)
 * NEUTRAL: 拒绝光暗 + 平衡之书 + 守卫勋章
 * FAKE:   未满足任何条件 —— 王座无回应，主角如蜉蝣消失
 * </pre>
 */
public enum EndingType
{
    /** 三条既定的真结局 */
    LIGHT, SHADOW, NEUTRAL,
    /** 假结局：未满足任何真结局条件 */
    FAKE,
    /** 尚未触发 */
    NONE;

    public String getTitle()
    {
        switch (this) {
            case LIGHT:   return "光明结局";
            case SHADOW:  return "暗影结局";
            case NEUTRAL: return "中立结局";
            case FAKE:    return "……";
            default:      return "";
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
            case FAKE:
                return "似蜉蝣于天地，如沧海之一粟。\n"
                    + "王座没有任何回应。\n"
                    + "你没有留下任何痕迹，没有人记得你来过。";
            default:
                return "";
        }
    }

    /** 是否为三条真结局之一 */
    public boolean isCanon()
    {
        return this == LIGHT || this == SHADOW || this == NEUTRAL;
    }
}
