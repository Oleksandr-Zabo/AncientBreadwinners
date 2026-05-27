package com.example.ancientbreadwinners;

public enum InteractionMode {
    AUTOMATIC("AUTOMATIC"),
    MANUAL("MANUAL");

    private final String label;

    InteractionMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

