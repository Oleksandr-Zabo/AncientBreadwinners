package com.example.ancientbreadwinners;

import java.util.Objects;

public abstract class Farmer implements Cloneable, Comparable<Farmer> {
    public static final double WIDTH = 100;
    public static final double HEIGHT = 150;

    private String name;
    private int motivation;
    private double speed;
    private int maxLoad;
    private Tool tool;
    private double x;
    private double y;
    private boolean active;

    protected Farmer(String name, int motivation, double speed, int maxLoad, Tool tool, double x, double y) {
        this.name = name;
        this.motivation = motivation;
        this.speed = speed;
        this.maxLoad = maxLoad;
        this.tool = tool;
        this.x = x;
        this.y = y;
    }

    public abstract String getImageAsset();

    public abstract String getKind();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMotivation() {
        return motivation;
    }

    public void setMotivation(int motivation) {
        this.motivation = Math.max(0, Math.min(100, motivation));
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0.1, speed);
    }

    public int getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(int maxLoad) {
        this.maxLoad = Math.max(1, maxLoad);
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + WIDTH && py >= y && py <= y + HEIGHT;
    }

    @Override
    public Farmer clone() {
        try {
            Farmer cloned = (Farmer) super.clone();
            cloned.tool = tool == null ? null : tool.clone();
            cloned.active = false;
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
        return Objects.equals(name, farmer.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return getKind() + "{" +
                "name='" + name + '\'' +
                ", motivation=" + motivation +
                ", speed=" + speed +
                ", maxLoad=" + maxLoad +
                ", tool=" + tool +
                '}';
    }
}

