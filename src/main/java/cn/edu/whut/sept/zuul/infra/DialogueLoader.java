package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.domain.DialogueNode;
import cn.edu.whut.sept.zuul.domain.DialogueOption;
import cn.edu.whut.sept.zuul.domain.DialogueTree;

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
 * 加载 {@code assets/dialogue/<npcId>.json}（classpath: dialogue/）。
 * 不依赖 LibGDX，供 GameEngine 使用。
 */
public final class DialogueLoader
{
    private static final Pattern STRING_FIELD =
        Pattern.compile("\"(text|action|next|npcId|startNode)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern NULL_NEXT =
        Pattern.compile("\"next\"\\s*:\\s*null");

    private DialogueLoader()
    {
    }

    public static DialogueTree load(String npcId) throws IOException
    {
        String path = "dialogue/" + npcId + ".json";
        InputStream stream = DialogueLoader.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Dialogue file not found: " + path);
        }
        String json = readAll(stream);
        return parse(json, npcId);
    }

    static DialogueTree parse(String json, String expectedNpcId)
    {
        String npcId = extractStringField(json, "npcId");
        if (npcId == null) {
            npcId = expectedNpcId;
        }
        String startNode = extractStringField(json, "startNode");
        if (startNode == null) {
            throw new IllegalArgumentException("Missing startNode in dialogue for " + npcId);
        }

        Map<String, DialogueNode> nodes = new HashMap<>();
        int nodesIndex = json.indexOf("\"nodes\"");
        if (nodesIndex < 0) {
            throw new IllegalArgumentException("Missing nodes in dialogue for " + npcId);
        }
        int braceStart = json.indexOf('{', nodesIndex);
        String nodesBlock = extractBalancedBlock(json, braceStart);
        parseNodesBlock(nodesBlock, nodes);

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("No dialogue nodes parsed for " + npcId);
        }
        return new DialogueTree(npcId, startNode, nodes);
    }

    private static void parseNodesBlock(String nodesBlock, Map<String, DialogueNode> nodes)
    {
        int index = 0;
        while (index < nodesBlock.length()) {
            int quote = nodesBlock.indexOf('"', index);
            if (quote < 0) {
                break;
            }
            int quoteEnd = nodesBlock.indexOf('"', quote + 1);
            if (quoteEnd < 0) {
                break;
            }
            String nodeId = nodesBlock.substring(quote + 1, quoteEnd);
            int colon = nodesBlock.indexOf(':', quoteEnd);
            if (colon < 0) {
                break;
            }
            int nodeBrace = nodesBlock.indexOf('{', colon);
            if (nodeBrace < 0) {
                break;
            }
            String nodeBody = extractBalancedBlock(nodesBlock, nodeBrace);
            nodes.put(nodeId, parseNode(nodeId, nodeBody));
            index = nodeBrace + nodeBody.length();
        }
    }

    private static DialogueNode parseNode(String nodeId, String nodeBody)
    {
        String text = extractStringField(nodeBody, "text");
        if (text == null) {
            text = "";
        }
        String action = extractStringField(nodeBody, "action");
        List<DialogueOption> options = parseOptions(nodeBody);
        return new DialogueNode(nodeId, text, action, options);
    }

    private static List<DialogueOption> parseOptions(String nodeBody)
    {
        List<DialogueOption> options = new ArrayList<>();
        int optionsIndex = nodeBody.indexOf("\"options\"");
        if (optionsIndex < 0) {
            return options;
        }
        int arrayStart = nodeBody.indexOf('[', optionsIndex);
        if (arrayStart < 0) {
            return options;
        }
        String arrayBlock = extractBalancedBlock(nodeBody, arrayStart, '[', ']');
        int index = 0;
        while (index < arrayBlock.length()) {
            int objStart = arrayBlock.indexOf('{', index);
            if (objStart < 0) {
                break;
            }
            String optionBody = extractBalancedBlock(arrayBlock, objStart);
            String text = extractStringField(optionBody, "text");
            String next = extractStringField(optionBody, "next");
            if (next == null && NULL_NEXT.matcher(optionBody).find()) {
                next = null;
            }
            if (text != null && next != null) {
                options.add(new DialogueOption(text, next));
            }
            index = objStart + optionBody.length();
        }
        return options;
    }

    private static String extractStringField(String json, String field)
    {
        Pattern pattern = Pattern.compile(
            "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescape(matcher.group(1));
    }

    private static String unescape(String value)
    {
        return value.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n");
    }

    private static String extractBalancedBlock(String text, int start)
    {
        return extractBalancedBlock(text, start, '{', '}');
    }

    private static String extractBalancedBlock(String text, int start, char open, char close)
    {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new IllegalArgumentException("Unbalanced block starting at " + start);
    }

    private static String readAll(InputStream stream) throws IOException
    {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }
}
