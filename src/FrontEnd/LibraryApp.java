package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.Histories.UserInUserHistory;
import BackEnd.LibraryQ.Library;
import BackEnd.LibraryQ.QuanLyMuonTra;
import BackEnd.LibraryQ.SearchService;
import BackEnd.Sattistics.BookStatistic;
import BackEnd.Sattistics.UserStatistic;
import BackEnd.User.User;
import Database.DatabaseManager;
import XuLiAnh.ImageResizer;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LibraryApp extends Application {

    // 1. BACKEND CORE
    private final Library library = new Library();
    private final QuanLyMuonTra quanLyMuonTra = new QuanLyMuonTra(library);
    private final SearchService searchService = new SearchService(library);
    private final UserStatistic userStatistic = new UserStatistic(library); // Sửa tên lớp
    private final BookStatistic bookStatistic = new BookStatistic(library);

    // 2. DATA HOLDER (ObservableList)
    private final ObservableList<Book> bookData = FXCollections.observableArrayList();
    private final ObservableList<User> userData = FXCollections.observableArrayList();

    // 3. UI COMPONENTS
    private final TableView<User> userTable = new TableView<>();
    private final FlowPane bookFlowPane = new FlowPane();
    private BookGalleryTab bookGalleryTab;
    private final FlowPane searchGalleryPane = new FlowPane();
    private final TextField borrowReturnBookIdField = new TextField(); // Field để lưu ID sách đã chọn
    // darkMode=========
    private boolean isDarkMode = false;

    // Phương thức resize ảnh (giữ nguyên)
    public javafx.scene.image.Image convertAndResize(BufferedImage originalImage) {
        if (originalImage == null) return null;

        try {
            final int TARGET_WIDTH = 120;
            final int TARGET_HEIGHT = 160;

            BufferedImage resizedImage = ImageResizer.resizeImage(
                    originalImage,
                    TARGET_WIDTH,
                    TARGET_HEIGHT
            );

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", os);
            return new javafx.scene.image.Image(new ByteArrayInputStream(os.toByteArray()));

        } catch (IOException e) {
            System.err.println("Lỗi chuyển đổi/resize ảnh: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. KHỞI TẠO CẤU TRÚC DB (Tạo các bảng nếu chưa có)
        DatabaseManager.initializeDatabase();

        // 2. CHẠY LOGIC KHỞI TẠO DỮ LIỆU GIẢ (Chỉ chạy khi DB rỗng)
        initializeData();

        // 3. TẢI DỮ LIỆU TỪ DB LÊN CÁC ObservableList
        bookData.setAll(library.getBooks());
        userData.setAll(library.getListUsers());

        primaryStage.setTitle("📚 Ứng dụng Quản lý Thư viện");

        TabPane tabPane = new TabPane();
        bookGalleryTab = new BookGalleryTab(library, bookFlowPane, this::convertAndResize);

        // Khởi tạo các Tabs
        Tab tabBooks = new Tab("Quản lý sách", new BookManagementTab(library, bookData, bookFlowPane, bookGalleryTab).getPane());
        Tab tabUsers = new Tab("Quản lý người dùng", createUserPane());
        Tab tabBorrowReturn = new Tab("Mượn/Trả sách", createBorrowReturnPane());
        Tab tabSearch = new Tab("Tìm kiếm sách", createSearchPane());
        Tab tabHistory = new Tab("Lịch sử mượn trả", createHistoryPane()); // Đã sửa
        Tab tabGallery = new Tab("Gallery sách", bookGalleryTab);
        Tab tabStatistics = new Tab("📊 Thống kê người dùng", createStatisticsPane()); // Đã sửa

        tabPane.getTabs().addAll( tabGallery, tabBooks, tabUsers, tabBorrowReturn, tabSearch, tabHistory, tabStatistics);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Button themeToggleBtn = new Button("🌙 Theme Tối");
        HBox bottomBar = new HBox(themeToggleBtn);
        bottomBar.setAlignment(Pos.BOTTOM_RIGHT);
        bottomBar.setPadding(new Insets(10, 10, 10, 10));

        BorderPane rootLayout = new BorderPane();
        rootLayout.setCenter(tabPane);
        rootLayout.setBottom(bottomBar);

        Scene scene = new Scene(rootLayout, 1100, 750);

        themeToggleBtn.setOnAction(e -> toggleTheme(scene, themeToggleBtn));

        // Logic tải CSS
        java.net.URL cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl == null) { cssUrl = getClass().getResource("/styles/styles.css"); }
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("LỖI CẤU HÌNH: KHÔNG tìm thấy file styles.css.");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    // === TẠO GIAO DIỆN THỐNG KÊ (Sử dụng DAO, giữ nguyên logic UI) ===
    private Pane createStatisticsPane() {
        // ... (Giữ nguyên cấu hình TableView) ...
        TableView<User> topUserTable = new TableView<>();
        topUserTable.setPrefHeight(600);
        topUserTable.setMinWidth(500);

        TableColumn<User, String> colUserRank = new TableColumn<>("Hạng");
        colUserRank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topUserTable.getItems().indexOf(data.getValue()) + 1)));
        colUserRank.setPrefWidth(50);
        colUserRank.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        TableColumn<User, String> colUserId = new TableColumn<>("ID");
        colUserId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colUserId.setPrefWidth(100);

        TableColumn<User, String> colUserName = new TableColumn<>("Tên Người dùng");
        colUserName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colUserName.setPrefWidth(220);

        TableColumn<User, String> colUserBorrowedCount = new TableColumn<>("Số sách đã mượn");
        colUserBorrowedCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoSachDaMuon())));
        colUserBorrowedCount.setPrefWidth(120);
        colUserBorrowedCount.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        topUserTable.getColumns().addAll(colUserRank, colUserId, colUserName, colUserBorrowedCount);

        Label titleUser = new Label("TOP 20 NGƯỜI DÙNG MƯỢN SÁCH");
        titleUser.getStyleClass().add("topUser");
        titleUser.setStyle("-fx-font-size: 1.5em; fx-font-weight: bold;");
        VBox statsUser = new VBox(10, titleUser, topUserTable);
        statsUser.setAlignment(Pos.TOP_CENTER);

        TableView<Book> topBookTable = new TableView<>();
        topBookTable.setPrefHeight(600);
        topBookTable.setMinWidth(500);

        TableColumn<Book, String> colBookRank = new TableColumn<>("Hạng");
        colBookRank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topBookTable.getItems().indexOf(data.getValue()) + 1)));
        colBookRank.setPrefWidth(50);
        colBookRank.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        TableColumn<Book, String> colBookId = new TableColumn<>("ID");
        colBookId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colBookId.setPrefWidth(80);

        TableColumn<Book, String> colBookName = new TableColumn<>("Tên Sách");
        colBookName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colBookName.setPrefWidth(250);

        TableColumn<Book, String> colBookCount = new TableColumn<>("Số lượt mượn");
        colBookCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoLuotMuon())));
        colBookCount.setPrefWidth(120);
        colBookCount.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        topBookTable.getColumns().addAll(colBookRank, colBookId, colBookName, colBookCount);

        Label titleBook = new Label("TOP 20 SÁCH ĐƯỢC MƯỢN NHIỀU NHẤT");
        titleBook.getStyleClass().add("topBook");
        titleBook.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;");
        VBox statsBook = new VBox(10, titleBook, topBookTable);
        statsBook.setAlignment(Pos.TOP_CENTER);


        Button refreshBtn = new Button("🔄 Cập nhật Dữ liệu Thống kê");

        refreshBtn.setOnAction(e -> {
            // Logic Cập nhật Người dùng (Gọi DAO qua UserStatistic)
            List<User> allUsersSorted = userStatistic.danhSachNguoiDung();
            int userLimit = Math.min(20, allUsersSorted.size());
            List<User> topUsers = allUsersSorted.subList(0, userLimit);
            topUserTable.setItems(FXCollections.observableArrayList(topUsers));
            titleUser.setText(String.format("TOP %d NGƯỜI DÙNG MƯỢN SÁCH (Tổng: %d)", userLimit, allUsersSorted.size()));
            topUserTable.refresh();

            // Logic Cập nhật Sách (Gọi DAO qua BookStatistic)
            List<Book> allBooksSorted = bookStatistic.getTopBook();
            int bookLimit = Math.min(20, allBooksSorted.size());
            List<Book> topBooks = allBooksSorted.subList(0, bookLimit);
            topBookTable.setItems(FXCollections.observableArrayList(topBooks));
            titleBook.setText(String.format("TOP %d SÁCH ĐƯỢC MƯỢN NHIỀU NHẤT (Tổng: %d)", bookLimit, allBooksSorted.size()));
            topBookTable.refresh();

            LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Cập nhật", "Đã tải lại dữ liệu thống kê mới nhất.");
        });

        HBox statsLayout = new HBox(20, statsUser, statsBook);
        statsLayout.setAlignment(Pos.TOP_CENTER);
        statsLayout.setHgrow(statsUser, Priority.ALWAYS);
        statsLayout.setHgrow(statsBook, Priority.ALWAYS);

        VBox pane = new VBox(15, refreshBtn, statsLayout);
        pane.setPadding(new Insets(10));
        pane.setAlignment(Pos.TOP_CENTER);

        return pane;
    }

    private void toggleTheme(Scene scene, Button button) {
        // Giữ nguyên logic Dark Mode
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            scene.getRoot().getStyleClass().add("dark-mode");
            button.setText("☀️ Theme Sáng");
        } else {
            scene.getRoot().getStyleClass().remove("dark-mode");
            button.setText("🌙 Theme Tối");
        }
    }

    // === CÁC PHƯƠNG THỨC TẠO GIAO DIỆN KHÁC ===
    private Pane createUserPane() {
        // ... (Giữ nguyên logic CRUD vì Library đã được sửa để gọi DAO) ...
        TableColumn<User, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(150);

        TableColumn<User, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(300);

        userTable.getColumns().addAll(colId, colName);
        userTable.setItems(userData);

        TextField idField = new TextField(); idField.setPromptText("ID");
        TextField nameField = new TextField(); nameField.setPromptText("Name");

        Button addBtn = new Button("➕ Thêm người dùng");
        addBtn.setOnAction(e -> {
            if(!idField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                User user = new User(idField.getText(), nameField.getText());
                library.addUser(user); // Gọi Library -> UserDAO
                userData.setAll(library.getListUsers());
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Người dùng đã được thêm.");
                idField.clear(); nameField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi thêm người dùng", "ID và Tên không được để trống.");
            }
        });

        Button deleteBtn = new Button("❌ Xóa người dùng");
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                library.deleteUser(selected.getId()); // Gọi Library -> UserDAO
                userData.setAll(library.getListUsers());
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Người dùng đã được xóa.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn người dùng cần xóa.");
            }
        });

        Button viewUserHistoryBtn = new Button("📜 Xem Lịch sử Người dùng");
        viewUserHistoryBtn.setDisable(true);
        viewUserHistoryBtn.setOnAction(e -> handleViewUserHistory(userTable.getSelectionModel().getSelectedItem()));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            viewUserHistoryBtn.setDisable(newSelection == null);
        });

        TextField searchUserField = new TextField();
        searchUserField.setPromptText("Nhập ID hoặc Tên người dùng");
        searchUserField.setPrefWidth(200);

        Button searchIdBtn = new Button("🔍 Tìm theo ID");
        searchIdBtn.setOnAction(e -> handleSearchUserById(searchUserField.getText()));

        Button searchNameBtn = new Button("🔍 Tìm theo Tên");
        searchNameBtn.setOnAction(e -> handleSearchUserByName(searchUserField.getText()));

        Button clearSearchBtn = new Button("🔄 Xóa tìm kiếm");
        clearSearchBtn.setOnAction(e -> {
            userData.setAll(library.getListUsers());
            searchUserField.clear();
            userTable.getSelectionModel().clearSelection();
        });

        HBox searchControls = new HBox(10, searchUserField, searchIdBtn, searchNameBtn, clearSearchBtn);
        searchControls.setPadding(new Insets(10, 0, 10, 0));

        HBox crudControls = new HBox(10, idField, nameField, addBtn, deleteBtn, viewUserHistoryBtn);
        crudControls.setPadding(new Insets(10));

        VBox pane = new VBox(10, searchControls, userTable, crudControls);
        pane.setPadding(new Insets(10));

        return pane;
    }

    // === XỬ LÝ TÌM KIẾM THEO ID (Giữ nguyên vì Library đã gọi DAO) ===
    private void handleSearchUserById(String userId) {
        if (userId.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập ID để tìm kiếm.");
            return;
        }
        User foundUser = library.searchUserById(userId);

        if (foundUser != null) {
            userTable.getSelectionModel().select(foundUser);
            userTable.scrollTo(foundUser);
            showAlert(Alert.AlertType.INFORMATION, "Tìm thấy", "Đã tìm thấy người dùng có ID: " + userId + ".");
        } else {
            userTable.getSelectionModel().clearSelection();
            showAlert(Alert.AlertType.WARNING, "Không tìm thấy", "Không có người dùng nào với ID: " + userId);
        }
    }

    // === XỬ LÝ TÌM KIẾM THEO TÊN (Giữ nguyên vì Library đã gọi DAO) ===
    private void handleSearchUserByName(String userName) {
        if (userName.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập Tên để tìm kiếm.");
            return;
        }
        List<User> results = library.searchUserByName(userName);

        if (!results.isEmpty()) {
            userData.setAll(results);
            userTable.getSelectionModel().clearSelection();
            showAlert(Alert.AlertType.INFORMATION, "Tìm thấy", "Đã tìm thấy " + results.size() + " người dùng phù hợp.");
        } else {
            userData.setAll(library.getListUsers());
            userTable.getSelectionModel().clearSelection();
            showAlert(Alert.AlertType.WARNING, "Không tìm thấy", "Không có người dùng nào có tên chính xác là: " + userName);
        }
    }


    // ===  XỬ LÝ HIỂN THỊ LỊCH SỬ NGƯỜI DÙNG (SỬ DỤNG TRANSACTION DAO) ===
    private void handleViewUserHistory(User selectedUser) {
        if (selectedUser == null) return;

        Alert historyAlert = new Alert(Alert.AlertType.INFORMATION);
        historyAlert.setTitle("Lịch sử Giao dịch Người dùng");
        historyAlert.setHeaderText("Lịch sử Mượn/Trả của Người dùng ID: " + selectedUser.getId() + " - " + selectedUser.getName());

        TextArea historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setPrefRowCount(15);
        historyArea.setPrefColumnCount(50);

        StringBuilder sb = new StringBuilder();

        // GỌI TRANSACTION DAO ĐỂ LẤY LỊCH SỬ TỪ DB
        List<UserInUserHistory> historyFromDB = library.getTransactionDAO().getUserHistory(selectedUser.getId());

        if (historyFromDB != null && !historyFromDB.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (UserInUserHistory historyItem : historyFromDB) {
                sb.append(historyItem.getLocalDateTime().format(formatter))
                        .append(" - Sách ID: ").append(historyItem.getId())
                        .append(" - Tên Sách: ").append(historyItem.getBookName()) // Lấy BookName
                        .append(" - Trạng thái: ").append(historyItem.getTrangThai())
                        .append("\n");
            }
        }

        if (sb.length() == 0) {
            sb.append("Chưa có lịch sử giao dịch nào cho người dùng này.");
        }

        historyArea.setText(sb.toString());

        VBox dialogContent = new VBox(10, new Label("Chi tiết lịch sử giao dịch:"), historyArea);
        historyAlert.getDialogPane().setContent(dialogContent);

        historyAlert.showAndWait();
    }

    private Pane createBorrowReturnPane() {
        // Giữ nguyên logic, vì QuanLyMuonTra đã được sửa để gọi DAO
        TextField userIdField = new TextField(); userIdField.setPromptText("User ID");
        TextField bookIdField = new TextField(); bookIdField.setPromptText("Book ID");

        Button borrowBtn = new Button("📚 Mượn sách");
        borrowBtn.setOnAction(e -> {
            String message = quanLyMuonTra.choMuonSach(userIdField.getText(), bookIdField.getText());
            // Cập nhật lại UI sau khi mượn
            bookData.setAll(library.getBooks());
            userData.setAll(library.getListUsers()); // Cập nhật số sách đã mượn
            bookGalleryTab.updateBookGallery();

            Alert.AlertType type = message.contains("Thành công") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
            showAlert(type, "Mượn sách", message);
            userIdField.clear(); bookIdField.clear();
        });

        Button returnBtn = new Button("📖 Trả sách");
        returnBtn.setOnAction(e -> {
            String message = quanLyMuonTra.traSach(userIdField.getText(), bookIdField.getText());
            // Cập nhật lại UI sau khi trả
            bookData.setAll(library.getBooks());
            userData.setAll(library.getListUsers()); // Cập nhật số sách đã mượn
            bookGalleryTab.updateBookGallery();

            Alert.AlertType type = message.contains("Thành công") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
            showAlert(type, "Trả sách", message);
            userIdField.clear(); bookIdField.clear();
        });

        HBox pane = new HBox(10, userIdField, bookIdField, borrowBtn, returnBtn);
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-alignment: center-left;");
        return pane;
    }

    // Trong LibraryApp.java

    private Pane createSearchPane() {

        // --- KHAI BÁO CÁC THÀNH PHẦN MỚI ---

        // 1. Dùng FlowPane đã khai báo sẵn trong class để hiển thị Gallery kết quả
        // private final FlowPane searchGalleryPane = new FlowPane(); // Đã có trong LibraryApp
        searchGalleryPane.setHgap(15);
        searchGalleryPane.setVgap(15);
        searchGalleryPane.setPadding(new Insets(10));
        searchGalleryPane.setStyle("-fx-alignment: top-left;");

        // Dùng ScrollPane để chứa FlowPane
        ScrollPane galleryScrollPane = new ScrollPane(searchGalleryPane);
        galleryScrollPane.setFitToWidth(true);
        galleryScrollPane.setPrefHeight(600);

        // --- KHỐI ĐIỀU KHIỂN TÌM KIẾM (Controls) ---

        TextField searchField = new TextField();
        searchField.setPromptText("Nhập ID, Tên, hoặc Tác giả");
        searchField.setPrefWidth(300);

        Label resultLabel = new Label("Sử dụng các nút bên dưới để tìm kiếm sách.");
        resultLabel.setStyle("-fx-font-style: italic;");

        // Nút mới: Tìm kiếm theo ID (Khớp chính xác)
        Button searchByIdBtn = new Button("🔢 Tìm theo ID");
        searchByIdBtn.setOnAction(e -> handleSearch(searchField.getText(), "ID", resultLabel));

        // Nút mới: Tìm kiếm TỔNG HỢP (Tên hoặc Tác giả - LINH HOẠT)
        Button searchCombinedBtn = new Button("🔎 Tìm Tên/Tác giả");
        searchCombinedBtn.setOnAction(e -> handleSearch(searchField.getText(), "COMBINED", resultLabel));

        // Nút cũ: Tìm theo tên (LINH HOẠT)
        Button searchByNameBtn = new Button("📝 Tìm theo Tên");
        searchByNameBtn.setOnAction(e -> handleSearch(searchField.getText(), "NAME", resultLabel));

        // Nút cũ: Tìm theo tác giả (LINH HOẠT)
        Button searchByAuthorBtn = new Button("✍️ Tìm theo Tác giả");
        searchByAuthorBtn.setOnAction(e -> handleSearch(searchField.getText(), "AUTHOR", resultLabel));

        Button resetBtn = new Button("🗑️ Xóa kết quả");
        resetBtn.setOnAction(e -> {
            searchGalleryPane.getChildren().clear();
            searchField.clear();
            resultLabel.setText("Sẵn sàng cho tìm kiếm mới.");
        });

        HBox controls = new HBox(10, searchField, searchByIdBtn, searchCombinedBtn, searchByNameBtn, searchByAuthorBtn, resetBtn);
        controls.setPadding(new Insets(10));

        // Thay TableView bằng ScrollPane chứa FlowPane
        VBox pane = new VBox(10, resultLabel, controls, galleryScrollPane);
        pane.setPadding(new Insets(10));

        return pane;
    }
    /**
     * Xử lý logic tìm kiếm sách và cập nhật FlowPane kết quả.
     */
    private void handleSearch(String query, String searchType, Label resultLabel) {
        if (query.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập từ khóa tìm kiếm.");
            return;
        }

        List<Book> results = switch (searchType) {
            // ID: Khớp chính xác (Dùng getBookById vì BookDAO chỉ hỗ trợ tìm kiếm exact ID)
            case "ID" -> {
                Book found = library.getBookDAO().getBookById(query.trim());
                yield found != null ? List.of(found) : List.of();
            }
            // COMBINED: Tên hoặc Tác giả (Linh hoạt/Partial)
            case "COMBINED" -> searchService.searchCombined(query);
            // NAME: Tên (Linh hoạt/Partial)
            case "NAME" -> searchService.searchByName(query);
            // AUTHOR: Tác giả (Linh hoạt/Partial)
            case "AUTHOR" -> searchService.searchByAuthor(query);
            default -> List.of();
        };

        // 1. Cập nhật Gallery
        updateSearchGallery(results);

        // 2. Cập nhật Label thông báo
        String typeText = switch (searchType) {
            case "ID" -> "ID";
            case "COMBINED" -> "Tên hoặc Tác giả";
            case "NAME" -> "Tên";
            case "AUTHOR" -> "Tác giả";
            default -> "";
        };

        resultLabel.setText("Đã tìm thấy " + results.size() + " kết quả theo " + typeText + ".");

        if (results.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không tìm thấy sách nào phù hợp.");
        }
    }
    /**
     * Cập nhật FlowPane kết quả tìm kiếm (searchGalleryPane) với các thẻ sách.
     */
    private void updateSearchGallery(List<Book> books) {
        searchGalleryPane.getChildren().clear();

        // Xác định URL ảnh mặc định an toàn
        URL defaultUrl = getClass().getResource("/resources/default_cover.png");
        if (defaultUrl == null) {
            defaultUrl = getClass().getResource("/default_cover.png");
        }
        final String DEFAULT_IMAGE_URL = (defaultUrl != null) ? defaultUrl.toExternalForm() : bookGalleryTab.getPlaceholderBase64Url(); // Giả định có getter

        for (Book book : books) {
            // Tái sử dụng logic tạo VBox (Thẻ sách) từ BookGalleryTab
            VBox bookBox = new VBox(5);
            bookBox.setPrefWidth(150);
            bookBox.getStyleClass().add("gallery-book-box");
            bookBox.setPadding(new Insets(10)); // Thêm padding để nhìn đẹp hơn

            // --- 1. LOGIC TẢI ẢNH BÌA VÀ RESIZE ---
            Image image;
            String path = book.getImagePath();
            final int TARGET_WIDTH = 120;
            final int TARGET_HEIGHT = 160;

            try {
                if (path != null && !path.isEmpty()) {
                    File imageFile = new File(path);
                    if (imageFile.exists()) {
                        BufferedImage originalAWTImage = ImageIO.read(imageFile);
                        // Gọi hàm resize chung của LibraryApp
                        image = convertAndResize(originalAWTImage);
                        if (image == null) throw new IOException("Resize thất bại hoặc file rỗng.");
                    } else {
                        throw new IOException("File ảnh không tồn tại: " + path);
                    }
                } else {
                    image = new Image(DEFAULT_IMAGE_URL, TARGET_WIDTH, TARGET_HEIGHT, true, true);
                }
            } catch (Exception e) {
                System.err.println("Lỗi tải/resize ảnh cho sách " + book.getId() + ": " + e.getMessage());
                image = new Image(DEFAULT_IMAGE_URL, TARGET_WIDTH, TARGET_HEIGHT, true, true);
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(TARGET_WIDTH);
            imageView.setFitHeight(TARGET_HEIGHT);
            imageView.setPreserveRatio(true);

            // --- 2. THÔNG TIN SÁCH (LABELS) ---
            Label idLabel = new Label("ID: " + book.getId());

            Label nameLabel = new Label(book.getName());
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(140);
            nameLabel.getStyleClass().add("book-name-label");

            Label authorLabel = new Label("Tác giả: " + book.getAuthor());
            authorLabel.getStyleClass().add("book-author-label");

            // 3. Trạng thái sách
            Label statusLabel = new Label(book.isStatus() ? "CÓ SẴN" : "ĐÃ MƯỢN");
            statusLabel.getStyleClass().add(book.isStatus() ? "available-status" : "borrowed-status");

            bookBox.getChildren().addAll(
                    imageView,
                    new Separator(),
                    idLabel,
                    nameLabel,
                    authorLabel,
                    statusLabel
            );
            searchGalleryPane.getChildren().add(bookBox);
        }
    }
    // === TẠO GIAO DIỆN LỊCH SỬ CHUNG (SỬ DỤNG TRANSACTION DAO) ===
    private Pane createHistoryPane() {
        TextArea historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setPrefHeight(500);
        historyArea.setPromptText("Nhấn 'Cập nhật lịch sử' để xem chi tiết mượn trả.");

        Button refreshBtn = new Button("📜 Cập nhật lịch sử");
        refreshBtn.setOnAction(e -> {
            StringBuilder sb = new StringBuilder("--- LỊCH SỬ GIAO DỊCH ---\n");
            sb.append(String.format("%-20s | %-8s | %-25s | %s\n", "Thời Gian", "User ID", "Tên Sách", "Trạng Thái"));
            sb.append("--------------------------------------------------------------------------------\n");

            // GỌI TRANSACTION DAO ĐỂ LẤY TẤT CẢ LỊCH SỬ
            List<UserInUserHistory> allHistory = library.getTransactionDAO().getAllTransactionsHistory();

            if (allHistory.isEmpty()) {
                sb.append("Chưa có giao dịch nào được ghi lại.");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (UserInUserHistory h : allHistory) {
                    sb.append(String.format("%-20s | %-8s | %-25s | %s\n",
                            h.getLocalDateTime().format(formatter),
                            h.getName(),
                            h.getBookName(),
                            h.getTrangThai()
                    ));
                }
            }
            historyArea.setText(sb.toString());
        });

        VBox pane = new VBox(10, refreshBtn, historyArea);
        pane.setPadding(new Insets(10));
        return pane;
    }

    // =======================================================================
    // PHƯƠNG THỨC HỖ TRỢ CHUNG
    // =======================================================================

    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // === LOGIC KHỞI TẠO DỮ LIỆU ĐÃ ĐƯỢC SỬA ĐỔI ĐỂ KIỂM TRA DB ===
    private void initializeData() {
        // Chỉ thêm dữ liệu giả nếu DB rỗng
        if (library.getListUsers().isEmpty() && library.getBooks().isEmpty()) {
            System.out.println("-> Khởi tạo dữ liệu giả lần đầu.");

            library.addBook(new Book("B001", "Dune", "Frank Herbert", "1965"));
            library.addBook(new Book("B002", "1984", "George Orwell", "1949"));
            library.addBook(new Book("B003", "Harry Potter", "J.K. Rowling", "1997"));
            library.addBook(new Book("B004", "To Kill a Mockingbird", "Harper Lee", "1960"));
            library.addBook(new Book("B005", "The Great Gatsby", "F. Scott Fitzgerald", "1925"));
            library.addBook(new Book("B006", "Pride and Prejudice", "Jane Austen", "1813"));
            library.addBook(new Book("B007", "The Lord of the Rings", "J.R.R. Tolkien", "1954"));
            library.addBook(new Book("B008", "Moby Dick", "Herman Melville", "1851"));
            library.addBook(new Book("B009", "The Hobbit", "J.R.R. Tolkien", "1937"));
            library.addBook(new Book("B010", "Crime and Punishment", "Fyodor Dostoevsky", "1866"));
            library.addBook(new Book("B011", "One Hundred Years of Solitude", "Gabriel García Márquez", "1967"));
            library.addBook(new Book("B012", "The Alchemist", "Paulo Coelho", "1988"));
            library.addBook(new Book("B013", "Sapiens: A Brief History of Humankind", "Yuval Noah Harari", "2011"));

            library.addUser(new User("U001", "Nguyễn Văn Hiếu"));
            library.addUser(new User("U002", "Trần Thị Quỳnh"));
            library.addUser(new User("U003", "Lê Văn Cường"));
            library.addUser(new User("U004", "Phạm Thị Dung"));
            library.addUser(new User("U005", "Hoàng Anh Tuấn"));
            library.addUser(new User("U006", "Vũ Mai Phương"));
            library.addUser(new User("U007", "Đặng Quang Huy"));
            library.addUser(new User("U008", "Ngô Thanh Thảo"));
            library.addUser(new User("U009", "Bùi Trọng Nghĩa"));
            library.addUser(new User("U010", "Dương Thu Huyền"));
            library.addUser(new User("U011", "Đỗ Minh Khải"));
            library.addUser(new User("U012", "Trịnh Thị Ngọc"));
            library.addUser(new User("U013", "Cao Xuân Trường"));
            library.addUser(new User("U014", "Nguyễn Diệu Linh"));
            library.addUser(new User("U015", "Tô Đức Anh"));
            library.addUser(new User("U016", "Lý Cẩm Tú"));
            library.addUser(new User("U017", "Chu Văn Kiên"));
            library.addUser(new User("U018", "Tống Thị Hằng"));
            library.addUser(new User("U019", "Hồ Phúc Lộc"));
            library.addUser(new User("U020", "Phan Kim Chi"));
            library.addUser(new User("U021", "Vương Đình Tùng"));
            library.addUser(new User("U022", "Lê Hải Yến"));
            library.addUser(new User("U023", "Nguyễn Trường Giang"));
            library.addUser(new User("U024", "Mai Thị Quỳnh"));
            library.addUser(new User("U025", "Võ Thành Đạt"));
            library.addUser(new User("U026", "Hồ Ngọc Hà"));
            library.addUser(new User("U027", "Trần Bá Đạo"));
            library.addUser(new User("U028", "Đoàn Minh Hậu"));
            library.addUser(new User("U029", "Tạ Bích Loan"));
            library.addUser(new User("U030", "Nguyễn Lệ Quyên"));
        } else {
            System.out.println("-> DB đã có dữ liệu, bỏ qua khởi tạo giả.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}