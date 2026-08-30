package com.nightlegion.livexp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parsing helpers for NightLegion PB sources. */
final class NightLegionPbParser
{
    private static final String TIME = "[0-9]+(?::[0-9]{1,2}){0,2}(?:\\.[0-9]+)?";
    private static final Pattern CHAT_TIME = Pattern.compile(
        "(?i)(?:fight\\s+duration|lap\\s+time|lap\\s+duration|challenge\\s+duration|corrupted\\s+challenge\\s+duration|duration|completion\\s+time|subdued\\s+in)\\s*:?\\s*(" + TIME + ")"
    );
    private static final Pattern CHAT_TEAM = Pattern.compile("(?i)team\\s+size\\s*:?\\s*(solo|[0-9]+(?:\\s*[-+]\\s*[0-9]+)?\\s*players?)");
    private static final Pattern ADVENTURE_FASTEST = Pattern.compile(
        "(?i)^fastest\\s+(kill|run|room\\s+time|overall\\s+time)"
            + "(?:\\s*-\\s*\\(team\\s+size\\s*:\\s*([^)]+)\\))?\\s*:\\s*(" + TIME + ")?\\s*$"
    );
    private static final Pattern TIME_ONLY = Pattern.compile("^(" + TIME + ")$");
    private static final Pattern CA_PB = Pattern.compile("(?i)personal\\s+best\\s*:\\s*(" + TIME + ")");
    private static final Pattern BOARD_HEADER = Pattern.compile("(?i)^(.+?)\\s+statistics\\s*$");
    private static final Pattern BOARD_PB = Pattern.compile("(?i)^(awakened\\s+)?personal\\s+best\\s+time\\s*:?\\s*(" + TIME + ")\\s*$");
    private static final Pattern TEAM_NUMBER = Pattern.compile("([0-9]+)");

    private NightLegionPbParser()
    {
    }

    static final class PbRecord
    {
        final String boss;
        final String mode;
        final int teamSize;
        final String timeType;
        final double seconds;
        final String source;
        final String label;

        PbRecord(String boss, String mode, int teamSize, String timeType, double seconds, String source, String label)
        {
            this.boss = clean(boss, "In-game PB");
            this.mode = clean(mode, "");
            this.teamSize = Math.max(0, teamSize);
            this.timeType = clean(timeType, "");
            this.seconds = seconds;
            this.source = clean(source, "game");
            this.label = clean(label, this.boss);
        }

        String category()
        {
            StringBuilder out = new StringBuilder(boss);
            if (!mode.isEmpty())
            {
                out.append(" · ").append(mode);
            }
            if (teamSize > 0)
            {
                out.append(" · ").append(teamSize == 1 ? "Solo" : teamSize + "p");
            }
            if (!timeType.isEmpty())
            {
                out.append(" · ").append(timeType);
            }
            return out.length() > 80 ? out.substring(0, 80) : out.toString();
        }
    }

    static PbRecord parseChat(String raw)
    {
        String message = normalizeText(raw);
        String lower = message.toLowerCase(Locale.ROOT);
        if (message.isEmpty() || !lower.contains("new personal best"))
        {
            return null;
        }
        Matcher time = CHAT_TIME.matcher(message);
        if (!time.find())
        {
            return null;
        }
        double seconds = parseTime(time.group(1));
        if (seconds <= 0)
        {
            return null;
        }

        int teamSize = 0;
        Matcher team = CHAT_TEAM.matcher(message);
        if (team.find())
        {
            teamSize = parseTeamSize(team.group(1));
        }

        String bossGuess = inferChatBoss(message);
        Identity identity = normalizeIdentity(bossGuess, message);
        String label = message.length() > 120 ? message.substring(0, 120) : message;
        return new PbRecord(identity.boss, identity.mode, teamSize, "", seconds, "game-message", label);
    }

