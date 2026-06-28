package me.petulikan1.zMachines.utils;

public class VersionUtils {
    public enum Version {
        OLDER_VERSION, NEWER_VERSION, SAME_VERSION, UNKNOWN
    }

    public static Version getVersion(String version, String compareVersion) {
        if (version == null || compareVersion == null)
            return Version.UNKNOWN;

        version = version.replaceAll("[^0-9.]+", "").trim();
        compareVersion = compareVersion.replaceAll("[^0-9.]+", "").trim();

        if (version.isEmpty() || compareVersion.isEmpty())
            return Version.UNKNOWN;

        String[] primaryVersion = version.split("\\.");
        String[] compareToVersion = compareVersion.split("\\.");

        int max = Math.max(primaryVersion.length, compareToVersion.length);
        for (int i = 0; i <= max; ++i) {
            String number = i >= primaryVersion.length ? "0" : "1" + primaryVersion[i];
            if (compareToVersion.length <= i) {
                if (compareToVersion.length == i && compareToVersion.length == max)
                    break;
                return Version.NEWER_VERSION;
            }
            if (StringUtils.getInt(number) > StringUtils.getInt("1" + compareToVersion[i]))
                return Version.NEWER_VERSION;
            if (StringUtils.getInt(number) < StringUtils.getInt("1" + compareToVersion[i]))
                return Version.OLDER_VERSION;
        }
        return Version.SAME_VERSION;
    }

    public static double convertToDouble(String version) {
        double ver = 0;
        int dotPos = 1;
        for (String split : version.replaceAll("[^0-9.]+", "").split("\\.")) {
            int additional = 1;
            if (split.length() > 1)
                for (int i = 1; i < split.length(); ++i)
                    additional *= 10;

            ver += StringUtils.getDouble(split) / dotPos / additional;
            dotPos = dotPos * 10;
        }
        return ver;
    }
}
