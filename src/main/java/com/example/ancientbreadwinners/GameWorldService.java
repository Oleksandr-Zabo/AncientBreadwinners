package com.example.ancientbreadwinners;

import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class GameWorldService {
    private static final int MAX_FARMERS_IN_MACRO = 4;

    private final HelloApplication app;
    private final ConversationService conversation;
    private final SelectionService selection;
    private final CameraService camera;

    GameWorldService(HelloApplication app) {
        this.app = app;
        this.conversation = new ConversationService(app);
        this.selection = new SelectionService(app);
        this.camera = new CameraService(app);
    }

    void tick(long now) {
        double dt = app.lastFrameNano == 0 ? 0.016 : (now - app.lastFrameNano) / 1_000_000_000.0;
        dt = Math.min(dt, 0.1);
        app.lastFrameNano = now;

        if (now - app.lastBlinkNano > 500_000_000L) {
            app.blinkOn = !app.blinkOn;
            app.lastBlinkNano = now;
        }

        List<Farmer> snapshot = new ArrayList<>(app.village.getFarmers());
        conversation.checkFarmerTalking(snapshot, now);
        for (Farmer f : snapshot) updateFarmerAI(f, now, dt);

        app.renderer.redraw();
    }

    void seedData() {
        double centerX = (app.WORLD_WIDTH - HelloApplication.MACRO_SIZE) / 2;
        double centerY = (app.WORLD_HEIGHT - HelloApplication.MACRO_SIZE) / 2;
        double margin = app.WORLD_WIDTH * 0.15;
        double offsetY = app.WORLD_HEIGHT * 0.25;
        MacroObject church = new Church(centerX, centerY + offsetY);
        MacroObject mill = new Mill(app.WORLD_WIDTH - margin - HelloApplication.MACRO_SIZE, centerY);
        MacroObject field = new WheatField(margin, centerY - offsetY);
        app.village.addMacroObject(church);
        app.village.addMacroObject(mill);
        app.village.addMacroObject(field);

        Farmer f1 = new Gardener("Іван", 72, 1.1, 12, new Tool(ToolTypes.NoTool, 1.0f), centerX + 50, centerY + 50);
        Farmer f2 = new FreePeasant("Петро", 64, 1.3, 10, new Tool(ToolTypes.NoTool, 1.0f), centerX + 80, centerY + 50);
        Farmer f3 = new MasterFarmer("Сергій", 90, 1.7, 20, new Tool(ToolTypes.NoTool, 1.0f), centerX + 50, centerY + 80);
        Farmer f4 = new Gardener("Микола", 50, 1.0, 10, new Tool(ToolTypes.NoTool, 1.0f), centerX + 80, centerY + 80);

        app.village.addFarmer(f1);
        app.village.addFarmer(f2);
        app.village.addFarmer(f3);
        app.village.addFarmer(f4);

        for (Farmer f : app.village.getFarmers()) {
            clampFarmerInsideWorld(f);
            syncMembershipByTouch(f);
        }
    }

    void clearSelection() { selection.clearSelection(); }

    void cloneSelected() { selection.cloneSelected(); }

    private double[] findClonePosition(Farmer source) {
        int index = app.village.getFarmers().size();
        double offsetX = (index % 3) * 40;
        double offsetY = (index / 3) * 40;
        return new double[]{source.getX() + offsetX, source.getY() + offsetY};
    }

    void moveSelected(KeyCode code) { selection.moveSelected(code); }

    void deleteSelected() { selection.deleteSelected(); }

    void detachSelected() { selection.detachSelected(); }

    void enterSelectedToMacro() { selection.enterSelectedToMacro(); }

    void exitSelectedFromMacro() { selection.exitSelectedFromMacro(); }

    void upgradeSelectedFarmers() { selection.upgradeSelectedFarmers(); }

    void moveCamera(double dx, double dy) { camera.moveCamera(dx, dy); }

    void clampCamera() { camera.clampCamera(); }


    void setupMinimapClick() { camera.setupMinimapClick(); }

    void toggleInteractionMode() {
        app.interactionMode = app.interactionMode == InteractionMode.AUTOMATIC
                ? InteractionMode.MANUAL
                : InteractionMode.AUTOMATIC;
        app.ui.showInfo("Режим взаємодії: " + app.interactionMode.label());
    }

    List<Farmer> activeFarmers() { return selection.activeFarmers(); }

    List<Farmer> sortedByCriteria(List<Farmer> input) { return selection.sortedByCriteria(input); }

    MacroObject findMacroByType(Class<? extends MacroObject> type) {
        return app.village.getMacroObjects().stream()
                .filter(type::isInstance)
                .findFirst().orElse(null);
    }

    void checkFarmerTalking(List<Farmer> farmers, long now) { conversation.checkFarmerTalking(farmers, now); }

    void updateFarmerAI(Farmer farmer, long now, double dt) {
        if (farmer.isActive()) return;

        FarmerState state = farmer.getState();

        if (state == FarmerState.TALKING || state == FarmerState.TALKING_TRIPLE) {
            if (now >= farmer.getTalkTimerEnd()) farmer.setState(farmer.getStateBeforeTalk());
            return;
        }

        if (farmer.getMotivation() <= 15
                && state != FarmerState.WALKING_TO_CHURCH
                && state != FarmerState.RESTING_AT_CHURCH) {
            farmer.setState(FarmerState.WALKING_TO_CHURCH);
        }

        MacroObject church = findMacroByType(Church.class);

        switch (farmer.getState()) {
            case IDLE -> {
                MacroObject field = findNearestMacro(farmer, WheatField.class);
                if (field != null) {
                    farmer.setState(FarmerState.WALKING_TO_FIELD);
                }
            }
            case WALKING_TO_FIELD -> {
                MacroObject field = findNearestMacro(farmer, WheatField.class);
                if (field == null) { farmer.setState(FarmerState.IDLE); break; }
                double[] spot = app.interactionMode == InteractionMode.MANUAL
                        ? findSpotOutsideMacro(field, farmer)
                        : findSpotInsideMacro(field, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    if (app.interactionMode == InteractionMode.MANUAL) {
                        syncMembershipByTouch(farmer);
                    } else {
                        syncMembershipByTouch(farmer);
                        double coeff = farmer.getTool().getType().speedCoeff();
                        farmer.setWorkTimerEnd(now + workDurationNs(farmer.getMaxLoad(), coeff));
                        farmer.setState(FarmerState.WORKING_AT_FIELD);
                    }
                }
            }
            case WORKING_AT_FIELD -> {
                if (now >= farmer.getWorkTimerEnd()) {
                    farmer.setCurrentLoad(farmer.getMaxLoad());
                    farmer.setMotivation(farmer.getMotivation() - farmer.motivationDropOnWork());
                    resetMembershipAfterWorkCycle(farmer);
                    MacroObject mill = findNearestMacro(farmer, Mill.class);
                    farmer.setState(mill != null ? FarmerState.WALKING_TO_MILL : FarmerState.WALKING_TO_CHURCH);
                }
            }
            case WALKING_TO_MILL -> {
                MacroObject mill = findNearestMacro(farmer, Mill.class);
                if (mill == null) { farmer.setState(FarmerState.WALKING_TO_CHURCH); break; }
                double[] spot = app.interactionMode == InteractionMode.MANUAL
                        ? findSpotOutsideMacro(mill, farmer)
                        : findSpotInsideMacro(mill, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    if (app.interactionMode == InteractionMode.MANUAL) {
                        syncMembershipByTouch(farmer);
                    } else {
                        syncMembershipByTouch(farmer);
                        double coeff = farmer.getTool().getType().speedCoeff();
                        farmer.setWorkTimerEnd(now + workDurationNs(farmer.getMaxLoad(), coeff));
                        farmer.setState(FarmerState.WORKING_AT_MILL);
                    }
                }
            }
            case WORKING_AT_MILL -> {
                if (now >= farmer.getWorkTimerEnd()) {
                    farmer.setMotivation(farmer.getMotivation() - farmer.motivationDropOnWork());
                    resetMembershipAfterWorkCycle(farmer);
                    farmer.setState(FarmerState.WALKING_TO_CHURCH_WITH_MONEY);
                }
            }
            case WALKING_TO_CHURCH_WITH_MONEY -> {
                if (church == null) { farmer.setState(FarmerState.IDLE); break; }
                double[] spot = app.interactionMode == InteractionMode.MANUAL
                        ? findSpotOutsideMacro(church, farmer)
                        : findSpotInsideMacro(church, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    int earnings = farmer.getCurrentLoad() * ((farmer.getMaxLoad() + 1) / 2);
                    app.village.addCoins(earnings);
                    farmer.setCurrentLoad(0);
                    farmer.setMotivation(Math.min(100, farmer.getMotivation() + 30));
                    resetMembershipAfterWorkCycle(farmer);
                    farmer.setState(FarmerState.IDLE);
                }
            }
            case WALKING_TO_CHURCH -> {
                if (church == null) { farmer.setState(FarmerState.IDLE); break; }
                double[] spot = app.interactionMode == InteractionMode.MANUAL
                        ? findSpotOutsideMacro(church, farmer)
                        : findSpotInsideMacro(church, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    if (farmer.getCurrentLoad() > 0) {
                        int earnings = farmer.getCurrentLoad() * ((farmer.getMaxLoad() + 1) / 2);
                        app.village.addCoins(earnings);
                        farmer.setCurrentLoad(0);
                    }
                    if (app.interactionMode == InteractionMode.MANUAL) {
                        syncMembershipByTouch(farmer);
                    } else {
                        syncMembershipByTouch(farmer);
                        farmer.setRestTimerEnd(now + 3_000_000_000L);
                        farmer.setState(FarmerState.RESTING_AT_CHURCH);
                    }
                }
            }
            case RESTING_AT_CHURCH -> {
                if (now >= farmer.getRestTimerEnd()) {
                    farmer.setMotivation(100);
                    resetMembershipAfterWorkCycle(farmer);
                    farmer.setState(FarmerState.IDLE);
                }
            }
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



    private void resetMembershipAfterWorkCycle(Farmer farmer) {
        if (app.interactionMode == InteractionMode.MANUAL) {
            app.village.clearMembership(farmer);
        }
    }

    private boolean moveFarmerTowards(Farmer farmer, double tx, double ty, double dt) {
        double speed = HelloApplication.BASE_SPEED_PPS * farmer.getSpeed() * farmer.getTool().speedCoeff() * app.speedMod;
        double dx = tx - farmer.getX();
        double dy = ty - farmer.getY();
        double dist = Math.hypot(dx, dy);
        if (dist < 4.0) {
            farmer.setX(tx);
            farmer.setY(ty);
            return true;
        }
        double moved = Math.min(speed * dt, dist);
        farmer.setX(farmer.getX() + dx / dist * moved);
        farmer.setY(farmer.getY() + dy / dist * moved);
        clampFarmerInsideWorld(farmer);
        syncMembershipByTouch(farmer);
        return false;
    }

    private long workDurationNs(int maxLoad, double speedCoeff) {
        double seconds = maxLoad * 0.5 / Math.max(0.1, speedCoeff);
        return (long) (seconds * 1_000_000_000L);
    }

    private MacroObject findNearestMacro(Farmer farmer, Class<? extends MacroObject> type) {
        return app.village.getMacroObjects().stream()
                .filter(type::isInstance)
                .min((a, b) -> {
                    double da = Math.hypot(a.getX() - farmer.getX(), a.getY() - farmer.getY());
                    double db = Math.hypot(b.getX() - farmer.getX(), b.getY() - farmer.getY());
                    return Double.compare(da, db);
                }).orElse(null);
    }

    private double[] findSpotInsideMacro(MacroObject mo, Farmer farmer) {
        double baseX = mo.getX() + 20;
        double baseY = mo.getY() + 20;
        int index = app.village.getFarmers().indexOf(farmer);
        int col = index % 3;
        int row = index / 3;
        double x = baseX + col * (Farmer.WIDTH * 0.6);
        double y = baseY + row * (Farmer.HEIGHT * 0.6);
        return new double[]{x, y};
    }

    private double[] findSpotOutsideMacro(MacroObject macro, Farmer farmer) {
        double mx = macro.getX() + macro.getWidth() / 2;
        double my = macro.getY() + macro.getHeight() / 2;
        double radius = macro.getWidth() / 2 + 50;

        int index = app.village.getFarmers().indexOf(farmer);
        int totalFarmers = app.village.getFarmers().size();
        double angle = (2 * Math.PI * index) / Math.max(totalFarmers, 1);

        double px = mx + radius * Math.cos(angle) - Farmer.WIDTH / 2;
        double py = my + radius * Math.sin(angle) - Farmer.HEIGHT / 2;

        px = clampMicroX(px);
        py = clampMicroY(py);
        return new double[]{px, py};
    }

    private Optional<MacroObject> findTouchedMacroByBody(Farmer farmer) {
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

    private boolean intersectsInclusive(double x1, double y1, double w1, double h1,
                                        double x2, double y2, double w2, double h2) {
        return x1 <= x2 + w2 && x1 + w1 >= x2 && y1 <= y2 + h2 && y1 + h1 >= y2;
    }


    private void teleportToFreePositionInMacro(Farmer farmer, MacroObject macro) {
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

    private double clampMicroX(double x) {
        return Math.clamp(x, HelloApplication.MICRO_FRAME_PADDING, app.WORLD_WIDTH - HelloApplication.MICRO_BLOCK_SIZE - HelloApplication.MICRO_FRAME_PADDING);
    }

    private double clampMicroY(double y) {
        return Math.clamp(y, HelloApplication.MICRO_FRAME_PADDING, app.WORLD_HEIGHT - HelloApplication.MICRO_BLOCK_SIZE - HelloApplication.MICRO_FRAME_PADDING);
    }

    private String cloneBaseName(String name) {
        String marker = "_копія";
        int idx = name.lastIndexOf(marker);
        if (idx < 0) return name;
        String suffix = name.substring(idx + marker.length());
        return suffix.matches("\\d+") ? name.substring(0, idx) : name;
    }
}