    static List<PbRecord> parseAdventureLog(List<String> rawLines)
    {
        List<PbRecord> out = new ArrayList<>();
        if (rawLines == null || rawLines.isEmpty())
        {
            return out;
        }

        List<String> lines = new ArrayList<>();
        for (String raw : rawLines)
        {
            lines.add(normalizeText(raw));
        }

        String currentBoss = "";
        Map<String, PbRecord> unique = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++)
        {
            String line = lines.get(i);
            if (line.isEmpty())
            {
                continue;
            }

            Matcher fastest = ADVENTURE_FASTEST.matcher(line);
            if (!fastest.matches())
            {
                if (!TIME_ONLY.matcher(line).matches() && !looksLikeCounter(line))
                {
                    currentBoss = line;
                }
                continue;
            }

            String time = fastest.group(3);
            if ((time == null || time.isEmpty()) && i + 1 < lines.size())
            {
                Matcher next = TIME_ONLY.matcher(lines.get(i + 1));
                if (next.matches())
                {
                    time = next.group(1);
                    i++;
                }
            }
            double seconds = parseTime(time);
            if (seconds <= 0 || currentBoss.isEmpty())
            {
                continue;
            }

            String details = fastest.group(2) == null ? "" : fastest.group(2);
            Identity identity = normalizeIdentity(currentBoss, details);
            int teamSize = parseTeamSize(details);
            String kind = fastest.group(1).toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
            String timeType = "ROOM TIME".equals(kind) ? "ROOM" : "OVERALL TIME".equals(kind) ? "OVERALL" : "";
            PbRecord record = new PbRecord(
                identity.boss,
                identity.mode,
                teamSize,
                timeType,
                seconds,
                "adventure-log",
                "Adventure Log · " + currentBoss + " · " + kind
            );
            unique.put(record.category(), record);
        }
        out.addAll(unique.values());
        return out;
    }

    static PbRecord parseCombatAchievement(String rawBoss, List<String> rawStats)
    {
        String boss = normalizeText(rawBoss).replaceFirst("(?i)^combat achievements\\s*-\\s*", "").trim();
        if (boss.isEmpty() || rawStats == null)
        {
            return null;
        }
        StringBuilder combined = new StringBuilder();
        for (String stat : rawStats)
        {
            String clean = normalizeText(stat);
            if (!clean.isEmpty())
            {
                combined.append(' ').append(clean);
            }
        }
        Matcher pb = CA_PB.matcher(combined.toString());
        if (!pb.find())
        {
            return null;
        }
        double seconds = parseTime(pb.group(1));
        if (seconds <= 0)
        {
            return null;
        }
        Identity identity = normalizeIdentity(boss, boss);
        return new PbRecord(identity.boss, identity.mode, 0, "", seconds, "combat-achievements", "Combat Achievements · " + boss);
    }

    static List<PbRecord> parseBossStatistics(List<String> rawLines)
    {
        List<PbRecord> out = new ArrayList<>();
        if (rawLines == null || rawLines.isEmpty())
        {
            return out;
        }
        String currentBoss = "";
        Map<String, PbRecord> unique = new LinkedHashMap<>();

        for (String raw : rawLines)
        {
            String line = normalizeText(raw);
            if (line.isEmpty())
            {
                continue;
            }
            Matcher header = BOARD_HEADER.matcher(line);
            if (header.matches())
            {
                currentBoss = header.group(1).trim();
                continue;
            }
            if (currentBoss.isEmpty())
            {
                continue;
            }
            Matcher pb = BOARD_PB.matcher(line);
            if (!pb.matches())
            {
                continue;
            }
            double seconds = parseTime(pb.group(2));
            if (seconds <= 0)
            {
                continue;
            }
            String extra = pb.group(1) == null ? "" : "Awakened";
            Identity identity = normalizeIdentity(currentBoss, currentBoss + " " + extra);
            PbRecord record = new PbRecord(
                identity.boss,
                identity.mode,
                0,
                "",
                seconds,
                "boss-statistics",
                "Boss Statistics · " + currentBoss + (extra.isEmpty() ? "" : " · Awakened")
            );
            unique.put(record.category(), record);
        }
        out.addAll(unique.values());
        return out;
    }

    static double parseTime(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return -1;
        }
        try
        {
            String[] parts = value.trim().split(":");
            double seconds = 0;
            for (String part : parts)
            {
                seconds = seconds * 60.0 + Double.parseDouble(part);
            }
            return seconds;
        }
        catch (NumberFormatException ignored)
        {
            return -1;
        }
    }

    private static int parseTeamSize(String value)
    {
        if (value == null)
        {
            return 0;
        }
        String clean = value.trim().toLowerCase(Locale.ROOT);
        if (clean.contains("solo"))
        {
            return 1;
        }
        Matcher matcher = TEAM_NUMBER.matcher(clean);
        if (matcher.find())
        {
            try
            {
                return Integer.parseInt(matcher.group(1));
            }
            catch (NumberFormatException ignored)
            {
                return 0;
            }
        }
        return 0;
    }

    private static String inferChatBoss(String message)
    {
        String clean = message.replaceFirst("(?i)\\s*\\(new personal best\\).*", "").trim();
        int team = clean.toLowerCase(Locale.ROOT).indexOf("team size:");
        if (team > 0)
        {
            String before = clean.substring(0, team).trim();
            if (!before.isEmpty())
            {
                return stripTrailingPunctuation(before);
            }
        }
        int colon = clean.indexOf(':');
        if (colon > 0)
        {
            String before = clean.substring(0, colon).trim();
            if (!isTimingLabel(before) && before.length() <= 100)
            {
                return stripTrailingPunctuation(before);
            }
        }
        return "In-game PB";
    }

    private static boolean isTimingLabel(String value)
    {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("duration") || lower.contains("completion time") || lower.contains("subdued in") || lower.contains("lap time");
    }

    private static boolean looksLikeCounter(String value)
    {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("kill count") || lower.startsWith("completion count") || lower.startsWith("personal best")
            || lower.startsWith("tasks completed") || lower.startsWith("combat level") || lower.startsWith("global ");
    }

    private static Identity normalizeIdentity(String bossValue, String contextValue)
    {
        String boss = clean(bossValue, "In-game PB").trim();
        String context = (boss + " " + clean(contextValue, "")).toLowerCase(Locale.ROOT);
        String mode = "";

        if (containsAny(context, "hard mode", " hard", " hm"))
        {
            mode = "Hard Mode";
        }
        else if (containsAny(context, "expert mode", " expert"))
        {
            mode = "Expert Mode";
        }
        else if (containsAny(context, "entry mode", " entry"))
        {
            mode = "Entry Mode";
        }
        else if (containsAny(context, "challenge mode", " cm"))
        {
            mode = "Challenge Mode";
        }
        else if (context.contains("awakened"))
        {
            mode = "Awakened";
        }

        String lowerBoss = boss.toLowerCase(Locale.ROOT);
        if (lowerBoss.contains("theatre of blood"))
        {
            boss = "Theatre of Blood";
            if (mode.isEmpty()) mode = "Normal";
        }
        else if (lowerBoss.contains("tombs of amascut"))
        {
            boss = "Tombs of Amascut";
            if (mode.isEmpty()) mode = "Normal";
        }
        else if (lowerBoss.contains("chambers of xeric"))
        {
            boss = "Chambers of Xeric";
            if (mode.isEmpty()) mode = "Normal";
        }
        else if (lowerBoss.contains("tztok-jad") || lowerBoss.contains("tzhaar fight cave"))
        {
            boss = "TzHaar Fight Cave";
            mode = "";
        }
        else if (lowerBoss.contains("tzkal-zuk") || lowerBoss.equals("the inferno") || lowerBoss.equals("inferno"))
        {
            boss = "Inferno";
            mode = "";
        }
        else if (lowerBoss.contains("leviathan"))
        {
            boss = "The Leviathan";
        }
        else if (lowerBoss.contains("whisperer"))
        {
            boss = "The Whisperer";
        }
        else if (lowerBoss.contains("vardorvis"))
        {
            boss = "Vardorvis";
        }
        else if (lowerBoss.contains("duke sucellus"))
        {
            boss = "Duke Sucellus";
        }

        boss = boss
            .replaceAll("(?i)\\s*[-:(]?\\s*(hard|entry|expert|challenge)\\s+mode\\)?\\s*$", "")
            .replaceAll("(?i)\\s*[-:(]?\\s*(hm|cm|awakened)\\)?\\s*$", "")
            .trim();
        return new Identity(boss.isEmpty() ? "In-game PB" : boss, mode);
    }

    private static boolean containsAny(String haystack, String... needles)
    {
        for (String needle : needles)
        {
            if (haystack.contains(needle))
            {
                return true;
            }
        }
        return false;
    }

    private static String normalizeText(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        return raw
            .replaceAll("(?i)<br\\s*/?>", ": ")
            .replaceAll("<[^>]+>", "")
            .replace('&', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String stripTrailingPunctuation(String value)
    {
        return value.replaceAll("[\\s:;,-]+$", "").trim();
    }

    private static String clean(String value, String fallback)
    {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    private static final class Identity
    {
        final String boss;
        final String mode;

        Identity(String boss, String mode)
        {
            this.boss = boss;
            this.mode = mode;
        }
    }
}
