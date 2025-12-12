package FrontEnd.UI;

import Database.VisitDAO;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class VisitStatisticsUI {

    private final VisitDAO visitDAO = new VisitDAO();
    private final BorderPane root;
    private BarChart<String, Number> barChart;
    private ComboBox<String> cmbViewType;
    private DatePicker datePicker;
    private Label lblTitle;

    public VisitStatisticsUI() {
        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;"); // Nền trắng sạch
        initUI();
    }

    private void initUI() {
        // --- 1. THANH ĐIỀU KHIỂN (CONTROL BAR) ---
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 0, 20, 0));

        // Chọn loại thống kê
        Label lblType = new Label("Chế độ xem:");
        cmbViewType = new ComboBox<>();
        cmbViewType.setItems(FXCollections.observableArrayList("Theo Tuần", "Theo Tháng", "Theo Năm"));
        cmbViewType.setValue("Theo Tuần"); // Mặc định

        // Chọn mốc thời gian
        Label lblDate = new Label("Mốc thời gian:");
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);

        // Nút Xem
        Button btnShow = new Button("📊 Xem Biểu Đồ");
        btnShow.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
        btnShow.setOnAction(e -> updateChart());

        controls.getChildren().addAll(lblType, cmbViewType, lblDate, datePicker, btnShow);

        // --- 2. BIỂU ĐỒ (BAR CHART) ---
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Thời Gian");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Số lượt khách");

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false); // Ẩn chú thích vì chỉ có 1 series
        barChart.setAnimated(true);

        lblTitle = new Label("Thống kê lượt vào thư viện");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox centerBox = new VBox(10, lblTitle, barChart);
        centerBox.setAlignment(Pos.TOP_CENTER);

        root.setTop(controls);
        root.setCenter(centerBox);

        // Vẽ lần đầu mặc định
        updateChart();
    }

    private void updateChart() {
        String viewType = cmbViewType.getValue();
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) selectedDate = LocalDate.now();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lượt khách");

        // --- XỬ LÝ 3 TRƯỜNG HỢP ---

        if (viewType.equals("Theo Tuần")) {
            setupWeekView(series, selectedDate);
        } else if (viewType.equals("Theo Tháng")) {
            setupMonthView(series, selectedDate);
        } else if (viewType.equals("Theo Năm")) {
            setupYearView(series, selectedDate);
        }

        // Cập nhật dữ liệu vào Chart
        barChart.getData().clear();
        barChart.getData().add(series);
    }

    // --- LOGIC 1: XEM THEO TUẦN (So sánh Thứ 2 -> CN) ---
    private void setupWeekView(XYChart.Series<String, Number> series, LocalDate date) {
        // Tìm Thứ 2 đầu tuần và Chủ Nhật cuối tuần của ngày được chọn
        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = date.with(DayOfWeek.SUNDAY);

        lblTitle.setText("Thống kê tuần: " + startOfWeek + " đến " + endOfWeek);

        // Lấy dữ liệu từ DB
        Map<String, Integer> data = visitDAO.getVisitsInDateRange(startOfWeek, endOfWeek);

        // Vòng lặp từ Thứ 2 đến CN để đảm bảo đủ 7 cột (ngay cả khi không có khách)
        for (LocalDate d = startOfWeek; !d.isAfter(endOfWeek); d = d.plusDays(1)) {
            String keyDate = d.toString();
            int count = data.getOrDefault(keyDate, 0);

            // Label cột: "Thứ 2\n(10/12)"
            String label = getDayName(d.getDayOfWeek()) + "\n" + d.format(DateTimeFormatter.ofPattern("dd/MM"));
            series.getData().add(new XYChart.Data<>(label, count));
        }
    }

    // --- LOGIC 2: XEM THEO THÁNG (So sánh Ngày 1 -> Ngày 30/31) ---
    private void setupMonthView(XYChart.Series<String, Number> series, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        lblTitle.setText("Thống kê tháng " + yearMonth.getMonthValue() + "/" + yearMonth.getYear());

        Map<String, Integer> data = visitDAO.getVisitsInDateRange(startOfMonth, endOfMonth);

        // Vòng lặp tất cả các ngày trong tháng
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate d = yearMonth.atDay(day);
            String keyDate = d.toString();
            int count = data.getOrDefault(keyDate, 0);

            // Label cột chỉ hiện số ngày (1, 2, 3...) cho đỡ chật
            series.getData().add(new XYChart.Data<>(String.valueOf(day), count));
        }
    }

    // --- LOGIC 3: XEM THEO NĂM (So sánh Tháng 1 -> Tháng 12) ---
    private void setupYearView(XYChart.Series<String, Number> series, LocalDate date) {
        int year = date.getYear();
        lblTitle.setText("Thống kê năm " + year);

        Map<Integer, Integer> data = visitDAO.getVisitsByMonthInYear(year);

        for (int month = 1; month <= 12; month++) {
            int count = data.getOrDefault(month, 0);
            series.getData().add(new XYChart.Data<>("Tháng " + month, count));
        }
    }

    // Tiện ích chuyển đổi tên thứ sang tiếng Việt
    private String getDayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "CN";
        };
    }

    public BorderPane getView() {
        return root;
    }
}