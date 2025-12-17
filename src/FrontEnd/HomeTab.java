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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class HomeTab extends ScrollPane {
    private final Library library;
    private final VisitDAO visitDAO = new VisitDAO();

    // UI Components
    private final Label lblTotalBooks = new Label("...");
    private final Label lblTotalUsers = new Label("...");
    private final Label lblBorrowedBooks = new Label("...");
    private final Label lblAvailableBooks = new Label("...");
    private final Label lblTotalRevenue = new Label("...");

    private PieChart statusPieChart;
    private BarChart<String, Number> topBooksChart;
    private LineChart<String, Number> userGrowthChart;
    private PieChart returnRateChart;

    // Visit Components
    private BarChart<String, Number> visitChart;
    private ComboBox<String> cmbVisitViewType;
    private DatePicker datePickerVisit;
    private Label lblVisitTitle;

    // Data for PieChart updates
    private PieChart.Data dataAvailable;
    private PieChart.Data dataBorrowed;

    // Inventory Components
    private TextField txtActualCount;
    private Label lblSystemCountDisplay;
    private PieChart inventoryChart;

    // Control Flag for Background Thread
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    // --- Biến lưu thời gian refresh (Mặc định 20s = 20000ms) ---
    private final AtomicLong refreshInterval = new AtomicLong(20000);

    private NumberAxis yAxis;
    private double currentUpperBound = 0;
    public HomeTab(Library library) {
        this.library = library;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");
        this.setPannable(true);

        // --- LAYOUT SETUP ---
        VBox mainLayout = new VBox(25);
        mainLayout.setPadding(new Insets(25, 25, 25, 25)); // Padding chuẩn

        mainLayout.getChildren().addAll(
                createMetricsCards(),
                createChartsRow1(),
                createChartsRow2(),
                createVisitStatsSection(),
                createInventorySection()
        );

        // ---SỬ DỤNG STACKPANE ĐỂ ĐẶT NÚT NỔI ---
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: transparent;");

        // 1. Lớp dưới: Giao diện chính
        rootPane.getChildren().add(mainLayout);

        // 2. Lớp trên: Nút cài đặt (Floating Button)
        MenuButton settingsBtn = createSettingsButton();
        rootPane.getChildren().add(settingsBtn);

        // Căn chỉnh nút về góc trên phải
        StackPane.setAlignment(settingsBtn, Pos.TOP_RIGHT);
        // Cách lề một chút để đẹp hơn (margin top/right)
        StackPane.setMargin(settingsBtn, new Insets(10, 25, 0, 0));

        this.setContent(rootPane);

        // --- BẮT ĐẦU LUỒNG CẬP NHẬT NGẦM ---
        startBackgroundAutoRefresh();
    }

    // =========================================================================
    // --- TẠO NÚT CÀI ĐẶT TRÒN ---
    // =========================================================================
    private MenuButton createSettingsButton() {
        MenuButton btn = new MenuButton();

        // 1. TẠO ICON BẰNG SVG (Sắc nét, chuẩn hiện đại)
        SVGPath icon = new SVGPath();
        // Đây là mã vector của icon bánh răng (Gear icon path)
        icon.setContent("M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.06-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.73,8.87 C2.62,9.08,2.66,9.34,2.86,9.49l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.06,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z");
        // Chọn màu xám xanh hiện đại (thay vì đen sì)
        icon.setFill(Color.web("#546e7a"));
        // Thu nhỏ icon lại một chút cho vừa nút
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);

        // Đặt icon vào một StackPane để nó tự động căn giữa hoàn hảo
        StackPane iconContainer = new StackPane(icon);
        iconContainer.setPrefSize(24, 24);
        btn.setGraphic(iconContainer);

        // 2. CSS INLINE CẢI TIẾN (Đẹp hơn, bóng mềm hơn)
        btn.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 50%;" + // Tròn vo
                        "-fx-min-width: 50px; -fx-min-height: 50px;" + // Kích thước lớn hơn xíu
                        "-fx-max-width: 50px; -fx-max-height: 50px;" +
                        // Đổ bóng mềm (Soft Shadow): màu nhạt hơn, bán kính mờ lớn hơn
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 2);" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;" // Quan trọng: reset padding để icon nằm chính giữa
        );

        // Thêm class để sau này xử lý ẩn mũi tên bằng CSS ngoài (nếu cần)
        btn.getStyleClass().add("floating-menu-btn");

        // --- Tạo các Menu Items (Giữ nguyên logic cũ) ---
        MenuItem itemRefresh = new MenuItem("Refresh Now");
        // Thêm icon nhỏ cho menu item
        Label refreshIcon = new Label("↻"); refreshIcon.setStyle("-fx-font-size: 14px;");
        itemRefresh.setGraphic(refreshIcon);
        itemRefresh.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9;");
        itemRefresh.setOnAction(e -> refreshData());

        Menu menuAuto = new Menu("Auto Update Interval");
        Label timerIcon = new Label("⏱"); timerIcon.setStyle("-fx-font-size: 14px;");
        menuAuto.setGraphic(timerIcon);

        ToggleGroup group = new ToggleGroup();
        RadioMenuItem r20 = createIntervalItem("Every 20 seconds", 20000, group);
        RadioMenuItem r40 = createIntervalItem("Every 40 seconds", 40000, group);
        RadioMenuItem r60 = createIntervalItem("Every 60 seconds", 60000, group);
        r20.setSelected(true);

        menuAuto.getItems().addAll(r20, r40, r60);
        btn.getItems().addAll(itemRefresh, new SeparatorMenuItem(), menuAuto);

        return btn;
    }

    private RadioMenuItem createIntervalItem(String text, long timeMs, ToggleGroup group) {
        RadioMenuItem item = new RadioMenuItem(text);
        item.setToggleGroup(group);
        item.setOnAction(e -> {
            refreshInterval.set(timeMs); // Cập nhật biến thời gian
            // (Optional) Có thể thêm thông báo hoặc log
            System.out.println("Auto refresh set to: " + text);
        });
        return item;
    }

    // =========================================================================
    // 0. BACKGROUND THREAD LOGIC
    // =========================================================================

    private static class DashboardData {
        int totalBooks, totalUsers, available, borrowed;
        double revenue;
        List<Book> topBooks;
        Map<String, Integer> userGrowth;
        List<long[]> returnStats;
        String visitTitle;
        XYChart.Series<String, Number> visitSeries;
    }

    private void startBackgroundAutoRefresh() {
        Thread bgThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    // 1. Thu thập dữ liệu UI
                    final LocalDate[] selectedDate = {LocalDate.now()};
                    final String[] selectedView = {"Week View"};

                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    Platform.runLater(() -> {
                        if (datePickerVisit != null && datePickerVisit.getValue() != null)
                            selectedDate[0] = datePickerVisit.getValue();
                        if (cmbVisitViewType != null && cmbVisitViewType.getValue() != null)
                            selectedView[0] = cmbVisitViewType.getValue();
                        latch.countDown();
                    });
                    latch.await();

                    // 2. Load dữ liệu
                    DashboardData data = loadDashboardData(selectedDate[0], selectedView[0]);

                    // 3. Update UI
                    Platform.runLater(() -> updateUI(data));

                    // 4. Nghỉ theo thời gian đã cài đặt (Dùng biến refreshInterval)
                    Thread.sleep(refreshInterval.get());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        bgThread.setDaemon(true);
        bgThread.start();
    }
    // Hàm tính toán dữ liệu (Nặng - Chạy ngầm)
    private DashboardData loadDashboardData(LocalDate visitDate, String visitViewType) {
        DashboardData data = new DashboardData();

        // a. Số liệu tổng quan
        List<Book> allBooks = library.getBooks(); // Giả sử list này cached hoặc query nhanh
        data.totalBooks = allBooks.size();
        data.totalUsers = library.getListUsers().size();

        int avail = 0;
        for (Book b : allBooks) {
            if (b.isStatus()) avail++;
        }
        data.available = avail;
        data.borrowed = data.totalBooks - avail;

        // b. Doanh thu
        data.revenue = library.getFinancialDAO().getTotalRevenue();

        // c. Top Books & Growth (Query nặng)
        data.topBooks = library.getBookDAO().getBooksSortedByBorrowCount();
        data.userGrowth = library.getUserDAO().getUserGrowthStats();
        data.returnStats = library.getTransactionDAO().getReturnDurations();

        // d. Tính toán Visit Data
        data.visitSeries = calculateVisitSeries(visitDate, visitViewType);
        // Set title cho visit chart (logic giống updateVisitChart cũ)
        if ("Week View".equals(visitViewType)) {
            LocalDate start = visitDate.with(DayOfWeek.MONDAY);
            LocalDate end = visitDate.with(DayOfWeek.SUNDAY);
            data.visitTitle = "Visitors: " + start.format(DateTimeFormatter.ofPattern("dd/MM")) + " to " + end.format(DateTimeFormatter.ofPattern("dd/MM"));
        } else if ("Month View".equals(visitViewType)) {
            data.visitTitle = "Visitors: " + visitDate.getMonth() + " " + visitDate.getYear();
        } else {
            data.visitTitle = "Visitors in " + visitDate.getYear();
        }

        return data;
    }

    // Hàm hiển thị lên màn hình (Nhẹ - Chạy trên UI Thread)
    private void updateUI(DashboardData data) {
        // --- 1. UPDATE LABELS ---
        lblTotalBooks.setText(String.valueOf(data.totalBooks));
        lblTotalUsers.setText(String.valueOf(data.totalUsers));
        lblAvailableBooks.setText(String.valueOf(data.available));
        lblBorrowedBooks.setText(String.valueOf(data.borrowed));

        String currency = library.getSettingsDAO().getSetting("currency_unit");
        if (currency == null || currency.isEmpty()) currency = "VNĐ";
        lblTotalRevenue.setText(String.format("%,.0f %s", data.revenue, currency));

        if (lblSystemCountDisplay != null) {
            lblSystemCountDisplay.setText(LanguageManager.getText("label.system_count") + " " + data.totalBooks);
        }

        // --- 2. UPDATE PIE CHART 1 ---
        dataAvailable.setPieValue(data.available);
        dataBorrowed.setPieValue(data.borrowed);
        dataAvailable.setName(LanguageManager.getText("status.available") + " (" + data.available + ")");
        dataBorrowed.setName(LanguageManager.getText("status.borrowed") + " (" + data.borrowed + ")");

        // --- 3. UPDATE BAR CHART (TOP BOOKS) - LOGIC TRỤC Y CHUẨN ---
        double maxBorrowCount = 0;
        if (!data.topBooks.isEmpty()) {
            maxBorrowCount = data.topBooks.get(0).getSoLuotMuon();
        }

        if (currentUpperBound == 0) {
            currentUpperBound = (maxBorrowCount > 0 ? maxBorrowCount : 5) * 1.5;
            yAxis.setUpperBound(currentUpperBound);
        }

        if (maxBorrowCount >= currentUpperBound * 0.93) {
            currentUpperBound = Math.ceil(maxBorrowCount * 1.5);
            yAxis.setUpperBound(currentUpperBound);
        }

        if (maxBorrowCount < currentUpperBound * 0.2 && maxBorrowCount > 0) {
            currentUpperBound = maxBorrowCount * 1.5;
            yAxis.setUpperBound(currentUpperBound);
        }

        // Vẽ BarChart (Tái sử dụng Series)
        XYChart.Series<String, Number> seriesTop;
        if (topBooksChart.getData().isEmpty()) {
            seriesTop = new XYChart.Series<>();
            seriesTop.setName("Loans");
            topBooksChart.getData().add(seriesTop);
        } else {
            seriesTop = topBooksChart.getData().get(0);
        }

        ObservableList<XYChart.Data<String, Number>> newDataTop = FXCollections.observableArrayList();
        int limit = Math.min(5, data.topBooks.size());
        for (int i = 0; i < limit; i++) {
            Book b = data.topBooks.get(i);
            if (b.getSoLuotMuon() > 0) {
                String shortName = b.getName().length() > 15 ? b.getName().substring(0, 12) + "..." : b.getName();
                newDataTop.add(new XYChart.Data<>(shortName, b.getSoLuotMuon()));
            }
        }
        seriesTop.setData(newDataTop);

        // --- 4. UPDATE LINE CHART (GROWTH) ---
        // Thay vì clear(), ta lấy series cũ ra dùng lại
        XYChart.Series<String, Number> seriesUser;
        if (userGrowthChart.getData().isEmpty()) {
            seriesUser = new XYChart.Series<>();
            seriesUser.setName("Users");
            userGrowthChart.getData().add(seriesUser);
        } else {
            seriesUser = userGrowthChart.getData().get(0);
        }

        ObservableList<XYChart.Data<String, Number>> newDataUser = FXCollections.observableArrayList();
        int totalSoFar = 0;
        for (Map.Entry<String, Integer> entry : data.userGrowth.entrySet()) {
            totalSoFar += entry.getValue();
            newDataUser.add(new XYChart.Data<>(entry.getKey(), totalSoFar));
        }
        // Cập nhật dữ liệu mới vào series cũ -> MƯỢT MÀ, KHÔNG NHÁY
        seriesUser.setData(newDataUser);

        // --- 5. UPDATE RETURN RATE CHART ---
        String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
        int maxDays = (maxDaysStr == null || maxDaysStr.isEmpty()) ? 60 : Integer.parseInt(maxDaysStr);
        int onTime = 0;
        int late = 0;
        for (long[] r : data.returnStats) {
            if (r[0] <= maxDays) onTime++;
            else late++;
        }
        returnRateChart.getData().clear();
        if (onTime + late > 0) {
            returnRateChart.getData().add(new PieChart.Data("On time (" + onTime + ")", onTime));
            returnRateChart.getData().add(new PieChart.Data("Late (" + late + ")", late));
        }

        // --- 6. UPDATE VISIT CHART ---
        lblVisitTitle.setText(data.visitTitle);

        if (data.visitSeries != null) {
            // Kiểm tra xem chart đã có series chưa để tái sử dụng
            if (visitChart.getData().isEmpty()) {
                visitChart.getData().add(data.visitSeries);
            } else {
                // Lấy series hiện tại ra
                XYChart.Series<String, Number> currentSeries = visitChart.getData().get(0);
                // Gán tên mới (phòng khi đổi view từ tuần sang tháng)
                currentSeries.setName(data.visitSeries.getName());
                // Thay thế cục data bên trong -> Không bị mất hiệu ứng
                currentSeries.setData(data.visitSeries.getData());
            }
        } else {
            visitChart.getData().clear();
        }
    }

    // Logic tính toán Visit Series
    private XYChart.Series<String, Number> calculateVisitSeries(LocalDate selectedDate, String viewType) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Visitors");

        if ("Week View".equals(viewType)) {
            LocalDate start = selectedDate.with(DayOfWeek.MONDAY);
            LocalDate end = selectedDate.with(DayOfWeek.SUNDAY);
            Map<String, Integer> map = visitDAO.getVisitsInDateRange(start, end);
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                String label = d.getDayOfWeek().toString().substring(0,3) + "\n" + d.format(DateTimeFormatter.ofPattern("dd/MM"));
                series.getData().add(new XYChart.Data<>(label, map.getOrDefault(d.toString(), 0)));
            }
        } else if ("Month View".equals(viewType)) {
            YearMonth ym = YearMonth.from(selectedDate);
            Map<String, Integer> map = visitDAO.getVisitsInDateRange(ym.atDay(1), ym.atEndOfMonth());
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                series.getData().add(new XYChart.Data<>(String.valueOf(day), map.getOrDefault(ym.atDay(day).toString(), 0)));
            }
        } else { // Year View
            int year = selectedDate.getYear();
            Map<Integer, Integer> map = visitDAO.getVisitsByMonthInYear(year);
            for (int m = 1; m <= 12; m++) {
                series.getData().add(new XYChart.Data<>("M" + m, map.getOrDefault(m, 0)));
            }
        }
        return series;
    }

    // =========================================================================
    // UI CREATION METHODS
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

    private HBox createChartsRow1() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        statusPieChart = new PieChart();
        statusPieChart.setTitle(LanguageManager.getText("chart.status"));
        statusPieChart.setLabelsVisible(false);
        statusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        dataAvailable = new PieChart.Data(LanguageManager.getText("status.available"), 0);
        dataBorrowed = new PieChart.Data(LanguageManager.getText("status.borrowed"), 0);
        statusPieChart.setData(FXCollections.observableArrayList(dataAvailable, dataBorrowed));

        CategoryAxis xAxis = new CategoryAxis();
        yAxis = new NumberAxis();
        yAxis.setLabel("Book");
        yAxis.setTickUnit(1); // Chia vạch theo số nguyên
        yAxis.setMinorTickVisible(false);

        // --- TẮT TỰ ĐỘNG ĐỂ KIỂM SOÁT LOGIC ---
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(10); // Giá trị tạm ban đầu

        // Formatter số nguyên
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return (object.intValue() == object.doubleValue()) ? String.valueOf(object.intValue()) : "";
            }
            @Override
            public Number fromString(String string) { return Double.parseDouble(string); }
        });

        topBooksChart = new BarChart<>(xAxis, yAxis);
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
        Label icon = new Label("📊");
        icon.setStyle("-fx-font-size: 20px;");
        lblVisitTitle = new Label("Visitor Statistics");
        lblVisitTitle.getStyleClass().add("card-title");
        lblVisitTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        cmbVisitViewType = new ComboBox<>();
        cmbVisitViewType.setItems(FXCollections.observableArrayList("Week View", "Month View", "Year View"));
        cmbVisitViewType.setValue("Week View");
        // Xử lý sự kiện: Trigger reload ngay lập tức (không đợi 2s)
        cmbVisitViewType.setOnAction(e -> triggerImmediateVisitUpdate());

        datePickerVisit = new DatePicker(LocalDate.now());
        datePickerVisit.setPrefWidth(120);
        // Xử lý sự kiện: Trigger reload ngay lập tức
        datePickerVisit.setOnAction(e -> triggerImmediateVisitUpdate());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(icon, lblVisitTitle, spacer, cmbVisitViewType, datePickerVisit);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setLabel("Visitors");
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return (object.intValue() == object.doubleValue()) ? String.valueOf(object.intValue()) : "";
            }
            @Override
            public Number fromString(String string) { return Double.parseDouble(string); }
        });
        visitChart = new BarChart<>(xAxis, yAxis);
        visitChart.setLegendVisible(false);

        visitChart.setAnimated(false);
        visitChart.setMinHeight(300);

        container.getChildren().addAll(headerBox, new Separator(), visitChart);
        return container;
    }

    // Sự kiện khi bấm nút trên UI (Chạy async để không lag)
    private void triggerImmediateVisitUpdate() {
        LocalDate d = datePickerVisit.getValue() != null ? datePickerVisit.getValue() : LocalDate.now();
        String v = cmbVisitViewType.getValue() != null ? cmbVisitViewType.getValue() : "Week View";

        // Chạy thread riêng để update chart
        new Thread(() -> {
            XYChart.Series<String, Number> series = calculateVisitSeries(d, v);

            // Tính toán Title
            String title;
            if ("Week View".equals(v)) {
                LocalDate start = d.with(DayOfWeek.MONDAY);
                LocalDate end = d.with(DayOfWeek.SUNDAY);
                title = "Visitors: " + start.format(DateTimeFormatter.ofPattern("dd/MM")) + " to " + end.format(DateTimeFormatter.ofPattern("dd/MM"));
            } else if ("Month View".equals(v)) {
                title = "Visitors: " + d.getMonth() + " " + d.getYear();
            } else {
                title = "Visitors in " + d.getYear();
            }

            Platform.runLater(() -> {
                lblVisitTitle.setText(title);
                visitChart.getData().clear();
                visitChart.getData().add(series);
            });
        }).start();
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
        Button btnAnalyze = new Button(LanguageManager.getText("btn.analyze"));
        btnAnalyze.getStyleClass().addAll("action-btn", "btn-purple");
        btnAnalyze.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
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

    private void handleAnalyzeInventory() {
        try {
            int systemCount = library.getBooks().size(); // Query nhanh nên chạy UI thread ok
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
    /**
     * Hàm này được gọi từ bên ngoài (LibraryApp) khi cần làm mới dữ liệu ngay lập tức.
     * Ví dụ: Khi vừa Mượn/Trả sách xong, hoặc khi chuyển từ Tab khác về Home.
     */
    public void refreshData() {
        // Tạo một luồng riêng để cập nhật ngay lập tức (không cần đợi vòng lặp 2 giây)
        Thread oneTimeUpdateThread = new Thread(() -> {
            try {
                // 1. Lấy trạng thái từ UI (DatePicker, ComboBox) an toàn
                final LocalDate[] selectedDate = {LocalDate.now()};
                final String[] selectedView = {"Week View"};

                // Dùng CountDownLatch để đợi UI trả về giá trị (tránh lỗi Not on FX Thread)
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                Platform.runLater(() -> {
                    if (datePickerVisit != null && datePickerVisit.getValue() != null) {
                        selectedDate[0] = datePickerVisit.getValue();
                    }
                    if (cmbVisitViewType != null && cmbVisitViewType.getValue() != null) {
                        selectedView[0] = cmbVisitViewType.getValue();
                    }
                    latch.countDown();
                });
                latch.await(); // Chờ UI đọc xong

                // 2. Tính toán lại dữ liệu (Nặng)
                DashboardData data = loadDashboardData(selectedDate[0], selectedView[0]);

                // 3. Đẩy lên giao diện
                Platform.runLater(() -> updateUI(data));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        oneTimeUpdateThread.setDaemon(true);
        oneTimeUpdateThread.start();
    }
}