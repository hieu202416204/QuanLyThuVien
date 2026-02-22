package FrontEnd.Views;

import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;

/**
 * VIEW: Chỉ vẽ giao diện Dashboard (Thẻ thống kê, Biểu đồ, Input).
 */
public class HomeTabView extends ScrollPane {

    // --- METRIC LABELS ---
    private final Label lblTotalBooks = new Label("...");
    private final Label lblTotalUsers = new Label("...");
    private final Label lblBorrowedBooks = new Label("...");
    private final Label lblAvailableBooks = new Label("...");
    private final Label lblTotalRevenue = new Label("...");

    // --- CHARTS ---
    private PieChart statusPieChart;
    private PieChart.Data dataAvailable;
    private PieChart.Data dataBorrowed;

    private BarChart<String, Number> topBooksChart;
    private NumberAxis yAxisTopBooks;

    private LineChart<String, Number> userGrowthChart;
    private PieChart returnRateChart;

    // --- VISIT COMPONENTS ---
    private BarChart<String, Number> visitChart;
    private ComboBox<String> cmbVisitViewType;
    private DatePicker datePickerVisit;
    private Label lblVisitTitle;

    // --- INVENTORY COMPONENTS ---
    private TextField txtActualCount;
    private Label lblSystemCountDisplay;
    private PieChart inventoryChart;
    private Button btnAnalyze;

    // --- SETTINGS BUTTON COMPONENTS ---
    private MenuItem itemRefresh;
    private RadioMenuItem r20, r40, r60;

    public HomeTabView() {
        initUI();
    }

    private void initUI() {
        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");
        this.setPannable(true);

        VBox mainLayout = new VBox(25);
        mainLayout.setPadding(new Insets(25, 25, 25, 25));

        mainLayout.getChildren().addAll(
                createMetricsCards(),
                createChartsRow1(),
                createChartsRow2(),
                createVisitStatsSection(),
                createInventorySection()
        );

        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: transparent;");
        rootPane.getChildren().add(mainLayout);

        MenuButton settingsBtn = createSettingsButton();
        rootPane.getChildren().add(settingsBtn);
        StackPane.setAlignment(settingsBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsBtn, new Insets(10, 25, 0, 0));

        this.setContent(rootPane);
    }

    private MenuButton createSettingsButton() {
        MenuButton btn = new MenuButton();

        SVGPath icon = new SVGPath();
        icon.setContent("M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.06-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.73,8.87 C2.62,9.08,2.66,9.34,2.86,9.49l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.06,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z");
        icon.setFill(Color.web("#546e7a"));
        icon.setScaleX(0.8); icon.setScaleY(0.8);

        StackPane iconContainer = new StackPane(icon);
        iconContainer.setPrefSize(24, 24);
        btn.setGraphic(iconContainer);
        btn.setStyle("-fx-background-color: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px; -fx-max-width: 50px; -fx-max-height: 50px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 2); -fx-cursor: hand; -fx-padding: 0;");
        btn.getStyleClass().add("floating-menu-btn");

        itemRefresh = new MenuItem("Refresh Now");
        Label refreshIcon = new Label("↻"); refreshIcon.setStyle("-fx-font-size: 14px;");
        itemRefresh.setGraphic(refreshIcon);
        itemRefresh.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9;");

        Menu menuAuto = new Menu("Auto Update Interval");
        Label timerIcon = new Label("⏱"); timerIcon.setStyle("-fx-font-size: 14px;");
        menuAuto.setGraphic(timerIcon);

        ToggleGroup group = new ToggleGroup();
        r20 = new RadioMenuItem("Every 1 minutes"); r20.setToggleGroup(group); r20.setSelected(true);
        r40 = new RadioMenuItem("Every 3 minutes"); r40.setToggleGroup(group);
        r60 = new RadioMenuItem("Every 5 minutes"); r60.setToggleGroup(group);

        menuAuto.getItems().addAll(r20, r40, r60);
        btn.getItems().addAll(itemRefresh, new SeparatorMenuItem(), menuAuto);

        return btn;
    }

