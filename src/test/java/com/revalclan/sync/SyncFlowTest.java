package com.revalclan.sync;

import com.revalclan.api.RevalApiService;
import com.revalclan.ui.*;
import java.lang.reflect.Field;
import java.util.List;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SyncFlowTest {
    @Test public void initialSyncWaitsForGameStateToSettle() {
        SyncSchedule s = new SyncSchedule(); s.reset(1000);
        assertEquals(-1, s.begin(3999)); assertTrue(s.begin(4000) > 0);
    }
    @Test public void oneFlightAndBurstsAreCoalesced() {
        SyncSchedule s = new SyncSchedule(); s.reset(0);
        long id = s.begin(3000);
        for (int i=0;i<100;i++) { s.request(); assertEquals(-1,s.begin(3500)); }
        assertTrue(s.finish(id,true,4000));
        assertEquals(-1,s.begin(8999)); assertTrue(s.begin(9000)>id);
    }
    @Test public void successfulSyncDoesNotStopPeriodicUpdates() {
        SyncSchedule s = new SyncSchedule();s.reset(0);
        long id=s.begin(3000);s.finish(id,true,4000);
        assertEquals(-1,s.begin(63000));assertTrue(s.begin(64000)>id);
    }
    @Test public void failuresBackOffAndRetry() {
        SyncSchedule s=new SyncSchedule();s.reset(0);
        long id=s.begin(3000);s.finish(id,false,4000);
        assertEquals(-1,s.begin(18999));assertTrue(s.begin(19000)>id);
    }
    @Test public void logoutAndTokenChangesRejectOldCompletion() {
        SyncSchedule s=new SyncSchedule();s.reset(0);
        long id=s.begin(3000);s.reset(4000);
        assertFalse(s.finish(id,true,5000));assertFalse(s.isCurrent(id));
        assertTrue(s.begin(7000)>id);
    }
    @Test public void missingCallbacksTimeoutWithoutOverlappingRequests() {
        SyncSchedule s=new SyncSchedule();s.reset(0);
        long id=s.begin(3000);
        assertEquals(-1,s.begin(48000));assertFalse(s.finish(id,true,48001));
        assertTrue(s.begin(63000)>id);
    }
    @Test public void bankedCapeAndAvernicAreObservedOnlyWhileBankIsOpen() throws Exception {
        Client c=mock(Client.class); ItemManager items=mock(ItemManager.class);
        when(c.getAccountHash()).thenReturn(7L);
        ItemContainer bank=mock(ItemContainer.class);
        when(bank.getItems()).thenReturn(new Item[]{new Item(6570,1),new Item(22322,1)});
        when(c.getItemContainer(95)).thenReturn(bank);
        for(int id:new int[]{6570,22322}) {
            ItemComposition def=mock(ItemComposition.class);
            when(def.getPlaceholderTemplateId()).thenReturn(-1);
            when(items.getItemComposition(id)).thenReturn(def);
            when(items.canonicalize(id)).thenReturn(id);
        }
        MilestoneEvidence e=new MilestoneEvidence();set(e,"client",c);set(e,"itemManager",items);
        assertEquals(List.of(),e.collect());
        Widget w=mock(Widget.class);when(c.getWidget(WidgetInfo.BANK_ITEM_CONTAINER)).thenReturn(w);
        assertEquals(List.of(6570,22322),e.collect());assertTrue(e.hasSeenBank());
        when(w.isHidden()).thenReturn(true);
        assertEquals(List.of(6570,22322),e.collect()); // keep known achievement evidence
        when(c.getAccountHash()).thenReturn(8L);
        assertEquals(List.of(),e.collect());assertFalse(e.hasSeenBank());
    }
    @Test public void zeroQuantityAndPlaceholdersNeverAwardOwnership() throws Exception {
        Client c=mock(Client.class);ItemManager items=mock(ItemManager.class);
        when(c.getAccountHash()).thenReturn(7L);
        ItemContainer inventory=mock(ItemContainer.class);
        when(inventory.getItems()).thenReturn(new Item[]{new Item(6570,0),new Item(12954,1)});
        when(c.getItemContainer(93)).thenReturn(inventory);
        ItemComposition placeholder=mock(ItemComposition.class);
        when(placeholder.getPlaceholderTemplateId()).thenReturn(14401);
        when(items.getItemComposition(12954)).thenReturn(placeholder);
        MilestoneEvidence e=new MilestoneEvidence();set(e,"client",c);set(e,"itemManager",items);
        assertTrue(e.collect().isEmpty());
    }
    @Test public void savedProgressRefreshesAllDependentTabsWithoutNavigation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                RevalPanel shell=new RevalPanel();
                RevalApiService api=mock(RevalApiService.class);
                ProfilePanel profile=mock(ProfilePanel.class);
                AchievementsPanel achievements=mock(AchievementsPanel.class);
                DiaryPanel diary=mock(DiaryPanel.class);
                set(shell,"apiService",api);set(shell,"profilePanel",profile);
                set(shell,"achievementsPanel",achievements);set(shell,"diaryPanel",diary);
                shell.showTab("DIARY");shell.onProgressChanged();
                verify(api).clearAccountCache();verify(profile).refresh();
                verify(achievements).refresh();verify(diary).refresh();
                assertEquals("DIARY",shell.getActiveTab());
            } catch(Exception e) {throw new AssertionError(e);}
        });
    }
    private static void set(Object object,String field,Object value)throws Exception {
        Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);f.set(object,value);
    }
}
