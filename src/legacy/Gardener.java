package com.example.ancientbreadwinners;

public class Gardener extends Farmer {
    public Gardener(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        super(name, motivation, speed, maxLoad, tool, x, y);
    }

    public Gardener(String name, double x, double y) {
        this(name, 70, 1.1, 12, new Tool(ToolTypes.Sickle, 1.0f), x, y);
    }

    @Override
    public String getImageAsset() {
        return "/assets/gardener.png";
    }

    @Override
    public String getKind() {
        return "Gardener";
    }
}

