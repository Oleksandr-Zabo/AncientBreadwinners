package com.example.ancientbreadwinners;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

class GameLogic {
    final HelloApplication app;
    final GameWorldService world;

    GameLogic(HelloApplication app) {
        this.app = app;
        this.world = new GameWorldService(app);
    }

    void setupKeyHandlers(Scene scene, Stage stage) {
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> { clearSelection(); app.renderer.redraw(); }
                case DELETE -> { deleteSelected(); app.renderer.redraw(); }
                case C -> { if (event.isControlDown()) { cloneSelected(); app.renderer.redraw(); } }
                case S -> { if (event.isControlDown()) app.ui.showSaveDialog(); }
                case O -> { if (event.isControlDown()) app.ui.showLoadDialog(); }
                case INSERT -> app.ui.showCreateDialog();
                case UP -> { moveSelected(event.getCode()); app.renderer.redraw(); event.consume(); }
                case DOWN -> { moveSelected(event.getCode()); app.renderer.redraw(); event.consume(); }
                case LEFT -> { moveSelected(event.getCode()); app.renderer.redraw(); event.consume(); }
                case RIGHT -> { moveSelected(event.getCode()); app.renderer.redraw(); event.consume(); }
                case W -> { moveCamera(0, -HelloApplication.CAMERA_STEP); event.consume(); }
                case A -> { moveCamera(-HelloApplication.CAMERA_STEP, 0); event.consume(); }
                case D -> { moveCamera(HelloApplication.CAMERA_STEP, 0); event.consume(); }
                case Z -> { moveCamera(0, HelloApplication.CAMERA_STEP); event.consume(); }
                case I -> app.ui.showBuyToolDialog(stage);
                case F -> app.ui.showFindDialog(stage);
                case E -> app.ui.showListDialog(findMacroByType(WheatField.class), "Пшеничне Поле");
                case M -> app.ui.showListDialog(findMacroByType(Mill.class), "Млин");
                case J -> app.ui.showListDialog(findMacroByType(Church.class), "Церква");
                case G -> app.ui.showListDialog(null, "Без макрооб'єкта");
                case Q -> { exitSelectedFromMacro(); app.renderer.redraw(); }
                case ENTER -> { enterSelectedToMacro(); app.renderer.redraw(); }
                case V -> app.ui.showCountInfo("Активних хліборобів", activeFarmers().size());
                case K -> { toggleInteractionMode(); app.renderer.redraw(); }
                case T -> app.ui.showCountInfo("Хліборобів без інструменту", (int) app.village.countWithoutTool());
                case R -> app.ui.showSortDialog(stage);
                case U -> upgradeSelectedFarmers();
                case L -> { app.speedMod = 0.5; app.ui.showInfo("Рух сповільнено вдвічі. Натисніть X для відновлення."); }
                case X -> { app.speedMod = 1.0; app.ui.showInfo("Нормальна швидкість."); }
                default -> {}
            }
        });
    }

    void startAnimationTimer() {
        app.animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                world.tick(now);
            }
        };
        app.animationTimer.start();
    }

    void seedData() { world.seedData(); }
    void clearSelection() { world.clearSelection(); }
    void cloneSelected() { world.cloneSelected(); }
    void moveSelected(KeyCode code) { world.moveSelected(code); }
    void deleteSelected() { world.deleteSelected(); }
    void detachSelected() { world.detachSelected(); }
    void enterSelectedToMacro() { world.enterSelectedToMacro(); }
    void exitSelectedFromMacro() { world.exitSelectedFromMacro(); }
    void upgradeSelectedFarmers() { world.upgradeSelectedFarmers(); }
    void moveCamera(double dx, double dy) { world.moveCamera(dx, dy); }
    void clampCamera() { world.clampCamera(); }
    void setupMinimapClick() { world.setupMinimapClick(); }
    void toggleInteractionMode() { world.toggleInteractionMode(); }
    java.util.List<Farmer> activeFarmers() { return world.activeFarmers(); }
    java.util.List<Farmer> sortedByCriteria(java.util.List<Farmer> input) { return world.sortedByCriteria(input); }
    MacroObject findMacroByType(Class<? extends MacroObject> type) { return world.findMacroByType(type); }
    void checkFarmerTalking(java.util.List<Farmer> farmers, long now) { world.checkFarmerTalking(farmers, now); }
    void updateFarmerAI(Farmer farmer, long now, double dt) { world.updateFarmerAI(farmer, now, dt); }
    void syncMembershipByTouch(Farmer farmer) { world.syncMembershipByTouch(farmer); }
    void clampFarmerInsideWorld(Farmer f) { world.clampFarmerInsideWorld(f); }
}

