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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class HomeTab extends ScrollPane {

    private final Library library;

    // --- CÁC COMPONENT HIỂN THỊ SỐ LIỆU ---
    private final Label lblTotalBooks = new Label("0");
    private final Label lblTotalUsers = new Label("0");
    private final Label lblBorrowedBooks = new Label("0");
    private final Label lblAvailableBooks = new Label("0");

    // --- CÁC BIỂU ĐỒ ---
    // Hàng 1
    private PieChart statusPieChart;
    private BarChart<String, Number> topBooksChart;
    // Hàng 2 (Mới)
    private LineChart<String, Number> userGrowthChart;
    private PieChart returnRateChart;

    // --- PHẦN KIỂM KÊ (SMART INVENTORY) ---
    private TextField txtActualCount;
    private Label lblSystemCountDisplay;
    private PieChart inventoryChart;

    public HomeTab(Library library) {
        this.library = library;

        // Cấu hình ScrollPane
        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");
        this.setPannable(true); // Cho phép kéo chuột để cuộn

        // Layout chính (VBox chứa tất cả)
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: transparent;");

        // 1. Hàng Thẻ số liệu (Metrics Cards)
        HBox cardsBox = createMetricsCards();

        // 2. Hàng Biểu đồ 1 (Trạng thái sách + Top sách)
        HBox chartsBox1 = createChartsRow1();

        // 3. Hàng Biểu đồ 2 (Tăng trưởng User + Tỷ lệ Trả đúng hạn)
        HBox chartsBox2 = createChartsRow2();

        // 4. Khu vực Kiểm kê sách (Inventory)
        VBox inventoryBox = createInventorySection();

        // Thêm tất cả vào Layout
        mainLayout.getChildren().addAll(
                cardsBox,
                chartsBox1,
                new Separator(),
                chartsBox2,
                new Separator(),
                inventoryBox
        );

        this.setContent(mainLayout);

        // Tải dữ liệu lần đầu
        refreshData();
    }

    // =========================================================================
    // PHẦN 1: TẠO GIAO DIỆN (UI CREATION)
    // =========================================================================

    private HBox createMetricsCards() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        // Tạo 4 thẻ màu sắc
        box.getChildren().addAll(
                createCard(LanguageManager.getText("dash.total_books"), lblTotalBooks, "card-blue"),
                createCard(LanguageManager.getText("dash.available"), lblAvailableBooks, "card-green"),
                createCard(LanguageManager.getText("dash.borrowed"), lblBorrowedBooks, "card-orange"),
                createCard(LanguageManager.getText("dash.total_users"), lblTotalUsers, "card-red")
        );
        return box;
    }

    private VBox createCard(String title, Label numberLabel, String styleClass) {
        VBox card = new VBox(5);
        card.getStyleClass().addAll("dashboard-card", styleClass);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");

        numberLabel.getStyleClass().add("card-number");

        card.getChildren().addAll(lblTitle, numberLabel);
        HBox.setHgrow(card, Priority.ALWAYS); // Tự co giãn
        return card;
    }

    private HBox createChartsRow1() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);
        VBox.setVgrow(box, Priority.ALWAYS);

        // A. PieChart: Trạng thái Sách
        statusPieChart = new PieChart();
        statusPieChart.setTitle(LanguageManager.getText("chart.status"));
        statusPieChart.setLabelsVisible(true);
        // statusPieChart.setLegendVisible(false); // Tùy chọn tắt chú thích

        // B. BarChart: Top Sách Hot
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        topBooksChart = new BarChart<>(xAxis, yAxis);
        topBooksChart.setTitle(LanguageManager.getText("chart.top5"));
        topBooksChart.setLegendVisible(false);
        topBooksChart.setAnimated(false); // Tắt hiệu ứng để load nhanh hơn

        HBox.setHgrow(statusPieChart, Priority.ALWAYS);
        HBox.setHgrow(topBooksChart, Priority.ALWAYS);

        box.getChildren().addAll(statusPieChart, topBooksChart);
        return box;
    }

    private HBox createChartsRow2() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        // C. LineChart: Tăng trưởng Người dùng
        CategoryAxis dateAxis = new CategoryAxis();
        dateAxis.setLabel(LanguageManager.getText("axis.month"));
        NumberAxis countAxis = new NumberAxis();
        countAxis.setLabel(LanguageManager.getText("axis.users"));

        userGrowthChart = new LineChart<>(dateAxis, countAxis);
        userGrowthChart.setTitle(LanguageManager.getText("chart.growth"));
        userGrowthChart.setLegendVisible(false);

        // D. PieChart: Tỷ lệ Trả sách
        returnRateChart = new PieChart();
        returnRateChart.setTitle(LanguageManager.getText("chart.return_rate"));

        HBox.setHgrow(userGrowthChart, Priority.ALWAYS);
        HBox.setHgrow(returnRateChart, Priority.ALWAYS);

        box.getChildren().addAll(userGrowthChart, returnRateChart);
        return box;
    }
    private VBox createInventorySection() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Tiêu đề: Lấy từ từ điển
        Label title = new Label(LanguageManager.getText("title.inventory"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Khu vực điều khiển
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        // Label hiển thị số hệ thống
        lblSystemCountDisplay = new Label(LanguageManager.getText("label.system_count") + " 0");
        lblSystemCountDisplay.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        // Input nhập số thực tế
        Label lblInput = new Label(LanguageManager.getText("label.actual_input"));
        txtActualCount = new TextField();
        txtActualCount.setPromptText("0");
        txtActualCount.setPrefWidth(100);

        // Nút phân tích
        Button btnAnalyze = new Button(LanguageManager.getText("btn.analyze"));
        btnAnalyze.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAnalyze.setOnAction(e -> handleAnalyzeInventory());

        controls.getChildren().addAll(lblSystemCountDisplay, new Separator(javafx.geometry.Orientation.VERTICAL), lblInput, txtActualCount, btnAnalyze);

        // Biểu đồ hao hụt
        inventoryChart = new PieChart();
        inventoryChart.setTitle(LanguageManager.getText("chart.inventory"));
        inventoryChart.setLabelsVisible(true);
        inventoryChart.setPrefHeight(300);
        inventoryChart.setMaxHeight(300);

        container.getChildren().addAll(title, controls, inventoryChart);
        return container;
    }

    /**
     * Xử lý logic tính toán hao hụt (Đã cập nhật đa ngôn ngữ cho biểu đồ)
     */
    private void handleAnalyzeInventory() {
        try {
            int systemCount = library.getBooks().size();
            String input = txtActualCount.getText().trim();

            if (input.isEmpty()) {
                LibraryApp.showAlert(Alert.AlertType.WARNING, "Warning", LanguageManager.getText("msg.invalid_number"));
                return;
            }

            int actualCount = Integer.parseInt(input);
            int diff = systemCount - actualCount;

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

            if (diff > 0) {
                // MẤT SÁCH -> Dùng key slice.existing và slice.lost
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.existing") + " (" + actualCount + ")", actualCount));
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.lost") + " (" + diff + ")", diff));

            } else if (diff < 0) {
                // DƯ SÁCH -> Dùng key slice.surplus
                int surplus = Math.abs(diff);
                pieData.add(new PieChart.Data(LanguageManager.getText("label.system_count") + " (" + systemCount + ")", systemCount));
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.surplus") + " (" + surplus + ")", surplus));

            } else {
                // KHỚP -> Dùng key slice.existing
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.existing") + " (100%)", actualCount));
            }

            inventoryChart.setData(pieData);

            // Cập nhật lại tiêu đề biểu đồ kèm kết quả
            String statusText = (diff == 0 ? "OK" : (diff > 0 ? "-" + diff : "+" + Math.abs(diff)));
            inventoryChart.setTitle(LanguageManager.getText("chart.inventory") + " (" + statusText + ")");

        } catch (NumberFormatException e) {
            LibraryApp.showAlert(Alert.AlertType.ERROR, "Error", LanguageManager.getText("msg.invalid_number"));
        }
    }
    // =========================================================================
    // PHẦN 2: XỬ LÝ LOGIC & DỮ LIỆU (DATA LOGIC)
    // =========================================================================

    /**
     * Làm mới toàn bộ dữ liệu trên Dashboard
     * Được gọi khi mở App, sau khi Mượn/Trả, hoặc khi đổi ngôn ngữ.
     */
    public void refreshData() {
        List<Book> allBooks = library.getBooks();
        int totalBooks = allBooks.size();
        int totalUsers = library.getListUsers().size();

        // 1. Tính toán Sách Còn / Sách Mượn
        int availableCount = 0;
        int borrowedCount = 0;
        for (Book b : allBooks) {
            if (b.isStatus()) availableCount++;
            else borrowedCount++;
        }

        // 2. Cập nhật Thẻ số liệu
        lblTotalBooks.setText(String.valueOf(totalBooks));
        lblTotalUsers.setText(String.valueOf(totalUsers));
        lblAvailableBooks.setText(String.valueOf(availableCount));
        lblBorrowedBooks.setText(String.valueOf(borrowedCount));

        // Cập nhật số hệ thống ở phần Kiểm kê
        if (lblSystemCountDisplay != null) {
            lblSystemCountDisplay.setText(LanguageManager.getText("label.system_count") + " " + totalBooks);
        }

        // 3. Cập nhật PieChart 1 (Trạng thái)
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data(LanguageManager.getText("status.available") + " (" + availableCount + ")", availableCount),
                new PieChart.Data(LanguageManager.getText("status.borrowed") + " (" + borrowedCount + ")", borrowedCount)
        );
        statusPieChart.setData(pieData);

        // 4. Cập nhật BarChart (Top Sách)
        List<Book> topBooks = library.getBookDAO().getBooksSortedByBorrowCount();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lượt mượn");
        int limit = Math.min(5, topBooks.size());

        for (int i = 0; i < limit; i++) {
            Book b = topBooks.get(i);
            if (b.getSoLuotMuon() > 0) {
                // Cắt tên nếu quá dài
                String shortName = b.getName().length() > 15 ? b.getName().substring(0, 12) + "..." : b.getName();
                series.getData().add(new XYChart.Data<>(shortName, b.getSoLuotMuon()));
            }
        }
        topBooksChart.getData().clear();
        topBooksChart.getData().add(series);

        // 5. Cập nhật LineChart (Tăng trưởng User)
        Map<String, Integer> userStats = library.getUserDAO().getUserGrowthStats();
        XYChart.Series<String, Number> seriesUser = new XYChart.Series<>();
        seriesUser.setName("Users");

        int totalSoFar = 0; // Tính tổng tích lũy
        for (Map.Entry<String, Integer> entry : userStats.entrySet()) {
            totalSoFar += entry.getValue();
            seriesUser.getData().add(new XYChart.Data<>(entry.getKey(), totalSoFar));
        }

        userGrowthChart.getData().clear();
        userGrowthChart.getData().add(seriesUser);

        // 6. Cập nhật PieChart 2 (Tỷ lệ Trả Sách)
        // Lấy cấu hình số ngày tối đa từ DB (mặc định 60 nếu chưa cài)
        String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
        int maxDays = maxDaysStr.isEmpty() ? 60 : Integer.parseInt(maxDaysStr);

        List<long[]> returns = library.getTransactionDAO().getReturnDurations();
        int onTime = 0;
        int late = 0;
        for (long[] r : returns) {
            if (r[0] <= maxDays) onTime++;
            else late++;
        }

        if (returns.isEmpty()) {
            returnRateChart.setTitle("Tỷ lệ Trả sách (Chưa có dữ liệu)");
            returnRateChart.setData(FXCollections.observableArrayList());
        } else {
            returnRateChart.setTitle("Tỷ lệ Trả sách (Tổng: " + returns.size() + ")");
            ObservableList<PieChart.Data> rateData = FXCollections.observableArrayList(
                    new PieChart.Data("Đúng hạn (" + onTime + ")", onTime),
                    new PieChart.Data("Quá hạn (" + late + ")", late)
            );
            returnRateChart.setData(rateData);
        }
    }
}