package com.rokidsmartlife.api;

import java.io.Serializable;

public class PoiResult implements Serializable {
    public String id;
    public String name;
    public String type;
    public String typecode;
    public String address;
    public String tel;
    public String distance;
    public double longitude;
    public double latitude;
    public String rating;
    public String cost;
    public String openTime;
    public String photoUrl;
    public String cityname;
    public String adname;

    public String getFormattedDistance() {
        if (distance == null) return "";
        try {
            int d = Integer.parseInt(distance);
            if (d >= 1000) {
                return String.format("%.1fkm", d / 1000.0);
            }
            return d + "m";
        } catch (NumberFormatException e) {
            return distance;
        }
    }

    public String getFormattedRating() {
        if (rating == null || rating.isEmpty() || "0".equals(rating)) return "";
        return rating;
    }

    public String getFormattedCost() {
        if (cost == null || cost.isEmpty() || "0".equals(cost)) return "";
        return "¥" + cost + "/人";
    }

    public String getCategoryIcon() {
        if (typecode == null) return "?";
        if (typecode.startsWith("05")) return "\uD83C\uDF5C";  // 餐饮
        if (typecode.startsWith("06")) return "\uD83D\uDED2";  // 购物
        if (typecode.startsWith("08")) return "\uD83C\uDFA4";  // 娱乐
        if (typecode.startsWith("07")) return "\uD83C\uDFE8";  // 酒店
        if (typecode.startsWith("09")) return "\uD83D\uDE97";  // 交通
        if (typecode.startsWith("10")) return "\uD83C\uDFE5";  // 医疗
        if (typecode.startsWith("14")) return "\uD83C\uDFDB";  // 景点
        if (typecode.startsWith("11")) return "\uD83C\uDFEB";  // 教育
        return "\uD83D\uDCCD";
    }
}
