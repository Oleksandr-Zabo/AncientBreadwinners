package com.example.ancientbreadwinners;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class GameUi {
    private final HelloApplication app;

    GameUi(HelloApplication app) {
        this.app = app;
    }

    VBox createTopBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(
                buildFileMenu(stage),
                buildSelectionMenu(),
                buildControlsMenu(),
                buildWindowsMenu(stage),
                buildAboutMenu());

        app.activeLabel.setPadding(new Insets(4, 8, 4, 8));
        app.activeLabel.setFont(javafx.scene.text.Font.font(12));
        app.activeLabel.setText("Активні мікрооб'єкти: немає");

        app.statusLabel.setPadding(new Insets(4, 8, 4, 8));
        app.statusLabel.setFont(javafx.scene.text.Font.font(12));
        app.statusLabel.setText("Останній вибір: немає");

        app.coinsLabel.setPadding(new Insets(4, 8, 4, 8));
        app.coinsLabel.setFont(javafx.scene.text.Font.font(12));
        app.coinsLabel.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox infoRow = new HBox(app.activeLabel, spacer, app.coinsLabel, app.statusLabel);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.setStyle("-fx-background-color: transparent;");

        VBox topBar = new VBox(menuBar, infoRow);
        topBar.setStyle("-fx-background-color: rgba(255,255,255,0.9);");
        topBar.setViewOrder(-1000);
        return topBar;
    }

    private Menu buildFileMenu(Stage stage) {
        Menu file = new Menu("Файл");

        MenuItem save = new MenuItem("Зберегти гру  [Ctrl+S]");
        save.setOnAction(e -> showSaveDialog());

        MenuItem load = new MenuItem("Завантажити гру  [Ctrl+O]");
        load.setOnAction(e -> showLoadDialog());

        MenuItem exit = new MenuItem("Вихід");
        exit.setOnAction(e -> stage.close());

        file.getItems().addAll(save, load, new SeparatorMenuItem(), exit);
        return file;
    }

    private Menu buildSelectionMenu() {
        Menu selection = new Menu("Виділені");

        MenuItem copy = new MenuItem("Копіювати виділених  [Ctrl+C]");
        copy.setOnAction(e -> { app.logic.cloneSelected(); app.renderer.redraw(); });

        MenuItem delete = new MenuItem("Видалити виділених  [Delete]");
        delete.setOnAction(e -> { app.logic.deleteSelected(); app.renderer.redraw(); });

        MenuItem clear = new MenuItem("Скасувати виділення  [Esc]");
        clear.setOnAction(e -> { app.logic.clearSelection(); app.renderer.redraw(); });

        MenuItem detach = new MenuItem("Вилучити з макрооб'єкта");
        detach.setOnAction(e -> { app.logic.detachSelected(); app.renderer.redraw(); });

        MenuItem edit = new MenuItem("Редагувати хлібороба  [ПКМ]");
        edit.setOnAction(e -> {
            if (app.selectedFarmer != null) showEditDialog(app.selectedFarmer);
            else showInfo("Спочатку виберіть хлібороба");
        });

        selection.getItems().addAll(copy, delete, clear, new SeparatorMenuItem(), detach, edit);
        return selection;
    }

    private Menu buildControlsMenu() {
        Menu ctrl = new Menu("Керування");
        MenuItem showControls = new MenuItem("Показати керування");
        showControls.setOnAction(e -> showControlsDialog());
        ctrl.getItems().add(showControls);
        return ctrl;
    }

    private Menu buildWindowsMenu(Stage stage) {
        Menu windows = new Menu("Вікна");

        MenuItem create = new MenuItem("Створення хлібороба  [Insert]");
        create.setOnAction(e -> showCreateDialog());

        MenuItem buyTool = new MenuItem("Купити інструмент  [I]");
        buyTool.setOnAction(e -> showBuyToolDialog(stage));

        MenuItem find = new MenuItem("Знайти хлібороба  [F]");
        find.setOnAction(e -> showFindDialog(stage));

        MenuItem listField = new MenuItem("Список у Пшеничному Полі  [E]");
        listField.setOnAction(e -> showListDialog(app.logic.findMacroByType(WheatField.class), "Пшеничне Поле"));

        MenuItem listMill = new MenuItem("Список у Млині  [M]");
        listMill.setOnAction(e -> showListDialog(app.logic.findMacroByType(Mill.class), "Млин"));

        MenuItem listChurch = new MenuItem("Список у Церкві  [J]");
        listChurch.setOnAction(e -> showListDialog(app.logic.findMacroByType(Church.class), "Церква"));

        MenuItem listWithoutMacro = new MenuItem("Мікрооб'єкти без макрооб'єкта  [G]");
        listWithoutMacro.setOnAction(e -> showListDialog(null, "Без макрооб'єкта"));

        MenuItem countActive = new MenuItem("Активних хліборобів  [V]");
        countActive.setOnAction(e -> showCountInfo("Активних хліборобів", app.logic.activeFarmers().size()));

        MenuItem countMotivation = new MenuItem("З мотивацією > 50%");
        countMotivation.setOnAction(e -> showCountInfo("Хліборобів з мотивацією > 50%", (int) app.village.countWithHighMotivation(50)));

        MenuItem countNoTool = new MenuItem("Без інструменту  [T]");
        countNoTool.setOnAction(e -> showCountInfo("Хліборобів без інструменту", (int) app.village.countWithoutTool()));

        MenuItem sort = new MenuItem("Критерій сортування  [R]");
        sort.setOnAction(e -> showSortDialog(stage));

        windows.getItems().addAll(
                create, new SeparatorMenuItem(),
                buyTool, find, new SeparatorMenuItem(),
                listField, listMill, listChurch, listWithoutMacro, new SeparatorMenuItem(),
                countActive, countMotivation, countNoTool, new SeparatorMenuItem(),
                sort
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

    VBox createBottomBar() {
        VBox bottomBar = new VBox();
        bottomBar.setStyle("-fx-background-color: transparent;");
        bottomBar.setPrefHeight(0);
        bottomBar.setMinHeight(0);
        bottomBar.setMaxHeight(0);
        return bottomBar;
    }

    void updateStatus() {
        List<Farmer> active = app.logic.activeFarmers();
        String modeInfo = " | Режим: " + app.interactionMode.label();
        if (!active.isEmpty()) {
            String names = active.stream().limit(3).map(Farmer::getName).collect(Collectors.joining(", "));
            if (active.size() > 3) names += ", …";
            app.activeLabel.setText("Активні: " + active.size() + " → " + names + modeInfo);
        } else {
            app.activeLabel.setText("Активні мікрооб'єкти: немає" + modeInfo);
        }

        app.coinsLabel.setText("Зароблено монет: " + app.village.getTotalCoins());

        if (app.selectedFarmer != null) {
            List<MacroObject> ms = app.village.memberships(app.selectedFarmer);
            String macro = ms.isEmpty() ? "жодному" : ms.getFirst().getName();
            String kindLabel;
            if (app.selectedFarmer instanceof MasterFarmer) kindLabel = "Майстер-Хлібороб";
            else if (app.selectedFarmer instanceof FreePeasant) kindLabel = "Вільний Селянин";
            else if (app.selectedFarmer instanceof Gardener) kindLabel = "Городник";
            else kindLabel = app.selectedFarmer.getKind();
            app.statusLabel.setText(app.selectedFarmer.getName() + " [" + kindLabel + "] | мотивація: "
                    + app.selectedFarmer.getMotivation() + " | " + macro);
        } else if (app.selectedMacro != null) {
            app.statusLabel.setText("Вибрано макрооб'єкт: " + app.selectedMacro.getName());
        } else {
            app.statusLabel.setText("Останній вибір: немає");
        }
    }

    void showControlsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Керування");
        alert.setHeaderText("Керування грою");
        alert.initOwner(app.primaryStage);
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
  T — кількість без інструменту

Режим взаємодії:
  K — перемкнути режим AUTOMATIC / MANUAL

Швидкість гри:
  L — сповільнити рух вдвічі
  X — нормальна швидкість

Списки:
  G — список хліборобів без макрооб'єкта

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

    void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Інформація");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(app.primaryStage);
        a.showAndWait();
    }

    void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Помилка");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(app.primaryStage);
        a.showAndWait();
    }

    void showCreateDialog() {
        Dialog<Farmer> dialog = new Dialog<>();
        dialog.setTitle("Створення хлібороба");
        dialog.initOwner(app.primaryStage);

        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        TextField nameField = new TextField("Іван");

        RadioButton rbGardener = new RadioButton("Городник (Gardener)");
        RadioButton rbPeasant = new RadioButton("Вільний Селянин (FreePeasant)");
        RadioButton rbMaster = new RadioButton("Майстер-Хлібороб (MasterFarmer)");
        ToggleGroup typeGroup = new ToggleGroup();
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
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
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
            if (rbMaster.isSelected()) f = new MasterFarmer(name, motiv, 1.8, 20, new Tool(ToolTypes.NoTool, 1.0f), 0, 0);
            else if (rbPeasant.isSelected()) f = new FreePeasant(name, motiv, 1.3, 10, new Tool(ToolTypes.NoTool, 1.0f), 0, 0);
            else f = new Gardener(name, motiv, 1.1, 12, new Tool(ToolTypes.NoTool, 1.0f), 0, 0);
            f.setActive(activeCheck.isSelected());
            return f;
        });

        Optional<Farmer> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        Farmer farmer = result.get();
        Optional<Village.Placement> pos = app.village.findFreeAdjacentPosition(
                app.village.getFarmers().isEmpty() ? farmer : app.village.getFarmers().getFirst(),
                app.WORLD_WIDTH, app.WORLD_HEIGHT);
        if (pos.isPresent()) {
            farmer.setX(pos.get().x());
            farmer.setY(pos.get().y());
        } else {
            farmer.setX(app.cameraX + 100);
            farmer.setY(app.cameraY + 200);
        }
        app.logic.clampFarmerInsideWorld(farmer);
        app.village.addFarmer(farmer);
        app.logic.syncMembershipByTouch(farmer);
        app.selectedFarmer = farmer;
        app.renderer.redraw();
    }

    void showEditDialog(Farmer farmer) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Редагування: " + farmer.getName());
        dialog.initOwner(app.primaryStage);

        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        TextField nameField = new TextField(farmer.getName());
        CheckBox activeCheck = new CheckBox("Активний");
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
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Ім'я:"), nameField);
        grid.addRow(1, new Label("Тип:"), kindLabel);
        grid.addRow(2, new Label("Стан:"), activeCheck);
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
        app.renderer.redraw();
    }

    void showBuyToolDialog(Stage owner) {
        if (app.selectedFarmer == null) {
            showInfo("Спочатку виберіть хлібороба (ЛКМ).");
            return;
        }
        Farmer farmer = app.selectedFarmer;

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

        ButtonType ok = new ButtonType("Купити", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        ComboBox<ToolTypes> toolCombo = new ComboBox<>();
        toolCombo.getItems().addAll(buyable);
        toolCombo.getSelectionModel().selectFirst();
        toolCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ToolTypes t) { return t == null ? "" : t.displayName(); }
            @Override public ToolTypes fromString(String s) { return null; }
        });

        Label balanceLabel = new Label("Баланс: " + app.village.getTotalCoins() + " монет");
        Label currentLabel = new Label("Поточний: " + farmer.getTool().getType().displayName() + " (" + currentPrice + " монет)");
        Label priceLabel = new Label();
        toolCombo.setOnAction(e -> {
            ToolTypes t = toolCombo.getSelectionModel().getSelectedItem();
            if (t != null) priceLabel.setText("Ціна: " + t.price() + " монет");
        });
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
        if (app.village.getTotalCoins() < price) {
            showInfo("Недостатньо монет! Потрібно: " + price + ", є: " + app.village.getTotalCoins());
            return;
        }
        app.village.addCoins(-price);
        farmer.setToolType(chosen);
        showInfo(farmer.getName() + " придбав " + chosen.displayName() + ". Залишок: " + app.village.getTotalCoins() + " монет.");
    }

    void showFindDialog(Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Знайти хлібороба");
        dialog.initOwner(owner);

        ButtonType find = new ButtonType("Знайти", ButtonBar.ButtonData.OK_DONE);
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
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Ім'я:"), nameField);
        grid.addRow(1, new Label("Тип:"), typeCombo);
        grid.addRow(2, loadFilter, loadSpinner);
        grid.add(new Label("Результати:"), 0, 3);
        grid.add(resultArea, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);

        Button findBtn = (Button) dialog.getDialogPane().lookupButton(find);
        findBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            String nameFilt = nameField.getText().trim().toLowerCase();
            String typeFilt = typeCombo.getSelectionModel().getSelectedItem();
            int minLoad = loadFilter.isSelected() ? loadSpinner.getValue() : 0;

            List<Farmer> found = app.village.getFarmers().stream()
                    .filter(f -> nameFilt.isEmpty() || f.getName().toLowerCase().contains(nameFilt))
                    .filter(f -> {
                        if ("Городник".equals(typeFilt)) return f.getClass() == Gardener.class;
                        if ("Вільний Селянин".equals(typeFilt)) return f.getClass() == FreePeasant.class;
                        if ("Майстер-Хлібороб".equals(typeFilt)) return f.getClass() == MasterFarmer.class;
                        return true;
                    })
                    .filter(f -> !loadFilter.isSelected() || f.getMaxLoad() >= minLoad)
                    .collect(Collectors.toList());

            if (found.isEmpty()) {
                resultArea.setText("Нічого не знайдено.");
                return;
            }
            List<Farmer> sorted = app.logic.sortedByCriteria(found);
            StringBuilder sb = new StringBuilder();
            String criteria = switch (app.sortCriteria) {
                case BY_LOAD -> "навантаження";
                case BY_SPEED -> "швидкість";
                default -> "ім'я";
            };
            sb.append("Знайдено: ").append(sorted.size()).append(" (сортування: ").append(criteria).append(")\n\n");
            for (Farmer f : sorted) {
                List<MacroObject> ms = app.village.memberships(f);
                String macro = ms.isEmpty() ? "вільний" : ms.stream().map(MacroObject::getName).collect(Collectors.joining(", "));
                sb.append(String.format("%-14s [%s]  x=%.0f y=%.0f  макро: %s%n",
                        f.getName(), f.getKind(), f.getX(), f.getY(), macro));
            }
            resultArea.setText(sb.toString());
        });

        dialog.showAndWait();
    }

    void showListDialog(MacroObject macro, String title) {
        List<Farmer> raw;
        if (macro == null) {
            raw = app.village.getFarmers().stream()
                    .filter(f -> app.village.memberships(f).isEmpty())
                    .collect(Collectors.toList());
        } else {
            raw = new ArrayList<>(macro.getMembers());
        }
        List<Farmer> sorted = app.logic.sortedByCriteria(raw);

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

        String criteria = switch (app.sortCriteria) {
            case BY_LOAD -> "навантаження";
            case BY_SPEED -> "швидкість";
            default -> "ім'я";
        };

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Список: " + title);
        alert.setHeaderText(title + "  (сортування: " + criteria + ")  — " + sorted.size() + " хліборобів");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefRowCount(10);
        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(true);
        alert.initOwner(app.primaryStage);
        alert.showAndWait();
    }

    void showCountInfo(String label, int count) {
        showInfo(label + ": " + count);
    }

    void showSortDialog(Stage owner) {
        Dialog<SortCriteria> dialog = new Dialog<>();
        dialog.setTitle("Критерій сортування");
        dialog.initOwner(owner);

        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        RadioButton byName = new RadioButton("За ім'ям");
        RadioButton byLoad = new RadioButton("За навантаженням (maxLoad)");
        RadioButton bySpeed = new RadioButton("За швидкістю (speed)");
        ToggleGroup group = new ToggleGroup();
        byName.setToggleGroup(group);
        byLoad.setToggleGroup(group);
        bySpeed.setToggleGroup(group);
        switch (app.sortCriteria) {
            case BY_LOAD -> byLoad.setSelected(true);
            case BY_SPEED -> bySpeed.setSelected(true);
            default -> byName.setSelected(true);
        }

        VBox box = new VBox(8, byName, byLoad, bySpeed);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(bt -> {
            if (bt != ok) return app.sortCriteria;
            if (byLoad.isSelected()) return SortCriteria.BY_LOAD;
            if (bySpeed.isSelected()) return SortCriteria.BY_SPEED;
            return SortCriteria.BY_NAME;
        });

        Optional<SortCriteria> result = dialog.showAndWait();
        result.ifPresent(sc -> {
            app.sortCriteria = sc;
            switch (sc) {
                case BY_LOAD -> app.village.sortByMaxLoadAnonymous();
                case BY_SPEED -> app.village.sortBySpeed();
                default -> app.village.sortByName();
            }
            String criteriaName = sc == SortCriteria.BY_LOAD ? "навантаженням"
                    : sc == SortCriteria.BY_SPEED ? "швидкістю" : "ім'ям";
            showSortedFarmersList(criteriaName);
        });
    }

    void showSortedFarmersList(String criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append("Відсортовано за: ").append(criteria).append("\n\n");
        if (app.village.getFarmers().isEmpty()) {
            sb.append("Список порожній.");
        } else {
            for (Farmer f : app.village.getFarmers()) {
                sb.append(String.format("%-14s [%s]  мотив: %d%%  maxLoad: %d  швидкість: %.1f  інструмент: %s%n",
                        f.getName(), f.getKind(), f.getMotivation(), f.getMaxLoad(),
                        f.getSpeed(), f.getTool().getType().displayName()));
            }
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Список хліборобів");
        alert.setHeaderText("Всі хлібороби (" + app.village.getFarmers().size() + ")");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefRowCount(15);
        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(true);
        alert.initOwner(app.primaryStage);
        alert.showAndWait();
    }

    void showSaveDialog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Зберегти гру");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстові файли (*.txt)", "*.txt"));
        File file = fc.showSaveDialog(app.primaryStage);
        if (file == null) return;
        try {
            GameSave save = new GameSave(app.village, app.cameraX, app.cameraY);
            save.saveToFile(file);
            showInfo("Збережено: " + file.getName());
        } catch (IOException ex) {
            showError("Помилка збереження: " + ex.getMessage());
        }
    }

    void showLoadDialog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Завантажити гру");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстові файли (*.txt)", "*.txt"));
        File file = fc.showOpenDialog(app.primaryStage);
        if (file == null) return;
        try {
            GameSave save = GameSave.loadFromFile(file);
            app.village.clearAll();
            app.village.loadFrom(save.village);
            app.cameraX = save.cameraX;
            app.cameraY = save.cameraY;
            app.logic.clampCamera();
            app.selectedFarmer = null;
            app.selectedMacro = null;
            showInfo("Завантажено: " + file.getName());
            app.renderer.redraw();
        } catch (IOException ex) {
            showError("Помилка завантаження: " + ex.getMessage());
        }
    }

    void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Про гру");
        alert.setHeaderText("Ancient Breadwinners");
        alert.initOwner(app.primaryStage);
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

    private void selectClosestMotivation(ListView<MotivationLevel> list, int value) {
        MotivationLevel best = null;
        int delta = Integer.MAX_VALUE;
        for (MotivationLevel ml : list.getItems()) {
            int d = Math.abs(ml.value() - value);
            if (d < delta) {
                delta = d;
                best = ml;
            }
        }
        if (best != null) list.getSelectionModel().select(best);
    }

    private record MotivationLevel(String label, int value) {
        @Override
        public String toString() {
            return label;
        }
    }
}


