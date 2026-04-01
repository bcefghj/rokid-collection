package com.rokidsmartlife.api;

import java.io.Serializable;
import java.util.List;

public class TransitResult implements Serializable {
    public String origin;
    public String destination;
    public String totalDistance;
    public String totalDuration;
    public String totalWalkingDistance;
    public String totalPrice;
    public List<TransitPlan> plans;

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

    public static class TransitPlan implements Serializable {
        public String cost;
        public String duration;
        public String walkingDistance;
        public boolean nightflag;
        public List<Segment> segments;

        public String getFormattedDuration() {
            if (duration == null) return "";
            try {
                int seconds = Integer.parseInt(duration);
                if (seconds >= 3600) {
                    return String.format("%d小时%d分", seconds / 3600, (seconds % 3600) / 60);
                }
                if (seconds >= 60) {
                    return (seconds / 60) + "分钟";
                }
                return seconds + "秒";
            } catch (NumberFormatException e) {
                return duration;
            }
        }

        public String getFormattedCost() {
            if (cost == null || cost.isEmpty() || "0".equals(cost)) return "免费";
            return "¥" + cost;
        }
    }

    public static class Segment implements Serializable {
        public static final int TYPE_WALKING = 0;
        public static final int TYPE_BUS = 1;
        public static final int TYPE_SUBWAY = 2;
        public static final int TYPE_TAXI = 3;

        public int type;
        public String instruction;

        // Walking fields
        public String walkingDistance;
        public String walkingDuration;

        // Bus/Subway fields
        public String lineName;
        public String departureStop;
        public String arrivalStop;
        public int passStops;
        public String viaBusStops;
        public String busDuration;

        public String getIcon() {
            switch (type) {
                case TYPE_WALKING: return "\uD83D\uDEB6";
                case TYPE_BUS: return "\uD83D\uDE8C";
                case TYPE_SUBWAY: return "\uD83D\uDE87";
                case TYPE_TAXI: return "\uD83D\uDE95";
                default: return "\u27A1";
            }
        }

        public String getTypeName() {
            switch (type) {
                case TYPE_WALKING: return "步行";
                case TYPE_BUS: return "公交";
                case TYPE_SUBWAY: return "地铁";
                case TYPE_TAXI: return "打车";
                default: return "";
            }
        }

        public String getFormattedDistance() {
            if (walkingDistance == null) return "";
            try {
                int d = Integer.parseInt(walkingDistance);
                if (d >= 1000) return String.format("%.1fkm", d / 1000.0);
                return d + "m";
            } catch (NumberFormatException e) {
                return walkingDistance;
            }
        }

        public String getSummary() {
            switch (type) {
                case TYPE_WALKING:
                    return "步行 " + getFormattedDistance();
                case TYPE_BUS:
                case TYPE_SUBWAY:
                    StringBuilder sb = new StringBuilder();
                    if (lineName != null) sb.append(lineName);
                    if (departureStop != null) sb.append(" (").append(departureStop).append(" 上车");
                    if (arrivalStop != null) sb.append(" → ").append(arrivalStop).append(" 下车)");
                    if (passStops > 0) sb.append(" ").append(passStops).append("站");
                    return sb.toString();
                default:
                    return instruction != null ? instruction : "";
            }
        }
    }
}
