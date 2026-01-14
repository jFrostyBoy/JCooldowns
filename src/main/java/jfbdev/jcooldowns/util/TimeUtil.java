package jfbdev.jcooldowns.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdSMHD])");

    public static long parseToMillis(String input) {
        if (input == null || input.isEmpty()) return -1;
        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase());
        if (!matcher.matches()) return -1;
        long value = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2).toLowerCase()) {
            case "s" -> value * 1000;
            case "m" -> value * 60_000;
            case "h" -> value * 3_600_000;
            case "d" -> value * 86_400_000;
            default -> -1;
        };
    }

    public static String formatMillis(long millis) {
        if (millis <= 0) return "0 сек.";
        long sec = millis / 1000;
        long d = sec / 86400; sec %= 86400;
        long h = sec / 3600;  sec %= 3600;
        long m = sec / 60;    sec %= 60;

        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append(" дн. ");
        if (h > 0) sb.append(h).append(" ч. ");
        if (m > 0) sb.append(m).append(" мин. ");
        if (sec > 0 || sb.isEmpty()) sb.append(sec).append(" сек.");
        return sb.toString().trim();
    }
}