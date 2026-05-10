package com.example.ancientbreadwinners;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class HelloApplication extends Application {

    private static final double MICRO_IMAGE_SIZE      = 75;
    private static final double MICRO_BLOCK_SIZE      = 125;
    private static final double MICRO_TOOL_SIZE       = 40;
    private static final double MICRO_TOOL_OFFSET_X   = 75;
    private static final double MICRO_TOOL_OFFSET_Y   = 75;
    private static final double MICRO_FRAME_PADDING   = 10;
    private static final double MICRO_FRAME_STROKE_W  = 4.0;
    private static final double MOTIVATION_BAR_W      = 125;
    private static final double MOTIVATION_BAR_H      = 18;
    private static final double MOTIVATION_OFFSET_Y   = -10;
    private static final double NAME_OFFSET_Y         = 90;

    private static final double MACRO_SIZE            = 250;
    private static final double MACRO_IMAGE_SIZE      = 150;
    private static final double MACRO_IMAGE_OFFSET    = 50;
    private static final double MACRO_TITLE_OFFSET_Y  = 225;
    private static final double MACRO_STROKE_W        = 4.0;

    private static final double MINIMAP_W             = 240;
    private static final double MINIMAP_H             = 160;
    private static final double CAMERA_STEP           = 60;

    private static final double BASE_SPEED_PPS        = 80.0;
    private static final double TALK_DISTANCE         = 110.0;
    private static final long   TALK_COOLDOWN_NS      = 20_000_000_000L;
    private static final long   TALK_DURATION_NS      = 3_000_000_000L;

    private double WORLD_WIDTH;
    private double WORLD_HEIGHT;
    private double cameraX = 0;
    private double cameraY = 0;

    private double speedMod     = 1.0;
    private boolean blinkOn     = false;
    private long lastBlinkNano  = 0;
    private long lastFrameNano  = 0;
    private AnimationTimer animationTimer;
    private Stage primaryStage;
    private SortCriteria sortCriteria = SortCriteria.BY_NAME;

    private enum SortCriteria { BY_NAME, BY_LOAD, BY_SPEED }

    private final Village village = new Village();

    private final Pane   worldPane    = new Pane();
    private final Canvas minimapCanvas = new Canvas(MINIMAP_W, MINIMAP_H);
    private final Label  activeLabel  = new Label();
    private final Label  statusLabel  = new Label();
    private final Label  coinsLabel   = new Label("Зароблено монет: 0");

    private final Map<String, Image> imageCache = new HashMap<>();
    private Farmer selectedFarmer = null;
    private MacroObject selectedMacro = null;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        WORLD_WIDTH  = screen.getWidth()  * 2;
        WORLD_HEIGHT = screen.getHeight() * 2;

        seedData();

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, screen.getWidth(), screen.getHeight());

        worldPane.setPrefSize(screen.getWidth(), screen.getHeight());
        worldPane.setFocusTraversable(true);
        setBackground(worldPane);
        worldPane.setOnMouseClicked(e -> { worldPane.requestFocus(); e.consume(); });

        VBox topBar    = createTopBar(stage);
        VBox bottomBar = createBottomBar();

        root.setTop(topBar);
        root.setCenter(worldPane);
        root.setBottom(bottomBar);

        worldPane.prefWidthProperty().bind(root.widthProperty());
        worldPane.prefHeightProperty().bind(root.heightProperty()
                .subtract(topBar.heightProperty())
                .subtract(bottomBar.heightProperty()));

        setupKeyHandlers(scene, stage);
        setupMinimapClick();
        startAnimationTimer();

        stage.setTitle("Ancient Breadwinners");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();

        redraw();
        worldPane.requestFocus();
    }

    private VBox createTopBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(
                buildFileMenu(stage),
                buildEditMenu(),
                buildControlsMenu(),
                buildWindowsMenu(),
                buildAboutMenu());

        activeLabel.setPadding(new Insets(4, 8, 4, 8));
        activeLabel.setFont(Font.font(12));
        activeLabel.setText("Активні мікрооб'єкти: немає");

        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setFont(Font.font(12));
        statusLabel.setText("Останній вибір: немає");

        coinsLabel.setPadding(new Insets(4, 8, 4, 8));
        coinsLabel.setFont(Font.font(12));
        coinsLabel.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox infoRow = new HBox(activeLabel, spacer, coinsLabel, statusLabel);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.setStyle("-fx-background-color: transparent;");

        VBox topBar = new VBox(menuBar, infoRow);
        topBar.setStyle("-fx-background-color: rgba(255,255,255,0.9);");
        topBar.setViewOrder(-1000);
        return topBar;
    }

    private Menu buildFileMenu(Stage stage) {
        Menu file = new Menu("Файл");

        MenuItem create = new MenuItem("Створити хлібороба  [Insert]");
        create.setOnAction(e -> { showCreateDialog(); redraw(); });

        MenuItem save = new MenuItem("Зберегти гру  [Ctrl+S]");
        save.setOnAction(e -> showSaveDialog());

        MenuItem load = new MenuItem("Завантажити гру  [Ctrl+O]");
        load.setOnAction(e -> { showLoadDialog(); redraw(); });

        MenuItem exit = new MenuItem("Вихід");
        exit.setOnAction(e -> stage.close());

        file.getItems().addAll(create, new SeparatorMenuItem(), save, load, new SeparatorMenuItem(), exit);
        return file;
    }

    private Menu buildEditMenu() {
        Menu edit = new Menu("Редагувати");

        MenuItem copy   = new MenuItem("Копіювати виділених  [Ctrl+C]");
        copy.setOnAction(e -> { cloneSelected(); redraw(); });

        MenuItem delete = new MenuItem("Видалити виділених  [Delete]");
        delete.setOnAction(e -> { deleteSelected(); redraw(); });

        MenuItem clear  = new MenuItem("Скасувати виділення  [Esc]");
        clear.setOnAction(e -> { clearSelection(); redraw(); });

        MenuItem detach = new MenuItem("Вилучити з макрооб'єкта");
        detach.setOnAction(e -> { detachSelected(); redraw(); });

        edit.getItems().addAll(copy, delete, clear, new SeparatorMenuItem(), detach);
        return edit;
    }

    private Menu buildControlsMenu() {
        Menu ctrl = new Menu("Керування");
        MenuItem showControls = new MenuItem("Показати керування");
        showControls.setOnAction(e -> showControlsDialog());
        ctrl.getItems().add(showControls);
        return ctrl;
    }

    private void showControlsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Керування");
        alert.setHeaderText("Керування грою");
        alert.initOwner(primaryStage);
        TextArea ta = new TextArea("""
Керування грою

Взаємодія з хліборобами:
  ЛКМ на хліборобі — вибрати/скасувати
  ПКМ на хліборобі — редагувати

Керування виділеними:
  Insert — створити хлібороба
  Delete — видалити виділених
  Esc — скасувати виділення
  Ctrl+C — клонувати виділених
  ← ↑ → ↓ — рухати виділених

Навігація по карті:
  W — прокрутити вліво
  A — прокрутити вправо
  D — вгору
  Z — вниз

Дії з хліборобами:
  U — оновити хлібороба до наступної стадії
  I — купити інструмент
  R — критерій сортування
  F — знайти хлібороба

Взаємодія з макрооб'єктами:
  Enter — увійти в макрооб'єкт (при колізії)
  Q — вийти з макрооб'єкта (для обраних)

Список хліборобів:
  E — список у Пшеничному Полі
  M — список у Млині
  J — список у Церкві

Статистика:
  V — кількість активних
  K — кількість із мотивацією > 50%
  T — кількість без інструменту

Швидкість гри:
  L — сповільнити рух вдвічі
  X — нормальна швидкість

Збереження:
  Ctrl+S — зберегти гру
  Ctrl+O — завантажити гру
""");
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(25);
        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(true);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    private Menu buildWindowsMenu() {
        Menu windows = new Menu("Вікна");

        MenuItem create = new MenuItem("Створення хлібороба  [Insert]");
        create.setOnAction(e -> { showCreateDialog(); redraw(); });

        MenuItem edit = new MenuItem("Редагування хлібороба  [ПКМ]");
        edit.setOnAction(e -> {
            if (selectedFarmer != null) showEditDialog(selectedFarmer);
            else showInfo("Спочатку виберіть хлібороба");
        });

        MenuItem buyTool = new MenuItem("Купити інструмент  [I]");
        buyTool.setOnAction(e -> showBuyToolDialog(primaryStage));

        MenuItem find = new MenuItem("Знайти хлібороба  [F]");
        find.setOnAction(e -> showFindDialog(primaryStage));

        MenuItem listField = new MenuItem("Список у Пшеничному Полі  [E]");
        listField.setOnAction(e -> showListDialog(findMacroByType(WheatField.class), "Пшеничне Поле"));

        MenuItem listMill = new MenuItem("Список у Млині  [M]");
        listMill.setOnAction(e -> showListDialog(findMacroByType(Mill.class), "Млин"));

        MenuItem listChurch = new MenuItem("Список у Церкві  [J]");
        listChurch.setOnAction(e -> showListDialog(findMacroByType(Church.class), "Церква"));

        MenuItem countActive = new MenuItem("Активних хліборобів  [V]");
        countActive.setOnAction(e -> showCountInfo("Активних хліборобів", activeFarmers().size()));

        MenuItem countMotivation = new MenuItem("З мотивацією > 50%  [K]");
        countMotivation.setOnAction(e -> showCountInfo("Хліборобів з мотивацією > 50%", (int) village.countWithHighMotivation(50)));

        MenuItem countNoTool = new MenuItem("Без інструменту  [T]");
        countNoTool.setOnAction(e -> showCountInfo("Хліборобів без інструменту", (int) village.countWithoutTool()));

        MenuItem sort = new MenuItem("Критерій сортування  [R]");
        sort.setOnAction(e -> showSortDialog(primaryStage));

        MenuItem save = new MenuItem("Зберегти гру  [Ctrl+S]");
        save.setOnAction(e -> showSaveDialog());

        MenuItem load = new MenuItem("Завантажити гру  [Ctrl+O]");
        load.setOnAction(e -> { showLoadDialog(); redraw(); });

        windows.getItems().addAll(
            create, edit, new SeparatorMenuItem(),
            buyTool, find, new SeparatorMenuItem(),
            listField, listMill, listChurch, new SeparatorMenuItem(),
            countActive, countMotivation, countNoTool, new SeparatorMenuItem(),
            sort, new SeparatorMenuItem(),
            save, load
        );
        return windows;
    }

    private Menu buildAboutMenu() {
        Menu about = new Menu("Про гру");
        MenuItem aboutItem = new MenuItem("Про гру");
        aboutItem.setOnAction(e -> showAboutDialog());
        about.getItems().add(aboutItem);
        return about;
    }

    private VBox createBottomBar() {
        VBox bottomBar = new VBox();
        bottomBar.setStyle("-fx-background-color: transparent;");
        bottomBar.setPrefHeight(0);
        bottomBar.setMinHeight(0);
        bottomBar.setMaxHeight(0);
        return bottomBar;
    }

    private void setupKeyHandlers(Scene scene, Stage stage) {
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> { clearSelection(); redraw(); }
                case DELETE -> { deleteSelected(); redraw(); }
                case C      -> { if (event.isControlDown()) { cloneSelected(); redraw(); } }
                case S      -> { if (event.isControlDown()) showSaveDialog(); }
                case O      -> { if (event.isControlDown()) { showLoadDialog(); redraw(); } }
                case INSERT -> { showCreateDialog(); redraw(); }
                case UP     -> { moveSelected(event.getCode()); redraw(); event.consume(); }
                case DOWN   -> { moveSelected(event.getCode()); redraw(); event.consume(); }
                case LEFT   -> { moveSelected(event.getCode()); redraw(); event.consume(); }
                case RIGHT  -> { moveSelected(event.getCode()); redraw(); event.consume(); }
                case W      -> { moveCamera(0, -CAMERA_STEP); event.consume(); }
                case A      -> { moveCamera(-CAMERA_STEP, 0); event.consume(); }
                case D      -> { moveCamera(CAMERA_STEP, 0); event.consume(); }
                case Z      -> { moveCamera(0,  CAMERA_STEP); event.consume(); }
                case I      -> showBuyToolDialog(stage);
                case F      -> showFindDialog(stage);
                case E      -> showListDialog(findMacroByType(WheatField.class), "Пшеничне Поле");
                case M      -> showListDialog(findMacroByType(Mill.class), "Млин");
                case J      -> showListDialog(findMacroByType(Church.class), "Церква");
                case Q      -> { exitSelectedFromMacro(); redraw(); }
                case ENTER  -> { enterSelectedToMacro(); redraw(); }
                case V      -> showCountInfo("Активних хліборобів", activeFarmers().size());
                case K      -> showCountInfo("Хліборобів з мотивацією > 50 %",
                                             (int) village.countWithHighMotivation(50));
                case T      -> showCountInfo("Хліборобів без інструменту",
                                             (int) village.countWithoutTool());
                case R      -> showSortDialog(stage);
                case U      -> upgradeSelectedFarmers();
                case L      -> { speedMod = 0.5; showInfo("Рух сповільнено вдвічі. Натисніть X для відновлення."); }
                case X      -> { speedMod = 1.0; showInfo("Нормальна швидкість."); }
                default     -> {}
            }
        });
    }

    private void moveCamera(double dx, double dy) {
        cameraX += dx;
        cameraY += dy;
        clampCamera();
        redraw();
    }

    private void clampCamera() {
        double vw = currentViewportWidth();
        double vh = currentViewportHeight();
        cameraX = Math.max(0, Math.min(cameraX, WORLD_WIDTH  - vw));
        cameraY = Math.max(0, Math.min(cameraY, WORLD_HEIGHT - vh));
    }

    private void setupMinimapClick() {
        minimapCanvas.setOnMousePressed(e -> {
            double sx = MINIMAP_W / WORLD_WIDTH;
            double sy = MINIMAP_H / WORLD_HEIGHT;
            double worldX = e.getX() / sx;
            double worldY = e.getY() / sy;
            cameraX = worldX - currentViewportWidth()  / 2;
            cameraY = worldY - currentViewportHeight() / 2;
            clampCamera();
            redraw();
            e.consume();
        });
    }

    private void startAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = lastFrameNano == 0 ? 0.016 : (now - lastFrameNano) / 1_000_000_000.0;
                dt = Math.min(dt, 0.1);
                lastFrameNano = now;

                if (now - lastBlinkNano > 500_000_000L) {
                    blinkOn = !blinkOn;
                    lastBlinkNano = now;
                }

                List<Farmer> snapshot = new ArrayList<>(village.getFarmers());
                checkFarmerTalking(snapshot, now);
                for (Farmer f : snapshot) updateFarmerAI(f, now, dt);

                redraw();
            }
        };
        animationTimer.start();
    }

    private void checkFarmerTalking(List<Farmer> farmers, long now) {
        for (int i = 0; i < farmers.size(); i++) {
            Farmer a = farmers.get(i);
            if (a.getState() == FarmerState.TALKING || a.getState() == FarmerState.RESTING_AT_CHURCH || a.getState() == FarmerState.WALKING_TO_CHURCH_WITH_MONEY) continue;
            for (int j = i + 1; j < farmers.size(); j++) {
                Farmer b = farmers.get(j);
                if (b.getState() == FarmerState.TALKING || b.getState() == FarmerState.RESTING_AT_CHURCH || b.getState() == FarmerState.WALKING_TO_CHURCH_WITH_MONEY) continue;
                double dist = Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
                if (dist < TALK_DISTANCE) {
                    if (a.getMotivation() > 75 || b.getMotivation() > 75) continue;
                    boolean aReady = (now - a.getLastSpokeNano()) > TALK_COOLDOWN_NS;
                    boolean bReady = (now - b.getLastSpokeNano()) > TALK_COOLDOWN_NS;
                    if (aReady && bReady) {
                        a.speak(b);
                        long end = now + TALK_DURATION_NS;
                        a.setStateBeforeTalk(a.getState());
                        b.setStateBeforeTalk(b.getState());
                        a.setTalkTimerEnd(end);
                        b.setTalkTimerEnd(end);
                        a.setLastSpokeNano(now);
                        b.setLastSpokeNano(now);
                        a.setState(FarmerState.TALKING);
                        b.setState(FarmerState.TALKING);
                    }
                }
            }
        }
    }

    private void updateFarmerAI(Farmer farmer, long now, double dt) {
        if (farmer.isActive()) return;

        FarmerState state = farmer.getState();

        if (state == FarmerState.TALKING) {
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
                double[] spot = findSpotInsideMacro(field, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    syncMembershipByTouch(farmer);
                    double coeff = farmer.getTool().getType().speedCoeff();
                    farmer.setWorkTimerEnd(now + workDurationNs(farmer.getMaxLoad(), coeff));
                    farmer.setState(FarmerState.WORKING_AT_FIELD);
                }
            }
            case WORKING_AT_FIELD -> {
                if (now >= farmer.getWorkTimerEnd()) {
                    farmer.setCurrentLoad(farmer.getMaxLoad());
                    farmer.setMotivation(farmer.getMotivation() - farmer.motivationDropOnWork());
                    MacroObject mill = findNearestMacro(farmer, Mill.class);
                    farmer.setState(mill != null ? FarmerState.WALKING_TO_MILL : FarmerState.WALKING_TO_CHURCH);
                }
            }
            case WALKING_TO_MILL -> {
                MacroObject mill = findNearestMacro(farmer, Mill.class);
                if (mill == null) { farmer.setState(FarmerState.WALKING_TO_CHURCH); break; }
                double[] spot = findSpotInsideMacro(mill, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    syncMembershipByTouch(farmer);
                    double coeff = farmer.getTool().getType().speedCoeff();
                    farmer.setWorkTimerEnd(now + workDurationNs(farmer.getMaxLoad(), coeff));
                    farmer.setState(FarmerState.WORKING_AT_MILL);
                }
            }
            case WORKING_AT_MILL -> {
                if (now >= farmer.getWorkTimerEnd()) {
                    farmer.setMotivation(farmer.getMotivation() - farmer.motivationDropOnWork());
                    farmer.setState(FarmerState.WALKING_TO_CHURCH_WITH_MONEY);
                }
            }
            case WALKING_TO_CHURCH_WITH_MONEY -> {
                if (church == null) { farmer.setState(FarmerState.IDLE); break; }
                double[] spot = findSpotInsideMacro(church, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    int earnings = farmer.getCurrentLoad() * ((farmer.getMaxLoad() + 1) / 2);
                    village.addCoins(earnings);
                    farmer.setCurrentLoad(0);
                    farmer.setMotivation(Math.min(100, farmer.getMotivation() + 30));
                    farmer.setState(FarmerState.IDLE);
                }
            }
            case WALKING_TO_CHURCH -> {
                if (church == null) { farmer.setState(FarmerState.IDLE); break; }
                double[] spot = findSpotInsideMacro(church, farmer);
                boolean arrived = moveFarmerTowards(farmer, spot[0], spot[1], dt);
                if (arrived) {
                    if (farmer.getCurrentLoad() > 0) {
                        int earnings = farmer.getCurrentLoad() * ((farmer.getMaxLoad() + 1) / 2);
                        village.addCoins(earnings);
                        farmer.setCurrentLoad(0);
                    }
                    syncMembershipByTouch(farmer);
                    farmer.setRestTimerEnd(now + 3_000_000_000L);
                    farmer.setState(FarmerState.RESTING_AT_CHURCH);
                }
            }
            case RESTING_AT_CHURCH -> {
                if (now >= farmer.getRestTimerEnd()) {
                    farmer.setMotivation(100);
                    farmer.setState(FarmerState.IDLE);
                }
            }
        }
    }

    private boolean moveFarmerTowards(Farmer farmer, double tx, double ty, double dt) {
        double speed = BASE_SPEED_PPS * farmer.getSpeed() * farmer.getTool().speedCoeff() * speedMod;
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

    private MacroObject findMacroByType(Class<? extends MacroObject> type) {
        return village.getMacroObjects().stream()
                .filter(type::isInstance)
                .findFirst().orElse(null);
    }

    private MacroObject findNearestMacro(Farmer farmer, Class<? extends MacroObject> type) {
        return village.getMacroObjects().stream()
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
        int index = village.getFarmers().indexOf(farmer);
        int col = index % 3;
        int row = index / 3;
        double x = baseX + col * (Farmer.WIDTH * 0.6);
        double y = baseY + row * (Farmer.HEIGHT * 0.6);
        return new double[]{x, y};
    }

    private void redraw() {
        worldPane.getChildren().clear();
        setBackground(worldPane);

        for (MacroObject mo : village.getMacroObjects()) drawMacro(mo);
        for (Farmer f : village.getFarmers()) drawFarmer(f);

        drawMinimap();
        worldPane.getChildren().add(minimapCanvas);

        updateStatus();
    }

    private void drawMacro(MacroObject mo) {
        double sx = mo.getX() - cameraX;
        double sy = mo.getY() - cameraY;
        double size = mo.getWidth();

        if (sx + size < 0 || sx > currentViewportWidth() || sy + size < 0 || sy > currentViewportHeight()) return;

        double halfStroke = MACRO_STROKE_W / 2;
        Rectangle rect = new Rectangle(sx + halfStroke, sy + halfStroke, size - MACRO_STROKE_W, size - MACRO_STROKE_W);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(macroStroke(mo));
        rect.setStrokeWidth(MACRO_STROKE_W);

        ImageView img = new ImageView(loadImage(mo.getImageAsset()));
        img.setFitWidth(MACRO_IMAGE_SIZE);
        img.setFitHeight(MACRO_IMAGE_SIZE);
        img.setLayoutX(sx + MACRO_IMAGE_OFFSET);
        img.setLayoutY(sy + MACRO_IMAGE_OFFSET);

        Text countText = new Text(sx + MACRO_IMAGE_OFFSET, sy + MACRO_TITLE_OFFSET_Y,
                "Breadwinners inside: " + mo.getCount());
        countText.setFont(Font.font(14));
        countText.setFill(Color.BLACK);

        Rectangle textBg = new Rectangle(
            sx + MACRO_IMAGE_OFFSET - 5,
            sy + MACRO_TITLE_OFFSET_Y - 14,
            countText.getBoundsInLocal().getWidth() + 10,
            18
        );
        textBg.setFill(Color.WHITE);
        textBg.setOpacity(0.9);

        worldPane.getChildren().addAll(rect, img, textBg, countText);

        rect.setOnMouseClicked(e -> {
            selectedMacro = mo;
            selectedFarmer = null;
            redraw();
            e.consume();
        });
    }

    private void drawFarmer(Farmer farmer) {
        clampFarmerInsideWorld(farmer);
        double sx = farmer.getX() - cameraX;
        double sy = farmer.getY() - cameraY;

        if (sx + MICRO_BLOCK_SIZE < 0 || sx > currentViewportWidth()
                || sy + MICRO_BLOCK_SIZE < 0 || sy > currentViewportHeight()) return;

        boolean talking = farmer.getState() == FarmerState.TALKING;
        boolean showBlink = talking && blinkOn;

        List<MacroObject> memberships = village.memberships(farmer);
        Color strokeColor = farmer.isActive()
                ? microStroke(memberships)
                : (showBlink ? Color.GOLD : Color.TRANSPARENT);

        Rectangle motivBar = new Rectangle(0, MOTIVATION_OFFSET_Y,
                MOTIVATION_BAR_W * farmer.getMotivation() / 100.0, MOTIVATION_BAR_H);
        motivBar.setFill(Color.LIMEGREEN);
        motivBar.setStroke(Color.DARKGREEN);
        motivBar.setStrokeWidth(1);

        ImageView img = new ImageView(loadImage(farmer.getImageAsset()));
        img.setFitWidth(MICRO_IMAGE_SIZE);
        img.setFitHeight(MICRO_IMAGE_SIZE);

        Text nameText = new Text(0, NAME_OFFSET_Y, farmer.getName());
        nameText.setFill(Color.BLACK);
        nameText.setFont(Font.font(12));

        double nameWidth = nameText.getBoundsInLocal().getWidth();
        Rectangle nameBg = new Rectangle(-5, NAME_OFFSET_Y - 12, nameWidth + 10, 16);
        nameBg.setFill(Color.WHITE);
        nameBg.setOpacity(0.9);

        String toolPath = toolAsset(farmer.getTool().getType());
        ImageView toolIcon = toolPath != null ? new ImageView(loadImage(toolPath)) : null;
        if (toolIcon != null) {
            toolIcon.setFitWidth(MICRO_TOOL_SIZE);
            toolIcon.setFitHeight(MICRO_TOOL_SIZE);
            toolIcon.setLayoutX(MICRO_TOOL_OFFSET_X);
            toolIcon.setLayoutY(MICRO_TOOL_OFFSET_Y);
        }

        Rectangle frame = new Rectangle(
                -MICRO_FRAME_PADDING, MOTIVATION_OFFSET_Y - MICRO_FRAME_PADDING,
                MICRO_BLOCK_SIZE + MICRO_FRAME_PADDING * 2,
                MICRO_BLOCK_SIZE + MICRO_FRAME_PADDING * 2);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(strokeColor);
        frame.setStrokeWidth((farmer.isActive() || showBlink) ? MICRO_FRAME_STROKE_W : 0);

        Pane pane = toolIcon != null
                ? new Pane(frame, motivBar, img, nameBg, nameText, toolIcon)
                : new Pane(frame, motivBar, img, nameBg, nameText);
        pane.setPrefSize(MICRO_BLOCK_SIZE, MICRO_BLOCK_SIZE);
        pane.setLayoutX(sx);
        pane.setLayoutY(sy);
        pane.setPickOnBounds(true);
        pane.setOnMousePressed(e -> {
            worldPane.requestFocus();
            if (e.getButton() == MouseButton.PRIMARY) {
                farmer.setActive(!farmer.isActive());
                selectedFarmer = farmer;
                selectedMacro = null;
                redraw();
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                selectedFarmer = farmer;
                selectedMacro = null;
                showEditDialog(farmer);
                redraw();
                e.consume();
            }
        });
        worldPane.getChildren().add(pane);
    }

    private void drawMinimap() {
        double vw = currentViewportWidth();
        double vh = currentViewportHeight();

        minimapCanvas.setLayoutX(vw - MINIMAP_W - 10);
        minimapCanvas.setLayoutY(vh - MINIMAP_H - 10);

        GraphicsContext gc = minimapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, MINIMAP_W, MINIMAP_H);

        gc.setFill(Color.rgb(220, 210, 170, 0.9));
        gc.fillRect(0, 0, MINIMAP_W, MINIMAP_H);
        gc.setStroke(Color.SADDLEBROWN);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, MINIMAP_W, MINIMAP_H);

        double sx = MINIMAP_W / WORLD_WIDTH;
        double sy = MINIMAP_H / WORLD_HEIGHT;

        for (MacroObject mo : village.getMacroObjects()) {
            Color c = macroStroke(mo);
            gc.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.6));
            gc.fillRect(mo.getX() * sx, mo.getY() * sy, mo.getWidth() * sx, mo.getHeight() * sy);
        }

        for (Farmer f : village.getFarmers()) {
            gc.setFill(f.isActive() ? Color.YELLOW : Color.DARKGREEN);
            double fx = f.getX() * sx;
            double fy = f.getY() * sy;
            gc.fillOval(fx - 3, fy - 3, 6, 6);
        }

        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1.5);
        gc.strokeRect(cameraX * sx, cameraY * sy, vw * sx, vh * sy);

        gc.setFill(Color.BLACK);
        gc.setFont(javafx.scene.text.Font.font(9));
        gc.fillText("Мінікарта (ЛКМ — перейти)", 4, MINIMAP_H - 4);
    }

    private void updateStatus() {
        List<Farmer> active = activeFarmers();
        if (!active.isEmpty()) {
            String names = active.stream().limit(3).map(Farmer::getName).collect(Collectors.joining(", "));
            if (active.size() > 3) names += ", …";
            activeLabel.setText("Активні: " + active.size() + " → " + names);
        } else {
            activeLabel.setText("Активні мікрооб'єкти: немає");
        }

        coinsLabel.setText("Зароблено монет: " + village.getTotalCoins());

        if (selectedFarmer != null) {
            List<MacroObject> ms = village.memberships(selectedFarmer);
            String macro = ms.isEmpty() ? "жодному" : ms.getFirst().getName();
            String kindLabel;
            if (selectedFarmer instanceof MasterFarmer) kindLabel = "Майстер-Хлібороб";
            else if (selectedFarmer instanceof FreePeasant) kindLabel = "Вільний Селянин";
            else if (selectedFarmer instanceof Gardener) kindLabel = "Городник";
            else kindLabel = selectedFarmer.getKind();
            statusLabel.setText(selectedFarmer.getName() + " [" + kindLabel + "] | мотивація: "
                    + selectedFarmer.getMotivation() + " | " + macro);
        } else if (selectedMacro != null) {
            statusLabel.setText("Вибрано макрооб'єкт: " + selectedMacro.getName());
        } else {
            statusLabel.setText("Останній вибір: немає");
        }
    }

    private void showCreateDialog() {
        Dialog<Farmer> dialog = new Dialog<>();
        dialog.setTitle("Створення хлібороба");
        dialog.initOwner(primaryStage);

        ButtonType ok     = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        TextField nameField = new TextField("Іван");

        RadioButton rbGardener = new RadioButton("Городник (Gardener)");
        RadioButton rbPeasant  = new RadioButton("Вільний Селянин (FreePeasant)");
        RadioButton rbMaster   = new RadioButton("Майстер-Хлібороб (MasterFarmer)");
        ToggleGroup typeGroup  = new ToggleGroup();
        rbGardener.setToggleGroup(typeGroup);
        rbPeasant.setToggleGroup(typeGroup);
        rbMaster.setToggleGroup(typeGroup);
        rbGardener.setSelected(true);

        CheckBox activeCheck = new CheckBox("Активний після створення");
        activeCheck.setSelected(false);

        ListView<MotivationLevel> motivList = new ListView<>();
        motivList.getItems().setAll(
                new MotivationLevel("Низький (40)", 40),
                new MotivationLevel("Середній (70)", 70),
                new MotivationLevel("Високий (90)", 90));
        motivList.getSelectionModel().select(1);
        motivList.setPrefHeight(80);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Ім'я:"), nameField);
        grid.addRow(1, new Label("Тип:"), new VBox(4, rbGardener, rbPeasant, rbMaster));
        grid.addRow(2, new Label("Стан:"), activeCheck);
        grid.addRow(3, new Label("Мотивація:"), motivList);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != ok) return null;
            String name = nameField.getText().trim().isEmpty() ? "Безіменний" : nameField.getText().trim();
            MotivationLevel ml = motivList.getSelectionModel().getSelectedItem();
            int motiv = ml == null ? 70 : ml.value();
            Farmer f;
            if (rbMaster.isSelected())  f = new MasterFarmer(name, motiv, 1.8, 20, new Tool(ToolTypes.NoTool, 1.0f), 0, 0);
            else if (rbPeasant.isSelected()) f = new FreePeasant(name, motiv, 1.3, 10, new Tool(ToolTypes.NoTool, 1.0f), 0, 0);
            else                        f = new Gardener(name, motiv, 1.1, 12, new Tool(ToolTypes.NoTool, 1.0f), 0, 0);
            f.setActive(activeCheck.isSelected());
            return f;
        });

        Optional<Farmer> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        Farmer farmer = result.get();
        Optional<Village.Placement> pos = village.findFreeAdjacentPosition(
                village.getFarmers().isEmpty() ? farmer : village.getFarmers().getFirst(),
                WORLD_WIDTH, WORLD_HEIGHT);
        if (pos.isPresent()) {
            farmer.setX(pos.get().x());
            farmer.setY(pos.get().y());
        } else {
            farmer.setX(cameraX + 100);
            farmer.setY(cameraY + 200);
        }
        clampFarmerInsideWorld(farmer);
        village.addFarmer(farmer);
        syncMembershipByTouch(farmer);
        selectedFarmer = farmer;
    }

    private void showEditDialog(Farmer farmer) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Редагування: " + farmer.getName());
        dialog.initOwner(primaryStage);

        ButtonType ok     = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        TextField nameField  = new TextField(farmer.getName());
        CheckBox  activeCheck = new CheckBox("Активний");
        activeCheck.setSelected(farmer.isActive());

        ListView<MotivationLevel> motivList = new ListView<>();
        motivList.getItems().setAll(
                new MotivationLevel("Низький (40)", 40),
                new MotivationLevel("Середній (70)", 70),
                new MotivationLevel("Високий (90)", 90));
        motivList.setPrefHeight(80);
        selectClosestMotivation(motivList, farmer.getMotivation());

        Label kindLabel = new Label(farmer.getKind());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Ім'я:"),      nameField);
        grid.addRow(1, new Label("Тип:"),       kindLabel);
        grid.addRow(2, new Label("Стан:"),      activeCheck);
        grid.addRow(3, new Label("Мотивація:"), motivList);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> bt == ok);
        Optional<Boolean> res = dialog.showAndWait();
        if (res.isEmpty() || !res.get()) return;

        String newName = nameField.getText().trim();
        if (!newName.isEmpty()) farmer.setName(newName);
        farmer.setActive(activeCheck.isSelected());
        MotivationLevel sel = motivList.getSelectionModel().getSelectedItem();
        if (sel != null) farmer.setMotivation(sel.value());
    }

    private void showBuyToolDialog(Stage owner) {
        if (selectedFarmer == null) { showInfo("Спочатку виберіть хлібороба (ЛКМ)."); return; }
        Farmer farmer = selectedFarmer;

        int currentPrice = farmer.getTool().getType().price();
        Set<ToolTypes> buyable = new HashSet<>(farmer.allowedTools());
        buyable.remove(ToolTypes.NoTool);

        buyable.removeIf(t -> t.price() <= currentPrice);

        if (buyable.isEmpty()) {
            showInfo(farmer.getName() + " вже має найкращий доступний інструмент або немає дорожчих моделей.");
            return;
        }

        Dialog<ToolTypes> dialog = new Dialog<>();
        dialog.setTitle("Купити інструмент для " + farmer.getName());
        dialog.initOwner(owner);

        ButtonType ok     = new ButtonType("Купити", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        ComboBox<ToolTypes> toolCombo = new ComboBox<>();
        toolCombo.getItems().addAll(buyable);
        toolCombo.getSelectionModel().selectFirst();
        toolCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ToolTypes t) { return t == null ? "" : t.displayName(); }
            @Override public ToolTypes fromString(String s) { return null; }
        });

        Label balanceLabel = new Label("Баланс: " + village.getTotalCoins() + " монет");
        Label currentLabel = new Label("Поточний: " + farmer.getTool().getType().displayName() + " (" + currentPrice + " монет)");
        Label priceLabel   = new Label();
        toolCombo.setOnAction(e -> {
            ToolTypes t = toolCombo.getSelectionModel().getSelectedItem();
            if (t != null) priceLabel.setText("Ціна: " + t.price() + " монет");
        });
        toolCombo.getSelectionModel().selectFirst();
        ToolTypes first = toolCombo.getSelectionModel().getSelectedItem();
        if (first != null) priceLabel.setText("Ціна: " + first.price() + " монет");

        VBox vbox = new VBox(8,
                currentLabel,
                balanceLabel,
                new Label("Доступні дорожчі моделі:"),
                toolCombo,
                priceLabel);
        vbox.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(vbox);
        dialog.setResultConverter(bt -> bt == ok ? toolCombo.getSelectionModel().getSelectedItem() : null);

        Optional<ToolTypes> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == null) return;

        ToolTypes chosen = result.get();
        int price = chosen.price();
        if (village.getTotalCoins() < price) {
            showInfo("Недостатньо монет! Потрібно: " + price + ", є: " + village.getTotalCoins());
            return;
        }
        village.addCoins(-price);
        farmer.setToolType(chosen);
        showInfo(farmer.getName() + " придбав " + chosen.displayName() + ". Залишок: " + village.getTotalCoins() + " монет.");
    }

    private void showFindDialog(Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Знайти хлібороба");
        dialog.initOwner(owner);

        ButtonType find   = new ButtonType("Знайти", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Закрити", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(find, cancel);

        TextField nameField = new TextField();
        nameField.setPromptText("Ім'я (або порожньо — всі)");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Усі", "Городник", "Вільний Селянин", "Майстер-Хлібороб");
        typeCombo.getSelectionModel().select(0);

        CheckBox loadFilter = new CheckBox("Фільтр за навантаженням ≥");
        Spinner<Integer> loadSpinner = new Spinner<>(1, 50, 10);
        loadSpinner.setEditable(true);
        loadFilter.setSelected(false);
        loadSpinner.setDisable(true);
        loadFilter.setOnAction(e -> loadSpinner.setDisable(!loadFilter.isSelected()));

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefRowCount(8);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Ім'я:"),         nameField);
        grid.addRow(1, new Label("Тип:"),           typeCombo);
        grid.addRow(2, loadFilter,                  loadSpinner);
        grid.add(new Label("Результати:"), 0, 3);
        grid.add(resultArea, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);

        Button findBtn = (Button) dialog.getDialogPane().lookupButton(find);
        findBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            String nameFilt  = nameField.getText().trim().toLowerCase();
            String typeFilt  = typeCombo.getSelectionModel().getSelectedItem();
            int    minLoad   = loadFilter.isSelected() ? loadSpinner.getValue() : 0;

            List<Farmer> found = village.getFarmers().stream()
                    .filter(f -> nameFilt.isEmpty() || f.getName().toLowerCase().contains(nameFilt))
                    .filter(f -> {
                        if ("Городник".equals(typeFilt))           return f instanceof Gardener;
                        if ("Вільний Селянин".equals(typeFilt))     return f instanceof FreePeasant;
                        if ("Майстер-Хлібороб".equals(typeFilt))   return f instanceof MasterFarmer;
                        return true;
                    })
                    .filter(f -> !loadFilter.isSelected() || f.getMaxLoad() >= minLoad)
                    .collect(Collectors.toList());

            if (found.isEmpty()) {
                resultArea.setText("Нічого не знайдено.");
                return;
            }
            List<Farmer> sorted = sortedByCriteria(found);
            StringBuilder sb = new StringBuilder();
            String criteria = switch (sortCriteria) {
                case BY_LOAD  -> "навантаження";
                case BY_SPEED -> "швидкість";
                default       -> "ім'я";
            };
            sb.append("Знайдено: ").append(sorted.size()).append(" (сортування: ").append(criteria).append(")\n\n");
            for (Farmer f : sorted) {
                List<MacroObject> ms = village.memberships(f);
                String macro = ms.isEmpty() ? "вільний" : ms.stream().map(MacroObject::getName).collect(Collectors.joining(", "));
                sb.append(String.format("%-14s [%s]  x=%.0f y=%.0f  макро: %s%n",
                        f.getName(), f.getKind(), f.getX(), f.getY(), macro));
            }
            resultArea.setText(sb.toString());
        });

        dialog.showAndWait();
    }

    private void showListDialog(MacroObject macro, String title) {
        List<Farmer> raw;
        if (macro == null) {
            raw = village.getFarmers().stream()
                    .filter(f -> village.memberships(f).isEmpty())
                    .collect(Collectors.toList());
        } else {
            raw = new ArrayList<>(macro.getMembers());
        }
        List<Farmer> sorted = sortedByCriteria(raw);

        StringBuilder sb = new StringBuilder();
        if (sorted.isEmpty()) {
            sb.append("Список порожній.");
        } else {
            for (Farmer f : sorted) {
                sb.append(String.format("%-14s [%s]  мотив: %d  інструмент: %s%n",
                        f.getName(), f.getKind(), f.getMotivation(),
                        f.getTool().getType().displayName()));
            }
        }

        String criteria = switch (sortCriteria) {
            case BY_LOAD  -> "навантаження";
            case BY_SPEED -> "швидкість";
            default       -> "ім'я";
        };

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Список: " + title);
        alert.setHeaderText(title + "  (сортування: " + criteria + ")  — " + sorted.size() + " хліборобів");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefRowCount(10);
        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(true);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    private void showCountInfo(String label, int count) {
        showInfo(label + ": " + count);
    }

    private void showSortDialog(Stage owner) {
        Dialog<SortCriteria> dialog = new Dialog<>();
        dialog.setTitle("Критерій сортування");
        dialog.initOwner(owner);

        ButtonType ok     = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        RadioButton byName  = new RadioButton("За ім'ям");
        RadioButton byLoad  = new RadioButton("За навантаженням (maxLoad)");
        RadioButton bySpeed = new RadioButton("За швидкістю (speed)");
        ToggleGroup group   = new ToggleGroup();
        byName.setToggleGroup(group);
        byLoad.setToggleGroup(group);
        bySpeed.setToggleGroup(group);
        switch (sortCriteria) {
            case BY_LOAD  -> byLoad.setSelected(true);
            case BY_SPEED -> bySpeed.setSelected(true);
            default       -> byName.setSelected(true);
        }

        VBox box = new VBox(8, byName, byLoad, bySpeed);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(bt -> {
            if (bt != ok) return sortCriteria;
            if (byLoad.isSelected())  return SortCriteria.BY_LOAD;
            if (bySpeed.isSelected()) return SortCriteria.BY_SPEED;
            return SortCriteria.BY_NAME;
        });

        Optional<SortCriteria> result = dialog.showAndWait();
        result.ifPresent(sc -> {
            sortCriteria = sc;
            switch (sc) {
                case BY_LOAD  -> village.sortByMaxLoadAnonymous();
                case BY_SPEED -> village.sortBySpeed();
                default       -> village.sortByName();
            }
            String criteriaName = sc == SortCriteria.BY_LOAD ? "навантаженням"
                    : sc == SortCriteria.BY_SPEED ? "швидкістю" : "ім'ям";
            showSortedFarmersList(criteriaName);
        });
    }

    private void showSortedFarmersList(String criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append("Відсортовано за: ").append(criteria).append("\n\n");
        if (village.getFarmers().isEmpty()) {
            sb.append("Список порожній.");
        } else {
            for (Farmer f : village.getFarmers()) {
                sb.append(String.format("%-14s [%s]  мотив: %d%%  maxLoad: %d  швидкість: %.1f  інструмент: %s%n",
                        f.getName(), f.getKind(), f.getMotivation(), f.getMaxLoad(),
                        f.getSpeed(), f.getTool().getType().displayName()));
            }
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Список хліборобів");
        alert.setHeaderText("Всі хлібороби (" + village.getFarmers().size() + ")");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefRowCount(15);
        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(true);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    private void showSaveDialog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Зберегти гру");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстові файли (*.txt)", "*.txt"));
        File file = fc.showSaveDialog(primaryStage);
        if (file == null) return;
        try {
            GameSave save = new GameSave(village, cameraX, cameraY);
            save.saveToFile(file);
            showInfo("Збережено: " + file.getName());
        } catch (IOException ex) {
            showError("Помилка збереження: " + ex.getMessage());
        }
    }

    private void showLoadDialog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Завантажити гру");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстові файли (*.txt)", "*.txt"));
        File file = fc.showOpenDialog(primaryStage);
        if (file == null) return;
        try {
            GameSave save = GameSave.loadFromFile(file);
            village.clearAll();
            village.loadFrom(save.village);
            cameraX = save.cameraX;
            cameraY = save.cameraY;
            clampCamera();
            selectedFarmer = null;
            selectedMacro = null;
            showInfo("Завантажено: " + file.getName());
        } catch (IOException ex) {
            showError("Помилка завантаження: " + ex.getMessage());
        }
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Про гру");
        alert.setHeaderText("Ancient Breadwinners");
        alert.initOwner(primaryStage);
        TextArea ta = new TextArea("""
Про гру
Гра є симуляцією життя українських хліборобів...

Городник (Gardener) — це початок шляху. У традиційному середовищі кожен
селянин мав свій інструмент (Tool), який визначав рівень його майстерності та
можливостей, проте городник міг мати лише жнивний ніж.

Існують Вільні Селяни (Free Peasant) — вони вже виходять за межі
власного городу й працюють на полі. Для цього рівня вже доступні кілька
інструментів — серп та коса, які дозволяють працювати швидше й ефективніше,
ніж простий ніж городника.

Існують Майстри-Хлібороби (Master Farmer) — вершина розвитку. На
цьому рівні майстер може користуватися всіма інструментами, доступними
попереднім селянам, а також має власні нові — золоту косу.

У селі важливим осередком духовності та взаємодії була Дерев'яна
Церква (Wooden Church). Тут хлібороби відновлюють мотивацію до 100.

Робота кожного селянина починалася з особливого місця — Пшеничного
поля (Wheat Field). Тут збирають врожай.

Особливу увагу варто надати Млину (Mill), адже він був завершальним
етапом у селянському господарстві, де зерно з пшеничного поля
перетворювалося на борошно — основу для хліба. Тут заробляють монети.

І вся ця селянська праця відбувається в універсальному об'єкті (ігровому
світі) — Селі (Village). Навігація по ігровому світі полегшується завдяки
Мінікарті (MiniMap), яка показує основні об'єкти та допомагає швидко
орієнтуватися.

Розмір ігрового світу: вдвічі більший за екран по кожній осі (площа
видимої ділянки — 25 % від загальної).
""");
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(20);
        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(true);
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    private void clearSelection() {
        for (Farmer f : village.getFarmers()) f.setActive(false);
        selectedFarmer = null;
        selectedMacro  = null;
    }

    private void cloneSelected() {
        for (Farmer source : activeFarmers()) {
            Farmer clone = source.clone();
            clone.setName(nextCloneName(source.getName()));
            double[] pos = findClonePosition(source);
            clone.setX(pos[0]);
            clone.setY(pos[1]);
            clampFarmerInsideWorld(clone);
            clone.setActive(false);
            village.addFarmer(clone);
            syncMembershipByTouch(clone);
        }
    }

    private double[] findClonePosition(Farmer source) {
        int index = village.getFarmers().size();
        double offsetX = (index % 3) * 40;
        double offsetY = (index / 3) * 40;
        return new double[]{source.getX() + offsetX, source.getY() + offsetY};
    }

    private void moveSelected(KeyCode code) {
        for (Farmer f : activeFarmers()) {
            List<MacroObject> memberships = village.memberships(f);
            if (!memberships.isEmpty()) {
                ejectFromMacro(f, memberships.getFirst());
            }

            double step = 10;
            double nx = f.getX() + (code == KeyCode.LEFT ? -step : code == KeyCode.RIGHT ? step : 0);
            double ny = f.getY() + (code == KeyCode.UP   ? -step : code == KeyCode.DOWN  ? step : 0);
            f.setX(clampMicroX(nx));
            f.setY(clampMicroY(ny));
        }
    }

    private void ejectFromMacro(Farmer farmer, MacroObject macro) {
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

        village.clearMembership(farmer);
        clampFarmerInsideWorld(farmer);
    }

    private void deleteSelected() {
        new ArrayList<>(activeFarmers()).forEach(village::removeFarmer);
        selectedFarmer = null;
    }

    private void detachSelected() {
        activeFarmers().forEach(village::clearMembership);
    }

    private void enterSelectedToMacro() {
        for (Farmer f : activeFarmers()) {
            Optional<MacroObject> touched = findTouchedMacroByBody(f);
            if (touched.isPresent()) {
                MacroObject macro = touched.get();
                placeInMacroWithSpacing(f, macro);
                village.assignToMacro(f, macro);
            }
        }
    }

    private void exitSelectedFromMacro() {
        for (Farmer f : activeFarmers()) {
            List<MacroObject> memberships = village.memberships(f);
            if (!memberships.isEmpty()) {
                MacroObject macro = memberships.getFirst();
                ejectFromMacro(f, macro);
            }
        }
    }

    private void placeInMacroWithSpacing(Farmer farmer, MacroObject macro) {
        if (!canEnterMacro(macro)) {
            showInfo("Макрооб'єкт переповнений! Максимум " + MAX_FARMERS_IN_MACRO + " фермерів.");
            return;
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
    }

    private void upgradeSelectedFarmers() {
        final int UPGRADE_COST = 500;
        List<Farmer> toUpgrade = activeFarmers();
        int totalCost = toUpgrade.size() * UPGRADE_COST;

        if (village.getTotalCoins() < totalCost) {
            showInfo("Недостатньо монет! Потрібно: " + totalCost + ", є: " + village.getTotalCoins());
            return;
        }

        int upgradedCount = 0;
        for (Farmer f : toUpgrade) {
            Farmer newFarmer = null;
            Tool currentTool = f.getTool();

            if (f instanceof Gardener) {
                newFarmer = new FreePeasant(f.getName(), f.getMotivation(), f.getSpeed(), f.getMaxLoad(), currentTool, f.getX(), f.getY());
            } else if (f instanceof FreePeasant) {
                newFarmer = new MasterFarmer(f.getName(), f.getMotivation(), f.getSpeed(), f.getMaxLoad(), currentTool, f.getX(), f.getY());
            }

            if (newFarmer != null) {
                newFarmer.setActive(true);
                newFarmer.setState(f.getState());
                newFarmer.setCurrentLoad(f.getCurrentLoad());
                village.removeFarmer(f);
                village.addFarmer(newFarmer);
                upgradedCount++;
            }
        }

        if (upgradedCount > 0) {
            village.addCoins(-totalCost);
            showInfo("Оновлено " + upgradedCount + " хліборобів за " + totalCost + " монет! Нові інструменти доступні в меню купівлі (I).");
        } else {
            showInfo("Немає хліборобів для оновлення (Майстер-Хлібороб вже максимальний тип)");
        }
    }

    private void seedData() {
        double centerX = (WORLD_WIDTH - MACRO_SIZE) / 2;
        double centerY = (WORLD_HEIGHT - MACRO_SIZE) / 2;
        double margin = WORLD_WIDTH * 0.15;
        double offsetY = WORLD_HEIGHT * 0.25;
        MacroObject church = new Church(centerX, centerY + offsetY);
        MacroObject mill = new Mill(WORLD_WIDTH - margin - MACRO_SIZE, centerY);
        MacroObject field = new WheatField(margin, centerY - offsetY);
        village.addMacroObject(church);
        village.addMacroObject(mill);
        village.addMacroObject(field);

        Farmer f1 = new Gardener("Іван", 72, 1.1, 12, new Tool(ToolTypes.NoTool, 1.0f), centerX + 50, centerY + 50);
        Farmer f2 = new FreePeasant("Петро", 64, 1.3, 10, new Tool(ToolTypes.NoTool, 1.0f), centerX + 80, centerY + 50);
        Farmer f3 = new MasterFarmer("Сергій", 90, 1.7, 20, new Tool(ToolTypes.NoTool, 1.0f), centerX + 50, centerY + 80);
        Farmer f4 = new Gardener("Микола", 50, 1.0, 10, new Tool(ToolTypes.NoTool, 1.0f), centerX + 80, centerY + 80);

        village.addFarmer(f1);
        village.addFarmer(f2);
        village.addFarmer(f3);
        village.addFarmer(f4);

        for (Farmer f : village.getFarmers()) {
            clampFarmerInsideWorld(f);
            syncMembershipByTouch(f);
        }
    }

    private void syncMembershipByTouch(Farmer farmer) {
        Optional<MacroObject> touched = findTouchedMacroByBody(farmer);
        if (touched.isPresent()) {
            MacroObject macro = touched.get();
            boolean alreadyMember = macro.contains(farmer);
            if (!farmer.isActive() && !alreadyMember) {
                if (canEnterMacro(macro)) {
                    teleportToFreePositionInMacro(farmer, macro);
                    village.assignToMacro(farmer, macro);
                }
            }
        } else {
            if (!farmer.isActive()) {
                village.clearMembership(farmer);
            }
        }
    }

    private static final int MAX_FARMERS_IN_MACRO = 4;

    private boolean canEnterMacro(MacroObject macro) {
        return macro.getMembers().size() < MAX_FARMERS_IN_MACRO;
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

    private Optional<MacroObject> findTouchedMacroByBody(Farmer farmer) {
        double fx = farmer.getX() - 15;
        double fy = farmer.getY() - 25;
        double fw = Farmer.WIDTH  + 15;
        double fh = Farmer.HEIGHT + 15;
        MacroObject best = null;
        for (MacroObject mo : village.getMacroObjects()) {
            double half = MACRO_STROKE_W / 2;
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

    private double clampMicroX(double x) {
        return Math.clamp(x, MICRO_FRAME_PADDING, WORLD_WIDTH  - MICRO_BLOCK_SIZE - MICRO_FRAME_PADDING);
    }

    private double clampMicroY(double y) {
        return Math.clamp(y, MICRO_FRAME_PADDING, WORLD_HEIGHT - MICRO_BLOCK_SIZE - MICRO_FRAME_PADDING);
    }

    private void clampFarmerInsideWorld(Farmer f) {
        f.setX(clampMicroX(f.getX()));
        f.setY(clampMicroY(f.getY()));
    }

    private double currentViewportWidth() {
        return worldPane.getWidth() > 0 ? worldPane.getWidth() : 1200;
    }

    private double currentViewportHeight() {
        return worldPane.getHeight() > 0 ? worldPane.getHeight() : 800;
    }

    private List<Farmer> sortedByCriteria(List<Farmer> input) {
        List<Farmer> sorted = new ArrayList<>(input);
        switch (sortCriteria) {
            case BY_LOAD  -> sorted.sort(Comparator.comparingInt(Farmer::getMaxLoad));
            case BY_SPEED -> sorted.sort(Comparator.comparingDouble(Farmer::getSpeed));
            default       -> sorted.sort(Comparator.comparing(Farmer::getName, String.CASE_INSENSITIVE_ORDER));
        }
        return sorted;
    }

    private String nextCloneName(String sourceName) {
        String base   = cloneBaseName(sourceName);
        String prefix = base + "_копія";
        int next = 1;
        for (Farmer f : village.getFarmers()) {
            String n = f.getName();
            if (n.startsWith(prefix)) {
                String suffix = n.substring(prefix.length());
                if (suffix.matches("\\d+")) next = Math.max(next, Integer.parseInt(suffix) + 1);
            }
        }
        return prefix + next;
    }

    private String cloneBaseName(String name) {
        String marker = "_копія";
        int idx = name.lastIndexOf(marker);
        if (idx < 0) return name;
        String suffix = name.substring(idx + marker.length());
        return suffix.matches("\\d+") ? name.substring(0, idx) : name;
    }

    private Color macroStroke(MacroObject mo) {
        if (mo instanceof Church)     return Color.RED;
        if (mo instanceof Mill)       return Color.GRAY;
        if (mo instanceof WheatField) return Color.GOLDENROD;
        return Color.DARKSLATEBLUE;
    }

    private Color microStroke(List<MacroObject> memberships) {
        if (memberships.isEmpty()) return Color.ORANGE;
        return macroStroke(memberships.getFirst());
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

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return emptyImage();
        return imageCache.computeIfAbsent(path, k -> {
            try {
                var s = getClass().getResourceAsStream(k);
                return s == null ? emptyImage() : new Image(s);
            } catch (Exception e) { return emptyImage(); }
        });
    }

    private Image emptyImage() {
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7YkH8AAAAASUVORK5CYII=");
    }

    private String toolAsset(ToolTypes type) {
        return switch (type) {
            case Knife       -> "/assets/knife.png";
            case Sickle      -> "/assets/sickle.png";
            case Scythe      -> "/assets/scythe.png";
            case GoldenScythe-> "/assets/g_scythe.png";
            default          -> null;
        };
    }

    private List<Farmer> activeFarmers() {
        return village.getFarmers().stream().filter(Farmer::isActive).collect(Collectors.toList());
    }

    private void selectClosestMotivation(ListView<MotivationLevel> list, int value) {
        MotivationLevel best = null;
        int delta = Integer.MAX_VALUE;
        for (MotivationLevel ml : list.getItems()) {
            int d = Math.abs(ml.value() - value);
            if (d < delta) { delta = d; best = ml; }
        }
        if (best != null) list.getSelectionModel().select(best);
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Інформація");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(primaryStage);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Помилка");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(primaryStage);
        a.showAndWait();
    }

    private record MotivationLevel(String label, int value) {
        @Override public String toString() { return label; }
    }
}
