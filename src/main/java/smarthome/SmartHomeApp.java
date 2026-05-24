package smarthome;

import smarthome.database.SmartHomeDAO;
import smarthome.model.*;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class SmartHomeApp extends Application {

    private final SmartHomeDAO dao = new SmartHomeDAO();
    private String currentUserRole = null;
    private String currentUsername = null;

    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;

    private final ObservableList<Room> roomList = FXCollections.observableArrayList();
    private final ObservableList<Device> deviceList = FXCollections.observableArrayList();
    private final ObservableList<Event> eventList = FXCollections.observableArrayList();
    private final ObservableList<Scenario> scenarioList = FXCollections.observableArrayList();

    private TableView<Room> roomTable;
    private TableView<Device> deviceTable;
    private TableView<Event> eventTable;
    private TableView<Scenario> scenarioTable;

    private ListView<String> scenarioDevicesList;

    private ComboBox<Room> deviceRoomCombo;
    private ComboBox<String> deviceTypeCombo;
    private ComboBox<Device> scenarioDeviceCombo;

    // Форматтер для красивого отображения даты и времени
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("Система Умного Дома - Авторизация");

        createLoginScene();
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void createLoginScene() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f4f6f9;");

        Label titleLabel = new Label("Панель Умного Дома");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField usernameField = new TextField("admin");
        usernameField.setPromptText("Имя пользователя");
        usernameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setText("admin");
        passwordField.setPromptText("Пароль");
        passwordField.setMaxWidth(250);

        Button loginButton = new Button("Войти");
        loginButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        loginButton.setMinWidth(120);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");

        loginButton.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            try {
                String role = dao.authenticate(user, pass);
                if (role != null) {
                    currentUserRole = role;
                    currentUsername = user;
                    primaryStage.setTitle("Умный Дом - Панель управления [" + user + " (" + role + ")]");
                    createMainScene();
                    primaryStage.setScene(mainScene);
                    loadDataFromDatabase();
                } else {
                    errorLabel.setText("Неверный логин или пароль!");
                }
            } catch (SQLException ex) {
                showErrorAlert("Ошибка подключения к СУБД", "Проверьте настройки в config.properties и состояние базы данных.\n\n" + ex.getMessage());
            }
        });

        root.getChildren().addAll(titleLabel, usernameField, passwordField, loginButton, errorLabel);
        loginScene = new Scene(root, 400, 300);
    }

    private void createMainScene() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab roomsTab = new Tab("Комнаты", createRoomsLayout());
        Tab devicesTab = new Tab("Устройства", createDevicesLayout());
        Tab scenariosTab = new Tab("Сценарии автоматизации", createScenariosLayout());
        Tab eventsTab = new Tab("Журнал событий", createEventsLayout());

        tabPane.getTabs().addAll(roomsTab, devicesTab, scenariosTab, eventsTab);

        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1px 0px 0px 0px;");
        Label userLabel = new Label("Пользователь: " + currentUsername + " | Роль: " + currentUserRole);
        Button logoutBtn = new Button("Сменить аккаунт");
        logoutBtn.setOnAction(e -> {
            currentUserRole = null;
            currentUsername = null;
            primaryStage.setTitle("Система Умного Дома - Авторизация");
            primaryStage.setScene(loginScene);
        });
        statusBar.getChildren().addAll(userLabel, new Spacer(), logoutBtn);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(tabPane);
        mainLayout.setBottom(statusBar);

        mainScene = new Scene(mainLayout, 1100, 680);
    }

    private Pane createRoomsLayout() {
        roomTable = new TableView<>();
        VBox.setVgrow(roomTable, Priority.ALWAYS); // ИСПРАВЛЕНИЕ: таблица сжимается, оставляя место кнопкам

        TableColumn<Room, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        TableColumn<Room, String> nameCol = new TableColumn<>("Название комнаты");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Room, String> floorCol = new TableColumn<>("Этаж");
        floorCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getFloor())));

        roomTable.getColumns().addAll(idCol, nameCol, floorCol);
        roomTable.setItems(roomList);
        roomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("Название комнаты");
        Spinner<Integer> floorSpinner = new Spinner<>(1, 10, 1);

        Button addBtn = new Button("Добавить комнату");
        Button delBtn = new Button("Удалить выбранную");

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> loadDataFromDatabase());

        HBox controlBox = new HBox(10, roomNameField, new Label("Этаж:"), floorSpinner, addBtn, delBtn, refreshBtn);
        controlBox.setPadding(new Insets(10));
        controlBox.setAlignment(Pos.CENTER_LEFT);

        if (currentUserRole.equals("Guest")) {
            addBtn.setDisable(true);
            delBtn.setDisable(true);
            roomNameField.setDisable(true);
            floorSpinner.setDisable(true);
        } else if (currentUserRole.equals("User")) {
            delBtn.setDisable(true);
        }

        addBtn.setOnAction(e -> {
            String name = roomNameField.getText().trim();
            if (!name.isEmpty()) {
                try {
                    dao.addRoom(name, floorSpinner.getValue(), currentUsername);
                    roomNameField.clear();
                    loadDataFromDatabase();
                } catch (SQLException ex) {
                    showErrorAlert("Ошибка добавления комнаты", ex.getMessage());
                }
            }
        });

        delBtn.setOnAction(e -> {
            Room selected = roomTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Подтверждение удаления комнаты");
                alert.setHeaderText("Вы действительно хотите удалить комнату '" + selected.getName() + "'?");
                alert.setContentText("ВНИМАНИЕ: Все устройства, привязанные к этой комнате, останутся без места расположения!\nОни будут подсвечены красным цветом.");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        dao.deleteRoom(selected.getId(), selected.getName(), currentUsername);
                        loadDataFromDatabase();
                    } catch (SQLException ex) {
                        showErrorAlert("Ошибка удаления", ex.getMessage());
                    }
                }
            }
        });

        VBox layout = new VBox(10, roomTable, controlBox);
        layout.setPadding(new Insets(10));
        return layout;
    }

    private Pane createDevicesLayout() {
        deviceTable = new TableView<>();
        VBox.setVgrow(deviceTable, Priority.ALWAYS); // ИСПРАВЛЕНИЕ: таблица сжимается, оставляя место кнопкам

        TableColumn<Device, String> nameCol = new TableColumn<>("Имя устройства");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Device, String> roomCol = new TableColumn<>("Комната");
        roomCol.setCellValueFactory(data -> {
            String rName = data.getValue().getRoomName();
            return new SimpleStringProperty(rName == null ? "БЕЗ КОМНАТЫ (Требуется настройка)" : rName);
        });

        TableColumn<Device, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTypeName()));

        TableColumn<Device, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Device, String> ipCol = new TableColumn<>("IP адрес");
        ipCol.setCellValueFactory(data -> {
            String ip = data.getValue().getIpAddress();
            return new SimpleStringProperty(ip == null ? "-" : ip);
        });

        deviceTable.getColumns().addAll(nameCol, roomCol, typeCol, statusCol, ipCol);
        deviceTable.setItems(deviceList);
        deviceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        deviceTable.setRowFactory(tv -> new TableRow<Device>() {
            @Override
            protected void updateItem(Device device, boolean empty) {
                super.updateItem(device, empty);
                if (empty || device == null) {
                    setStyle("");
                } else if (device.getRoomName() == null) {
                    setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #990000; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        TextField devName = new TextField();
        devName.setPromptText("Название девайса");
        devName.setPrefWidth(120);

        deviceRoomCombo = new ComboBox<>();
        deviceRoomCombo.setPromptText("Комната");

        deviceTypeCombo = new ComboBox<>();
        deviceTypeCombo.setPromptText("Тип");

        // КНОПКА ДОБАВЛЕНИЯ НОВОГО ТИПА УСТРОЙСТВА
        Button addTypeBtn = new Button("+");
        addTypeBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        addTypeBtn.setTooltip(new Tooltip("Создать новый тип устройства"));
        addTypeBtn.setOnAction(e -> showAddDeviceTypeDialog());

        HBox typeBox = new HBox(3, deviceTypeCombo, addTypeBtn);
        typeBox.setAlignment(Pos.CENTER_LEFT);

        TextField ipField = new TextField();
        ipField.setPromptText("IP-адрес (IPv4)");
        ipField.setPrefWidth(120);

        // МАСКА ДЛЯ IP: Разрешаем только цифры и точки
        ipField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("[0-9\\.]*")) {
                return change;
            }
            return null;
        }));

        Button addBtn = new Button("Добавить");
        Button delBtn = new Button("Удалить");

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> loadDataFromDatabase());

        HBox controlBox = new HBox(8, devName, deviceRoomCombo, typeBox, ipField, addBtn, delBtn, refreshBtn);
        controlBox.setPadding(new Insets(10));
        controlBox.setAlignment(Pos.CENTER_LEFT);

        if (currentUserRole.equals("Guest")) {
            devName.setDisable(true);
            deviceRoomCombo.setDisable(true);
            deviceTypeCombo.setDisable(true);
            addTypeBtn.setDisable(true);
            ipField.setDisable(true);
            addBtn.setDisable(true);
            delBtn.setDisable(true);
        } else if (currentUserRole.equals("User")) {
            delBtn.setDisable(true);
        }

        addBtn.setOnAction(e -> {
            String name = devName.getText().trim();
            Room room = deviceRoomCombo.getValue();
            String typeStr = deviceTypeCombo.getValue();
            String ip = ipField.getText().trim();

            if (name.isEmpty() || room == null || typeStr == null) {
                showErrorAlert("Ошибка", "Заполните имя, выберите комнату и тип устройства!");
                return;
            }

            // ВАЛИДАЦИЯ ПРАВИЛЬНОСТИ IPv4
            String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
            if (!ip.isEmpty() && !ip.matches(ipRegex)) {
                showErrorAlert("Неверный формат IP", "Введите корректный IPv4-адрес (например, 192.168.1.15) или оставьте поле пустым.");
                return;
            }

            int typeId = Integer.parseInt(typeStr.split(" - ")[0]);
            try {
                dao.addDevice(name, room.getId(), typeId, "Online", ip.isEmpty() ? null : ip, currentUsername);
                devName.clear();
                ipField.clear();
                loadDataFromDatabase();
            } catch (SQLException ex) {
                showErrorAlert("Ошибка добавления девайса", "Проверьте уникальность IP-адреса.\n\nДетали: " + ex.getMessage());
            }
        });

        delBtn.setOnAction(e -> {
            Device selected = deviceTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    dao.deleteDevice(selected.getId(), selected.getName(), currentUsername);
                    loadDataFromDatabase();
                } catch (SQLException ex) {
                    showErrorAlert("Ошибка удаления устройства", ex.getMessage());
                }
            }
        });

        VBox layout = new VBox(10, deviceTable, controlBox);
        layout.setPadding(new Insets(10));
        return layout;
    }

    // ДИАЛОГОВОЕ ОКНО ДЛЯ ДОБАВЛЕНИЯ НОВОГО ТИПА УСТРОЙСТВА
    private void showAddDeviceTypeDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавление типа устройства");
        dialog.setHeaderText("Создать новый тип (Датчик, Камера, Розетка и т.д.)");

        ButtonType saveBtnType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField typeName = new TextField();
        typeName.setPromptText("Название (Умная колонка)");

        TextField manufacturer = new TextField();
        manufacturer.setPromptText("Производитель");

        ComboBox<String> protocol = new ComboBox<>(FXCollections.observableArrayList("Wi-Fi", "Zigbee", "Z-Wave", "Bluetooth"));
        protocol.setValue("Wi-Fi");

        grid.add(new Label("Название типа:"), 0, 0);
        grid.add(typeName, 1, 0);
        grid.add(new Label("Производитель:"), 0, 1);
        grid.add(manufacturer, 1, 1);
        grid.add(new Label("Протокол:"), 0, 2);
        grid.add(protocol, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtnType) {
            if (!typeName.getText().trim().isEmpty()) {
                try {
                    dao.addDeviceType(typeName.getText().trim(), manufacturer.getText().trim(), protocol.getValue(), currentUsername);
                    loadDataFromDatabase();
                } catch (SQLException ex) {
                    showErrorAlert("Ошибка сохранения", ex.getMessage());
                }
            } else {
                showErrorAlert("Ошибка", "Поле 'Название типа' не может быть пустым.");
            }
        }
    }

    private Pane createScenariosLayout() {
        scenarioTable = new TableView<>();
        VBox.setVgrow(scenarioTable, Priority.ALWAYS); // ИСПРАВЛЕНИЕ: таблица сжимается

        TableColumn<Scenario, String> nameCol = new TableColumn<>("Сценарий");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Scenario, String> activeCol = new TableColumn<>("Активность");
        activeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Да" : "Нет"));

        TableColumn<Scenario, String> dateCol = new TableColumn<>("Создан");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt().toLocalDateTime().format(timeFormatter)));

        scenarioTable.getColumns().addAll(nameCol, activeCol, dateCol);
        scenarioTable.setItems(scenarioList);
        scenarioTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox rightSide = new VBox(10);
        rightSide.setPadding(new Insets(10));
        rightSide.setPrefWidth(350);
        rightSide.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 0px 0px 0px 1px;");

        Label rightTitle = new Label("Устройства в выбранном сценарии:");
        rightTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        scenarioDevicesList = new ListView<>();
        VBox.setVgrow(scenarioDevicesList, Priority.ALWAYS); // ИСПРАВЛЕНИЕ

        Label addDeviceTitle = new Label("Добавить устройство к сценарию:");
        addDeviceTitle.setStyle("-fx-font-weight: bold;");

        scenarioDeviceCombo = new ComboBox<>();
        scenarioDeviceCombo.setPromptText("Выберите устройство");
        scenarioDeviceCombo.setMaxWidth(Double.MAX_VALUE);

        TextField targetActionField = new TextField();
        targetActionField.setPromptText("Действие (например, Turn Off)");

        Button linkBtn = new Button("Добавить девайс в сценарий");
        linkBtn.setMaxWidth(Double.MAX_VALUE);
        linkBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        rightSide.getChildren().addAll(rightTitle, scenarioDevicesList, new Separator(), addDeviceTitle, scenarioDeviceCombo, targetActionField, linkBtn);

        scenarioTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            updateLinkedDevicesList(newSel);
        });

        TextField scName = new TextField();
        scName.setPromptText("Название сценария");

        CheckBox activeCheck = new CheckBox("Активен");
        activeCheck.setSelected(true);

        Button addBtn = new Button("Создать сценарий");
        Button toggleBtn = new Button("Вкл/Выкл");
        Button delBtn = new Button("Удалить");

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> loadDataFromDatabase());

        HBox controlBox = new HBox(10, scName, activeCheck, addBtn, toggleBtn, delBtn, refreshBtn);
        controlBox.setPadding(new Insets(10));
        controlBox.setAlignment(Pos.CENTER_LEFT);

        if (currentUserRole.equals("Guest")) {
            scName.setDisable(true);
            activeCheck.setDisable(true);
            addBtn.setDisable(true);
            toggleBtn.setDisable(true);
            delBtn.setDisable(true);
            rightSide.setDisable(true);
        }

        linkBtn.setOnAction(e -> {
            Scenario selectedScenario = scenarioTable.getSelectionModel().getSelectedItem();
            Device selectedDevice = scenarioDeviceCombo.getValue();
            String targetState = targetActionField.getText().trim();

            if (selectedScenario == null) {
                showErrorAlert("Сценарий не выбран", "Пожалуйста, сначала выберите нужный сценарий в левой таблице!");
                return;
            }
            if (selectedDevice == null || targetState.isEmpty()) {
                showErrorAlert("Заполните поля", "Выберите устройство и укажите целевое действие!");
                return;
            }

            try {
                dao.addDeviceToScenario(selectedScenario.getId(), selectedDevice.getId(), targetState, currentUsername);
                targetActionField.clear();
                updateLinkedDevicesList(selectedScenario);
                loadDataFromDatabase();
            } catch (SQLException ex) {
                showErrorAlert("Ошибка связывания", ex.getMessage());
            }
        });

        addBtn.setOnAction(e -> {
            String name = scName.getText().trim();
            if (!name.isEmpty()) {
                try {
                    dao.addScenario(name, activeCheck.isSelected(), currentUsername);
                    scName.clear();
                    loadDataFromDatabase();
                } catch (SQLException ex) {
                    showErrorAlert("Ошибка создания сценария", ex.getMessage());
                }
            }
        });

        toggleBtn.setOnAction(e -> {
            Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    dao.toggleScenarioActive(selected.getId(), selected.getName(), !selected.isActive(), currentUsername);
                    loadDataFromDatabase();
                    scenarioTable.getSelectionModel().select(selected);
                } catch (SQLException ex) {
                    showErrorAlert("Ошибка изменения статуса", ex.getMessage());
                }
            }
        });

        delBtn.setOnAction(e -> {
            Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    dao.deleteScenario(selected.getId(), selected.getName(), currentUsername);
                    loadDataFromDatabase();
                } catch (SQLException ex) {
                    showErrorAlert("Ошибка удаления сценария", ex.getMessage());
                }
            }
        });

        VBox leftSide = new VBox(10, scenarioTable, controlBox);
        leftSide.setPadding(new Insets(10));
        HBox.setHgrow(leftSide, Priority.ALWAYS);

        HBox layout = new HBox(leftSide, rightSide);
        return layout;
    }

    private void updateLinkedDevicesList(Scenario scenario) {
        if (scenario == null) {
            scenarioDevicesList.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            List<String> linked = dao.getDevicesInScenario(scenario.getId());
            scenarioDevicesList.setItems(FXCollections.observableArrayList(linked));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private Pane createEventsLayout() {
        eventTable = new TableView<>();
        VBox.setVgrow(eventTable, Priority.ALWAYS); // ИСПРАВЛЕНИЕ: таблица сжимается

        // КРАСИВОЕ ФОРМАТИРОВАНИЕ ДАТЫ И ВРЕМЕНИ
        TableColumn<Event, String> dateCol = new TableColumn<>("Время срабатывания");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp().toLocalDateTime().format(timeFormatter)));

        TableColumn<Event, String> devCol = new TableColumn<>("Связанное устройство");
        devCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeviceName()));

        TableColumn<Event, String> typeCol = new TableColumn<>("Тип события");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventType()));

        TableColumn<Event, String> descCol = new TableColumn<>("Описание / Инициатор");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        eventTable.getColumns().addAll(dateCol, devCol, typeCol, descCol);
        eventTable.setItems(eventList);
        eventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button refreshBtn = new Button("Обновить журнал");
        refreshBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> {
            try {
                eventList.setAll(dao.getAllEvents());
            } catch (SQLException ex) {
                showErrorAlert("Ошибка обновления событий", ex.getMessage());
            }
        });

        HBox controlBox = new HBox(refreshBtn);
        controlBox.setPadding(new Insets(10));

        VBox layout = new VBox(10, eventTable, controlBox);
        layout.setPadding(new Insets(10));
        return layout;
    }

    private void loadDataFromDatabase() {
        try {
            List<Room> allRooms = dao.getAllRooms();
            List<Device> allDevices = dao.getAllDevices();

            roomList.setAll(allRooms);
            deviceList.setAll(allDevices);
            scenarioList.setAll(dao.getAllScenarios());
            eventList.setAll(dao.getAllEvents());

            if (deviceRoomCombo != null) {
                Room currentSelectedRoom = deviceRoomCombo.getValue();
                deviceRoomCombo.setItems(FXCollections.observableArrayList(allRooms));
                if (currentSelectedRoom != null) {
                    deviceRoomCombo.getSelectionModel().select(
                            allRooms.stream().filter(r -> r.getId() == currentSelectedRoom.getId()).findFirst().orElse(null)
                    );
                }
            }

            if (deviceTypeCombo != null) {
                deviceTypeCombo.setItems(FXCollections.observableArrayList(dao.getDeviceTypesList()));
            }

            if (scenarioDeviceCombo != null) {
                scenarioDeviceCombo.setItems(FXCollections.observableArrayList(allDevices));
            }

            Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
            updateLinkedDevicesList(selected);

        } catch (SQLException ex) {
            showErrorAlert("Ошибка загрузки данных", ex.getMessage());
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Системная ошибка");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(400);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class Spacer extends Region {
        public Spacer() {
            HBox.setHgrow(this, Priority.ALWAYS);
        }
    }
}