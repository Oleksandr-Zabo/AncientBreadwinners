package com.example.ancientbreadwinners;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.stage.Screen;

import java.util.*;
import java.util.stream.Collectors;

public class HelloApplication extends Application {
    private static final double MICRO_IMAGE_SIZE = 75;
    private static final double MICRO_BLOCK_SIZE = 125;
    private static final double MICRO_TOOL_SIZE = 40;
    private static final double MICRO_TOOL_OFFSET_X = 75;
    private static final double MICRO_TOOL_OFFSET_Y = 75;
    private static final double MICRO_FRAME_PADDING = 10;
    private static final double MICRO_FRAME_STROKE_WIDTH = 4.0;
    private static final double MOTIVATION_BAR_WIDTH = 150;
    private static final double MOTIVATION_BAR_HEIGHT = 20;
    private static final double MOTIVATION_OFFSET_Y = -15;
    private static final double NAME_OFFSET_Y = 90;

    private static final double MACRO_SIZE = 250;
    private static final double MACRO_IMAGE_SIZE = 150;
    private static final double MACRO_IMAGE_OFFSET = 50;
    private static final double MACRO_TITLE_OFFSET_Y = 225;
    private static final double MACRO_STROKE_WIDTH = 4.0;

    private double worldWidth;
    private double worldHeight;

    private final Village village = new Village();
    private final Pane worldPane = new Pane();
    private final Label activeLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label macroInfoLabel = new Label();
    private final Map<String, Image> imageCache = new HashMap<>();
    private Farmer selectedFarmer = null;
    private MacroObject selectedMacro = null;

    @Override
    public void start(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        worldWidth = bounds.getWidth();
        worldHeight = bounds.getHeight();

        seedData();

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, worldWidth, worldHeight);

        worldPane.setPrefSize(worldWidth, worldHeight);
        worldPane.setFocusTraversable(true);
        setBackground(worldPane);
        worldPane.setOnMouseClicked(event -> {
            worldPane.requestFocus();
            event.consume();
        });

        VBox topBar = createTopBar(stage);
        VBox bottomBar = createBottomBar();

        root.setTop(topBar);
        root.setCenter(worldPane);
        root.setBottom(bottomBar);

