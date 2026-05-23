package com.example.ancientbreadwinners;

import java.util.Set;

public class Gardener extends Farmer {
    private static final long serialVersionUID = 1L;

    public Gardener(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        super(name, motivation, speed, maxLoad, tool, x, y);
    }

    public Gardener(String name, double x, double y) {
        this(name, 70, 1.1, 12, new Tool(ToolTypes.NoTool, 1.0f), x, y);
    }

    @Override
    public String getImageAsset() { return "/assets/gardener.png"; }

    @Override
    public String getKind() { return "Городник"; }

    @Override
    public Set<ToolTypes> allowedTools() {
        return Set.of(ToolTypes.NoTool, ToolTypes.Knife);
    }

    @Override
    public ToolTypes defaultToolType() { return ToolTypes.NoTool; }

    @Override
    public void speak() {
        super.applySpeakGain(15);
    }

    @Override
    public int motivationDropOnWork() { return 30; }
}
