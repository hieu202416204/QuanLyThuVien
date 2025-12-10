package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class HomeTab extends VBox {

    private final Library library;

    // Các Label hiển thị số liệu để cập nhật sau này
    private final Label lblTotalBooks = new Label("0");
    private final Label lblTotalUsers = new Label("0");
    private final Label lblBorrowedBooks = new Label("0");
    private final Label lblAvailableBooks = new Label("0");

    // Các biểu đồ
    private PieChart statusPieChart;
    private BarChart<String, Number> topBooksChart;

    public HomeTab(Library library) {
        this.library = library;

        this.setPadding(new Insets(20));
        this.setSpacing(20);
        this.setStyle("-fx-background-color: transparent;");

        // 1. Tạo hàng Thẻ số liệu (Metrics Cards)
        HBox cardsBox = createMetricsCards();

        // 2. Tạo hàng Biểu đồ (Charts)
        HBox chartsBox = createCharts();

        // Thêm vào VBox chính
        this.getChildren().addAll(cardsBox, chartsBox);

        // Tải dữ liệu lần đầu
        refreshData();
    }

    /**
     * Tạo hàng chứa 4 thẻ số liệu
     */
    private HBox createMetricsCards() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        box.getChildren().addAll(
                createCard(LanguageManager.getText("dash.total_books"), lblTotalBooks, "card-blue"),
                createCard(LanguageManager.getText("dash.available"), lblAvailableBooks, "card-green"),
                createCard(LanguageManager.getText("dash.borrowed"), lblBorrowedBooks, "card-orange"),
                createCard(LanguageManager.getText("dash.total_users"), lblTotalUsers, "card-red")
        );
        return box;
    }

    /**
     * Tạo 1 thẻ đơn lẻ
     */
    private VBox createCard(String title, Label numberLabel, String styleClass) {
        VBox card = new VBox(5);
        card.getStyleClass().addAll("dashboard-card", styleClass);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");

        numberLabel.getStyleClass().add("card-number");

        card.getChildren().addAll(lblTitle, numberLabel);
        HBox.setHgrow(card, Priority.ALWAYS); // Để thẻ tự co giãn
        return card;
    }

    /**
     * Tạo khu vực chứa 2 biểu đồ
     */
    /**
     * Tạo khu vực chứa 2 biểu đồ
     */
    private HBox createCharts() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);
        VBox.setVgrow(box, Priority.ALWAYS);

        // --- 1. KHỞI TẠO ĐỐI TƯỢNG TRƯỚC (QUAN TRỌNG) ---
        statusPieChart = new PieChart();

        // --- 2. SAU ĐÓ MỚI ĐẶT TIÊU ĐỀ ---
        statusPieChart.setTitle(BackEnd.Utils.LanguageManager.getText("chart.status"));
        statusPieChart.setLabelsVisible(true);

        // --- TƯƠNG TỰ VỚI BIỂU ĐỒ CỘT ---
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        // 1. Khởi tạo
        topBooksChart = new BarChart<>(xAxis, yAxis);

        // 2. Đặt tiêu đề
        topBooksChart.setTitle(BackEnd.Utils.LanguageManager.getText("chart.top5"));
        topBooksChart.setLegendVisible(false);
        topBooksChart.setAnimated(false);

        // Layout cho 2 biểu đồ
        HBox.setHgrow(statusPieChart, Priority.ALWAYS);
        HBox.setHgrow(topBooksChart, Priority.ALWAYS);

        box.getChildren().addAll(statusPieChart, topBooksChart);
        return box;
    }

    /**
     * Hàm làm mới dữ liệu (Gọi khi mở Tab hoặc sau khi Mượn/Trả)
     */
    public void refreshData() {
        List<Book> allBooks = library.getBooks();
        int totalBooks = allBooks.size();
        int totalUsers = library.getListUsers().size();

        // Đếm sách
        int availableCount = 0;
        int borrowedCount = 0;
        for (Book b : allBooks) {
            if (b.isStatus()) availableCount++;
            else borrowedCount++;
        }

        // 1. Cập nhật Số liệu trên thẻ
        lblTotalBooks.setText(String.valueOf(totalBooks));
        lblTotalUsers.setText(String.valueOf(totalUsers));
        lblAvailableBooks.setText(String.valueOf(availableCount));
        lblBorrowedBooks.setText(String.valueOf(borrowedCount));

        // 2. Cập nhật PieChart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data(LanguageManager.getText("status.available") + " (" + availableCount + ")", availableCount),
                new PieChart.Data(LanguageManager.getText("status.borrowed") + " (" + borrowedCount + ")", borrowedCount)
        );
        statusPieChart.setData(pieData);

        // 3. Cập nhật BarChart (Lấy Top 5 từ DB thông qua BookStatistic hoặc logic sắp xếp)
        // Ở đây ta dùng hàm sort có sẵn của BookDAO để lấy dữ liệu mới nhất
        List<Book> topBooks = library.getBookDAO().getBooksSortedByBorrowCount();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Number of book borrowing");

        int limit = Math.min(5, topBooks.size()); // Chỉ lấy 5 cuốn
        for (int i = 0; i < limit; i++) {
            Book b = topBooks.get(i);
            // Chỉ hiển thị nếu có lượt mượn > 0
            if (b.getSoLuotMuon() > 0) {
                // Cắt tên sách nếu quá dài để hiển thị đẹp hơn
                String shortName = b.getName().length() > 15 ? b.getName().substring(0, 12) + "..." : b.getName();
                series.getData().add(new XYChart.Data<>(shortName, b.getSoLuotMuon()));
            }
        }

        topBooksChart.getData().clear();
        topBooksChart.getData().add(series);
    }
}