package com.revalclan.ui;

import com.google.gson.Gson;
import com.revalclan.api.RevalApiService;
import com.revalclan.api.points.PointsResponse;
import org.junit.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class RankingSessionBoundaryTest {
    @Test public void resetAndNextSessionIgnoreOldRankingSuccessAndError() throws Exception {
        FakeApi api = new FakeApi();
        RankingPanel[] panel = new RankingPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new RankingPanel();
            panel[0].init(api, null, null, null);
            panel[0].load();
            panel[0].resetSession();
        });
        List<String> resetLabels = labels(panel[0]);
        api.success.get(0).accept(new PointsResponse());
        api.errors.get(0).accept(new RuntimeException("Previous login"));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(resetLabels, labels(panel[0]));
        SwingUtilities.invokeAndWait(panel[0]::load);
        api.success.get(1).accept(new Gson().fromJson("{\"status\":\"success\",\"data\":{\"ranks\":[],\"pointSources\":{}}}", PointsResponse.class));
        SwingUtilities.invokeAndWait(() -> {});
        List<String> currentLabels = labels(panel[0]);
        assertTrue(currentLabels.toString(), currentLabels.contains("POINT SOURCES"));
        api.success.get(0).accept(new PointsResponse());
        api.errors.get(0).accept(new RuntimeException("Previous login"));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(currentLabels, labels(panel[0]));
    }

    private static List<String> labels(Container container) {
        List<String> labels = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel) labels.add(((JLabel) child).getText());
            if (child instanceof Container) labels.addAll(labels((Container) child));
        }
        return labels;
    }
    private static class FakeApi extends RevalApiService {
        final List<Consumer<PointsResponse>> success = new ArrayList<>();
        final List<Consumer<Exception>> errors = new ArrayList<>();
        FakeApi() { super(null, new Gson()); }
        @Override public void fetchPoints(Consumer<PointsResponse> ok, Consumer<Exception> error) {
            success.add(ok); errors.add(error);
        }
    }
}
