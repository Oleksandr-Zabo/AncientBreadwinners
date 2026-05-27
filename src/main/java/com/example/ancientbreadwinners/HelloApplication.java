package com.example.ancientbreadwinners;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    public static final double MICRO_IMAGE_SIZE = 75;
    public static final double MICRO_BLOCK_SIZE = 125;
    public static final double MICRO_TOOL_SIZE = 40;
    public static final double MICRO_TOOL_OFFSET_X = 75;
    public static final double MICRO_TOOL_OFFSET_Y = 75;
    public static final double MICRO_FRAME_PADDING = 10;
    public static final double MICRO_FRAME_STROKE_W = 4.0;
    public static final double MOTIVATION_BAR_W = 125;
    public static final double MOTIVATION_BAR_H = 18;
    public static final double MOTIVATION_OFFSET_Y = -10;
    public static final double NAME_OFFSET_Y = 90;
    public static final double MACRO_SIZE = 250;
    public static final double MACRO_IMAGE_SIZE = 150;
    public static final double MACRO_IMAGE_OFFSET = 50;
    public static final double MACRO_TITLE_OFFSET_Y = 225;
    public static final double MACRO_STROKE_W = 4.0;
    public static final double MINIMAP_W = 360;
    public static final double MINIMAP_H = 240;
    public static final double CAMERA_STEP = 60;
    public static final double BASE_SPEED_PPS = 80.0;
    public static final double TALK_DISTANCE = 110.0;
    public static final long TALK_COOLDOWN_NS = 20_000_000_000L;
    public static final long TALK_DURATION_NS = 3_000_000_000L;

    public double WORLD_WIDTH;
    public double WORLD_HEIGHT;
    public double cameraX = 0;
    public double cameraY = 0;
    public double speedMod = 1.0;
    public boolean blinkOn = false;
    public long lastBlinkNano = 0;
    public long lastFrameNano = 0;
    public javafx.animation.AnimationTimer animationTimer;
    public Stage primaryStage;
    public SortCriteria sortCriteria = SortCriteria.BY_NAME;
    public InteractionMode interactionMode = InteractionMode.AUTOMATIC;
    public final Village village = new Village();
    public final Pane worldPane = new Pane();
    public final Canvas minimapCanvas = new Canvas(MINIMAP_W, MINIMAP_H);
    public final Label activeLabel = new Label();
    public final Label statusLabel = new Label();
    public final Label coinsLabel = new Label("Зароблено монет: 0");
    public Farmer selectedFarmer = null;
    public MacroObject selectedMacro = null;
    public GameRenderer renderer;
    public GameUi ui;
    public GameLogic logic;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        WORLD_WIDTH = screen.getWidth() * 2;
        WORLD_HEIGHT = screen.getHeight() * 2;

        renderer = new GameRenderer(this);
        ui = new GameUi(this);
        logic = new GameLogic(this);

        logic.seedData();

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, screen.getWidth(), screen.getHeight());

        worldPane.setPrefSize(screen.getWidth(), screen.getHeight());
        worldPane.setOnMouseClicked(e -> { worldPane.requestFocus(); e.consume(); });

        VBox topBar = ui.createTopBar(stage);
        VBox bottomBar = ui.createBottomBar();

        root.setTop(topBar);
        root.setCenter(worldPane);
        root.setBottom(bottomBar);

        worldPane.prefWidthProperty().bind(root.widthProperty());
        worldPane.prefHeightProperty().bind(root.heightProperty()
                .subtract(topBar.heightProperty())
                .subtract(bottomBar.heightProperty()));

        logic.setupKeyHandlers(scene, stage);
        logic.setupMinimapClick();
        logic.startAnimationTimer();

        stage.setTitle("Ancient Breadwinners");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();

        renderer.redraw();
        worldPane.requestFocus();
    }
}
