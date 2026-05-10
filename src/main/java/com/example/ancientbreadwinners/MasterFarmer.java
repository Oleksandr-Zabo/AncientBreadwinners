package com.example.ancientbreadwinners;

import java.util.Set;

public class MasterFarmer extends Farmer {
    private static final long serialVersionUID = 1L;

    public MasterFarmer(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        super(name, motivation, speed, maxLoad, tool, x, y);
    }

    public MasterFarmer(String name, double x, double y) {
        this(name, 85, 1.8, 20, new Tool(ToolTypes.NoTool, 1.0f), x, y);
    }

    @Override
    public String getImageAsset() { return "/assets/master_farmer.png"; }

    @Override
    public String getKind() { return "Майстер-Хлібороб"; }

    @Override
    public Set<ToolTypes> allowedTools() {
        return Set.of(ToolTypes.NoTool, ToolTypes.Knife, ToolTypes.Sickle, ToolTypes.Scythe, ToolTypes.GoldenScythe);
    }

    @Override
    public ToolTypes defaultToolType() { return ToolTypes.NoTool; }

    @Override
    public void speak() {
        setMotivation(getMotivation() + 25);
    }

    @Override
    public int motivationDropOnWork() { return 20; }
}
