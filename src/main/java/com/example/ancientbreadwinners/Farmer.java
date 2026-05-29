package com.example.ancientbreadwinners;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

public abstract class Farmer implements Cloneable, Comparable<Farmer>, Serializable {
    private static final long serialVersionUID = 1L;

    public static final double WIDTH = 135;
    public static final double HEIGHT = 135;

    private String name;
    private int motivation;
    private double speed;
    private int maxLoad;
    private Tool tool;
    private double x;
    private double y;
    private boolean active;
    private int currentLoad;

    private transient FarmerState state;
    private transient long workTimerEnd;
    private transient long restTimerEnd;
    private transient long talkTimerEnd;
    private transient long lastSpokeNano;
    private transient FarmerState stateBeforeTalk;
    private transient double distWalkedSinceDrain;

    protected Farmer(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        this.name = name;
        this.motivation = motivation;
        this.speed = speed;
        this.maxLoad = maxLoad;
        setTool(tool);
        this.x = x;
        this.y = y;
    }

    public abstract String getImageAsset();
    public abstract String getKind();
    public abstract Set<ToolTypes> allowedTools();
    public abstract ToolTypes defaultToolType();

    public abstract void speak();

    protected final void applySpeakGain(int motivationGain) {
        setMotivation(getMotivation() + motivationGain);
    }

    public abstract int motivationDropOnWork();

    public void speak(Farmer other) {
        this.speak();
        other.speak();
    }

    public void speak(Farmer f1, Farmer f2) {
        int thisBefore = this.getMotivation();
        int f1Before = f1.getMotivation();
        int f2Before = f2.getMotivation();

        this.speak();
        f1.speak();
        f2.speak();

        applyTripleTalkBonus(this, thisBefore);
        applyTripleTalkBonus(f1, f1Before);
        applyTripleTalkBonus(f2, f2Before);
    }

    private void applyTripleTalkBonus(Farmer farmer, int beforeMotivation) {
        int gained = farmer.getMotivation() - beforeMotivation;
        if (gained > 0) {
            farmer.setMotivation(farmer.getMotivation() + (int) Math.round(gained * 0.5));
        }
    }

    public double effectiveSpeed() {
        return speed * getTool().speedCoeff();
    }

    public FarmerState getState() {
        return state == null ? FarmerState.IDLE : state;
    }

    public void setState(FarmerState state) {
        this.state = state;
    }

    public long getWorkTimerEnd() { return workTimerEnd; }
    public void setWorkTimerEnd(long v) { workTimerEnd = v; }

    public long getRestTimerEnd() { return restTimerEnd; }
    public void setRestTimerEnd(long v) { restTimerEnd = v; }

    public long getTalkTimerEnd() { return talkTimerEnd; }
    public void setTalkTimerEnd(long v) { talkTimerEnd = v; }

    public long getLastSpokeNano() { return lastSpokeNano; }
    public void setLastSpokeNano(long v) { lastSpokeNano = v; }

    public FarmerState getStateBeforeTalk() {
        return stateBeforeTalk == null ? FarmerState.IDLE : stateBeforeTalk;
    }
    public void setStateBeforeTalk(FarmerState v) { stateBeforeTalk = v; }

    public int getCurrentLoad() { return currentLoad; }
    public void setCurrentLoad(int v) { currentLoad = Math.max(0, v); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMotivation() { return motivation; }
    public void setMotivation(int motivation) { this.motivation = Math.max(0, Math.min(100, motivation)); }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.max(0.1, speed); }

    public int getMaxLoad() { return maxLoad; }
    public void setMaxLoad(int maxLoad) { this.maxLoad = Math.max(1, maxLoad); }

    public Tool getTool() { return tool; }

    public void setTool(Tool tool) {
        Tool candidate = tool == null ? new Tool(defaultToolType(), 1.0f) : tool;
        if (candidate.getType() != ToolTypes.NoTool && !allowedTools().contains(candidate.getType())) {
            candidate.setType(defaultToolType());
        }
        this.tool = candidate;
    }

    public void setToolType(ToolTypes toolType) {
        if (tool == null) tool = new Tool(ToolTypes.NoTool, 1.0f);
        ToolTypes normalized = toolType == null ? ToolTypes.NoTool : toolType;
        if (normalized != ToolTypes.NoTool && !allowedTools().contains(normalized)) {
            normalized = defaultToolType();
        }
        tool.setType(normalized);
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + WIDTH && py >= y && py <= y + HEIGHT;
    }

    @Override
    public Farmer clone() {
        try {
            Farmer cloned = (Farmer) super.clone();
            cloned.tool = tool == null ? null : tool.clone();
            cloned.active = false;
            cloned.currentLoad = 0;
            cloned.state = FarmerState.IDLE;
            cloned.workTimerEnd = 0;
            cloned.restTimerEnd = 0;
            cloned.talkTimerEnd = 0;
            cloned.lastSpokeNano = 0;
            cloned.stateBeforeTalk = FarmerState.IDLE;
            cloned.distWalkedSinceDrain = 0;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int compareTo(Farmer other) {
        return Integer.compare(motivation, other.motivation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Farmer farmer)) return false;
        if (this.getClass() != farmer.getClass()) return false;
        if (this.getMotivation() != farmer.getMotivation()) return false;
        return Objects.equals(name, farmer.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return getKind() + "{name='" + name + "', motivation=" + motivation +
                ", speed=" + speed + ", maxLoad=" + maxLoad + ", tool=" + tool + '}';
    }
}
