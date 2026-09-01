package com.nightlegion.livexp;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class NightLegionNavigationTest
{
    @Test
    public void eventsDropdownContainsOnlyApprovedEntries()
    {
        assertArrayEquals(new String[]{"BOTW", "SOTW", "GIVEAWAY"}, NightLegionPanel.eventSections());
    }
}
