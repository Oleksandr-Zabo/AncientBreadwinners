package com.example.ancientbreadwinners;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GameRenderer {
    private final HelloApplication app;
    private final Map<String, Image> imageCache = new HashMap<>();

    GameRenderer(HelloApplication app) {
        this.app = app;
    }

    void redraw() {
        app.worldPane.getChildren().clear();
        setBackground(app.worldPane);

        for (MacroObject mo : app.village.getMacroObjects()) {
            drawMacro(mo);
        }
        for (Farmer farmer : app.village.getFarmers()) {
            drawFarmer(farmer);
        }

        drawMinimap();
        app.worldPane.getChildren().add(app.minimapCanvas);
        app.ui.updateStatus();
    }

    private void drawMacro(MacroObject mo) {
        double sx = mo.getX() - app.cameraX;
        double sy = mo.getY() - app.cameraY;
        double size = mo.getWidth();

        if (sx + size < 0 || sx > currentViewportWidth() || sy + size < 0 || sy > currentViewportHeight()) return;

        double halfStroke = HelloApplication.MACRO_STROKE_W / 2;
        Rectangle rect = new Rectangle(sx + halfStroke, sy + halfStroke, size - HelloApplication.MACRO_STROKE_W, size - HelloApplication.MACRO_STROKE_W);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(macroStroke(mo));
        rect.setStrokeWidth(HelloApplication.MACRO_STROKE_W);

        ImageView img = new ImageView(loadImage(mo.getImageAsset()));
        img.setFitWidth(HelloApplication.MACRO_IMAGE_SIZE);
        img.setFitHeight(HelloApplication.MACRO_IMAGE_SIZE);
        img.setLayoutX(sx + HelloApplication.MACRO_IMAGE_OFFSET);
        img.setLayoutY(sy + HelloApplication.MACRO_IMAGE_OFFSET);

        Text countText = new Text(sx + HelloApplication.MACRO_IMAGE_OFFSET, sy + HelloApplication.MACRO_TITLE_OFFSET_Y,
                "Breadwinners inside: " + mo.getCount());
        countText.setFont(Font.font(14));
        countText.setFill(Color.BLACK);

        Rectangle textBg = new Rectangle(
                sx + HelloApplication.MACRO_IMAGE_OFFSET - 5,
                sy + HelloApplication.MACRO_TITLE_OFFSET_Y - 14,
                countText.getBoundsInLocal().getWidth() + 10,
                18
        );
        textBg.setFill(Color.WHITE);
        textBg.setOpacity(0.9);

        app.worldPane.getChildren().addAll(rect, img, textBg, countText);

        rect.setOnMouseClicked(e -> {
            app.selectedMacro = mo;
            app.selectedFarmer = null;
            redraw();
            e.consume();
        });
    }

    private void drawFarmer(Farmer farmer) {
        app.logic.clampFarmerInsideWorld(farmer);
        double sx = farmer.getX() - app.cameraX;
        double sy = farmer.getY() - app.cameraY;

        if (sx + HelloApplication.MICRO_BLOCK_SIZE < 0 || sx > currentViewportWidth()
                || sy + HelloApplication.MICRO_BLOCK_SIZE < 0 || sy > currentViewportHeight()) return;

        boolean talking = farmer.getState() == FarmerState.TALKING;
        boolean tripleTalking = farmer.getState() == FarmerState.TALKING_TRIPLE;
        boolean showBlink = (talking || tripleTalking) && app.blinkOn;
        Color talkColor = tripleTalking ? Color.LIMEGREEN : Color.GOLD;

        List<MacroObject> memberships = app.village.memberships(farmer);
        Color strokeColor = farmer.isActive()
                ? (showBlink ? talkColor : microStroke(memberships))
                : (showBlink ? talkColor : Color.TRANSPARENT);

        Rectangle motivBar = new Rectangle(0, HelloApplication.MOTIVATION_OFFSET_Y,
                HelloApplication.MOTIVATION_BAR_W * farmer.getMotivation() / 100.0, HelloApplication.MOTIVATION_BAR_H);
        motivBar.setFill(Color.LIMEGREEN);
        motivBar.setStroke(Color.DARKGREEN);
        motivBar.setStrokeWidth(1);

        ImageView img = new ImageView(loadImage(farmer.getImageAsset()));
        img.setFitWidth(HelloApplication.MICRO_IMAGE_SIZE);
        img.setFitHeight(HelloApplication.MICRO_IMAGE_SIZE);

        Text nameText = new Text(0, HelloApplication.NAME_OFFSET_Y, farmer.getName());
        nameText.setFill(Color.BLACK);
        nameText.setFont(Font.font(12));

        double nameWidth = nameText.getBoundsInLocal().getWidth();
        Rectangle nameBg = new Rectangle(-5, HelloApplication.NAME_OFFSET_Y - 12, nameWidth + 10, 16);
        nameBg.setFill(Color.WHITE);
        nameBg.setOpacity(0.9);

        String toolPath = toolAsset(farmer.getTool().getType());
        ImageView toolIcon = toolPath != null ? new ImageView(loadImage(toolPath)) : null;
        if (toolIcon != null) {
            toolIcon.setFitWidth(HelloApplication.MICRO_TOOL_SIZE);
            toolIcon.setFitHeight(HelloApplication.MICRO_TOOL_SIZE);
            toolIcon.setLayoutX(HelloApplication.MICRO_TOOL_OFFSET_X);
            toolIcon.setLayoutY(HelloApplication.MICRO_TOOL_OFFSET_Y);
        }

        Rectangle frame = new Rectangle(
                -HelloApplication.MICRO_FRAME_PADDING, HelloApplication.MOTIVATION_OFFSET_Y - HelloApplication.MICRO_FRAME_PADDING,
                HelloApplication.MICRO_BLOCK_SIZE + HelloApplication.MICRO_FRAME_PADDING * 2,
                HelloApplication.MICRO_BLOCK_SIZE + HelloApplication.MICRO_FRAME_PADDING * 2);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(strokeColor);
        frame.setStrokeWidth((farmer.isActive() || showBlink) ? HelloApplication.MICRO_FRAME_STROKE_W : 0);

        Pane pane = toolIcon != null
                ? new Pane(frame, motivBar, img, nameBg, nameText, toolIcon)
                : new Pane(frame, motivBar, img, nameBg, nameText);
        pane.setPrefSize(HelloApplication.MICRO_BLOCK_SIZE, HelloApplication.MICRO_BLOCK_SIZE);
        pane.setLayoutX(sx);
        pane.setLayoutY(sy);
        pane.setPickOnBounds(true);
        pane.setOnMousePressed(e -> {
            app.worldPane.requestFocus();
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (!e.isControlDown()) {
                    app.logic.clearSelection();
                    farmer.setActive(true);
                } else {
                    farmer.setActive(!farmer.isActive());
                }
                app.selectedFarmer = farmer;
                app.selectedMacro = null;
                redraw();
                e.consume();
            } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                app.selectedFarmer = farmer;
                app.selectedMacro = null;
                app.ui.showEditDialog(farmer);
                redraw();
                e.consume();
            }
        });
        app.worldPane.getChildren().add(pane);
    }

    private void drawMinimap() {
        double vw = currentViewportWidth();
        double vh = currentViewportHeight();

        app.minimapCanvas.setLayoutX(vw - HelloApplication.MINIMAP_W - 10);
        app.minimapCanvas.setLayoutY(vh - HelloApplication.MINIMAP_H - 10);

        GraphicsContext gc = app.minimapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, HelloApplication.MINIMAP_W, HelloApplication.MINIMAP_H);

        gc.setFill(Color.rgb(220, 210, 170, 0.9));
        gc.fillRect(0, 0, HelloApplication.MINIMAP_W, HelloApplication.MINIMAP_H);
        gc.setStroke(Color.SADDLEBROWN);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, HelloApplication.MINIMAP_W, HelloApplication.MINIMAP_H);

        double sx = HelloApplication.MINIMAP_W / app.WORLD_WIDTH;
        double sy = HelloApplication.MINIMAP_H / app.WORLD_HEIGHT;

        for (MacroObject mo : app.village.getMacroObjects()) {
            double mx = mo.getX() * sx;
            double my = mo.getY() * sy;
            double mw = Math.max(6, mo.getWidth() * sx);
            double mh = Math.max(6, mo.getHeight() * sy);
            gc.drawImage(loadImage(mo.getImageAsset()), mx, my, mw, mh);
            if (mo == app.selectedMacro) {
                gc.setStroke(Color.DEEPSKYBLUE);
                gc.setLineWidth(1.5);
                gc.strokeRect(mx, my, mw, mh);
            }
        }

        for (Farmer f : app.village.getFarmers()) {
            double fx = f.getX() * sx;
            double fy = f.getY() * sy;
            double fw = Math.max(4, Farmer.WIDTH * sx);
            double fh = Math.max(4, Farmer.HEIGHT * sy);
            gc.drawImage(loadImage(f.getImageAsset()), fx, fy, fw, fh);
            boolean talking = f.getState() == FarmerState.TALKING;
            boolean tripleTalking = f.getState() == FarmerState.TALKING_TRIPLE;
            if (talking || tripleTalking) {
                Color talkColor = tripleTalking ? Color.LIMEGREEN : Color.GOLD;
                gc.setStroke(talkColor);
                gc.setLineWidth(app.blinkOn ? 2.2 : 1.2);
                gc.strokeRect(fx - 1, fy - 1, fw + 2, fh + 2);
                if (app.blinkOn) {
                    gc.setFill(Color.color(talkColor.getRed(), talkColor.getGreen(), talkColor.getBlue(), 0.35));
                    gc.fillRect(fx - 1, fy - 1, fw + 2, fh + 2);
                }
            }
            if (f.isActive() || f == app.selectedFarmer) {
                gc.setStroke(f == app.selectedFarmer ? Color.GOLD : Color.ORANGE);
                gc.setLineWidth(1.2);
                gc.strokeRect(fx, fy, fw, fh);
            }
        }

        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1.5);
        gc.strokeRect(app.cameraX * sx, app.cameraY * sy, vw * sx, vh * sy);

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(9));
        gc.fillText("Мінікарта (ЛКМ — перейти)", 4, HelloApplication.MINIMAP_H - 4);
    }

    private void setBackground(Pane pane) {
        Image bg = loadImage("/assets/fon.jpg");
        double vw = currentViewportWidth();
        double vh = currentViewportHeight();
        pane.setBackground(new Background(new BackgroundImage(
                bg, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(vw, vh, false, false, false, false))));
    }

    private double currentViewportWidth() {
        return app.worldPane.getWidth() > 0 ? app.worldPane.getWidth() : 1200;
    }

    private double currentViewportHeight() {
        return app.worldPane.getHeight() > 0 ? app.worldPane.getHeight() : 800;
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return emptyImage();
        return imageCache.computeIfAbsent(path, k -> {
            try {
                var s = getClass().getResourceAsStream(k);
                return s == null ? emptyImage() : new Image(s);
            } catch (Exception e) {
                return emptyImage();
            }
        });
    }

    private Image emptyImage() {
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7YkH8AAAAASUVORK5CYII=");
    }

    private String toolAsset(ToolTypes type) {
        return switch (type) {
            case Knife -> "/assets/knife.png";
            case Sickle -> "/assets/sickle.png";
            case Scythe -> "/assets/scythe.png";
            case GoldenScythe -> "/assets/g_scythe.png";
            default -> null;
        };
    }

    private Color macroStroke(MacroObject mo) {
        if (mo instanceof Church) return Color.RED;
        if (mo instanceof Mill) return Color.GRAY;
        if (mo instanceof WheatField) return Color.GOLDENROD;
        return Color.DARKSLATEBLUE;
    }

    private Color microStroke(List<MacroObject> memberships) {
        if (memberships.isEmpty()) return Color.ORANGE;
        return macroStroke(memberships.getFirst());
    }
}

