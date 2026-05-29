package com.example.ancientbreadwinners;

import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class SelectionService {
    private static final int MAX_FARMERS_IN_MACRO = 4;
    private final HelloApplication app;

    SelectionService(HelloApplication app) {
        this.app = app;
    }

    void clearSelection() {
        for (Farmer f : app.village.getFarmers()) f.setActive(false);
        app.selectedFarmer = null;
        app.selectedMacro = null;
    }

    void cloneSelected() {
        for (Farmer source : activeFarmers()) {
            Farmer clone = source.clone();
            clone.setName(nextCloneName(source.getName()));
            double[] pos = findClonePosition(source);
            clone.setX(pos[0]);
            clone.setY(pos[1]);
            clampFarmerInsideWorld(clone);
            clone.setActive(false);
            app.village.addFarmer(clone);
            syncMembershipByTouch(clone);
        }
    }

    double[] findClonePosition(Farmer source) {
        int index = app.village.getFarmers().size();
        double offsetX = (index % 3) * 40;
        double offsetY = (index / 3) * 40;
        return new double[]{source.getX() + offsetX, source.getY() + offsetY};
    }

    void moveSelected(KeyCode code) {
        for (Farmer f : activeFarmers()) {
            List<MacroObject> memberships = app.village.memberships(f);
            if (!memberships.isEmpty()) {
                ejectFromMacro(f, memberships.getFirst());
            }

            double step = 10;
            double nx = f.getX() + (code == KeyCode.LEFT ? -step : code == KeyCode.RIGHT ? step : 0);
            double ny = f.getY() + (code == KeyCode.UP ? -step : code == KeyCode.DOWN ? step : 0);
            f.setX(clampMicroX(nx));
            f.setY(clampMicroY(ny));
        }
    }

    void deleteSelected() {
        new ArrayList<>(activeFarmers()).forEach(app.village::removeFarmer);
        app.selectedFarmer = null;
    }

    void detachSelected() {
        activeFarmers().forEach(app.village::clearMembership);
    }

    void enterSelectedToMacro() {
        long now = System.nanoTime();
        for (Farmer f : activeFarmers()) {
            Optional<MacroObject> target = findTouchedMacroByBody(f);
            if (target.isEmpty() && app.interactionMode == InteractionMode.MANUAL) {
                target = findNearbyMacroForManualEnter(f, 30);
            }
            if (target.isEmpty()) continue;

            MacroObject macro = target.get();
            if (placeInMacroWithSpacing(f, macro)) {
                app.village.assignToMacro(f, macro);
                double coeff = f.getTool().getType().speedCoeff();
                f.setWorkTimerEnd(now + workDurationNs(f.getMaxLoad(), coeff));

                switch (macro) {
                    case WheatField ignored -> f.setState(FarmerState.WORKING_AT_FIELD);
                    case Mill ignored -> f.setState(FarmerState.WORKING_AT_MILL);
                    case Church ignored -> {
                        f.setRestTimerEnd(now + 3_000_000_000L);
                        f.setState(FarmerState.RESTING_AT_CHURCH);
                    }
                    default -> {}
                }
            }
        }
    }

    void exitSelectedFromMacro() {
        for (Farmer f : activeFarmers()) {
            List<MacroObject> memberships = app.village.memberships(f);
            if (!memberships.isEmpty()) {
                ejectFromMacro(f, memberships.getFirst());
            }
        }
    }

    void upgradeSelectedFarmers() {
        final int UPGRADE_COST = 500;
        List<Farmer> toUpgrade = activeFarmers();
        int totalCost = toUpgrade.size() * UPGRADE_COST;

        if (app.village.getTotalCoins() < totalCost) {
            app.ui.showInfo("Недостатньо монет! Потрібно: " + totalCost + ", є: " + app.village.getTotalCoins());
            return;
        }

        int upgradedCount = 0;
        for (Farmer f : toUpgrade) {
            Farmer newFarmer = null;
            Tool currentTool = f.getTool();

            if (f.getClass() == Gardener.class) {
                newFarmer = new FreePeasant(f.getName(), f.getMotivation(), f.getSpeed(), f.getMaxLoad(), currentTool, f.getX(), f.getY());
            } else if (f.getClass() == FreePeasant.class) {
                newFarmer = new MasterFarmer(f.getName(), f.getMotivation(), f.getSpeed(), f.getMaxLoad(), currentTool, f.getX(), f.getY());
            }

            if (newFarmer != null) {
                newFarmer.setActive(true);
                newFarmer.setState(f.getState());
                newFarmer.setCurrentLoad(f.getCurrentLoad());
                app.village.removeFarmer(f);
                app.village.addFarmer(newFarmer);
                upgradedCount++;
            }
        }

        if (upgradedCount > 0) {
            app.village.addCoins(-totalCost);
            app.ui.showInfo("Оновлено " + upgradedCount + " хліборобів за " + totalCost + " монет! Нові інструменти доступні в меню купівлі (I).");
        } else {
            app.ui.showInfo("Немає хліборобів для оновлення (Майстер-Хлібороб вже максимальний тип)");
        }
    }

    void syncMembershipByTouch(Farmer farmer) {
        if (app.interactionMode == InteractionMode.MANUAL) return;

        Optional<MacroObject> touched = findTouchedMacroByBody(farmer);
        if (touched.isPresent()) {
            MacroObject macro = touched.get();
            boolean alreadyMember = macro.contains(farmer);
            if (!alreadyMember) {
                if (canEnterMacro(macro)) {
                    teleportToFreePositionInMacro(farmer, macro);
                    app.village.assignToMacro(farmer, macro);
                }
            }
        } else {
            app.village.clearMembership(farmer);
        }
    }

    boolean canEnterMacro(MacroObject macro) {
        return macro.getMembers().size() < MAX_FARMERS_IN_MACRO;
    }

    void clampFarmerInsideWorld(Farmer f) {
        f.setX(clampMicroX(f.getX()));
        f.setY(clampMicroY(f.getY()));
    }

    boolean placeInMacroWithSpacing(Farmer farmer, MacroObject macro) {
        if (!macro.contains(farmer) && !canEnterMacro(macro)) {
            app.ui.showInfo("Макрооб'єкт переповнений! Максимум " + MAX_FARMERS_IN_MACRO + " фермерів.");
            return false;
        }

        double margin = 20;
        double availableSpace = macro.getWidth() - 2 * margin;
        double slotSize = availableSpace / 2;

        int index = macro.getMembers().size();
        int col = index % 2;
        int row = index / 2;

        double x = macro.getX() + margin + col * slotSize;
        double y = macro.getY() + margin + row * slotSize;

        double centerOffsetX = (slotSize - Farmer.WIDTH) / 2;
        double centerOffsetY = (slotSize - Farmer.HEIGHT) / 2;

        farmer.setX(x + centerOffsetX);
        farmer.setY(y + centerOffsetY);
        return true;
    }

    Optional<MacroObject> findNearbyMacroForManualEnter(Farmer farmer, double proximityPx) {
        double fx = farmer.getX();
        double fy = farmer.getY();
        MacroObject best = null;
        double bestDistance = Double.MAX_VALUE;

        for (MacroObject mo : app.village.getMacroObjects()) {
            double expandedLeft = mo.getX() - proximityPx;
            double expandedTop = mo.getY() - proximityPx;
            double expandedRight = mo.getX() + mo.getWidth() + proximityPx;
            double expandedBottom = mo.getY() + mo.getHeight() + proximityPx;

            boolean near = fx + Farmer.WIDTH >= expandedLeft
                    && fx <= expandedRight
                    && fy + Farmer.HEIGHT >= expandedTop
                    && fy <= expandedBottom;
            if (!near) continue;

            double dx = (mo.getX() + mo.getWidth() / 2.0) - (fx + Farmer.WIDTH / 2.0);
            double dy = (mo.getY() + mo.getHeight() / 2.0) - (fy + Farmer.HEIGHT / 2.0);
            double dist = Math.hypot(dx, dy);
            if (dist < bestDistance) {
                bestDistance = dist;
                best = mo;
            }
        }

        return Optional.ofNullable(best);
    }

    boolean ejectFromMacro(Farmer farmer, MacroObject macro) {
        double centerX = macro.getX() + macro.getWidth() / 2;
        double centerY = macro.getY() + macro.getHeight() / 2;
        double fx = farmer.getX();
        double fy = farmer.getY();

        double dx = fx - centerX;
        double dy = fy - centerY;

        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) {
                farmer.setX(macro.getX() + macro.getWidth() + 10);
            } else {
                farmer.setX(macro.getX() - Farmer.WIDTH - 10);
            }
        } else {
            if (dy > 0) {
                farmer.setY(macro.getY() + macro.getHeight() + 10);
            } else {
                farmer.setY(macro.getY() - Farmer.HEIGHT - 10);
            }
        }

        app.village.clearMembership(farmer);
        clampFarmerInsideWorld(farmer);
        return true;
    }

    void teleportToFreePositionInMacro(Farmer farmer, MacroObject macro) {
        double margin = 20;
        double availableSpace = macro.getWidth() - 2 * margin;
        double slotSize = availableSpace / 2;

        int index = macro.getMembers().size();
        int col = index % 2;
        int row = index / 2;

        double x = macro.getX() + margin + col * slotSize;
        double y = macro.getY() + margin + row * slotSize;

        double centerOffsetX = (slotSize - Farmer.WIDTH) / 2;
        double centerOffsetY = (slotSize - Farmer.HEIGHT) / 2;

        farmer.setX(x + centerOffsetX);
        farmer.setY(y + centerOffsetY);
    }

    Optional<MacroObject> findTouchedMacroByBody(Farmer farmer) {
        double fx = farmer.getX() - 15;
        double fy = farmer.getY() - 25;
        double fw = Farmer.WIDTH + 15;
        double fh = Farmer.HEIGHT + 15;
        MacroObject best = null;
        for (MacroObject mo : app.village.getMacroObjects()) {
            double half = HelloApplication.MACRO_STROKE_W / 2;
            double mx = mo.getX() - half, my = mo.getY() - half;
            double mw = mo.getWidth() + half * 2, mh = mo.getHeight() + half * 2;
            if (intersectsInclusive(fx, fy, fw, fh, mx, my, mw, mh)) {
                if (best == null || mo.getX() < best.getX()) best = mo;
            }
        }
        return Optional.ofNullable(best);
    }

    boolean intersectsInclusive(double x1, double y1, double w1, double h1,
                                        double x2, double y2, double w2, double h2) {
        return x1 <= x2 + w2 && x1 + w1 >= x2 && y1 <= y2 + h2 && y1 + h1 >= y2;
    }

    double clampMicroX(double x) {
        return Math.clamp(x, HelloApplication.MICRO_FRAME_PADDING, app.WORLD_WIDTH - HelloApplication.MICRO_BLOCK_SIZE - HelloApplication.MICRO_FRAME_PADDING);
    }

    double clampMicroY(double y) {
        return Math.clamp(y, HelloApplication.MICRO_FRAME_PADDING, app.WORLD_HEIGHT - HelloApplication.MICRO_BLOCK_SIZE - HelloApplication.MICRO_FRAME_PADDING);
    }

    String nextCloneName(String sourceName) {
        String base = cloneBaseName(sourceName);
        String prefix = base + "_копія";
        int next = 1;
        for (Farmer f : app.village.getFarmers()) {
            String n = f.getName();
            if (n.startsWith(prefix)) {
                String suffix = n.substring(prefix.length());
                if (suffix.matches("\\d+")) next = Math.max(next, Integer.parseInt(suffix) + 1);
            }
        }
        return prefix + next;
    }

    String cloneBaseName(String name) {
        String marker = "_копія";
        int idx = name.lastIndexOf(marker);
        if (idx < 0) return name;
        String suffix = name.substring(idx + marker.length());
        return suffix.matches("\\d+") ? name.substring(0, idx) : name;
    }

    List<Farmer> activeFarmers() {
        return app.village.getFarmers().stream().filter(Farmer::isActive).toList();
    }

    List<Farmer> sortedByCriteria(List<Farmer> input) {
        List<Farmer> sorted = new ArrayList<>(input);
        switch (app.sortCriteria) {
            case BY_LOAD -> sorted.sort(java.util.Comparator.comparingInt(Farmer::getMaxLoad));
            case BY_SPEED -> sorted.sort(java.util.Comparator.comparingDouble(Farmer::getSpeed));
            default -> sorted.sort(java.util.Comparator.comparing(Farmer::getName, String.CASE_INSENSITIVE_ORDER));
        }
        return sorted;
    }

    private long workDurationNs(int maxLoad, double speedCoeff) {
        double seconds = maxLoad * 0.5 / Math.max(0.1, speedCoeff);
        return (long) (seconds * 1_000_000_000L);
    }
}