    private HBox createMetricsCards() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                createCard(LanguageManager.getText("dash.total_books"), lblTotalBooks, "📚", "card-decoration-blue"),
                createCard(LanguageManager.getText("dash.available"), lblAvailableBooks, "✅", "card-decoration-green"),
                createCard(LanguageManager.getText("dash.borrowed"), lblBorrowedBooks, "📖", "card-decoration-orange"),
                createCard(LanguageManager.getText("dash.total_users"), lblTotalUsers, "👥", "card-decoration-red"),
                createCard("TOTAL MONEY", lblTotalRevenue, "💰", "card-decoration-purple")
        );
        return box;
    }

    private VBox createCard(String title, Label numberLabel, String iconEmoji, String decorationClass) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("dashboard-card", decorationClass);
        card.setMinWidth(180);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(iconEmoji);
        icon.getStyleClass().add("card-icon");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");
        lblTitle.setWrapText(true);
        header.getChildren().addAll(icon, lblTitle);
        numberLabel.getStyleClass().add("card-number");
        card.getChildren().addAll(header, numberLabel);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox createChartsRow1() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        statusPieChart = new PieChart();
        statusPieChart.setTitle(LanguageManager.getText("chart.status"));
        statusPieChart.setLabelsVisible(false);
        statusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        dataAvailable = new PieChart.Data(LanguageManager.getText("status.available"), 0);
        dataBorrowed = new PieChart.Data(LanguageManager.getText("status.borrowed"), 0);
        statusPieChart.getData().addAll(dataAvailable, dataBorrowed);

        CategoryAxis xAxis = new CategoryAxis();
        yAxisTopBooks = new NumberAxis();
        yAxisTopBooks.setLabel("Book");
        yAxisTopBooks.setTickUnit(1);
        yAxisTopBooks.setMinorTickVisible(false);
        yAxisTopBooks.setAutoRanging(false);
        yAxisTopBooks.setLowerBound(0);
        yAxisTopBooks.setUpperBound(10);

        yAxisTopBooks.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return (object.intValue() == object.doubleValue()) ? String.valueOf(object.intValue()) : "";
            }
            @Override
            public Number fromString(String string) { return Double.parseDouble(string); }
        });

        topBooksChart = new BarChart<>(xAxis, yAxisTopBooks);
        topBooksChart.setTitle(LanguageManager.getText("chart.top5"));
        topBooksChart.setLegendVisible(false);
        topBooksChart.setAnimated(false);

        box.getChildren().addAll(wrapChartInCard(statusPieChart), wrapChartInCard(topBooksChart));
        return box;
    }

    private HBox createChartsRow2() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        CategoryAxis dateAxis = new CategoryAxis();
        dateAxis.setLabel(LanguageManager.getText("axis.month"));
        NumberAxis countAxis = new NumberAxis();
        countAxis.setLabel(LanguageManager.getText("axis.users"));
        userGrowthChart = new LineChart<>(dateAxis, countAxis);
        userGrowthChart.setTitle(LanguageManager.getText("chart.growth"));
        userGrowthChart.setLegendVisible(false);
        userGrowthChart.setAnimated(false);

        returnRateChart = new PieChart();
        returnRateChart.setTitle(LanguageManager.getText("chart.return_rate"));
        returnRateChart.setLegendSide(javafx.geometry.Side.RIGHT);
        returnRateChart.setLabelsVisible(false);
        returnRateChart.setAnimated(false);

        box.getChildren().addAll(wrapChartInCard(userGrowthChart), wrapChartInCard(returnRateChart));
        return box;
    }

    private VBox wrapChartInCard(Chart chart) {
        VBox card = new VBox(chart);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(10));
        chart.setMinHeight(300);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createVisitStatsSection() {
        VBox container = new VBox(15);
        container.getStyleClass().add("dashboard-card");

        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📊"); icon.setStyle("-fx-font-size: 20px;");
        lblVisitTitle = new Label("Visitor Statistics");
        lblVisitTitle.getStyleClass().add("card-title");
        lblVisitTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        cmbVisitViewType = new ComboBox<>();
        cmbVisitViewType.getItems().addAll("Week View", "Month View", "Year View");
        cmbVisitViewType.setValue("Week View");

        datePickerVisit = new DatePicker(LocalDate.now());
        datePickerVisit.setPrefWidth(120);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(icon, lblVisitTitle, spacer, cmbVisitViewType, datePickerVisit);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setLabel("Visitors");

        visitChart = new BarChart<>(xAxis, yAxis);
        visitChart.setLegendVisible(false);
        visitChart.setAnimated(false);
        visitChart.setMinHeight(300);

        container.getChildren().addAll(headerBox, new Separator(), visitChart);
        return container;
    }

    private VBox createInventorySection() {
        VBox container = new VBox(15);
        container.getStyleClass().add("dashboard-card");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📦"); icon.setStyle("-fx-font-size: 20px;");
        Label title = new Label(LanguageManager.getText("title.inventory"));
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        headerBox.getChildren().addAll(icon, title);

        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        lblSystemCountDisplay = new Label(LanguageManager.getText("label.system_count") + " 0");
        lblSystemCountDisplay.getStyleClass().add("inventory-system-text");
        Label lblInput = new Label(LanguageManager.getText("label.actual_input"));
        lblInput.getStyleClass().add("input-label");

        txtActualCount = new TextField();
        txtActualCount.setPromptText("0");
        txtActualCount.setPrefWidth(100);
        txtActualCount.getStyleClass().add("modern-textfield");

        btnAnalyze = new Button(LanguageManager.getText("btn.analyze"));
        btnAnalyze.getStyleClass().addAll("action-btn", "btn-purple");
        btnAnalyze.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");

        controls.getChildren().addAll(lblSystemCountDisplay, new Separator(javafx.geometry.Orientation.VERTICAL), lblInput, txtActualCount, btnAnalyze);

        inventoryChart = new PieChart();
        inventoryChart.setTitle(LanguageManager.getText("chart.inventory"));
        inventoryChart.setLabelsVisible(true);
        inventoryChart.setPrefHeight(250);
        inventoryChart.setMaxHeight(250);
        inventoryChart.setAnimated(false);

        container.getChildren().addAll(headerBox, new Separator(), controls, inventoryChart);
        return container;
    }

    // ==========================================
    // GETTERS CHO CONTROLLER
    // ==========================================
    public Label getLblTotalBooks() { return lblTotalBooks; }
    public Label getLblTotalUsers() { return lblTotalUsers; }
    public Label getLblBorrowedBooks() { return lblBorrowedBooks; }
    public Label getLblAvailableBooks() { return lblAvailableBooks; }
    public Label getLblTotalRevenue() { return lblTotalRevenue; }
    public Label getLblVisitTitle() { return lblVisitTitle; }
    public Label getLblSystemCountDisplay() { return lblSystemCountDisplay; }

    public PieChart.Data getDataAvailable() { return dataAvailable; }
    public PieChart.Data getDataBorrowed() { return dataBorrowed; }

    public BarChart<String, Number> getTopBooksChart() { return topBooksChart; }
    public NumberAxis getYAxisTopBooks() { return yAxisTopBooks; }
    public LineChart<String, Number> getUserGrowthChart() { return userGrowthChart; }
    public PieChart getReturnRateChart() { return returnRateChart; }
    public BarChart<String, Number> getVisitChart() { return visitChart; }
    public PieChart getInventoryChart() { return inventoryChart; }

    public ComboBox<String> getCmbVisitViewType() { return cmbVisitViewType; }
    public DatePicker getDatePickerVisit() { return datePickerVisit; }
    public TextField getTxtActualCount() { return txtActualCount; }
    public Button getBtnAnalyze() { return btnAnalyze; }

    public MenuItem getItemRefresh() { return itemRefresh; }
    public RadioMenuItem getR20() { return r20; }
    public RadioMenuItem getR40() { return r40; }
    public RadioMenuItem getR60() { return r60; }
}