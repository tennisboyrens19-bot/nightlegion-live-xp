package com.nightlegion.livexp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NightLegionRankTrackerTest
{
    @Test
    public void recognizesClanCongratulationsWithoutCountingNormalMessagesAsGz()
    {
        assertTrue(NightLegionRankTracker.isCongratulation("Gzz!!"));
        assertTrue(NightLegionRankTracker.isCongratulation("gz"));
        assertFalse(NightLegionRankTracker.isCongratulation("great kill"));
    }

    @Test
    public void recognizesGameKillCountActivity()
    {
        assertEquals("Vorkath", NightLegionRankTracker.killCountBoss("Your Vorkath kill count is: 1,234."));
        assertNull(NightLegionRankTracker.killCountBoss("Collection log: Vorkath"));
    }
}
