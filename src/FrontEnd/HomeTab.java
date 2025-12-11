package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

public class HomeTab extends ScrollPane {

    private final Library library;

    // Các Label hiển thị số liệu
    private final Label lblTotalBooks = new Label("0");
    private final Label lblTotalUsers = new Label("0");
    private final Label lblBorrowedBooks = new Label("0");
    private final Label lblAvailableBooks = new Label("0");
    private final Label lblTotalRevenue = new Label("0");

    // Các biểu đồ
    private PieChart statusPieChart;
    private BarChart<String, Number> topBooksChart;
    private LineChart<String, Number> userGrowthChart;
    private PieChart returnRateChart;

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
        VBox mainLayout = new VBox(25); // Tăng khoảng cách giữa các hàng
        mainLayout.setPadding(new Insets(25));

        // Tiêu đề Dashboard (Optional)
        Label dashTitle = new Label("Dashboard Overview");
        dashTitle.getStyleClass().add("page-title");

        // 1. Hàng Thẻ số liệu (Metrics Cards)
        HBox cardsBox = createMetricsCards();

        // 2. Hàng Biểu đồ 1
        HBox chartsBox1 = createChartsRow1();

        // 3. Hàng Biểu đồ 2
        HBox chartsBox2 = createChartsRow2();

        // 4. Kiểm kê
        VBox inventoryBox = createInventorySection();

        mainLayout.getChildren().addAll(
                // dashTitle, // Bỏ comment nếu muốn hiện tiêu đề to
                cardsBox,
                chartsBox1,
                chartsBox2,
                inventoryBox
        );

        this.setContent(mainLayout);

        // Load dữ liệu
        refreshData();
    }

    // =========================================================================
    // 1. METRICS CARDS (THẺ SỐ LIỆU - ĐẸP HƠN VỚI ICON)
    // =========================================================================
    private HBox createMetricsCards() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        // Thêm các thẻ với Icon + Màu sắc
        box.getChildren().addAll(
                createCard(LanguageManager.getText("dash.total_books"), lblTotalBooks, "📚", "card-decoration-blue"),
                createCard(LanguageManager.getText("dash.available"), lblAvailableBooks, "✅", "card-decoration-green"),
                createCard(LanguageManager.getText("dash.borrowed"), lblBorrowedBooks, "📖", "card-decoration-orange"),
                createCard(LanguageManager.getText("dash.total_users"), lblTotalUsers, "👥", "card-decoration-red"),
                createCard("DOANH THU", lblTotalRevenue, "💰", "card-decoration-purple")
        );
        return box;
    }

    private VBox createCard(String title, Label numberLabel, String iconEmoji, String decorationClass) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("dashboard-card", decorationClass);
        card.setMinWidth(180); // Đảm bảo độ rộng tối thiểu

        // Hàng trên: Icon + Tiêu đề
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconEmoji);
        icon.getStyleClass().add("card-icon");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");
        lblTitle.setWrapText(true); // Cho phép xuống dòng nếu tên dài

        header.getChildren().addAll(icon, lblTitle);

        // Số liệu
        numberLabel.getStyleClass().add("card-number");

        card.getChildren().addAll(header, numberLabel);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // =========================================================================
    // 2. CHART ROWS (ĐÓNG GÓI BIỂU ĐỒ VÀO WIDGET)
    // =========================================================================

    private HBox createChartsRow1() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        // A. PieChart Wrapper
        statusPieChart = new PieChart();
        statusPieChart.setTitle(LanguageManager.getText("chart.status"));
        statusPieChart.setLabelsVisible(true);
        statusPieChart.setAnimated(false); // Tắt animation để tránh lỗi chồng chữ
        statusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);

        // Init Data
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
        returnRateChart.setAnimated(false);

        VBox rateWrapper = wrapChartInCard(returnRateChart);

        box.getChildren().addAll(lineWrapper, rateWrapper);
        return box;
    }

    /**
     * Hàm phụ trợ: Đóng gói biểu đồ vào một thẻ trắng (Card) để đẹp hơn
     */
    private VBox wrapChartInCard(Chart chart) {
        VBox card = new VBox(chart);
        card.getStyleClass().add("dashboard-card"); // Tái sử dụng style thẻ trắng
        card.setPadding(new Insets(10));
        chart.setMinHeight(300); // Chiều cao cố định cho đẹp
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // =========================================================================
    // 3. INVENTORY (KIỂM KÊ)
    // =========================================================================
    private VBox createInventorySection() {
        VBox container = new VBox(15);
        container.getStyleClass().add("dashboard-card"); // Dùng style card cho đồng bộ

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📊"); icon.setStyle("-fx-font-size: 20px;");
        Label title = new Label(LanguageManager.getText("title.inventory"));
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50;"); // Override màu
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
        txtActualCount.getStyleClass().add("modern-textfield"); // Style đẹp

        Button btnAnalyze = new Button(LanguageManager.getText("btn.analyze"));
        btnAnalyze.getStyleClass().addAll("action-btn", "btn-purple");
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
    // 4. REFRESH DATA (LOGIC)
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

        // BarChart
        List<Book> topBooks = library.getBookDAO().getBooksSortedByBorrowCount();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lượt mượn");
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
            returnRateChart.getData().add(new PieChart.Data("Đúng hạn (" + onTime + ")", onTime));
            returnRateChart.getData().add(new PieChart.Data("Quá hạn (" + late + ")", late));
        }
    }

    private void handleAnalyzeInventory() {
        // Logic cũ giữ nguyên
        try {
            int systemCount = library.getBooks().size();
            String input = txtActualCount.getText().trim();
            if (input.isEmpty()) {
                // Alert...
                return;
            }
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