package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.NpcCombatDef.NpcSkill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 加载 {@code assets/combat/<npcId>.json}（classpath: combat/）。
 */
public final class CombatLoader
{
    private static final Pattern STRING_FIELD =
        Pattern.compile("\"(npcId|displayName|defaultState|unlock|text|selfBuff)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern INT_FIELD =
        Pattern.compile("\"(maxHp|damage|reputation)\"\\s*:\\s*(-?\\d+)");
    private static final Pattern DOUBLE_FIELD =
        Pattern.compile("\"whenHpBelow\"\\s*:\\s*([0-9.]+)");
    private static final Pattern BOOL_FIELD =
        Pattern.compile("\"markDefeated\"\\s*:\\s*(true|false)");

    private CombatLoader()
    {
    }

    public static NpcCombatDef load(String npcId) throws IOException
    {
        String path = "combat/" + npcId + ".json";
        InputStream stream = CombatLoader.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Combat file not found: " + path);
        }
        return parse(readAll(stream), npcId);
    }

    static NpcCombatDef parse(String json, String expectedNpcId)
    {
        String npcId = firstString(json, "npcId");
        if (npcId == null) {
            npcId = expectedNpcId;
        }
        String displayName = firstString(json, "displayName");
        if (displayName == null) {
            displayName = npcId;
        }
        int maxHp = firstInt(json, "maxHp", 50);
        String defaultState = firstString(json, "defaultState");
        if (defaultState == null) {
            defaultState = "normal";
        }

        Map<String, Double> stateThresholds = new HashMap<>();
        Map<String, List<String>> stateSkills = new HashMap<>();
        parseStates(json, stateThresholds, stateSkills);

        Map<String, NpcSkill> skills = parseSkills(json);

        int onRep = 0;
        String unlock = null;
        boolean markDefeated = true;
        int onDefeatIdx = json.indexOf("\"onDefeat\"");
        if (onDefeatIdx >= 0) {
            String onBlock = extractBalancedBlock(json, json.indexOf('{', onDefeatIdx));
            Matcher intM = INT_FIELD.matcher(onBlock);
            while (intM.find()) {
                if ("reputation".equals(intM.group(1))) {
                    onRep = Integer.parseInt(intM.group(2));
                }
            }
            Matcher strM = STRING_FIELD.matcher(onBlock);
            while (strM.find()) {
                if ("unlock".equals(strM.group(1))) {
                    unlock = strM.group(2);
                }
            }
            Matcher boolM = BOOL_FIELD.matcher(onBlock);
            if (boolM.find()) {
                markDefeated = Boolean.parseBoolean(boolM.group(1));
            }
        }

        return new NpcCombatDef(npcId, displayName, maxHp, defaultState,
            stateThresholds, stateSkills, skills, onRep, unlock, markDefeated);
    }

    private static void parseStates(String json,
        Map<String, Double> thresholds, Map<String, List<String>> skills)
    {
        int statesIdx = json.indexOf("\"states\"");
        if (statesIdx < 0) {
            return;
        }
        String statesBlock = extractBalancedBlock(json, json.indexOf('{', statesIdx));
        int pos = 0;
        while (pos < statesBlock.length()) {
            int quote = statesBlock.indexOf('"', pos);
            if (quote < 0) {
                break;
            }
            int quoteEnd = statesBlock.indexOf('"', quote + 1);
            if (quoteEnd < 0) {
                break;
            }
            String stateId = statesBlock.substring(quote + 1, quoteEnd);
            if ("states".equals(stateId) || "skills".equals(stateId)
                || "whenHpBelow".equals(stateId)) {
                pos = quoteEnd + 1;
                continue;
            }
            int blockStart = statesBlock.indexOf('{', quoteEnd);
            if (blockStart < 0) {
                break;
            }
            String stateBlock = extractBalancedBlock(statesBlock, blockStart);
            Matcher hpM = DOUBLE_FIELD.matcher(stateBlock);
            if (hpM.find()) {
                thresholds.put(stateId, Double.parseDouble(hpM.group(1)));
            }
            List<String> skillIds = parseStringArray(stateBlock, "skills");
            if (!skillIds.isEmpty()) {
                skills.put(stateId, skillIds);
            }
            pos = blockStart + stateBlock.length();
        }
    }

    private static Map<String, NpcSkill> parseSkills(String json)
    {
        Map<String, NpcSkill> skills = new HashMap<>();
        int skillsIdx = json.lastIndexOf("\"skills\"");
        if (skillsIdx < 0) {
            return skills;
        }
        String skillsBlock = extractBalancedBlock(json, json.indexOf('{', skillsIdx));
        int pos = 0;
        while (pos < skillsBlock.length()) {
            int quote = skillsBlock.indexOf('"', pos);
            if (quote < 0) {
                break;
            }
            int quoteEnd = skillsBlock.indexOf('"', quote + 1);
            if (quoteEnd < 0) {
                break;
            }
            String skillId = skillsBlock.substring(quote + 1, quoteEnd);
            if ("skills".equals(skillId) || "damage".equals(skillId)
                || "text".equals(skillId) || "selfBuff".equals(skillId)) {
                pos = quoteEnd + 1;
                continue;
            }
            int blockStart = skillsBlock.indexOf('{', quoteEnd);
            if (blockStart < 0) {
                break;
            }
            String skillBlock = extractBalancedBlock(skillsBlock, blockStart);
            int damage = 0;
            Matcher dmgM = Pattern.compile("\"damage\"\\s*:\\s*(\\d+)").matcher(skillBlock);
            if (dmgM.find()) {
                damage = Integer.parseInt(dmgM.group(1));
            }
            String text = firstString(skillBlock, "text");
            if (text == null) {
                text = skillId;
            }
            boolean defense = skillBlock.contains("\"selfBuff\"")
                && skillBlock.contains("defense");
            skills.put(skillId, new NpcSkill(skillId, damage, text, defense));
            pos = blockStart + skillBlock.length();
        }
        return skills;
    }

    private static List<String> parseStringArray(String block, String fieldName)
    {
        List<String> result = new ArrayList<>();
        int idx = block.indexOf("\"" + fieldName + "\"");
        if (idx < 0) {
            return result;
        }
        int arrStart = block.indexOf('[', idx);
        int arrEnd = block.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) {
            return result;
        }
        String arr = block.substring(arrStart + 1, arrEnd);
        Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(arr);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    private static String extractBalancedBlock(String json, int startBrace)
    {
        int depth = 0;
        for (int i = startBrace; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(startBrace, i + 1);
                }
            }
        }
        return json.substring(startBrace);
    }

    private static String firstString(String json, String field)
    {
        Matcher m = STRING_FIELD.matcher(json);
        while (m.find()) {
            if (field.equals(m.group(1))) {
                return m.group(2);
            }
        }
        return null;
    }

    private static int firstInt(String json, String field, int defaultValue)
    {
        Matcher m = INT_FIELD.matcher(json);
        while (m.find()) {
            if (field.equals(m.group(1))) {
                return Integer.parseInt(m.group(2));
            }
        }
        return defaultValue;
    }

    private static String readAll(InputStream stream) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
