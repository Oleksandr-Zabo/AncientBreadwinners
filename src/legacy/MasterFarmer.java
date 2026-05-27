package com.example.ancientbreadwinners;

public class MasterFarmer extends Farmer {
    public MasterFarmer(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        super(name, motivation, speed, maxLoad, tool, x, y);
    }

    public MasterFarmer(String name, double x, double y) {
        this(name, 100, 1.8, 20, new Tool(ToolTypes.GoldenScythe, 1.3f), x, y);
    }

    @Override
    public String getImageAsset() {
        return "/assets/master_farmer.png";
    }

    @Override
    public String getKind() {
        return "MasterFarmer";
    }
}

