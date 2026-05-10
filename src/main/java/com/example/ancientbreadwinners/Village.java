package com.example.ancientbreadwinners;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Village implements Serializable {
    private static final long serialVersionUID = 1L;

    public record Placement(double x, double y) {}

    private final List<MacroObject> macroObjects = new ArrayList<>();
    private final List<Farmer> farmers = new ArrayList<>();
    private int totalCoins = 0;

    public List<MacroObject> getMacroObjects() {
        return Collections.unmodifiableList(macroObjects);
    }

    public List<Farmer> getFarmers() {
        return Collections.unmodifiableList(farmers);
    }

    public int getTotalCoins() { return totalCoins; }

    public void addCoins(int amount) {
        totalCoins = Math.max(0, totalCoins + amount);
    }

    public void setTotalCoins(int v) { totalCoins = Math.max(0, v); }

    public void addMacroObject(MacroObject macroObject) {
        macroObjects.add(macroObject);
    }

    public void addFarmer(Farmer farmer) {
        farmers.add(farmer);
    }

    public void removeFarmer(Farmer farmer) {
        for (MacroObject mo : macroObjects) mo.removeFarmer(farmer);
        farmers.remove(farmer);
    }

    public void clearMembership(Farmer farmer) {
        for (MacroObject mo : macroObjects) mo.removeFarmer(farmer);
    }

    public void assignToMacro(Farmer farmer, MacroObject target) {
        if (target == null || !farmers.contains(farmer)) return;
        clearMembership(farmer);
        target.addFarmer(farmer);
    }

    public Optional<MacroObject> findTouchedMacro(Farmer farmer) {
        for (MacroObject mo : macroObjects) {
            if (intersects(farmer.getX(), farmer.getY(), mo.getX(), mo.getY(), mo.getWidth(), mo.getHeight())) {
                return Optional.of(mo);
            }
        }
        return Optional.empty();
    }

    public boolean placeInsideMacro(Farmer farmer, MacroObject mo) {
        Placement spot = findFreeSpot(mo.getX(), mo.getY(), mo.getWidth(), mo.getHeight(), farmer);
        if (spot == null) return false;
        farmer.setX(spot.x());
        farmer.setY(spot.y());
        return true;
    }

    public Optional<Placement> findFreeAdjacentPosition(Farmer source, double worldWidth, double worldHeight) {
        double step = 20;
        double[][] offsets = {
                {Farmer.WIDTH + step, 0},
                {-(Farmer.WIDTH + step), 0},
                {0, -(Farmer.HEIGHT + step)},
                {0, Farmer.HEIGHT + step},
                {Farmer.WIDTH + step, -(Farmer.HEIGHT + step)},
                {-(Farmer.WIDTH + step), -(Farmer.HEIGHT + step)},
                {Farmer.WIDTH + step, Farmer.HEIGHT + step},
                {-(Farmer.WIDTH + step), Farmer.HEIGHT + step}
        };
        for (double[] off : offsets) {
            double x = source.getX() + off[0];
            double y = source.getY() + off[1];
            if (x < 0 || y < 0 || x + Farmer.WIDTH > worldWidth || y + Farmer.HEIGHT > worldHeight) continue;
            if (isAreaFree(x, y, source)) return Optional.of(new Placement(x, y));
        }
        return Optional.empty();
    }

    public boolean isAreaFree(double x, double y, Farmer ignored) {
        for (Farmer other : farmers) {
            if (other == ignored) continue;
            if (intersects(x, y, other.getX(), other.getY(), Farmer.WIDTH, Farmer.HEIGHT)) return false;
        }
        return true;
    }

    private Placement findFreeSpot(double left, double top, double width, double height, Farmer ignored) {
        double step = 20;
        for (double y = top; y <= top + height - Farmer.HEIGHT; y += step) {
            for (double x = left; x <= left + width - Farmer.WIDTH; x += step) {
                if (isAreaFree(x, y, ignored)) return new Placement(x, y);
            }
        }
        return null;
    }

    private boolean intersects(double x1, double y1, double x2, double y2, double w2, double h2) {
        return x1 <= x2 + w2 && x1 + Farmer.WIDTH >= x2 && y1 <= y2 + h2 && y1 + Farmer.HEIGHT >= y2;
    }

    public List<MacroObject> memberships(Farmer farmer) {
        List<MacroObject> result = new ArrayList<>();
        for (MacroObject mo : macroObjects) {
            if (mo.contains(farmer)) result.add(mo);
        }
        return result;
    }

    public static class NameComparator implements Comparator<Farmer> {
        @Override
        public int compare(Farmer f1, Farmer f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    public void sortByName() {
        farmers.sort(new NameComparator());
    }

    public void sortByMaxLoadAnonymous() {
        farmers.sort(new Comparator<Farmer>() {
            @Override
            public int compare(Farmer f1, Farmer f2) {
                return Integer.compare(f1.getMaxLoad(), f2.getMaxLoad());
            }
        });
    }

    public void sortBySpeed() {
        farmers.sort((f1, f2) -> Double.compare(f1.getSpeed(), f2.getSpeed()));
    }

    public List<Farmer> farmersByType(Class<? extends Farmer> type) {
        return farmers.stream()
                .filter(type::isInstance)
                .collect(Collectors.toList());
    }

    public long countWithHighMotivation(int threshold) {
        return farmers.stream().filter(f -> f.getMotivation() > threshold).count();
    }

    public long countWithoutTool() {
        return farmers.stream().filter(f -> f.getTool().getType() == ToolTypes.NoTool).count();
    }

    public List<Farmer> cloneFarmersList() {
        List<Farmer> cloned = new ArrayList<>(farmers.size());
        for (Farmer f : farmers) cloned.add(f != null ? f.clone() : null);
        return cloned;
    }

    public int binarySearch(Farmer key, Comparator<Farmer> comparator) {
        return Collections.binarySearch(farmers, key, comparator);
    }

    public int[] findAllMatches(Farmer key, Comparator<Farmer> comparator) {
        int idx = binarySearch(key, comparator);
        if (idx < 0) return new int[0];
        List<Integer> matches = new ArrayList<>();
        for (int i = idx; i >= 0 && comparator.compare(farmers.get(i), key) == 0; i--) matches.add(i);
        for (int i = idx + 1; i < farmers.size() && comparator.compare(farmers.get(i), key) == 0; i++) matches.add(i);
        return matches.stream().mapToInt(Integer::intValue).toArray();
    }

    public void deleteByCategory(Predicate<Farmer> predicate) {
        farmers.removeIf(predicate);
    }

    public void loadFrom(Village other) {
        macroObjects.clear();
        macroObjects.addAll(other.macroObjects);
        farmers.clear();
        farmers.addAll(other.farmers);
        totalCoins = other.totalCoins;
    }

    public void clearAll() {
        macroObjects.clear();
        farmers.clear();
        totalCoins = 0;
    }
}
