package com.revalclan.api.common;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** One automatic overall MVP, with the three contributing monthly boards. */
@Data
public class MonthlyMvp {
    private String status;
    private String periodStart;
    private String generatedAt;
    private Winner winner;
    private Map<String, List<Entry>> boards;

    public boolean isReady() { return "ready".equals(status); }

    @Data
    public static class Winner {
        private String playerName;
        private int points;
    }

    @Data
    public static class Entry {
        private String rsn;
        private int position;
        private double value;
    }
}
