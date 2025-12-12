package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import Database.VisitDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class HomeTab extends ScrollPane {

    private final Library library;

    // --- MỚI: DAO CHO VISIT ---
    private final VisitDAO visitDAO = new VisitDAO();

    // Các Label hiển thị số liệu
    private final Label lblTotalBooks = new Label("0");
    private final Label lblTotalUsers = new Label("0");
    private final Label lblBorrowedBooks = new Label("0");
    private final Label lblAvailableBooks = new Label("0");
    private final Label lblTotalRevenue = new Label("0");

    // Các biểu đồ cũ
    private PieChart statusPieChart;
    private BarChart<String, Number> topBooksChart;
    private LineChart<String, Number> userGrowthChart;
    private PieChart returnRateChart;

    // --- MỚI: BIỂU ĐỒ VISIT ---
    private BarChart<String, Number> visitChart;
    private ComboBox<String> cmbVisitViewType;
    private DatePicker datePickerVisit;
    private Label lblVisitTitle;

    // Dữ liệu PieChart (Lưu lại để update value thay vì tạo mới)
    private PieChart.Data dataAvailable;
    private PieChart.Data dataBorrowed;

    // Phần kiểm kê
    private TextField txtActualCount;
    private Label lblSystemCountDisplay;
    private PieChart inventoryChart;

    public HomeTab(Library library) {
        this.library = library;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");
        this.setPannable(true);

        // Layout chính
        VBox mainLayout = new VBox(25);
        mainLayout.setPadding(new Insets(25));

        // 1. Hàng Thẻ số liệu (Metrics Cards)
        HBox cardsBox = createMetricsCards();

        // 2. Hàng Biểu đồ 1 (Trạng thái + Top sách)
        HBox chartsBox1 = createChartsRow1();

        // 3. Hàng Biểu đồ 2 (Tăng trưởng User + Tỷ lệ trả)
        HBox chartsBox2 = createChartsRow2();

        // 4. --- MỚI: PHẦN THỐNG KÊ LƯỢT VÀO ---
        VBox visitBox = createVisitStatsSection();

        // 5. Kiểm kê
        VBox inventoryBox = createInventorySection();

        mainLayout.getChildren().addAll(
                cardsBox,
                chartsBox1,
                chartsBox2,
                visitBox,       // Đặt phần Visit ở đây
                inventoryBox
        );

        this.setContent(mainLayout);

        // Load dữ liệu
        Platform.runLater(() -> {
            refreshData();
            updateVisitChart(); // Load biểu đồ Visit lần đầu
        });
    }

    // =========================================================================
    // 1. METRICS CARDS
    // =========================================================================
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

    // =========================================================================
    // 2. CHART ROWS
    // =========================================================================

    private HBox createChartsRow1() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        // A. PieChart Wrapper
        statusPieChart = new PieChart();
        statusPieChart.setTitle(LanguageManager.getText("chart.status"));
        statusPieChart.setLabelsVisible(false);
        statusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);

        dataAvailable = new PieChart.Data(LanguageManager.getText("status.available"), 0);
        dataBorrowed = new PieChart.Data(LanguageManager.getText("status.borrowed"), 0);
        statusPieChart.setData(FXCollections.observableArrayList(dataAvailable, dataBorrowed));

        VBox pieWrapper = wrapChartInCard(statusPieChart);

        // B. BarChart Wrapper
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        topBooksChart = new BarChart<>(xAxis, yAxis);
        topBooksChart.setTitle(LanguageManager.getText("chart.top5"));
        topBooksChart.setLegendVisible(false);
        topBooksChart.setAnimated(false);

        VBox barWrapper = wrapChartInCard(topBooksChart);

        box.getChildren().addAll(pieWrapper, barWrapper);
        return box;
    }

    private HBox createChartsRow2() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        // C. LineChart
        CategoryAxis dateAxis = new CategoryAxis();
        dateAxis.setLabel(LanguageManager.getText("axis.month"));
        NumberAxis countAxis = new NumberAxis();
        countAxis.setLabel(LanguageManager.getText("axis.users"));

        userGrowthChart = new LineChart<>(dateAxis, countAxis);
        userGrowthChart.setTitle(LanguageManager.getText("chart.growth"));
        userGrowthChart.setLegendVisible(false);
        userGrowthChart.setAnimated(false);

        VBox lineWrapper = wrapChartInCard(userGrowthChart);

        // D. Return Rate PieChart
        returnRateChart = new PieChart();
        returnRateChart.setTitle(LanguageManager.getText("chart.return_rate"));
        returnRateChart.setLegendSide(javafx.geometry.Side.RIGHT);
        returnRateChart.setLabelsVisible(false);
        returnRateChart.setAnimated(false);

        VBox rateWrapper = wrapChartInCard(returnRateChart);

        box.getChildren().addAll(lineWrapper, rateWrapper);
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

    // =========================================================================
    // 3. --- MỚI: VISITOR STATISTICS SECTION ---
    // =========================================================================
    private VBox createVisitStatsSection() {
        VBox container = new VBox(15);
        container.getStyleClass().add("dashboard-card"); // Style đồng bộ

        // --- Header: Icon + Title + Controls ---
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("📊");
        icon.setStyle("-fx-font-size: 20px;");

        lblVisitTitle = new Label("Visitor Statistics");
        lblVisitTitle.getStyleClass().add("card-title");
        lblVisitTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        // Controls
        cmbVisitViewType = new ComboBox<>();
        cmbVisitViewType.setItems(FXCollections.observableArrayList("Week View", "Month View", "Year View"));
        cmbVisitViewType.setValue("Week View");
        cmbVisitViewType.setOnAction(e -> updateVisitChart());

        datePickerVisit = new DatePicker(LocalDate.now());
        datePickerVisit.setPrefWidth(120);
        datePickerVisit.setOnAction(e -> updateVisitChart());

        // Spacer để đẩy Controls sang phải
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(icon, lblVisitTitle, spacer, cmbVisitViewType, datePickerVisit);

        // --- Chart ---
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Visitors");

        visitChart = new BarChart<>(xAxis, yAxis);
        visitChart.setLegendVisible(false);
        visitChart.setAnimated(false);
        visitChart.setMinHeight(300);

        container.getChildren().addAll(headerBox, new Separator(), visitChart);
        return container;
    }

    private void updateVisitChart() {
        String viewType = cmbVisitViewType.getValue();
        LocalDate selectedDate = datePickerVisit.getValue();
        if (selectedDate == null) selectedDate = LocalDate.now();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Visitors");

        if ("Week View".equals(viewType)) {
            LocalDate start = selectedDate.with(DayOfWeek.MONDAY);
            LocalDate end = selectedDate.with(DayOfWeek.SUNDAY);
            lblVisitTitle.setText("Visitors: " + start + " to " + end);

            Map<String, Integer> data = visitDAO.getVisitsInDateRange(start, end);
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                String label = d.getDayOfWeek().toString().substring(0,3) + "\n" + d.format(DateTimeFormatter.ofPattern("dd/MM"));
                series.getData().add(new XYChart.Data<>(label, data.getOrDefault(d.toString(), 0)));
            }

        } else if ("Month View".equals(viewType)) {
            YearMonth ym = YearMonth.from(selectedDate);
            lblVisitTitle.setText("Visitors: " + ym.getMonth() + " " + ym.getYear());

            Map<String, Integer> data = visitDAO.getVisitsInDateRange(ym.atDay(1), ym.atEndOfMonth());
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                String key = ym.atDay(day).toString();
                series.getData().add(new XYChart.Data<>(String.valueOf(day), data.getOrDefault(key, 0)));
            }

        } else if ("Year View".equals(viewType)) {
            int year = selectedDate.getYear();
            lblVisitTitle.setText("Visitors in " + year);

            Map<Integer, Integer> data = visitDAO.getVisitsByMonthInYear(year);
            for (int m = 1; m <= 12; m++) {
                series.getData().add(new XYChart.Data<>("M" + m, data.getOrDefault(m, 0)));
            }
        }

        visitChart.getData().clear();
        visitChart.getData().add(series);
    }

    // =========================================================================
    // 4. INVENTORY (KIỂM KÊ)
    // =========================================================================
    private VBox createInventorySection() {
        VBox container = new VBox(15);
        container.getStyleClass().add("dashboard-card");

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📦"); icon.setStyle("-fx-font-size: 20px;");
        Label title = new Label(LanguageManager.getText("title.inventory"));
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        headerBox.getChildren().addAll(icon, title);

        // Controls
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

        Button btnAnalyze = new Button(LanguageManager.getText("btn.analyze"));
        btnAnalyze.getStyleClass().addAll("action-btn", "btn-purple");
        btnAnalyze.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;"); // Fix màu trực tiếp
        btnAnalyze.setOnAction(e -> handleAnalyzeInventory());

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

    // =========================================================================
    // 5. REFRESH DATA (LOGIC)
    // =========================================================================
    public void refreshData() {
        List<Book> allBooks = library.getBooks();
        int totalBooks = allBooks.size();
        int totalUsers = library.getListUsers().size();

        int availableCount = 0;
        int borrowedCount = 0;
        for (Book b : allBooks) {
            if (b.isStatus()) availableCount++;
            else borrowedCount++;
        }

        // Labels
        lblTotalBooks.setText(String.valueOf(totalBooks));
        lblTotalUsers.setText(String.valueOf(totalUsers));
        lblAvailableBooks.setText(String.valueOf(availableCount));
        lblBorrowedBooks.setText(String.valueOf(borrowedCount));

        if (lblSystemCountDisplay != null) {
            lblSystemCountDisplay.setText(LanguageManager.getText("label.system_count") + " " + totalBooks);
        }

        // Doanh thu
        double revenue = library.getFinancialDAO().getTotalRevenue();
        String currency = library.getSettingsDAO().getSetting("currency_unit");
        if (currency.isEmpty()) currency = "VNĐ";
        lblTotalRevenue.setText(String.format("%,.0f %s", revenue, currency));

        // PieChart 1 (Update Value)
        dataAvailable.setPieValue(availableCount);
        dataBorrowed.setPieValue(borrowedCount);
        dataAvailable.setName(LanguageManager.getText("status.available") + " (" + availableCount + ")");
        dataBorrowed.setName(LanguageManager.getText("status.borrowed") + " (" + borrowedCount + ")");

        // BarChart Top Books
        List<Book> topBooks = library.getBookDAO().getBooksSortedByBorrowCount();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Number of Loans: ");
        int limit = Math.min(5, topBooks.size());
        for (int i = 0; i < limit; i++) {
            Book b = topBooks.get(i);
            if (b.getSoLuotMuon() > 0) {
                String shortName = b.getName().length() > 15 ? b.getName().substring(0, 12) + "..." : b.getName();
                series.getData().add(new XYChart.Data<>(shortName, b.getSoLuotMuon()));
            }
        }
        topBooksChart.getData().clear();
        topBooksChart.getData().add(series);

        // LineChart (User Growth)
        Map<String, Integer> userStats = library.getUserDAO().getUserGrowthStats();
        XYChart.Series<String, Number> seriesUser = new XYChart.Series<>();
        seriesUser.setName("Users");
        int totalSoFar = 0;
        for (Map.Entry<String, Integer> entry : userStats.entrySet()) {
            totalSoFar += entry.getValue();
            seriesUser.getData().add(new XYChart.Data<>(entry.getKey(), totalSoFar));
        }
        userGrowthChart.getData().clear();
        userGrowthChart.getData().add(seriesUser);

        // PieChart 2 (Return Rate)
        String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
        int maxDays = maxDaysStr.isEmpty() ? 60 : Integer.parseInt(maxDaysStr);
        List<long[]> returns = library.getTransactionDAO().getReturnDurations();
        int onTime = 0;
        int late = 0;
        for (long[] r : returns) {
            if (r[0] <= maxDays) onTime++;
            else late++;
        }

        returnRateChart.getData().clear();
        if (!returns.isEmpty()) {
            returnRateChart.getData().add(new PieChart.Data("On time (" + onTime + ")", onTime));
            returnRateChart.getData().add(new PieChart.Data("Out time (" + late + ")", late));
        }
    }

    private void handleAnalyzeInventory() {
        try {
            int systemCount = library.getBooks().size();
            String input = txtActualCount.getText().trim();
            if (input.isEmpty()) return;

            int actualCount = Integer.parseInt(input);
            int diff = systemCount - actualCount;

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            if (diff > 0) {
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.existing"), actualCount));
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.lost"), diff));
            } else if (diff < 0) {
                pieData.add(new PieChart.Data(LanguageManager.getText("label.system_count"), systemCount));
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.surplus"), Math.abs(diff)));
            } else {
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.existing"), actualCount));
            }
            inventoryChart.setData(pieData);
            String statusText = (diff == 0 ? "OK" : (diff > 0 ? "-" + diff : "+" + Math.abs(diff)));
            inventoryChart.setTitle(LanguageManager.getText("chart.inventory") + " (" + statusText + ")");
        } catch (Exception e) {}
    }
}