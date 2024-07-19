package de.gregorpoloczek.projectmaintainer.reporting.projectreport;

public enum ColumnTextAlignment {
    LEFT, CENTER, RIGHT;

    public static ColumnTextAlignment fromString(String textAlignment) {
        return ColumnTextAlignment.valueOf(textAlignment.toUpperCase());
    }
}