        worldPane.prefWidthProperty().bind(root.widthProperty());
        worldPane.prefHeightProperty().bind(root.heightProperty().subtract(topBar.heightProperty()).subtract(bottomBar.heightProperty()));

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                clearSelection();
                redraw();
                return;
            }
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelected();
                redraw();
                return;
            }
            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                cloneSelected();
                redraw();
                return;
            }
            if (event.getCode() == KeyCode.INSERT) {
                showCreateDialog(stage);
                redraw();
                return;
            }
            if (event.getCode().isArrowKey()) {
                moveSelected(event.getCode());
                redraw();
                event.consume();
            }
        });

        redraw();
        worldPane.requestFocus();

        stage.setTitle("Ancient Breadwinners");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createTopBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(buildFileMenu(stage), buildEditMenu(), buildWindowsMenu(), buildMoreMenu());

        activeLabel.setPadding(new Insets(6, 8, 8, 8));
        activeLabel.setFont(Font.font(12));
        activeLabel.setTextFill(Color.BLACK);
        activeLabel.setWrapText(true);
        activeLabel.setText("Активні мікрооб'єкти: немає");

        statusLabel.setPadding(new Insets(6, 8, 8, 8));
        statusLabel.setFont(Font.font(12));
        statusLabel.setTextFill(Color.BLACK);
        statusLabel.setText("Останній вибір: немає");
        statusLabel.setTextAlignment(TextAlignment.RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox infoRow = new HBox(activeLabel, spacer, statusLabel);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(menuBar, infoRow);
    }

    private Menu buildFileMenu(Stage stage) {
        Menu file = new Menu("Файл");
        MenuItem create = new MenuItem("Створити Городника");
        create.setOnAction(e -> {
            showCreateDialog(stage);
            redraw();
            e.consume();
        });
        MenuItem exit = new MenuItem("Вихід");
        exit.setOnAction(e -> {
            stage.close();
            e.consume();
        });
        file.getItems().addAll(create, new SeparatorMenuItem(), exit);
        return file;
    }

    private Menu buildEditMenu() {
        Menu edit = new Menu("Редагувати");
        MenuItem copy = new MenuItem("Копіювати виділені");
        copy.setOnAction(e -> {
            cloneSelected();
            redraw();
            e.consume();
        });
        MenuItem delete = new MenuItem("Видалити виділені");
        delete.setOnAction(e -> {
            deleteSelected();
            redraw();
            e.consume();
        });
        MenuItem clear = new MenuItem("Скасувати виділення");
        clear.setOnAction(e -> {
            clearSelection();
            redraw();
            e.consume();
        });
        MenuItem detach = new MenuItem("Вилучити з макрооб'єкта");
        detach.setOnAction(e -> {
            detachSelected();
            redraw();
            e.consume();
        });
        edit.getItems().addAll(copy, delete, clear, new SeparatorMenuItem(), detach);
        return edit;
    }

    private Menu buildWindowsMenu() {
        Menu windows = new Menu("Вікна");
        MenuItem status = new MenuItem("Оновити статус");
        status.setOnAction(e -> {
            updateStatus();
            e.consume();
        });
        windows.getItems().add(status);
        return windows;
    }

    private Menu buildMoreMenu() {
        Menu more = new Menu("Більше");
        MenuItem about = new MenuItem("Про програму");
        about.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Про програму");
            alert.setHeaderText("Ancient Breadwinners");
            alert.setContentText("""
                    Симулятор макро/мікрооб'єктів, дотикової приналежності та копіювання у вільні місця.

                    Керування: ЛКМ — вибрати весь мікрооб'єкт, ПКМ — редагувати, Ctrl+C — копія, ←↑→↓ — рух, Insert — створити через меню.
                    """);
            alert.showAndWait();
            e.consume();
        });
        more.getItems().add(about);
        return more;
    }

    private VBox createBottomBar() {
        macroInfoLabel.setPadding(new Insets(6, 10, 10, 10));
        macroInfoLabel.setFont(Font.font(11));
        macroInfoLabel.setTextFill(Color.BLACK);
        macroInfoLabel.setWrapText(true);

        return new VBox(macroInfoLabel);
    }

    private void redraw() {
        worldPane.getChildren().clear();
        setBackground(worldPane);

        for (MacroObject macroObject : village.getMacroObjects()) {
            drawMacro(macroObject);
        }

        for (Farmer farmer : village.getFarmers()) {
            drawFarmer(farmer);
        }

        updateStatus();
    }

    private void drawMacro(MacroObject macroObject) {
        double x = macroObject.getX();
        double y = macroObject.getY();
        double size = macroObject.getWidth();

        Rectangle rect = new Rectangle(x, y, size, size);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(macroStroke(macroObject));
        rect.setStrokeWidth(MACRO_STROKE_WIDTH);

        ImageView imageView = new ImageView(loadImage(macroObject.getImageAsset()));
        imageView.setFitWidth(MACRO_IMAGE_SIZE);
        imageView.setFitHeight(MACRO_IMAGE_SIZE);
        imageView.setLayoutX(x + MACRO_IMAGE_OFFSET);
        imageView.setLayoutY(y + MACRO_IMAGE_OFFSET);

        Text countText = new Text(x + MACRO_IMAGE_OFFSET, y + MACRO_TITLE_OFFSET_Y, "Breadwinners inside: " + macroObject.getCount());
        countText.setFont(Font.font(14));
        countText.setFill(Color.BLACK);

        worldPane.getChildren().addAll(rect, imageView, countText);

        rect.setOnMouseClicked(e -> {
            selectedMacro = macroObject;
            selectedFarmer = null;
            updateMacroInfo(macroObject);
            redraw();
            e.consume();
        });
    }

    private void drawFarmer(Farmer farmer) {
        clampFarmerInsideWorld(farmer);
        double x = farmer.getX();
        double y = farmer.getY();
        double blockWidth = MICRO_BLOCK_SIZE;
        double blockHeight = MICRO_BLOCK_SIZE;

        List<MacroObject> memberships = village.memberships(farmer);
        Color activeStroke = farmer.isActive() ? microStroke(memberships) : Color.TRANSPARENT;

        Rectangle motivationBar = new Rectangle(0, MOTIVATION_OFFSET_Y, MOTIVATION_BAR_WIDTH * farmer.getMotivation() / 100.0, MOTIVATION_BAR_HEIGHT);
        motivationBar.setFill(Color.LIMEGREEN);
        motivationBar.setStroke(Color.DARKGREEN);
        motivationBar.setStrokeWidth(1);

        ImageView imageView = new ImageView(loadImage(farmer.getImageAsset()));
        imageView.setFitWidth(MICRO_IMAGE_SIZE);
        imageView.setFitHeight(MICRO_IMAGE_SIZE);
        imageView.setLayoutX(0);
        imageView.setLayoutY(0);

        Text nameText = new Text(0, NAME_OFFSET_Y, farmer.getName());
        nameText.setFill(Color.BLACK);
        nameText.setFont(Font.font(12));

        ImageView toolIcon = new ImageView(loadImage(toolAsset(farmer.getTool().getType())));
        toolIcon.setFitWidth(MICRO_TOOL_SIZE);
        toolIcon.setFitHeight(MICRO_TOOL_SIZE);
        toolIcon.setLayoutX(MICRO_TOOL_OFFSET_X);
        toolIcon.setLayoutY(MICRO_TOOL_OFFSET_Y);

        Rectangle frame = new Rectangle(-MICRO_FRAME_PADDING, MOTIVATION_OFFSET_Y - MICRO_FRAME_PADDING,
                blockWidth + MICRO_FRAME_PADDING * 2.0, blockHeight + MICRO_FRAME_PADDING * 2.0);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(activeStroke);
        frame.setStrokeWidth(farmer.isActive() ? MICRO_FRAME_STROKE_WIDTH : 0);

        Pane pane = new Pane(frame, motivationBar, imageView, nameText, toolIcon);
        pane.setPrefSize(blockWidth, blockHeight);
        pane.setLayoutX(x);
        pane.setLayoutY(y);
        pane.setPickOnBounds(true);
        pane.setOnMouseClicked(e -> {
            worldPane.requestFocus();
            if (e.getButton() == MouseButton.PRIMARY) {
                farmer.setActive(!farmer.isActive());
                selectedFarmer = farmer;
                redraw();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                selectedFarmer = farmer;
                selectedMacro = null;
                showEditDialog(farmer);
                redraw();
            }
            e.consume();
        });

        worldPane.getChildren().add(pane);
    }

    private void clearSelection() {
        for (Farmer farmer : village.getFarmers()) {
            farmer.setActive(false);
        }
        selectedFarmer = null;
        selectedMacro = null;
    }

    private void cloneSelected() {
        List<Farmer> active = activeFarmers();
        for (Farmer source : active) {
            Farmer clone = source.clone();
            clone.setName(nextCloneName(source.getName()));
            Optional<Village.Placement> target = village.findFreeAdjacentPosition(source, worldWidth, worldHeight);
            if (target.isEmpty()) {
                continue;
            }
            clone.setX(target.get().x());
            clone.setY(target.get().y());
            clampFarmerInsideWorld(clone);
            clone.setActive(false);
            village.addFarmer(clone);
             syncMembershipByTouch(clone);
        }
    }

    private String nextCloneName(String sourceName) {
        String baseName = cloneBaseName(sourceName);
        int nextIndex = 1;
        String prefix = baseName + "_копія";

        for (Farmer farmer : village.getFarmers()) {
            String name = farmer.getName();
            if (name.equals(baseName)) {
                continue;
            }
            if (!name.startsWith(prefix)) {
                continue;
            }
            String suffix = name.substring(prefix.length());
            if (suffix.matches("\\d+")) {
                nextIndex = Math.max(nextIndex, Integer.parseInt(suffix) + 1);
            }
        }

        return prefix + nextIndex;
    }

    private String cloneBaseName(String name) {
        String marker = "_копія";
        int markerIndex = name.lastIndexOf(marker);
        if (markerIndex < 0) {
            return name;
        }

        String suffix = name.substring(markerIndex + marker.length());
        return suffix.matches("\\d+") ? name.substring(0, markerIndex) : name;
    }

    private void moveSelected(KeyCode code) {
        List<Farmer> active = activeFarmers();
        if (active.isEmpty()) return;

        for (Farmer farmer : active) {
            double dx = 0, dy = 0, step = 10;
            if (code == KeyCode.LEFT) dx = -step;
            if (code == KeyCode.RIGHT) dx = step;
            if (code == KeyCode.UP) dy = -step;
            if (code == KeyCode.DOWN) dy = step;

            double nx = farmer.getX() + dx;
            double ny = farmer.getY() + dy;
            nx = clampMicroX(nx);
            ny = clampMicroY(ny);

            if (nx == farmer.getX() && ny == farmer.getY()) {
                continue;
            }

            farmer.setX(nx);
            farmer.setY(ny);
            syncMembershipByTouch(farmer);
        }
    }

    private void deleteSelected() {
        List<Farmer> active = new ArrayList<>(activeFarmers());
        for (Farmer farmer : active) {
            village.removeFarmer(farmer);
        }
        selectedFarmer = null;
    }

    private void detachSelected() {
        for (Farmer farmer : activeFarmers()) {
            village.clearMembership(farmer);
        }
    }

    private void showCreateDialog(Stage owner) {
        Dialog<FarmerCreationData> dialog = new Dialog<>();
        dialog.setTitle("Створення Городника");
        dialog.initOwner(owner);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        TextField nameField = new TextField("Іван");
        CheckBox activeCheck = new CheckBox("Активний після створення");
        activeCheck.setSelected(true);

        ListView<MotivationLevel> motivationList = new ListView<>();
        motivationList.getItems().setAll(
                new MotivationLevel("Низький (40)", 40),
                new MotivationLevel("Середній (70)", 70),
                new MotivationLevel("Високий (90)", 90)
        );
        motivationList.getSelectionModel().select(1);
        motivationList.setPrefHeight(90);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        grid.addRow(0, new Label("Ім'я:"), nameField);
        grid.addRow(1, new Label("Активний:"), activeCheck);
        grid.addRow(2, new Label("Рівень мотивації:"), motivationList);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != okType) return null;
            MotivationLevel motivation = motivationList.getSelectionModel().getSelectedItem();
            int motivationValue = motivation == null ? 70 : motivation.value();
            return new FarmerCreationData(
                    nameField.getText().trim().isEmpty() ? "Іван" : nameField.getText().trim(),
                    activeCheck.isSelected(),
                    motivationValue,
                    ToolTypes.Knife
            );
        });

        Optional<FarmerCreationData> data = dialog.showAndWait();
        if (data.isEmpty()) return;

        FarmerCreationData d = data.get();
        Farmer farmer = new Gardener(d.name(), 40, 40);
        farmer.setActive(d.active());
        farmer.setMotivation(d.motivation());
        farmer.setToolType(d.tool());

        Optional<Village.Placement> freeNearStart = village.findFreeAdjacentPosition(farmer, worldWidth, worldHeight);
        freeNearStart.ifPresent(placement -> {
            farmer.setX(placement.x());
            farmer.setY(placement.y());
        });
        clampFarmerInsideWorld(farmer);
        village.addFarmer(farmer);
        syncMembershipByTouch(farmer);
        selectedFarmer = farmer;
    }

    private void showEditDialog(Farmer farmer) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Редагування");
        dialog.initOwner(worldPane.getScene().getWindow());

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        TextField nameField = new TextField(farmer.getName());
        CheckBox activeCheck = new CheckBox("Активний");
        activeCheck.setSelected(farmer.isActive());

        ListView<MotivationLevel> motivationList = new ListView<>();
        motivationList.getItems().setAll(
                new MotivationLevel("Низький (40)", 40),
                new MotivationLevel("Середній (70)", 70),
                new MotivationLevel("Високий (90)", 90)
        );
        motivationList.setPrefHeight(90);
        selectClosestMotivation(motivationList, farmer.getMotivation());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        grid.addRow(0, new Label("Ім'я:"), nameField);
        grid.addRow(1, new Label("Активний:"), activeCheck);
        grid.addRow(2, new Label("Рівень мотивації:"), motivationList);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> buttonType == okType);
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isEmpty() || !result.get()) return;

        farmer.setName(nameField.getText().trim().isEmpty() ? farmer.getName() : nameField.getText().trim());
        farmer.setActive(activeCheck.isSelected());

        MotivationLevel selectedLevel = motivationList.getSelectionModel().getSelectedItem();
        if (selectedLevel != null) {
            farmer.setMotivation(selectedLevel.value());
        }
    }

    private void updateStatus() {
        List<Farmer> active = activeFarmers();
        if (!active.isEmpty()) {
            String names = active.stream().limit(3).map(Farmer::getName).collect(Collectors.joining(", "));
            if (active.size() > 3) {
                names += ", ...";
            }
            activeLabel.setText("Активні мікрооб'єкти: " + active.size() + " -> " + names);
        } else {
            activeLabel.setText("Активні мікрооб'єкти: немає");
        }

        if (selectedFarmer != null) {
            List<MacroObject> memberships = village.memberships(selectedFarmer);
            String macro = memberships.isEmpty() ? "жодному" : memberships.getFirst().getName();
            statusLabel.setText("Останній вибір: " + selectedFarmer.getName() + " | Рівень: " + selectedFarmer.getMotivation() + " | Макрооб'єкт: " + macro);
        } else if (selectedMacro != null) {
            updateMacroInfo(selectedMacro);
        } else {
            statusLabel.setText("Останній вибір: немає");
            macroInfoLabel.setText("");
        }
    }

    private void updateMacroInfo(MacroObject macro) {
        statusLabel.setText("Останній вибір: макрооб'єкт " + macro.getName());
        StringBuilder info = new StringBuilder("Членів у '" + macro.getName() + "': " + macro.getCount() + " → ");
        List<Farmer> members = macro.getMembers();
        if (members.isEmpty()) {
            info.append("немає");
        } else {
            info.append(members.stream().map(Farmer::getName).collect(Collectors.joining(", ")));
        }
        macroInfoLabel.setText(info.toString());
    }

    private void seedData() {
        double macroY = Math.max(80, worldHeight * 0.18);
        double macroWidth = MACRO_SIZE;
        double gap = Math.max(40, (worldWidth - (macroWidth * 3)) / 4.0);
        MacroObject church = new Church(gap, macroY);
        MacroObject mill = new Mill(gap * 2 + macroWidth, macroY);
        MacroObject field = new WheatField(gap * 3 + macroWidth * 2, macroY);

        village.addMacroObject(church);
        village.addMacroObject(mill);
        village.addMacroObject(field);

        Farmer f1 = new Gardener("Іван", 72, 1.1, 12, new Tool(ToolTypes.Knife, 1.0f), 380, 470);
        Farmer f2 = new FreePeasant("Петро", 64, 1.25, 9, new Tool(ToolTypes.Scythe, 0.9f), 690, 270);
        Farmer f3 = new MasterFarmer("Сергій", 90, 1.7, 20, new Tool(ToolTypes.GoldenScythe, 1.4f), 1100, 270);
        Farmer f4 = new Gardener("Микола", 50, 1.0, 10, new Tool(ToolTypes.Sickle, 0.8f), 300, 500);
        village.addFarmer(f1);
        village.addFarmer(f2);
        village.addFarmer(f3);
        village.addFarmer(f4);

        f1.setX(church.getX() - MICRO_BLOCK_SIZE + MICRO_BLOCK_SIZE+40);
        f1.setY(church.getY() + 100);
        f2.setX(mill.getX() - MICRO_BLOCK_SIZE + MICRO_BLOCK_SIZE+40);
        f2.setY(mill.getY() + 100);
        f3.setX(field.getX() - MICRO_BLOCK_SIZE + MICRO_BLOCK_SIZE+40);
        f3.setY(field.getY() + 100);

        clampFarmerInsideWorld(f1);
        clampFarmerInsideWorld(f2);
        clampFarmerInsideWorld(f3);
        clampFarmerInsideWorld(f4);

        syncMembershipByTouch(f1);
        syncMembershipByTouch(f2);
        syncMembershipByTouch(f3);
    }

    private void setBackground(Pane pane) {
        Image bgImage = loadImage("/assets/fon.jpg");
        pane.setBackground(new Background(new BackgroundImage(
                bgImage,
                BackgroundRepeat.REPEAT,
                BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT
        )));
    }

    private Image loadImage(String assetPath) {
        if (assetPath == null || assetPath.isBlank()) {
            return emptyImage();
        }
        return imageCache.computeIfAbsent(assetPath, key -> {
            try {
                var stream = getClass().getResourceAsStream(key);
                if (stream == null) {
                    return emptyImage();
                }
                return new Image(stream);
            } catch (Exception e) {
                return emptyImage();
            }
        });
    }

    private Image emptyImage() {
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7YkH8AAAAASUVORK5CYII=");
    }

    private String toolAsset(ToolTypes type) {
        if (type == ToolTypes.Knife) return "/assets/knife.png";
        if (type == ToolTypes.Sickle) return "/assets/sickle.png";
        if (type == ToolTypes.Scythe) return "/assets/scythe.png";
        if (type == ToolTypes.GoldenScythe) return "/assets/g_scythe.png";
        return "/assets/knife.png";
    }

    private void syncMembershipByTouch(Farmer farmer) {
        Optional<MacroObject> touched = findTouchedMacroByBody(farmer);
        if (touched.isPresent()) {
            village.assignToMacro(farmer, touched.get());
        } else {
            village.clearMembership(farmer);
        }
    }

    private Optional<MacroObject> findTouchedMacroByBody(Farmer farmer) {
        double fx = farmer.getX()-15;
        double fy = farmer.getY()-25;
        double fw = Farmer.WIDTH+15;
        double fh = Farmer.HEIGHT+15;

        MacroObject leftmost = null;
        for (MacroObject macroObject : village.getMacroObjects()) {
            double strokeHalf = MACRO_STROKE_WIDTH / 2.0;
            double mx = macroObject.getX() - strokeHalf;
            double my = macroObject.getY() - strokeHalf;
            double mw = macroObject.getWidth() + strokeHalf * 2.0;
            double mh = macroObject.getHeight() + strokeHalf * 2.0;

            if (!intersectsInclusive(fx, fy, fw, fh, mx, my, mw, mh)) {
                continue;
            }
            if (leftmost == null || macroObject.getX() < leftmost.getX()) {
                leftmost = macroObject;
            }
        }
        return Optional.ofNullable(leftmost);
    }

    private boolean intersectsInclusive(double x1, double y1, double w1, double h1,
                                        double x2, double y2, double w2, double h2) {
        return x1 <= x2 + w2 && x1 + w1 >= x2 && y1 <= y2 + h2 && y1 + h1 >= y2;
    }


    private double clampMicroX(double x) {
        double availableWidth = currentWorldWidth();
        double minX = MICRO_FRAME_PADDING;
        double rightExtent = Math.max(MOTIVATION_BAR_WIDTH, MICRO_BLOCK_SIZE + MICRO_FRAME_PADDING);
        double maxX = Math.max(minX, availableWidth - rightExtent);
        return Math.clamp(x, minX, maxX);
    }

    private double clampMicroY(double y) {
        double availableHeight = currentWorldHeight();
        double minY = Math.max(0, -(MOTIVATION_OFFSET_Y - MICRO_FRAME_PADDING));
        double maxY = Math.max(minY, availableHeight - MICRO_BLOCK_SIZE);
        return Math.clamp(y, minY, maxY);
    }

    private void clampFarmerInsideWorld(Farmer farmer) {
        farmer.setX(clampMicroX(farmer.getX()));
        farmer.setY(clampMicroY(farmer.getY()));
    }

    private double currentWorldWidth() {
        return worldPane.getWidth() > 0 ? worldPane.getWidth() : worldWidth;
    }

    private double currentWorldHeight() {
        return worldPane.getHeight() > 0 ? worldPane.getHeight() : worldHeight;
    }

    private void selectClosestMotivation(ListView<MotivationLevel> motivationList, int motivation) {
        MotivationLevel best = null;
        int delta = Integer.MAX_VALUE;
        for (MotivationLevel level : motivationList.getItems()) {
            int currentDelta = Math.abs(level.value() - motivation);
            if (currentDelta < delta) {
                delta = currentDelta;
                best = level;
            }
        }
        if (best != null) {
            motivationList.getSelectionModel().select(best);
        }
    }

    private Color macroStroke(MacroObject macroObject) {
        if (macroObject instanceof Church) {
            return Color.RED;
        }
        if (macroObject instanceof Mill) {
            return Color.GRAY;
        }
        if (macroObject instanceof WheatField) {
            return Color.TURQUOISE;
        }
        return Color.DARKSLATEBLUE;
    }

    private Color microStroke(List<MacroObject> memberships) {
        if (memberships.isEmpty()) {
            return Color.ORANGE;
        }
        MacroObject owner = memberships.getFirst();
        return macroStroke(owner);
    }

    private List<Farmer> activeFarmers() {
        return village.getFarmers().stream().filter(Farmer::isActive).collect(Collectors.toList());
    }

    private record FarmerCreationData(String name, boolean active, int motivation, ToolTypes tool) {
    }

    private record MotivationLevel(String label, int value) {
        @Override
        public String toString() {
            return label;
        }
    }
}

