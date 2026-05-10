package com.example.ancientbreadwinners;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class MacroObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String imageAsset;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final List<Farmer> members = new ArrayList<>();

    protected MacroObject(String name, String imageAsset, double x, double y, double width, double height) {
        this.name = name;
        this.imageAsset = imageAsset;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getName() { return name; }
    public String getImageAsset() { return imageAsset; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<Farmer> getMembers() { return members; }
    public int getCount() { return members.size(); }

    public boolean contains(Farmer farmer) {
        return members.contains(farmer);
    }

    public boolean addFarmer(Farmer farmer) {
        if (farmer == null || members.contains(farmer)) return false;
        members.add(farmer);
        return true;
    }

    public boolean removeFarmer(Farmer farmer) {
        return members.remove(farmer);
    }

    public boolean inBounds(double px, double py, double w, double h) {
        return px >= x && py >= y && px + w <= x + width && py + h <= y + height;
    }

    @Override
    public String toString() {
        return name;
    }
}
