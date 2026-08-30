package com.nightlegion.livexp;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NightLegionPbParserTest
{
    @Test
    public void parsesAdventureLogRaidModesAndRoomSplits()
    {
        List<NightLegionPbParser.PbRecord> rows = NightLegionPbParser.parseAdventureLog(Arrays.asList(
            "Theatre of Blood - Hard",
            "Fastest Room time - (Team size: 3 player hard mode):",
            "20:21.60",
            "Fastest Overall time - (Team size: 3 player hard mode): 22:19.30",
            "Tombs of Amascut: Expert Mode",
            "Fastest run - (Team size: Solo): 31:22.40",
            "Chambers of Xeric CM",
            "Fastest run - (Team size: 4 players): 18:09.20"
        ));

        assertEquals(4, rows.size());
        assertEquals("Theatre of Blood · Hard Mode · 3p · ROOM", rows.get(0).category());
        assertEquals(1221.60, rows.get(0).seconds, 0.001);
        assertEquals("Theatre of Blood · Hard Mode · 3p · OVERALL", rows.get(1).category());
        assertEquals("Tombs of Amascut · Expert Mode · Solo", rows.get(2).category());
        assertEquals("Chambers of Xeric · Challenge Mode · 4p", rows.get(3).category());
    }

    @Test
    public void parsesCombatAchievementPages()
    {
        NightLegionPbParser.PbRecord vorkath = NightLegionPbParser.parseCombatAchievement(
            "Combat Achievements - Vorkath",
            Arrays.asList("Combat Level: 732 Kill Count: 702 Personal Best: 1:21.00")
        );
        assertNotNull(vorkath);
        assertEquals("Vorkath", vorkath.boss);
        assertEquals(81.0, vorkath.seconds, 0.001);

        NightLegionPbParser.PbRecord jad = NightLegionPbParser.parseCombatAchievement(
            "TzTok-Jad",
            Arrays.asList("Combat Level: 702", "Kill Count: 1", "Personal Best: 1:06:13.20")
        );
        assertNotNull(jad);
        assertEquals("TzHaar Fight Cave", jad.boss);
        assertEquals(3973.20, jad.seconds, 0.001);
    }

    @Test
    public void parsesNormalAndAwakenedStatisticsBoards()
    {
        List<NightLegionPbParser.PbRecord> rows = NightLegionPbParser.parseBossStatistics(Arrays.asList(
            "The Whisperer Statistics",
            "Personal Best Time<br>2:05.40",
            "Awakened Personal Best Time<br>3:48.20",
            "Leviathan (Awakened) Statistics",
            "Personal Best Time<br>5:37.20"
        ));

        assertEquals(3, rows.size());
        assertEquals("The Whisperer", rows.get(0).category());
        assertEquals(125.4, rows.get(0).seconds, 0.001);
        assertEquals("The Whisperer · Awakened", rows.get(1).category());
        assertEquals("The Leviathan · Awakened", rows.get(2).category());
        assertEquals(337.2, rows.get(2).seconds, 0.001);
    }

    @Test
    public void parsesThreePartTimes()
    {
        assertEquals(3973.2, NightLegionPbParser.parseTime("1:06:13.20"), 0.001);
        assertEquals(81.0, NightLegionPbParser.parseTime("1:21.00"), 0.001);
    }
}
