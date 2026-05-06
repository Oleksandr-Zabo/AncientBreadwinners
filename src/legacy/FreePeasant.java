package com.example.ancientbreadwinners;

public class FreePeasant extends Farmer {
    public FreePeasant(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        super(name, motivation, speed, maxLoad, tool, x, y);
    }

    public FreePeasant(String name, double x, double y) {
        this(name, 60, 1.3, 10, new Tool(ToolTypes.Knife, 1.0f), x, y);
    }

    @Override
    public String getImageAsset() {
        return "/assets/free_peasant.png";
    }

    @Override
    public String getKind() {
        return "FreePeasant";
    }
}

