package jfbdev.jcooldowns.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>");

    public static String colorize(String text) {
        if (text == null) return "";

        text = processGradients(text);
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + hex.charAt(0) + "§" + hex.charAt(1) +
                    "§" + hex.charAt(2) + "§" + hex.charAt(3) + "§" + hex.charAt(4) + "§" + hex.charAt(5));
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String start = matcher.group(1);
            String content = matcher.group(2);
            String end = matcher.group(3);
            String gradient = applySimpleGradient(content, start, end);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(gradient));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String applySimpleGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return "";
        int len = text.length();
        if (len == 1) return "&#" + startHex + text;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            double ratio = (double) i / (len - 1);
            String hex = interpolate(startHex, endHex, ratio);
            sb.append("&#").append(hex).append(text.charAt(i));
        }
        return sb.toString();
    }

    private static String interpolate(String hex1, String hex2, double ratio) {
        int r1 = Integer.parseInt(hex1.substring(0, 2), 16);
        int g1 = Integer.parseInt(hex1.substring(2, 4), 16);
        int b1 = Integer.parseInt(hex1.substring(4, 6), 16);
        int r2 = Integer.parseInt(hex2.substring(0, 2), 16);
        int g2 = Integer.parseInt(hex2.substring(2, 4), 16);
        int b2 = Integer.parseInt(hex2.substring(4, 6), 16);

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return String.format("%02x%02x%02x", r, g, b).toUpperCase();
    }
}