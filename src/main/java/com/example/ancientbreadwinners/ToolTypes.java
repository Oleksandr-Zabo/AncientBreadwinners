package com.example.ancientbreadwinners;

public enum ToolTypes {
    NoTool,
    Knife,
    Sickle,
    Scythe,
    GoldenScythe;

    public double speedCoeff() {
        return switch (this) {
            case Knife -> 1.1;
            case Sickle, Scythe -> 1.5;
            case GoldenScythe -> 2.0;
            default -> 1.0;
        };
    }

    public int price() {
        return switch (this) {
            case Knife -> 50;
            case Sickle, Scythe -> 150;
            case GoldenScythe -> 250;
            default -> 0;
        };
    }

    public String displayName() {
        return switch (this) {
            case Knife -> "Ніж (50 монет)";
            case Sickle -> "Серп (150 монет)";
            case Scythe -> "Коса (150 монет)";
            case GoldenScythe -> "Золота Коса (250 монет)";
            default -> "Без інструменту";
        };
    }
}
