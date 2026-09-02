package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NightLegionGroupsContractTest
{
    @Test
    public void filtersOpenListingsByActivity()
    {
        JsonObject overview = new JsonObject();
        JsonArray groups = new JsonArray();
        groups.add(group("TOA", "open"));
        groups.add(group("Nex", "open"));
        groups.add(group("TOA", "closed"));
        overview.add("groups", groups);

        List<JsonObject> toa = NightLegionGroupFinderPanel.filterGroups(overview, "TOA");
        assertEquals(1, toa.size());
        assertEquals(2, NightLegionGroupFinderPanel.filterGroups(overview, "All activities").size());
    }

    @Test
    public void runtimeUsesOnlyNightLegionBackend()
    {
        assertTrue(NightLegionApi.BASE.contains("nightlegion-livexp"));
        assertFalse(NightLegionApi.BASE.contains("raidmates"));
        assertFalse(NightLegionApi.BASE.contains("api.raidmates.nl"));
    }

    private static JsonObject group(String activity, String status)
    {
        JsonObject row = new JsonObject();
        row.addProperty("activity", activity);
        row.addProperty("status", status);
        return row;
    }
}
