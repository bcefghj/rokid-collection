package com.rokidsmartlife.api;

import java.io.Serializable;
import java.util.List;

public class RouteResult implements Serializable {
    public String origin;
    public String destination;
    public String totalDistance;
    public String totalDuration;
    public List<Step> steps;

    public String getFormattedDistance() {
        if (totalDistance == null) return "";
        try {
            int d = Integer.parseInt(totalDistance);
            if (d >= 1000) return String.format("%.1fkm", d / 1000.0);
            return d + "m";
        } catch (NumberFormatException e) {
            return totalDistance;
        }
    }

    public String getFormattedDuration() {
        if (totalDuration == null) return "";
        try {
            int seconds = Integer.parseInt(totalDuration);
            if (seconds >= 3600) {
                return String.format("%d小时%d分", seconds / 3600, (seconds % 3600) / 60);
            }
            if (seconds >= 60) {
                return (seconds / 60) + "分钟";
            }
            return seconds + "秒";
        } catch (NumberFormatException e) {
            return totalDuration;
        }
    }

    public static class Step implements Serializable {
        public String instruction;
        public String road;
        public String distance;
        public String duration;
        public String action;
        public String assistantAction;

        public String getActionIcon() {
            if (action == null) return "→";
            switch (action) {
                case "左转": return "↰";
                case "右转": return "↱";
                case "直行": return "↑";
                case "左前方转弯": return "↖";
                case "右前方转弯": return "↗";
                case "左后方转弯": return "↲";
                case "右后方转弯": return "↳";
                case "掉头": return "↶";
                case "到达目的地": return "◉";
                default: return "→";
            }
        }
    }
}
