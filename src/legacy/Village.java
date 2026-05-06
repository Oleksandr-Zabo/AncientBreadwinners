package com.example.ancientbreadwinners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class Village {
    private final List<MacroObject> macroObjects = new ArrayList<>();
    private final List<Farmer> farmers = new ArrayList<>();

    public List<MacroObject> getMacroObjects() {
        return Collections.unmodifiableList(macroObjects);
    }

    public List<Farmer> getFarmers() {
        return Collections.unmodifiableList(farmers);
    }

    public void addMacroObject(MacroObject macroObject) {
        macroObjects.add(macroObject);
    }

    public void addFarmer(Farmer farmer) {
        farmers.add(farmer);
    }

    public void removeFarmer(Farmer farmer) {
        for (MacroObject macroObject : macroObjects) {
            macroObject.removeFarmer(farmer);
        }
        farmers.remove(farmer);
    }

    public void clearMembership(Farmer farmer) {
        for (MacroObject macroObject : macroObjects) {
            macroObject.removeFarmer(farmer);
        }
    }

    public void assignToMacro(Farmer farmer, MacroObject target) {
        if (target == null || !farmers.contains(farmer)) {
            return;
        }
        target.addFarmer(farmer);
    }

    public List<MacroObject> memberships(Farmer farmer) {
        List<MacroObject> result = new ArrayList<>();
        for (MacroObject macroObject : macroObjects) {
            if (macroObject.contains(farmer)) {
                result.add(macroObject);
            }
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


    public List<Farmer> cloneFarmersList() {
        List<Farmer> cloned = new ArrayList<>(farmers.size());
        for (Farmer f : farmers) {
            cloned.add(f != null ? f.clone() : null);
        }
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
}

