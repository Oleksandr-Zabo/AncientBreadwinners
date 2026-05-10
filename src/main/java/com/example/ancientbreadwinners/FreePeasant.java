package com.example.ancientbreadwinners;

import java.util.Set;

public class FreePeasant extends Farmer {
    private static final long serialVersionUID = 1L;

    public FreePeasant(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        super(name, motivation, speed, maxLoad, tool, x, y);
    }

    public FreePeasant(String name, double x, double y) {
        this(name, 60, 1.3, 10, new Tool(ToolTypes.NoTool, 1.0f), x, y);
    }

    @Override
    public String getImageAsset() { return "/assets/free_peasant.png"; }

    @Override
    public String getKind() { return "Вільний Селянин"; }

    @Override
    public Set<ToolTypes> allowedTools() {
        return Set.of(ToolTypes.NoTool, ToolTypes.Sickle, ToolTypes.Scythe);
    }

    @Override
    public ToolTypes defaultToolType() { return ToolTypes.NoTool; }

    @Override
    public void speak() {
        setMotivation(getMotivation() + 20);
    }

    @Override
    public int motivationDropOnWork() { return 25; }
}
