package me.petulikan1.zMachines.utils;


import me.petulikan1.zMachines.config.StringContainer;

import java.awt.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static final Map<TimeFormat, TimeFormatter> timeConvertor = new ConcurrentHashMap<>();
    private static final Pattern mat = Pattern.compile("\\.([0-9])([0-9])?");
    private static final Pattern fixedSplit = Pattern
            .compile("(#[A-Fa-f0-9]{6}|[&§][Xx]([&§][A-Fa-f0-9]){6}|[&§][A-Fa-f0-9UuXx])");
    public static ColormaticFactory color = new ColormaticFactory() {
        final char[] characters = "abcdef0123456789".toCharArray();

        final SecureRandom random = new SecureRandom();
        final Pattern getLast = Pattern.compile("(#[A-Fa-f0-9k-oK-ORrXxUu]{6}|§[Xx](§[A-Fa-f0-9k-oK-ORrXxUu]){6}|§[A-Fa-f0-9k-oK-ORrXxUu]|&[Uu])");

        final Pattern hex = Pattern.compile("(#[a-fA-F0-9]{6})");

        @Override
        public String generateColor() {
            StringContainer b = new StringContainer(7).append("#");
            for (int i = 0; i < 6; ++i)
                b.append(characters[random.nextInt(16)]);
            return b.toString();
        }

        @Override
        public String[] getLastColors(String text) {
            return StringUtils.getLastColors(getLast, text);
        }

        @Override
        public String replaceHex(String text) {
            String msg = text;
            Matcher match = hex.matcher(msg);
            while (match.find()) {
                String color = match.group();
                StringContainer hex = new StringContainer(14).append("§x");
                for (char c : color.substring(1).toCharArray())
                    hex.append("§").append(Character.toLowerCase(c));
                msg = msg.replace(color, hex.toString());
            }
            return msg;
        }

        private boolean isColor(int charAt) {
            return charAt >= 97 && charAt <= 102 || charAt >= 65 && charAt <= 70 || charAt >= 48 && charAt <= 57;
        }

        private boolean isFormat(int charAt) {
            return charAt >= 107 && charAt <= 111 || charAt == 114;
        }

        @Override
        public String gradient(String msg, String fromHex, String toHex, List<String> protectedStrings) {
            return StringUtils.gradient(msg, fromHex, toHex, protectedStrings);
        }

        @Override
        public String rainbow(String msg, String fromHex, String toHex, List<String> protectedStrings) {
            return StringUtils.rainbow(msg, fromHex, toHex, protectedStrings);
        }
    };

    public static Pattern gradientFinder = Pattern.compile("!" + "(#[A-Fa-f0-9]{6})" + "(.*?)" + "!" + "(#[A-Fa-f0-9]{6})" + "|.*?(?=(?:" + "!" + "#[A-Fa-f0-9]{6}" + ".*?" + "!" + "#[A-Fa-f0-9]{6}" + "))");
    public static String timeFormat = "%time% %format%";
    static Pattern pattern = Pattern.compile("[^a-zA-Z0-9_ ]");
    static int[][] EMPTY_ARRAY = {};
    static Map<String, List<String>> actions = new HashMap<>();

    static {
        actions.put("Years", Arrays.asList("=,1,y", ">,1,y"));
        actions.put("Months", Arrays.asList("=,1,mon", ">,1,mon"));
        actions.put("Weeks", Arrays.asList("=,1,w", ">,1,w"));
        actions.put("Days", Arrays.asList("=,1,d", ">,1,d"));
        actions.put("Hours", Arrays.asList("=,1,h", ">,1,h"));
        actions.put("Minutes", Arrays.asList("=,1,m", ">,1,m"));
        actions.put("Seconds", Arrays.asList("=,1,s", ">,1,s"));
    }

    public static <T> T getRandomFromList(List<T> list) {
        return list != null && !list.isEmpty() ? list.get(generateRandomInt(list.size())) : null;
    }

    public static int generateRandomInt(int maxInt) {
        return generateRandomInt(0, maxInt);
    }

    protected static Random random = new Random();

    public static int generateRandomInt(int min, int maxInt) {
        if (maxInt == 0) {
            return maxInt;
        } else {
            boolean a = maxInt < 0;
            if (a) {
                maxInt *= -1;
            }

            int i = random.nextInt(maxInt);
            if (i < (min < 0 ? min * -1 : min)) {
                return min;
            } else {
                if (i > maxInt) {
                    i = maxInt;
                }

                return a ? -1 * i : i;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getRandomFromCollection(Collection<T> list) {
        if (list != null && !list.isEmpty()) {
            return list instanceof List ? getRandomFromList((List<T>) list) : (T) list.toArray()[generateRandomInt(list.size())];
        } else {
            return null;
        }
    }

    public static String sanitize(String a) {
        Matcher matcher = pattern.matcher(a);
        while (matcher.find()) {
            a = matcher.replaceAll("");
        }
        return a;
    }

    public static String fixedFormatDouble(final double val) {
        final String text = String.format(Locale.ENGLISH, "%.2f", val);
        final Matcher m = StringUtils.mat.matcher(text);
        if (!m.find()) {
            return text;
        }
        if (m.groupCount() != 2) {
            if (m.group(1).equals("0")) {
                return m.replaceFirst("");
            }
            return m.replaceFirst(".$1");
        } else if (m.group(1).equals("0")) {
            if (m.group(2).equals("0")) {
                return m.replaceFirst("");
            }
            return m.replaceFirst(".$1$2");
        } else {
            if (m.group(2).equals("0")) {
                return m.replaceFirst(".$1");
            }
            return m.replaceFirst(".$1$2");
        }
    }

    public static boolean isNumber(String fromString) {
        return isInt(fromString) || isDouble(fromString) || isLong(fromString) || isByte(fromString) || isShort(fromString) || isFloat(fromString);
    }

    public static boolean isBoolean(String fromString) {
        if (fromString == null) {
            return false;
        } else {
            return fromString.equalsIgnoreCase("true") || fromString.equalsIgnoreCase("false") || fromString.equalsIgnoreCase("yes") || fromString.equalsIgnoreCase("no");
        }
    }

    public static double getDouble(String fromString) {
        if (fromString == null) {
            return 0.0D;
        } else {
            String a = fromString.replaceAll("[^+0-9E.,-]+", "").replace(",", ".");

            try {
                return Double.parseDouble(a);
            } catch (NumberFormatException var3) {
                return 0.0D;
            }
        }
    }

    public static boolean isDouble(String fromString) {
        try {
            Double.parseDouble(fromString);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static long getLong(String fromString) {
        if (fromString == null) {
            return 0L;
        } else {
            String a = fromString.replaceAll("[^+0-9E.,-]+", "").replace(",", ".");

            try {
                return Long.parseLong(a);
            } catch (NumberFormatException var3) {
                return 0L;
            }
        }
    }

    public static boolean isLong(String fromString) {
        try {
            Long.parseLong(fromString);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static int getInt(String fromString) {
        if (fromString == null) {
            return 0;
        } else {
            String a = fromString.replaceAll("[^+0-9E.,-]+", "").replace(",", ".");
            if (!a.contains(".")) {
                try {
                    return Integer.parseInt(a);
                } catch (NumberFormatException var5) {
                    try {
                        return (int) Long.parseLong(a);
                    } catch (NumberFormatException var4) {
                    }
                }
            }

            try {
                return (int) Double.parseDouble(a);
            } catch (NumberFormatException var3) {
                return 0;
            }
        }
    }

    public static boolean isInt(String fromString) {
        try {
            Integer.parseInt(fromString);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static boolean isFloat(String fromString) {
        try {
            Float.parseFloat(fromString);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static float getFloat(String fromString) {
        if (fromString == null) {
            return 0.0F;
        } else {
            String a = fromString.replaceAll("[^+0-9E.,-]+", "").replace(",", ".");

            try {
                return Float.parseFloat(a);
            } catch (NumberFormatException var3) {
                return 0.0F;
            }
        }
    }

    public static boolean isByte(String fromString) {
        try {
            Byte.parseByte(fromString);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static byte getByte(String fromString) {
        if (fromString == null) {
            return 0;
        } else {
            String a = fromString.replaceAll("[^+0-9E-]+", "");

            try {
                return Byte.parseByte(a);
            } catch (NumberFormatException var3) {
                return 0;
            }
        }
    }

    public static boolean isShort(String fromString) {
        try {
            Short.parseShort(fromString);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static short getShort(String fromString) {
        if (fromString == null) {
            return 0;
        } else {
            String a = fromString.replaceAll("[^+0-9E-]+", "");

            try {
                return Short.parseShort(a);
            } catch (NumberFormatException var3) {
                return 0;
            }
        }
    }

    public static boolean getBoolean(String fromString) {
        try {
            return fromString.equalsIgnoreCase("true") || fromString.equalsIgnoreCase("yes");
        } catch (Exception var2) {
            return false;
        }
    }

    public static List<String> copyPartialMatches(String prefix, Iterable<String> originals) {
        List<String> matches = new ArrayList<>();
        for (String completion : originals)
            if (completion != null && (completion.regionMatches(true, 0, prefix, 0, prefix.length()) || completion.toLowerCase().contains(prefix.toLowerCase())))
                matches.add(completion);
        return matches;
    }

    public static Number getNumber(String o) {
        if (o == null) {
            return 0;
        } else {
            if (!o.contains(".")) {
                if (isInt(o)) {
                    return getInt(o);
                }

                if (isLong(o)) {
                    return getLong(o);
                }

                if (isByte(o)) {
                    return getByte(o);
                }

                if (isShort(o)) {
                    return getShort(o);
                }
            }

            if (isDouble(o)) {
                return getDouble(o);
            } else {
                return isFloat(o) ? getFloat(o) : 0;
            }
        }
    }

    public static Matcher getGradientMatcher(String text) {
        return gradientFinder.matcher(text);
    }

    private static boolean has(int c) {
        return c <= 102 && c >= 97 || c <= 57 && c >= 48 || c <= 70 && c >= 65 || c <= 79 && c >= 75 || c <= 111 && c >= 107 || c == 114 || c == 82 || c == 88 || c == 120;
    }

    private static char lower(int c) {
        switch (c) {
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 82:
                return (char) (c + 32);
            case 120:
                return (char) 88;
            default:
                return (char) c;
        }
    }

    public static String gradient(String originalMsg, List<String> protectedStrings) {
        if (originalMsg == null)
            return null;
        String legacyMsg = originalMsg;
        Matcher matcher = StringUtils.gradientFinder.matcher(legacyMsg);
        while (matcher.find()) {
            if (matcher.groupCount() == 0 || matcher.group().isEmpty())
                continue;
            String replace = StringUtils.color.gradient(matcher.group(2), matcher.group(1), matcher.group(3), protectedStrings);
            if (replace == null)
                continue;
            legacyMsg = legacyMsg.replace(matcher.group(), replace);
        }
        return legacyMsg;
    }

    public static String formColor(String s) {
        return formColor(s, null);
    }

    public static String formColor(String s, List<String> protectedStrings) {
        if (s == null || s.trim().isEmpty())
            return s;
        String msg = s;
        char[] b = msg.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == '&' && StringUtils.has(b[i + 1])) {
                b[i] = '§';
                b[i + 1] = StringUtils.lower(b[i + 1]);
            }
        }
        msg = new String(b);
        if (StringUtils.color != null) {
            msg = StringUtils.gradient(msg, protectedStrings);
            if (msg.contains("#"))
                msg = StringUtils.color.replaceHex(msg);
        }
        if (msg.contains("&u") && StringUtils.color != null) {
            msg = colorizeUnicode(msg);
        }
        return msg;
    }

    public static String colorizeUnicode(String msg) {
        StringBuilder d = new StringBuilder(msg.length());
        String[] split = fixedSplit.split(msg);
        Matcher m = fixedSplit.matcher(msg);
        int id = 1;
        while (m.find()) {
            try {
                split[id] = m.group(1) + split[id++];
            } catch (Exception err) {
            }
        }
        for (String ff : split) {
            if (ff.toLowerCase().contains("§u") || ff.toLowerCase().contains("&u"))
                ff = StringUtils.rainbow(ff.replaceAll("§[Uu]", "&u"), StringUtils.color.generateColor(),
                        StringUtils.color.generateColor(), null);
            d.append(ff);
        }
        return d.toString();
    }

    public static String colorize(String a) {
        return formColor(a, null);
    }

    public static String colorize(String a, List<String> protectedStrs) {
        return formColor(a, protectedStrs);
    }

    public static List<String> colorize(List<String> a) {
        a.replaceAll(StringUtils::colorize);
        return a;
    }

    public static String join(Iterable<?> args, String split) {
        return StringUtils.join(args, split, 0, -1);
    }

    public static String join(Iterable<?> args, String split, int start) {
        return StringUtils.join(args, split, start, -1);
    }

    public static String join(Iterable<?> args, String split, int start, int end) {
        if (args == null || split == null)
            return null;
        StringContainer msg = new StringContainer(split.length() + 32);
        Iterator<?> iterator = args.iterator();
        for (int i = start; iterator.hasNext() && (end == -1 || i < end); ++i) {
            if (msg.length() != 0)
                msg.append(split);
            msg.append(String.valueOf(iterator.next()));
        }
        return msg.toString();
    }

    public static String rainbow(String msg, String fromHex, String toHex, List<String> protectedStrings) {
        if (msg == null || fromHex == null || toHex == null)
            return msg;
        return rawGradient(msg, fromHex, toHex, false, protectedStrings);
    }

    public static String rawGradient(String msg, String from, String to, boolean defaultRainbow, List<String> protectedStrings) {
        boolean inRainbow = defaultRainbow;
        char prev = 0;
        String formats = "";

        int[][] skipRegions = EMPTY_ARRAY;
        int allocated = 0;

        int currentSkipAt = -1;
        int skipId = 0;

        int fixedSize = msg.length() * 14;
        int rgbSize = msg.length();
        if (protectedStrings != null) {
            for (String protect : protectedStrings) {
                int size = protect.length();

                int num = 0;
                while (true) {
                    int position = msg.indexOf(protect, num++);
                    if (position == -1)
                        break;
                    if (allocated == skipRegions.length) {
                        int[][] copy = new int[allocated << 1 + 1][2];
                        System.arraycopy(skipRegions, 0, copy, 0, skipRegions.length);
                        skipRegions = copy;
                    }
                    skipRegions[allocated++] = new int[]{position, size};
                    fixedSize -= size;
                    rgbSize -= size;
                }
            }
            if (allocated > 0)
                currentSkipAt = skipRegions[0][0];
        }

        StringContainer builder = new StringContainer(fixedSize);

        Color fromRGB = Color.decode(from);
        Color toRGB = Color.decode(to);
        double rStep = Math.abs((double) (fromRGB.getRed() - toRGB.getRed()) / rgbSize);
        double gStep = Math.abs((double) (fromRGB.getGreen() - toRGB.getGreen()) / rgbSize);
        double bStep = Math.abs((double) (fromRGB.getBlue() - toRGB.getBlue()) / rgbSize);
        if (fromRGB.getRed() > toRGB.getRed())
            rStep = -rStep;
        if (fromRGB.getGreen() > toRGB.getGreen())
            gStep = -gStep;
        if (fromRGB.getBlue() > toRGB.getBlue())
            bStep = -bStep;

        Color finalColor = new Color(fromRGB.getRGB());

        int skipForChars = 0;
        for (int i = 0; i < msg.length(); ++i) {
            char c = msg.charAt(i);
            if (c == 0)
                continue;

            if (skipForChars > 0) {
                builder.append(c);
                --skipForChars;
                continue;
            }

            if (currentSkipAt == i) {
                skipForChars = skipId + 1 == allocated ? -1 : skipRegions[skipId++][1];
                builder.append(c);
                continue;
            }

            if (prev == '&' || prev == '§') {
                char inLower = Character.toLowerCase(c);
                if (prev == '&' && inLower == 'u') {
                    builder.deleteCharAt(builder.length() - 1); // remove & char
                    inRainbow = true;
                    prev = c;
                    continue;
                }
                if (inRainbow && prev == '§' && (isColor(inLower) || isFormat(inLower))) { // color,
                    // destroy
                    // rainbow here
                    if (isFormat(inLower)) {
                        if (inLower == 'r')
                            formats = "§r";
                        else
                            formats += "§" + inLower;
                        prev = inLower;
                        builder.deleteCharAt(builder.length() - 1); // remove &<random color> string
                        continue;
                    }
                    builder.delete(builder.length() - 14, builder.length()); // remove &<random color> string
                    inRainbow = false;
                }
            }
            if (c != ' ' && inRainbow) {
                int red = (int) Math.round(finalColor.getRed() + rStep);
                int green = (int) Math.round(finalColor.getGreen() + gStep);
                int blue = (int) Math.round(finalColor.getBlue() + bStep);
                if (red > 255)
                    red = 255;
                if (red < 0)
                    red = 0;
                if (green > 255)
                    green = 255;
                if (green < 0)
                    green = 0;
                if (blue > 255)
                    blue = 255;
                if (blue < 0)
                    blue = 0;
                finalColor = new Color(red, green, blue);
                if (formats.equals("§r")) {
                    builder.append(formats); // add formats
                    builder.append(StringUtils.color.replaceHex("#" + String.format("%08x", finalColor.getRGB()).substring(2))); // add
                    // color
                    formats = "";
                } else {
                    builder.append(StringUtils.color.replaceHex("#" + String.format("%08x", finalColor.getRGB()).substring(2))); // add
                    // color
                    if (!formats.isEmpty())
                        builder.append(formats); // add formats
                }
            }
            builder.append(c);
            prev = c;
        }
        return builder.toString();
    }

    private static boolean isColor(int charAt) {
        return charAt >= 97 && charAt <= 102 || charAt >= 65 && charAt <= 70 || charAt >= 48 && charAt <= 57;
    }

    public static String[] getLastColors(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        String color = null;
        StringContainer formats = new StringContainer(8);
        while (m.find()) {
            String last = m.group(1).toLowerCase();
            if (last.charAt(1) != 'x' && isFormat(last.charAt(1))) {
                if (last.charAt(1) == 'r') {
                    formats.clear();
                    continue;
                }
                formats.append(last.charAt(1));
                continue;
            }
            color = last.replace("§", "").replace("&", "");
            formats.clear();
        }
        return new String[]{color, formats.length() == 0 ? null : formats.toString()};
    }

    private static boolean isFormat(int charAt) {
        return charAt >= 107 && charAt <= 111 || charAt == 114;
    }

    public static String gradient(String msg, String fromHex, String toHex, List<String> protectedStrings) {
        if (msg == null || fromHex == null || toHex == null)
            return msg;
        return rawGradient(msg, fromHex, toHex, true, protectedStrings);
    }

    public static String buildString(int start, int end, String[] args) {
        return StringUtils.join(args, " ", start, end);
    }

    public static String buildString(String[] args) {
        return StringUtils.join(args, " ", 0, args.length);
    }

    public static String buildString(int start, String[] args) {
        return StringUtils.join(args, " ", start, args.length);
    }

    public static String join(Object[] args, String split, int start, int end) {
        if (args == null || split == null)
            return null;
        StringContainer msg = new StringContainer(split.length() * (args.length - 1) + args.length * 4);
        boolean first = true;
        for (int i = start; i < args.length && i < end; ++i) {
            if (!first)
                msg.append(split);
            else
                first = false;
            msg.append(String.valueOf(args[i]));
        }
        return msg.toString();
    }

    public static String findCorrectFormat(long i, String string) {
        String result = i + string;
        for (String s : actions.get(string)) {
            if (s.startsWith("=,"))
                if (getInt(s.substring(1).split(",")[1]) == i)
                    return s.substring(3 + s.substring(1).split(",")[1].length());
            if (s.startsWith("<,"))
                if (getInt(s.substring(1).split(",")[1]) > i)
                    return s.substring(3 + s.substring(1).split(",")[1].length());
            if (s.startsWith(">,"))
                if (getInt(s.substring(1).split(",")[1]) < i)
                    return s.substring(3 + s.substring(1).split(",")[1].length());
        }
        return result;
    }

    private static String format(long time, String section) {
        return timeFormat.replace("%time%", "" + time).replace("%format%", findCorrectFormat(time, section));
    }

    @Deprecated(forRemoval = true)
    public static long timeFromString(String original) {
        if (original == null || original.isEmpty())
            return 0;

        String period = original;

        if (StringUtils.isFloat(period) && !period.endsWith("d") && !period.endsWith("economyEnabled"))
            return (long) StringUtils.getFloat(period);

        float time = 0;

        if (period.contains(":")) {
            String[] split = period.split(":");
            switch (split.length) {
                case 2: { // mm:ss
                    time += (float) (StringUtils.getFloat(split[0]) * TimeFormat.MINUTES.multiplier());
                    time += StringUtils.getFloat(split[1]);
                }
                case 3: { // hh:mm:ss
                    time += (float) (StringUtils.getFloat(split[0]) * TimeFormat.HOURS.multiplier());
                    time += (float) (StringUtils.getFloat(split[1]) * TimeFormat.MINUTES.multiplier());
                    time = time + StringUtils.getFloat(split[2]);
                }
                case 4: { // dd:hh:mm:ss
                    time += (float) (StringUtils.getFloat(split[0]) * TimeFormat.DAYS.multiplier());
                    time += (float) (StringUtils.getFloat(split[1]) * TimeFormat.HOURS.multiplier());
                    time += (float) (StringUtils.getFloat(split[2]) * TimeFormat.MINUTES.multiplier());
                    time += StringUtils.getFloat(split[3]);
                }
                case 5: { // mm:dd:hh:mm:ss
                    time += (float) (StringUtils.getFloat(split[0]) * TimeFormat.MONTHS.multiplier());
                    time += (float) (StringUtils.getFloat(split[1]) * TimeFormat.DAYS.multiplier());
                    time += (float) (StringUtils.getFloat(split[2]) * TimeFormat.HOURS.multiplier());
                    time += (float) (StringUtils.getFloat(split[3]) * TimeFormat.MINUTES.multiplier());
                    time += StringUtils.getFloat(split[4]);
                }
                default: { // yy:mm:dd:hh:mm:ss
                    time += (float) (StringUtils.getFloat(split[0]) * TimeFormat.YEARS.multiplier());
                    time += (float) (StringUtils.getFloat(split[1]) * TimeFormat.MONTHS.multiplier());
                    time += (float) (StringUtils.getFloat(split[2]) * TimeFormat.DAYS.multiplier());
                    time += (float) (StringUtils.getFloat(split[3]) * TimeFormat.HOURS.multiplier());
                    time += (float) (StringUtils.getFloat(split[4]) * TimeFormat.MINUTES.multiplier());
                    time += StringUtils.getFloat(split[5]);
                }
            }
            return (long) time;
        }

        for (int i = TimeFormat.values().length - 1; i > 0; --i) {
            Matcher matcher = StringUtils.timeConvertor.get(TimeFormat.values()[i]).matcher(period);
            while (matcher.find()) {
                time += (float) (StringUtils.getFloat(matcher.group()) * TimeFormat.values()[i].multiplier());
                period = matcher.replaceFirst("");
            }
        }
        return (long) time;
    }

    @Deprecated(forRemoval = true)
    public static String timeToString(long time) {
        time += 1;
        if (time == 0L) {
            return format(0L, "Seconds");
        } else {
            long minutes = time / 60L % 60L;
            long hours = time / 3600L;

            StringBuilder date = new StringBuilder(64);

            if (hours > 0L) {
                if (date.length() != 0) {
                    date.append(" ");
                }
                date.append(format(hours, "Hours"));
                return date.toString();
            }

            if (minutes > 0L) {
                if (date.length() != 0) {
                    date.append(" ");
                }
                date.append(format(minutes, "Minutes"));
                return date.toString();
            }

            if (time % 60L > 0L) {
                if (date.length() != 0) {
                    date.append(" ");
                }
                date.append(format(time % 60L, "Seconds"));
                return date.toString();
            }

            return date.toString();
        }
    }

    public enum TimeFormat {
        YEARS(31556926, 0), MONTHS(2629743.83, 12), WEEKS(604800, 4.34812141), DAYS(86400, 30.4368499), HOURS(3600, 24), MINUTES(60, 60), SECONDS(1, 60);

        private final double multiplier;
        private final double cast;

        TimeFormat(double multiplier, double cast) {
            this.multiplier = multiplier;
            this.cast = cast;
        }

        public double multiplier() {
            return multiplier;
        }

        public double cast() {
            return cast;
        }
    }

    public interface ColormaticFactory {
        String generateColor();

        String[] getLastColors(String text);

        String replaceHex(String msg);

        String gradient(String msg, String fromHex, String toHex, List<String> protectedStrings);

        String rainbow(String msg, String fromHex, String toHex, List<String> protectedStrings);
    }

    public interface TimeFormatter {
        String toString(long value);

        Matcher matcher(String text);
    }

    public static Number getNumber(CharSequence text) {
        if (text == null) {
            return 0;
        }
        return getNumber(text, 0, text.length());
    }

    public static Number getNumber(CharSequence text, int start, int end) {
        if (text == null || text.length() == 0) {
            return null;
        }
        int dotAt = -1;
        for (int i = start; i < text.length() && i < end; ++i) {
            char c = text.charAt(i);
            if (c == DOT) {
                dotAt = i;
                break;
            }
        }
        if (dotAt == -1) {
            if (isInt(text, start, end)) {
                return getInt(text, start, end);
            }
            if (isLong(text, start, end)) {
                return getLong(text, start, end);
            }
        }
        if (isDouble(text, start, end)) {
            return getDouble(text, start, end);
        }
        return null;
    }

    private static final char DOT = '.';
    private static final char COMMA = ',';
    private static final char SPACE = ' ';
    private static final char SMALL_E = 'e';
    private static final char BIG_E = 'E';
    private static final char MINUS = '-';

    public static boolean isInt(CharSequence text, int start, int end) {
        boolean foundZero = false;
        boolean minus = false;
        byte totalWidth = 0;
        byte overLimit = 0;
        boolean onLimit = false;
        int limit;

        for (int i = start; i < end; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case SPACE:
                    continue;
                case MINUS:
                    if (minus) {
                        break;
                    }
                    minus = true;
                    continue;
            }
            if (c < 48 || c > 57) {
                return false;
            }
            if (totalWidth == 0) {
                if (c == 48) {
                    foundZero = true;
                    continue;
                }
                onLimit = c == 50;
            }
            int digit = c - 48;

            if (onLimit) {
                limit = overIntLimit(minus, totalWidth);
                if (digit != limit) {
                    if (digit > limit) {
                        overLimit = 1;
                    } else {
                        onLimit = false;
                    }
                }
            }
            if (++totalWidth > 10 || totalWidth == 10 && overLimit == 1) {
                return false;
            }
        }
        return totalWidth > 0 || foundZero;
    }

    public static int getInt(CharSequence text, int start, int end) {
        if (text == null) {
            return 0;
        }
        return parseNonDecimalNumber((byte) 2, 10, text, start, end).intValue();
    }

    private static int overIntLimit(boolean minus, int pos) {
        switch (pos) {
            case 1:
                return 1;
            case 2:
            case 4:
            case 8:
                return 4;
            case 3:
                return 7;
            case 5:
                return 8;
            case 6:
                return 3;
            case 7:
                return 6;
            case 9:
                return minus ? 8 : 7;
            default:
                break;
        }
        return 2;
    }

    public static long getLong(CharSequence text) {
        if (text == null) {
            return 0;
        }
        return getLong(text, 0, text.length());
    }

    public static long getLong(CharSequence text, int start, int end) {
        if (text == null) {
            return 0;
        }
        return parseNonDecimalNumber((byte) 3, 19, text, start, end).longValue();
    }

    public static boolean isLong(CharSequence text) {
        if (text == null) {
            return false;
        }
        return isLong(text, 0, text.length());
    }

    public static boolean isLong(CharSequence text, int start, int end) {
        boolean foundZero = false;
        boolean minus = false;
        byte totalWidth = 0;
        byte overLimit = 0;
        boolean onLimit = false;
        int limit;

        for (int i = start; i < end; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case SPACE:
                    continue;
                case MINUS:
                    if (minus) {
                        break;
                    }
                    minus = true;
                    continue;
            }
            if (c < 48 || c > 57) {
                return false;
            }
            if (totalWidth == 0) {
                if (c == 48) {
                    foundZero = true;
                    continue;
                }
                onLimit = c == 57;
            }
            int digit = c - 48;

            if (onLimit) {
                limit = overLongLimit(minus, totalWidth);
                if (digit != limit) {
                    if (digit > limit) {
                        overLimit = 1;
                    } else {
                        onLimit = false;
                    }
                }
            }
            if (++totalWidth > 19 || totalWidth == 19 && overLimit == 1) {
                return false;
            }
        }
        return totalWidth > 0 || foundZero;
    }

    private static int overLongLimit(boolean minus, int pos) {
        switch (pos) {
            case 1:
            case 2:
            case 6:
                return 2;
            case 3:
            case 4:
            case 8:
                return 3;
            case 5:
            case 13:
            case 14:
                return 7;
            case 7:
            case 17:
                return 0;
            case 9:
                return 6;
            case 10:
            case 16:
                return 8;
            case 11:
            case 15:
                return 5;
            case 12:
                return 4;
            case 18:
                return minus ? 8 : 7;
            default:
                break;
        }
        return 9;
    }

    public static boolean isDouble(CharSequence text) {
        if (text == null) {
            return false;
        }
        return isDouble(text, 0, text.length());
    }

    public static double getDouble(CharSequence text, int start, int end) {
        if (text == null) {
            return 0;
        }
        return parseDecimalNumber(false, (short) 308, text, start, end).doubleValue();
    }

    public static boolean isDouble(CharSequence text, int start, int end) {
        boolean foundZero = false;
        boolean minus = false;
        short totalWidth = 0;

        boolean hasDecimal = false;
        boolean hasExponent = false;
        byte exponentSymbol = 0;

        for (int i = start; i < end; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case SPACE:
                    continue;
                case MINUS:
                    if (minus) {
                        break;
                    }
                    minus = true;
                    continue;
                case SMALL_E:
                case BIG_E:
                    if (hasExponent || totalWidth == 0 && !foundZero) {
                        return false;
                    }
                    hasExponent = true;
                    exponentSymbol = 1;
                    continue;
                case DOT:
                case COMMA:
                    if (hasDecimal || hasExponent || totalWidth == 0 && !foundZero) {
                        return false;
                    }
                    hasDecimal = true;
                    continue;
            }
            if (c < 48 || c > 57) {
                if (end - 1 == i && (c == 'd' || c == 'f' || c == 'D' || c == 'F')) {
                    continue;
                }
                if (!foundZero && totalWidth == 0 && c == 'N' && i + 3 <= end) {
                    return text.charAt(++i) == 'a' && text.charAt(++i) == 'N';
                }
                if (!foundZero && totalWidth == 0 && c == 'I' && i + 8 <= end) {
                    return text.charAt(++i) == 'n' && text.charAt(++i) == 'f' && text.charAt(++i) == 'i' && text.charAt(++i) == 'n' && text.charAt(++i) == 'i' && text.charAt(++i) == 't'
                            && text.charAt(++i) == 'y';
                }
                return false;
            }
            if (!hasDecimal && totalWidth == 0 && c == 48) {
                foundZero = true;
                continue;
            }
            ++totalWidth;
            exponentSymbol = 0;
        }
        return (totalWidth > 0 || foundZero) && exponentSymbol == 0;
    }

    public static double getDouble(CharSequence text) {
        if (text == null) {
            return 0;
        }
        return getDouble(text, 0, text.length());
    }

    private static Number parseNonDecimalNumber(byte type, int totalDigits, CharSequence text, int start, int end) {
        if (text == null) {
            return 0;
        }
        Number result = 0;
        boolean minus = false;
        byte totalWidth = 0;
        byte overLimit = 0;
        boolean onLimit = false;
        int limit;

        for (int i = start; i < end; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case SPACE:
                    continue;
                case MINUS:
                    if (minus) {
                        break;
                    }
                    minus = true;
                    switch (type) {
                        case 0: //Byte
                            result = -result.byteValue();
                            break;
                        case 1: //Short
                            result = -result.shortValue();
                            break;
                        case 2: //Integer
                            result = -result.intValue();
                            break;
                        case 3: //Long
                            result = -result.longValue();
                            break;
                    }
                    continue;
            }
            if (c < 48 || c > 57) {
                continue;
            }
            if (totalWidth == 0) {
                if (c == 48) {
                    continue;
                }
                onLimit = isOnLimit(type, c);
            }
            int digit = c - 48;

            if (onLimit) {
                limit = checkOverLimit(type, minus, totalWidth);
                if (digit != limit) {
                    if (digit > limit) {
                        overLimit = 1;
                    } else {
                        onLimit = false;
                    }
                }
            }
            if (++totalWidth > totalDigits || totalWidth == totalDigits && overLimit == 1) {
                return getInfinityOf(type, minus);
            }

            result = multiplyTen(type, result, minus ? -digit : digit);
        }
        return result;
    }

    private static boolean isOnLimit(byte type, char c) {
        switch (type) {
            case 0: //Byte
                return c == 49;
            case 1: //Short
                return c == 51;
            case 2: //Integer
                return c == 50;
            case 3: //Long
                return c == 57;
        }
        return false;
    }

    private static Number parseDecimalNumber(boolean isFloat, short maxTotalWidth, CharSequence text, int start, int end) {
        double result = 0;
        int decimal = 0;
        int exponent = 0;
        boolean minusExponent = false;

        boolean minus = false;
        boolean hasDecimal = false;
        boolean hasExponent = false;
        byte exponentSymbol = 0;

        short totalWidth = 0;

        charsLoop:
        for (int i = start; i < end; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case SPACE:
                    continue;
                case MINUS:
                    if (minus) {
                        if (hasExponent && exponent == 0) {
                            minusExponent = true;
                            continue;
                        }
                        break charsLoop;
                    }
                    if (hasExponent && exponent == 0) {
                        minusExponent = true;
                        continue;
                    }
                    minus = true;
                    continue;
                case SMALL_E:
                case BIG_E:
                    if (hasExponent) {
                        break charsLoop;
                    }
                    hasExponent = true;
                    exponentSymbol = 1;
                    continue;
                case DOT:
                case COMMA:
                    if (hasDecimal || hasExponent) {
                        break charsLoop;
                    }
                    hasDecimal = true;
                    continue;
            }
            if (c < 48 || c > 57) {
                if (totalWidth == 0) {
                    if (c == 'N' && i + 3 <= end) {
                        if (text.charAt(i + 1) == 'a' && text.charAt(i + 2) == 'N') {
                            return isFloat ? Float.NaN : Double.NaN;
                        }
                    }
                    if (c == 'I' && i + 8 <= end) {
                        if (text.charAt(i + 1) == 'n' && text.charAt(i + 2) == 'f' && text.charAt(i + 3) == 'i' && text.charAt(i + 4) == 'n' && text.charAt(i + 5) == 'i' && text.charAt(i + 6) == 't'
                                && text.charAt(i + 7) == 'y') {
                            return isFloat ? (minus ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY) : minus ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                        }
                    }
                }
                continue;
            }
            if (!hasDecimal && totalWidth == 0 && c == 48) {
                continue;
            }
            int digit = c - 48;
            if (++totalWidth > maxTotalWidth) {
                return isFloat ? (minus ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY) : minus ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }

            if (hasExponent) {
                exponent = exponent * 10 + digit;
                exponentSymbol = 0;
            } else {
                result = result * 10 + digit;
                if (hasDecimal) {
                    ++decimal;
                }
            }
        }
        return isFloat ? (float) calculateResult(result, decimal, exponent, minusExponent, minus, exponentSymbol) : calculateResult(result, decimal, exponent, minusExponent, minus, exponentSymbol);
    }

    private static double calculateResult(double result, int decimal, int exponent, boolean minusExponent, boolean minus, byte exponentSymbol) {
        int range = (minusExponent ? -exponent : exponent) - decimal;
        if (range != 0) {
            if (range > 0) {
                result *= Math.pow(10, range);
            } else {
                result /= Math.pow(10, range * -1);
            }
        }
        return exponentSymbol == 0 ? minus ? -result : result : 0;
    }

    private static Number getInfinityOf(byte type, boolean minus) {
        switch (type) {
            case 0: //Byte
                return minus ? Byte.MIN_VALUE : Byte.MAX_VALUE;
            case 1: //Short
                return minus ? Short.MIN_VALUE : Short.MAX_VALUE;
            case 2: //Integer
                return minus ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            case 3: //Long
                return minus ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return 0;
    }

    private static Number multiplyTen(byte type, Number result, int digit) {
        switch (type) {
            case 0: //Byte
                return result.byteValue() * 10 + digit;
            case 1: //Short
                return result.shortValue() * 10 + digit;
            case 2: //Integer
                return result.intValue() * 10 + digit;
            case 3: //Long
                return result.longValue() * 10 + digit;
        }
        return 0;
    }

    private static int checkOverLimit(byte type, boolean minus, int totalWidth) {
        switch (type) {
            case 0: //Byte
                return overByteLimit(minus, totalWidth);
            case 1: //Short
                return overShortLimit(minus, totalWidth);
            case 2: //Integer
                return overIntLimit(minus, totalWidth);
            case 3: //Long
                return overLongLimit(minus, totalWidth);
        }
        return 0;
    }

    private static int overShortLimit(boolean minus, int pos) {
        switch (pos) {
            case 1:
                return 2;
            case 2:
                return 7;
            case 3:
                return 6;
            case 4:
                return minus ? 8 : 7;
        }
        return 3;
    }

    private static int overByteLimit(boolean minus, int pos) {
        switch (pos) {
            case 1:
                return 2;
            case 2:
                return minus ? 8 : 7;
        }
        return 1;
    }
}
