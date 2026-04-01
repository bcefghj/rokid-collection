package com.rokidsmartlife.utils;

public class PoiCategory {
    public final String name;
    public final String typeCode;
    public final String icon;
    public final int color;

    public PoiCategory(String name, String typeCode, String icon, int color) {
        this.name = name;
        this.typeCode = typeCode;
        this.icon = icon;
        this.color = color;
    }

    public static final PoiCategory[] CATEGORIES = {
            new PoiCategory("美食", "050000", "\uD83C\uDF5C", 0xFFFF6B35),
            new PoiCategory("咖啡", "050500", "\u2615",       0xFF8B4513),
            new PoiCategory("购物", "060000", "\uD83D\uDED2", 0xFFE91E63),
            new PoiCategory("酒店", "070000", "\uD83C\uDFE8", 0xFF9C27B0),
            new PoiCategory("娱乐", "080000", "\uD83C\uDFA4", 0xFF00BCD4),
            new PoiCategory("景点", "140000", "\uD83C\uDFDB", 0xFF4CAF50),
            new PoiCategory("交通", "150000", "\uD83D\uDE8C", 0xFF2196F3),
            new PoiCategory("医院", "090000", "\uD83C\uDFE5", 0xFFF44336),
            new PoiCategory("银行", "160000", "\uD83C\uDFE6", 0xFF607D8B),
            new PoiCategory("超市", "060400", "\uD83C\uDFEA", 0xFFFF9800),
            new PoiCategory("药店", "090601", "\uD83D\uDC8A", 0xFF4DB6AC),
            new PoiCategory("加油站", "010100", "\u26FD",     0xFF795548),
    };
}
