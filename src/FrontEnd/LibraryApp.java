package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.Histories.UserInUserHistory;
import BackEnd.LibraryQ.Library;
import BackEnd.LibraryQ.QuanLyMuonTra;
import BackEnd.LibraryQ.SearchService;
import BackEnd.Sattistics.BookStatistic;
import BackEnd.Sattistics.UserStatistic;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import Database.DatabaseManager;
import XuLiAnh.ImageResizer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
    private static final String ADMIN_PASSWORD = "admin";

    // 1. BACKEND CORE
    private final Library library = new Library();
    private final QuanLyMuonTra quanLyMuonTra = new QuanLyMuonTra(library);
    private final SearchService searchService = new SearchService(library);
    private final UserStatistic userStatistic = new UserStatistic(library);
    private final BookStatistic bookStatistic = new BookStatistic(library);

    // 2. DATA HOLDER (ObservableList)
    private final ObservableList<Book> bookData = FXCollections.observableArrayList();
    private final ObservableList<User> userData = FXCollections.observableArrayList();

    // 3. UI COMPONENTS
    private final TableView<User> userTable = new TableView<>();
    private final FlowPane bookFlowPane = new FlowPane();
    private BookGalleryTab bookGalleryTab;
    private HomeTab homeTab;
    private final FlowPane searchGalleryPane = new FlowPane();


    // darkMode=========
    private boolean isDarkMode = false;
    private Stage primaryStage; // Lưu stage để dùng lại
    private BorderPane rootLayout;
    private TableView<BackEnd.Histories.CurrentTransaction> borrowedTable;

    // Phương thức resize ảnh
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
    public void start(Stage stage) {
        this.primaryStage = stage;
        DatabaseManager.initializeDatabase();
        initializeData();
        runAutoEmailCheck();

        // Gọi hàm vẽ giao diện lần đầu
        rebuildUI();
    }

    private void runAutoEmailCheck() {
        Task<Void> emailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Đang kiểm tra sách quá hạn để gửi mail...");

// 1. LẤY CẤU HÌNH TỪ DB TRƯỚC
                String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
                int maxDays = 60; // Mặc định nếu chưa cài đặt
                try {
                    if (maxDaysStr != null && !maxDaysStr.isEmpty()) {
                        maxDays = Integer.parseInt(maxDaysStr);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Lỗi đọc cấu hình ngày: " + e.getMessage());
                }

                // 2. TRUYỀN maxDays VÀO DAO
                List<String[]> overdueList = library.getTransactionDAO().getOverdueTransactionsWithEmail(maxDays);
                for (String[] record : overdueList) {
                    String email = record[0];
                    String userName = record[1];
                    String bookName = record[2];
                    long days = Long.parseLong(record[3]);

                    // Gửi mail (Hàm này tốn thời gian nên phải ở trong Task)
                    BackEnd.Utils.EmailService.sendOverdueNotification(library,email, userName, bookName, days);

                    // Nghỉ 2 giây giữa các lần gửi để tránh bị Google chặn vì spam
                    Thread.sleep(2000);
                }
                return null;
            }
        };

        // Chạy luồng
        Thread thread = new Thread(emailTask);
        thread.setDaemon(true); // Tự tắt khi tắt app
        thread.start();
    }
    /**
     * Hàm dựng lại toàn bộ giao diện (Gọi khi khởi động hoặc khi đổi ngôn ngữ)
     */
    private void rebuildUI() {
        // 1. Tải dữ liệu mới nhất từ DB
        bookData.setAll(library.getBooks());
        userData.setAll(library.getListUsers());

        // 2. KHỞI TẠO LAYOUT & SCENE TRƯỚC (Để truyền vào SettingsTab)
        rootLayout = new BorderPane();
        Scene scene = new Scene(rootLayout, 1100, 750);

        // Áp dụng CSS Dark Mode nếu biến trạng thái đang bật
        if (this.isDarkMode) {
            scene.getRoot().getStyleClass().add("dark-mode");
        }

        // 3. Khởi tạo TabPane (Ẩn header mặc định để dùng NavBar tùy chỉnh)
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("hidden-header-tab-pane");


        // 4. Khởi tạo các Tab con
        homeTab = new HomeTab(library);
        bookGalleryTab = new BookGalleryTab(library, bookFlowPane, this::convertAndResize);
// Thêm sự kiện: Khi chuyển sang tab "Trang chủ", tự động làm mới dữ liệu
        Tab tabHome = new Tab(LanguageManager.getText("tab.home"), homeTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabHome && homeTab != null) {
                homeTab.refreshData();
            }
        });
        // Khi đổi ngôn ngữ, ta cần lưu trạng thái Dark Mode hiện tại
        Runnable onLanguageChangeCallback = () -> {
            // Cập nhật biến cờ nhớ trạng thái từ giao diện hiện tại
            this.isDarkMode = scene.getRoot().getStyleClass().contains("dark-mode");
            // Sau đó mới vẽ lại
            rebuildUI();
        };

        SettingsTab settingsTabObj = new SettingsTab(
                library,
                scene,
                onLanguageChangeCallback,
                this.isDarkMode
        );

        // Tạo danh sách Tab với tên lấy từ file ngôn ngữ
        Tab tabGallery = new Tab(LanguageManager.getText("tab.gallery"), bookGalleryTab);
        Tab tabBooks = new Tab(LanguageManager.getText("tab.manage_book"), new BookManagementTab(library, bookData, bookFlowPane, bookGalleryTab).getPane());
        Tab tabUsers = new Tab(LanguageManager.getText("tab.users"), createUserPane());
        Tab tabBorrowReturn = new Tab(LanguageManager.getText("tab.borrow"), createBorrowReturnPane());
        Tab tabSearch = new Tab(LanguageManager.getText("tab.search"), createSearchPane());
        Tab tabHistory = new Tab(LanguageManager.getText("tab.history"), createHistoryPane());
        Tab tabStatistics = new Tab(LanguageManager.getText("tab.stats"), createStatisticsPane());
        Tab tabSettings = new Tab(LanguageManager.getText("tab.settings"), settingsTabObj);

        // Thêm tất cả vào TabPane (Tab Cài đặt nằm cuối cùng)
        tabPane.getTabs().addAll(tabHome, tabGallery, tabBooks, tabUsers, tabBorrowReturn, tabSearch, tabHistory, tabStatistics, tabSettings);

        // 5. Tạo Thanh Navigation trượt (NavBar)
        ScrollPane navBar = createScrollableNavBar(tabPane);

        // 6. Cấu hình Layout chính (Đã loại bỏ BottomBar cũ)
        rootLayout.setTop(navBar);
        rootLayout.setCenter(tabPane);
        // Không còn setBottom(...) nữa vì nút đã chuyển vào SettingsTab

        // 7. Load CSS và hiển thị Stage
        java.net.URL cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl == null) cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("📚 " + LanguageManager.getText("tab.manage_book"));
        primaryStage.show();
    }
    // === TẠO GIAO DIỆN THỐNG KÊ ===
    private Pane createStatisticsPane() {
        TableView<User> topUserTable = new TableView<>();
        topUserTable.setPrefHeight(600);
        topUserTable.setMinWidth(500);

        TableColumn<User, String> colUserRank = new TableColumn<>(LanguageManager.getText("col.rank"));
        colUserRank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topUserTable.getItems().indexOf(data.getValue()) + 1)));
        colUserRank.setPrefWidth(50);
        colUserRank.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        TableColumn<User, String> colUserId = new TableColumn<>(LanguageManager.getText("col.id"));
        colUserId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colUserId.setPrefWidth(100);

        TableColumn<User, String> colUserName = new TableColumn<>(LanguageManager.getText("col.user_name"));
        colUserName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colUserName.setPrefWidth(220);

        TableColumn<User, String> colUserBorrowedCount = new TableColumn<>(LanguageManager.getText("col.borrowed_books"));
        colUserBorrowedCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoSachDaMuon())));
        colUserBorrowedCount.setPrefWidth(120);
        colUserBorrowedCount.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        topUserTable.getColumns().addAll(colUserRank, colUserId, colUserName, colUserBorrowedCount);

        Label titleUser = new Label(LanguageManager.getText("title.top_users"));
        titleUser.getStyleClass().add("page-title");
        VBox statsUser = new VBox(10, titleUser, topUserTable);
        statsUser.setAlignment(Pos.TOP_CENTER);

        TableView<Book> topBookTable = new TableView<>();
        topBookTable.setPrefHeight(600);
        topBookTable.setMinWidth(500);

        TableColumn<Book, String> colBookRank = new TableColumn<>(LanguageManager.getText("col.rank"));
        colBookRank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topBookTable.getItems().indexOf(data.getValue()) + 1)));
        colBookRank.setPrefWidth(50);
        colBookRank.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        TableColumn<Book, String> colBookId = new TableColumn<>(LanguageManager.getText("col.id"));
        colBookId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colBookId.setPrefWidth(80);

        TableColumn<Book, String> colBookName = new TableColumn<>(LanguageManager.getText("col.name"));
        colBookName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colBookName.setPrefWidth(250);

        TableColumn<Book, String> colBookCount = new TableColumn<>(LanguageManager.getText("col.borrow_count"));
        colBookCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoLuotMuon())));
        colBookCount.setPrefWidth(120);
        colBookCount.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        topBookTable.getColumns().addAll(colBookRank, colBookId, colBookName, colBookCount);

        Label titleBook = new Label(LanguageManager.getText("title.top_books"));
        titleBook.getStyleClass().add("page-title");
        VBox statsBook = new VBox(10, titleBook, topBookTable);
        statsBook.setAlignment(Pos.TOP_CENTER);

        Button refreshBtn = new Button(LanguageManager.getText("btn.refresh"));
        refreshBtn.setOnAction(e -> {
            List<User> allUsersSorted = userStatistic.danhSachNguoiDung();
            int userLimit = Math.min(20, allUsersSorted.size());
            List<User> topUsers = allUsersSorted.subList(0, userLimit);
            topUserTable.setItems(FXCollections.observableArrayList(topUsers));
            titleUser.setText(LanguageManager.getText("title.top_users") + String.format(" (%d)", allUsersSorted.size()));
            topUserTable.refresh();

            List<Book> allBooksSorted = bookStatistic.getTopBook();
            int bookLimit = Math.min(20, allBooksSorted.size());
            List<Book> topBooks = allBooksSorted.subList(0, bookLimit);
            topBookTable.setItems(FXCollections.observableArrayList(topBooks));
            titleBook.setText(LanguageManager.getText("title.top_books") + String.format(" (%d)", allBooksSorted.size()));
            topBookTable.refresh();

            showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.stats_update_title"), LanguageManager.getText("msg.stats_updated"));
        });

        // nút xuất báo cáo
        Button exportBtn = new Button(LanguageManager.getText("btn.export"));
        exportBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        exportBtn.setOnAction(e -> handleExportReport());
        // nút reset ( nguy hiểm)
        Button resetBtn = new Button(LanguageManager.getText("btn.factory_reset"));
        // Màu đỏ cảnh báo
        resetBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        resetBtn.setOnAction(e -> handleFactoryReset());

        HBox statsLayout = new HBox(20, statsUser, statsBook);
        statsLayout.setAlignment(Pos.TOP_CENTER);
        statsLayout.setHgrow(statsUser, Priority.ALWAYS);
        statsLayout.setHgrow(statsBook, Priority.ALWAYS);

        HBox buttonBar = new HBox(10, refreshBtn, exportBtn, resetBtn);
        buttonBar.setAlignment(Pos.CENTER);

        VBox pane = new VBox(15, buttonBar, statsLayout);
        pane.setPadding(new Insets(10));
        pane.setAlignment(Pos.TOP_CENTER);

        return pane;
    }

    // === TẠO GIAO DIỆN QUẢN LÝ NGƯỜI DÙNG ===
    private File selectedUserAvatar = null;
    // === 2. TẠO GIAO DIỆN QUẢN LÝ NGƯỜI DÙNG (CÓ EMAIL + AVATAR + XÓA BẢO MẬT) ===
    private Pane createUserPane() {
        // --- 1. Setup TableView ---
        TableColumn<User, String> colId = new TableColumn<>(BackEnd.Utils.LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(100);

        TableColumn<User, String> colName = new TableColumn<>(BackEnd.Utils.LanguageManager.getText("col.user_name"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<User, String> colEmail = new TableColumn<>(BackEnd.Utils.LanguageManager.getText("col.email"));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colEmail.setPrefWidth(250);

        userTable.getColumns().clear();
        userTable.getColumns().addAll(colId, colName, colEmail);
        userTable.setItems(userData);

        // --- 1. VÙNG NHẬP LIỆU (Input Form) ---
        TextField idField = new TextField(); idField.setPromptText(LanguageManager.getText("col.id")); idField.setPrefWidth(120);
        TextField nameField = new TextField(); nameField.setPromptText(LanguageManager.getText("col.name.user")); nameField.setPrefWidth(200);
        TextField emailField = new TextField(); emailField.setPromptText(LanguageManager.getText("col.email")); emailField.setPrefWidth(200);

        // Nút chọn ảnh nhỏ gọn hơn
        Button btnAvatar = new Button("📷");
        btnAvatar.setTooltip(new Tooltip(LanguageManager.getText("btn.photo")));
        Label lblAvatarStatus = new Label(LanguageManager.getText("label.not_selected"));
        lblAvatarStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        btnAvatar.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
            File f = fc.showOpenDialog(null);
            if (f != null) {
                selectedUserAvatar = f; // Biến toàn cục lưu file ảnh
                lblAvatarStatus.setText(BackEnd.Utils.LanguageManager.getText("label.selected"));
            }
        });
        // Gom nhóm Input vào FlowPane
        FlowPane inputPane = new FlowPane(15, 10);
        inputPane.setPadding(new Insets(10));
        inputPane.getStyleClass().add("input-panel");
        inputPane.getChildren().addAll(
                idField, nameField, emailField,
                new HBox(5, btnAvatar, lblAvatarStatus)
        );

        // Nút Thêm
        Button addBtn = new Button(LanguageManager.getText("btn.add_user"));
        addBtn.setStyle("-fx-base: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;"); // Nổi bật nút Thêm
        addBtn.setOnAction(e -> {
            if (!idField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                String avatarFileName = null;

                // --- LOGIC LƯU ẢNH ---
                if (selectedUserAvatar != null) {
                    try {
                        // Copy ảnh vào thư mục dự án và lấy TÊN FILE (ví dụ: img_uuid.jpg)
                        avatarFileName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUserAvatar);
                        System.out.println("Đã lưu avatar mới: " + avatarFileName);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu ảnh: " + ex.getMessage());
                        return; // Dừng lại nếu lỗi ảnh
                    }
                }

                // Lấy thông tin từ giao diện
                String uId = idField.getText().trim();
                String uName = nameField.getText().trim();
                String uEmail = emailField.getText().trim();
                // Tạo User với tên file ảnh (chứ không phải đường dẫn tuyệt đối của file gốc)
                User user = new User(idField.getText(), nameField.getText(), emailField.getText(), avatarFileName);

                boolean success = library.getUserDAO().addUser(user); // Đảm bảo UserDAO trả về boolean

                if (success) {
                    userData.setAll(library.getListUsers());
                    showAlert(Alert.AlertType.INFORMATION, BackEnd.Utils.LanguageManager.getText("msg.success"), BackEnd.Utils.LanguageManager.getText("msg.user_added"));

                    // ============================================================
                    // GỬI EMAIL CHÀO MỪNG (CHẠY NGẦM)
                    // ============================================================
                    if (!uEmail.isEmpty()) {
                        new Thread(() -> {
                            BackEnd.Utils.EmailService.sendWelcomeEmail(
                                    library,  // Truyền đối tượng library để lấy cấu hình
                                    uEmail,   // Email người nhận
                                    uName,    // Tên
                                    uId       // ID
                            );
                        }).start();
                    }
                    // ============================================================

                    // Reset form
                    idField.clear(); nameField.clear(); emailField.clear();
                    selectedUserAvatar = null;
                    lblAvatarStatus.setText(BackEnd.Utils.LanguageManager.getText("label.not_selected"));
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm người dùng vào DB (Trùng ID?).");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, BackEnd.Utils.LanguageManager.getText("msg.error"), BackEnd.Utils.LanguageManager.getText("msg.user_missing_info"));
            }
        });

        // --- NÚT XÓA NGƯỜI DÙNG (CÓ MẬT KHẨU BẢO VỆ) ---
        Button deleteBtn = new Button(LanguageManager.getText("btn.delete_user"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, BackEnd.Utils.LanguageManager.getText("msg.error"), BackEnd.Utils.LanguageManager.getText("msg.select_user_delete"));
                return;
            }

            // 1. Tạo hộp thoại nhập mật khẩu
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle(BackEnd.Utils.LanguageManager.getText("title.confirm_delete"));
            passwordDialog.setHeaderText(BackEnd.Utils.LanguageManager.getText("header.delete_user") + "\n" + selected.getName());
            passwordDialog.setContentText(BackEnd.Utils.LanguageManager.getText("content.enter_pass"));

            // 2. Xử lý kết quả nhập
            java.util.Optional<String> result = passwordDialog.showAndWait();
            if (result.isPresent()) {
                String enteredPass = result.get();
                // Mật khẩu cứng là "admin"
                if ("admin".equals(enteredPass)) {
                    // Xóa trong DB
                    boolean deleted = library.deleteUser(selected.getId());
                    if (deleted) {
                        userData.setAll(library.getListUsers()); // Refresh bảng
                        showAlert(Alert.AlertType.INFORMATION, BackEnd.Utils.LanguageManager.getText("msg.success"), BackEnd.Utils.LanguageManager.getText("msg.user_deleted"));
                    } else {
                        showAlert(Alert.AlertType.ERROR, BackEnd.Utils.LanguageManager.getText("msg.error"), "Không thể xóa (Có thể User đang mượn sách?)");
                    }
                } else {
                    // Sai mật khẩu
                    showAlert(Alert.AlertType.ERROR, BackEnd.Utils.LanguageManager.getText("msg.error"), BackEnd.Utils.LanguageManager.getText("msg.wrong_pass"));
                }
            }
        });

        // --- Nút xem lịch sử ---
        Button viewUserHistoryBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.view_history_user"));
        viewUserHistoryBtn.setDisable(true);
        viewUserHistoryBtn.setOnAction(e -> handleViewUserHistory(userTable.getSelectionModel().getSelectedItem()));

        // --- Nút Tạo Thẻ ---
        Button btnCreateCard = new Button(BackEnd.Utils.LanguageManager.getText("btn.create_card"));
        btnCreateCard.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
        btnCreateCard.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING,
                        BackEnd.Utils.LanguageManager.getText("msg.error"),
                        BackEnd.Utils.LanguageManager.getText("msg.select_user_card"));
                return;
            }

            Alert previewAlert = new Alert(Alert.AlertType.CONFIRMATION);
            previewAlert.setTitle(BackEnd.Utils.LanguageManager.getText("title.card_preview"));
            previewAlert.setHeaderText(BackEnd.Utils.LanguageManager.getText("header.card_of") + " " + selected.getName());

            String libName = BackEnd.Utils.LanguageManager.getText("label.library_name");
            System.out.println("Tạo thẻ cho user: " + selected.getName() + ", Avatar Path: " + selected.getAvatarPath());            // --- TRUYỀN ẢNH TẠM (selectedUserAvatar) VÀO HÀM TẠO THẺ ---
            Pane cardView = FrontEnd.CardGenerator.createCardView(selected, libName, null);

            cardView.setScaleX(0.8); cardView.setScaleY(0.8);

            VBox box = new VBox(cardView);
            box.setPadding(new Insets(10));
            box.setAlignment(Pos.CENTER);
            box.setMinHeight(320);

            previewAlert.getDialogPane().setContent(box);

            ButtonType btnSave = new ButtonType(BackEnd.Utils.LanguageManager.getText("btn.save_card_img"), ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType(BackEnd.Utils.LanguageManager.getText("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
            previewAlert.getButtonTypes().setAll(btnSave, btnCancel);

            if (previewAlert.showAndWait().orElse(ButtonType.CANCEL) == btnSave) {

                // Chọn lưu file PDF
                javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                fc.setTitle("Lưu thẻ thư viện");
                fc.setInitialFileName("The_" + selected.getId() + ".pdf"); // Đổi đuôi thành .pdf
                fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

                File dest = fc.showSaveDialog(null);

                if (dest != null) {
                    // Gọi hàm xuất PDF
                    FrontEnd.CardGenerator.saveCardToPDF(selected, dest, selectedUserAvatar);

                    showAlert(Alert.AlertType.INFORMATION,
                            BackEnd.Utils.LanguageManager.getText("msg.success"),
                            BackEnd.Utils.LanguageManager.getText("msg.card_saved"));
                }
            }
        });
        Button editBtn = new Button("✏️ " + LanguageManager.getText("btn.edit"));
        editBtn.getStyleClass().addAll("action-btn", "btn-blue"); // Dùng style chuẩn
        editBtn.setDisable(true); // Mặc định tắt
        editBtn.setOnAction(e -> handleEditUser(userTable, idField, nameField, emailField));

        Button clearBtn = new Button("🔄 " + LanguageManager.getText("btn.refresh"));
        clearBtn.getStyleClass().addAll("action-btn", "btn-warning"); // Màu vàng/cam cho nút hủy
        clearBtn.setOnAction(e -> clearUserFields(userTable, idField, nameField, emailField)); // Gán action
        // Listener cho bảng
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            viewUserHistoryBtn.setDisable(newSelection == null);
        });
        // HBox chứa nút
        HBox actionBar = new HBox(15);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(5, 0, 10, 0));
        actionBar.getChildren().addAll(addBtn,editBtn, deleteBtn,clearBtn, new Separator(javafx.geometry.Orientation.VERTICAL), viewUserHistoryBtn, btnCreateCard);

        // --- 3. Khu vực Tìm kiếm ---
        TextField searchUserField = new TextField();
        searchUserField.setPromptText(BackEnd.Utils.LanguageManager.getText("field.search_user"));
        searchUserField.setPrefWidth(250);

        Button searchIdBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.search_id"));
        searchIdBtn.setOnAction(e -> handleSearchUserById(searchUserField.getText()));

        Button searchNameBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.search_name"));
        searchNameBtn.setOnAction(e -> handleSearchUserByName(searchUserField.getText()));

        Button clearSearchBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.clear_search"));
        clearSearchBtn.setOnAction(e -> {
            userData.setAll(library.getListUsers());
            searchUserField.clear();
            userTable.getSelectionModel().clearSelection();
        });
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("input-panel");
        searchBar.getChildren().addAll(searchUserField, searchIdBtn, searchNameBtn, clearSearchBtn);

        // --- TỔNG HỢP LAYOUT ---
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));


        layout.getChildren().addAll(searchBar, userTable, inputPane, actionBar);

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            // userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSel = newSelection != null;

            editBtn.setDisable(!hasSel);
            deleteBtn.setDisable(!hasSel);
            addBtn.setDisable(hasSel); // Tắt nút Thêm khi đang ở chế độ Sửa/Xem

            if (newSelection != null) {
                // 1. Đổ dữ liệu
                idField.setText(newSelection.getId());
                nameField.setText(newSelection.getName());
                emailField.setText(newSelection.getEmail());
                // Lấy avatarPath cũ để xử lý sau này
                // this.existingAvatarPath = newSelection.getAvatarPath();

                // 2. Khóa ID (KHÔNG CHO SỬA PRIMARY KEY)
                idField.setEditable(false);
            } else {
                // Xóa nội dung khi không chọn ai
                // clearUserFields(); // Giả sử bạn có hàm dọn dẹp này
                idField.clear(); nameField.clear(); emailField.clear();
                idField.setEditable(true);
            }
        });
        return layout;
    }
    // === XỬ LÝ TÌM KIẾM THEO ID (Đã cập nhật ngôn ngữ) ===
    private void handleSearchUserById(String userId) {
        // Kiểm tra dữ liệu đầu vào
        if (userId.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    BackEnd.Utils.LanguageManager.getText("msg.error"),
                    BackEnd.Utils.LanguageManager.getText("msg.search_input_required"));
            return;
        }

        // Gọi Backend tìm kiếm
        User foundUser = library.searchUserById(userId);

        if (foundUser != null) {
            // Nếu tìm thấy: Chọn dòng đó trong bảng và cuộn tới đó
            userTable.getSelectionModel().select(foundUser);
            userTable.scrollTo(foundUser);

            showAlert(Alert.AlertType.INFORMATION,
                    BackEnd.Utils.LanguageManager.getText("msg.success"),
                    BackEnd.Utils.LanguageManager.getText("msg.user_found") + ": " + userId);
        } else {
            // Nếu không thấy: Xóa chọn và báo lỗi
            userTable.getSelectionModel().clearSelection();

            showAlert(Alert.AlertType.WARNING,
                    BackEnd.Utils.LanguageManager.getText("msg.error"),
                    BackEnd.Utils.LanguageManager.getText("msg.user_not_found") + ": " + userId);
        }
    }

    private void handleSearchUserByName(String userName) {
        if (userName.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.search_input_required"));
            return;
        }
        List<User> results = library.searchUserByName(userName);

        if (!results.isEmpty()) {
            userData.setAll(results);
            userTable.getSelectionModel().clearSelection();
            showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.user_found") + " (" + results.size() + ")");
        } else {
            userData.setAll(library.getListUsers());
            userTable.getSelectionModel().clearSelection();
            showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.user_not_found") + ": " + userName);
        }
    }

    private void handleViewUserHistory(User selectedUser) {
        if (selectedUser == null) return;

        Alert historyAlert = new Alert(Alert.AlertType.INFORMATION);
        historyAlert.setTitle(LanguageManager.getText("title.user_history"));
        historyAlert.setHeaderText(LanguageManager.getText("header.user_history") + " " + selectedUser.getName());

        ButtonType btnExport = new ButtonType(LanguageManager.getText("btn.export"), ButtonBar.ButtonData.OTHER);
        ButtonType btnClose = new ButtonType(LanguageManager.getText("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        historyAlert.getButtonTypes().setAll(btnExport, btnClose);

        TextArea historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setPrefRowCount(15);
        historyArea.setPrefColumnCount(50);

        StringBuilder sb = new StringBuilder();
        List<UserInUserHistory> historyFromDB = library.getTransactionDAO().getUserHistory(selectedUser.getId());

        if (historyFromDB != null && !historyFromDB.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (UserInUserHistory historyItem : historyFromDB) {
                sb.append(historyItem.getLocalDateTime().format(formatter))
                        .append(" - ").append(LanguageManager.getText("col.id")).append(": ").append(historyItem.getId())
                        .append(" - ").append(LanguageManager.getText("col.name")).append(": ").append(historyItem.getBookName())
                        .append(" - ").append(LanguageManager.getText("col.status")).append(": ").append(historyItem.getTrangThai())
                        .append("\n");
            }
        }
        if (sb.length() == 0) sb.append(LanguageManager.getText("msg.no_transaction"));

        historyArea.setText(sb.toString());
        VBox dialogContent = new VBox(10, new Label(LanguageManager.getText("label.history_detail")), historyArea);
        historyAlert.getDialogPane().setContent(dialogContent);

        java.util.Optional<ButtonType> result = historyAlert.showAndWait();

        if (result.isPresent() && result.get() == btnExport) {
            if (historyFromDB == null || historyFromDB.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.no_transaction"));
                return;
            }
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle(LanguageManager.getText("title.save_history"));
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fc.setInitialFileName("LichSu_" + selectedUser.getId() + ".csv");
            File file = fc.showSaveDialog(null);

            if (file != null) {
                boolean ok = BackEnd.Utils.ExportUtil.exportHistoryToCSV(historyFromDB, file);
                if (ok) showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.export_success"));
                else showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.export_error"));
            }
        }
    }
    // --- HÀM HỖ TRỢ ĐỔI STYLE TRẠNG THÁI ---
    private void updateStatusStyle(Label label, String styleClass) {
        // Xóa hết các class trạng thái cũ để tránh bị chồng chéo
        label.getStyleClass().removeAll("scan-status-default", "scan-status-success", "scan-status-error");
        // Thêm class mới
        label.getStyleClass().add(styleClass);
    }

    //============================== Tab mượn trả sách ===========================================
    private Pane createBorrowReturnPane() {
        // 1. KHU VỰC NHẬP LIỆU (Cột bên trái)
        VBox inputSection = new VBox(15);
        inputSection.setPadding(new Insets(10));
        inputSection.setPrefWidth(350);
        inputSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 1px 0 0;");

        // Tiêu đề
        Label titleLabel = new Label(BackEnd.Utils.LanguageManager.getText("title.action_section"));
        titleLabel.getStyleClass().add("section-title");

        // --- KHAI BÁO CÁC Ô NHẬP LIỆU THỦ CÔNG ---
        TextField userIdField = new TextField();
        userIdField.setPromptText(BackEnd.Utils.LanguageManager.getText("field.user_id")); // "Nhập User ID..."

        TextField bookIdField = new TextField();
        bookIdField.setPromptText(BackEnd.Utils.LanguageManager.getText("field.book_id")); // "Nhập Book ID..."

        // --- KHU VỰC QUÉT MÃ VẠCH ---
        Label scanLabel = new Label(BackEnd.Utils.LanguageManager.getText("label.scan_title"));
        scanLabel.getStyleClass().add("scan-label");

        TextField barcodeField = new TextField();
        barcodeField.setPromptText(BackEnd.Utils.LanguageManager.getText("field.scan_hint"));

        Label statusLabel = new Label(BackEnd.Utils.LanguageManager.getText("msg.scan_ready"));
        statusLabel.setWrapText(true);
        updateStatusStyle(statusLabel, "scan-status-default");

        barcodeField.setOnAction(e -> {
            String scannedId = barcodeField.getText().trim();
            if (!scannedId.isEmpty()) {
                Book b = library.findBookById(scannedId);
                if (b != null) {
                    bookIdField.setText(b.getId());
                    barcodeField.clear();
                    statusLabel.setText(BackEnd.Utils.LanguageManager.getText("msg.scan_ok") + " " + b.getName());
                    updateStatusStyle(statusLabel, "scan-status-success");
                    java.awt.Toolkit.getDefaultToolkit().beep();
                } else {
                    statusLabel.setText(BackEnd.Utils.LanguageManager.getText("msg.scan_fail") + " " + scannedId);
                    updateStatusStyle(statusLabel, "scan-status-error");
                    barcodeField.selectAll();
                }
            }
        });

        VBox scanBox = new VBox(5, scanLabel, barcodeField, statusLabel);
        scanBox.getStyleClass().add("scan-box");

        // 2. KHU VỰC BẢNG HIỂN THỊ (Bên phải)
        borrowedTable = new TableView<>();
        VBox.setVgrow(borrowedTable, Priority.ALWAYS);

        // Cột Người mượn (Dùng key col.user_info)
        TableColumn<BackEnd.Histories.CurrentTransaction, String> colUser = new TableColumn<>(BackEnd.Utils.LanguageManager.getText("col.user_info"));
        colUser.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUserId() + " - " + data.getValue().getUserName()
        ));
        colUser.setPrefWidth(200);

        // Cột Sách (Dùng key col.book_info)
        TableColumn<BackEnd.Histories.CurrentTransaction, String> colBook = new TableColumn<>(BackEnd.Utils.LanguageManager.getText("col.book_info"));
        colBook.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getBookId() + " - " + data.getValue().getBookName()
        ));
        colBook.setPrefWidth(250);

        // Cột Trạng thái (Dùng key col.time_status)
        TableColumn<BackEnd.Histories.CurrentTransaction, String> colStatus = new TableColumn<>(BackEnd.Utils.LanguageManager.getText("col.time_status"));
        colStatus.setPrefWidth(200);

        colStatus.setCellValueFactory(data -> {
            long days = data.getValue().getDaysElapsed();
            return new SimpleStringProperty(days + " " + BackEnd.Utils.LanguageManager.getText("text.days"));
        });

        // --- 1. LẤY CẤU HÌNH RA NGOÀI (Chỉ đọc DB 1 lần duy nhất) ---
        String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
        final int maxDaysConfig = maxDaysStr.isEmpty() ? 60 : Integer.parseInt(maxDaysStr);
        final int warningThresholdConfig = Math.max(1, maxDaysConfig - 10);

        // --- 2. THIẾT LẬP CELL FACTORY ---
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setStyle("");

                if (empty || item == null) {
                    return;
                }

                // Lấy dữ liệu dòng hiện tại
                BackEnd.Histories.CurrentTransaction trans = getTableView().getItems().get(getIndex());
                long days = trans.getDaysElapsed();
                String daysText = item;

                // DÙNG BIẾN ĐÃ LẤY Ở NGOÀI (maxDaysConfig) -> KHÔNG ĐỌC DB NỮA
                if (days > maxDaysConfig) {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    setText(daysText + " " + BackEnd.Utils.LanguageManager.getText("status.overdue"));
                } else if (days >= warningThresholdConfig) {
                    setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    setText(daysText + " " + BackEnd.Utils.LanguageManager.getText("status.near_due"));
                } else {
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    setText(daysText + " " + BackEnd.Utils.LanguageManager.getText("status.borrowing"));
                }
            }
        });
        borrowedTable.getColumns().addAll(colUser, colBook, colStatus);
        refreshBorrowedTable();

        // 3. NÚT MƯỢN / TRẢ
        Button borrowBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.action_borrow"));
        borrowBtn.setMaxWidth(Double.MAX_VALUE);
        borrowBtn.setOnAction(e -> {
            String message = quanLyMuonTra.choMuonSach(userIdField.getText(), bookIdField.getText());
            bookData.setAll(library.getBooks());
            userData.setAll(library.getListUsers());
            bookGalleryTab.updateBookGallery();
            homeTab.refreshData();
            refreshBorrowedTable();

            boolean isSuccess = message.contains("Thành công") || message.contains("Success");
            Alert.AlertType type = isSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
            showAlert(type, BackEnd.Utils.LanguageManager.getText("title.borrow_dialog"), message);

            if (isSuccess) {
                userIdField.clear(); bookIdField.clear(); barcodeField.clear(); // Xóa sạch form
                statusLabel.setText(BackEnd.Utils.LanguageManager.getText("msg.borrow_done_next"));
                updateStatusStyle(statusLabel, "scan-status-default");
                userIdField.requestFocus();
            }
        });

        Button returnBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.action_return"));
        returnBtn.setMaxWidth(Double.MAX_VALUE);
        returnBtn.setOnAction(e -> {
            String uId = userIdField.getText().trim();
            String bId = bookIdField.getText().trim();

            if (uId.isEmpty() || bId.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, BackEnd.Utils.LanguageManager.getText("msg.error"), BackEnd.Utils.LanguageManager.getText("msg.user_missing_info"));
                return;
            }

            long fine = quanLyMuonTra.calculateFine(uId, bId);
            String currency = library.getSettingsDAO().getSetting("currency_unit");
            if (currency.isEmpty()) currency = "VNĐ";

            if (fine > 0) {
                String fineStr = String.format("%,d %s", fine, currency);
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle(BackEnd.Utils.LanguageManager.getText("msg.fine_warning"));
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText(String.format(BackEnd.Utils.LanguageManager.getText("msg.fine_content"), fineStr));

                // Nếu người dùng bấm OK (Đồng ý nộp phạt)
                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {

                    // --- [MỚI] GHI NHẬN DOANH THU VÀO DB ---
                    String description = "Phạt quá hạn: User " + uId + " - Sách " + bId;
                    library.getFinancialDAO().recordFine(description, fine);
                    System.out.println("Đã ghi nhận doanh thu: " + fineStr);
                    // ---------------------------------------

                } else {
                    return; // Nếu bấm Cancel thì dừng, không trả sách
                }
            }

            String message = quanLyMuonTra.traSach(uId, bId);
            bookData.setAll(library.getBooks());
            userData.setAll(library.getListUsers());
            bookGalleryTab.updateBookGallery();
            homeTab.refreshData();
            refreshBorrowedTable();

            boolean isSuccess = message.contains("Thành công") || message.contains("Success");
            if (isSuccess) {
                userIdField.clear(); bookIdField.clear(); barcodeField.clear(); // Xóa sạch form

                String displayMsg = BackEnd.Utils.LanguageManager.getText("msg.return_success");
                if (fine > 0) displayMsg += "\n" + String.format(BackEnd.Utils.LanguageManager.getText("msg.fine_collected"), String.format("%,d %s", fine, currency));
                showAlert(Alert.AlertType.INFORMATION, BackEnd.Utils.LanguageManager.getText("title.return_dialog"), displayMsg);

                statusLabel.setText(BackEnd.Utils.LanguageManager.getText("msg.return_success"));
                updateStatusStyle(statusLabel, "scan-status-success");
                userIdField.requestFocus();
            } else {
                showAlert(Alert.AlertType.ERROR, BackEnd.Utils.LanguageManager.getText("msg.error"), message);
            }
        });

        // 4. SẮP XẾP LAYOUT
        inputSection.getChildren().addAll(
                titleLabel,
                scanBox,
                new Separator(),
                new Label(BackEnd.Utils.LanguageManager.getText("label.manual_info")), // "Thông tin thủ công:"
                userIdField,
                bookIdField,
                new Separator(),
                borrowBtn, returnBtn
        );

        HBox mainLayout = new HBox(10, inputSection, borrowedTable);
        mainLayout.setPadding(new Insets(10));
        HBox.setHgrow(borrowedTable, Priority.ALWAYS);

        return mainLayout;
    }
    // ============================== login nút mượn/ trả khi muốn reset tất cả khi mượn hoặc trả =====================================
//        Button borrowBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.action_borrow"));
//        borrowBtn.setMaxWidth(Double.MAX_VALUE);
//        borrowBtn.setOnAction(e -> {
//            String message = quanLyMuonTra.choMuonSach(userIdField.getText(), bookIdField.getText());
//
//            // Cập nhật dữ liệu
//            bookData.setAll(library.getBooks());
//            userData.setAll(library.getListUsers());
//            bookGalleryTab.updateBookGallery();
//            homeTab.refreshData();
//            refreshBorrowedTable();
//
//            // Kiểm tra kết quả
//            boolean isSuccess = message.contains("Thành công") || message.contains("Success");
//
//            Alert.AlertType type = isSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
//            showAlert(type, LanguageManager.getText("title.borrow_dialog"), message);
//
//            // NẾU THÀNH CÔNG -> XÓA SẠCH MỌI Ô
//            if (isSuccess) {
//                userIdField.clear();  // <--- Thêm dòng này để xóa User ID
//                bookIdField.clear();
//                barcodeField.clear(); // Xóa cả ô quét mã nếu có
//
//                // Reset trạng thái
//                statusLabel.setText("Mượn thành công. Đã xóa form.");
//                statusLabel.setStyle("-fx-text-fill: blue;");
//
//                // Đưa con trỏ về ô User ID hoặc Barcode tùy thói quen
//                userIdField.requestFocus();
//            }
//        });
//
//        Button returnBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.action_return"));
//        returnBtn.setMaxWidth(Double.MAX_VALUE);
//        returnBtn.setOnAction(e -> {
//            String uId = userIdField.getText().trim();
//            String bId = bookIdField.getText().trim();
//
//            if (uId.isEmpty() || bId.isEmpty()) {
//                showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.user_missing_info"));
//                return;
//            }
//
//            // Logic tính phạt (Giữ nguyên)
//            long fine = quanLyMuonTra.calculateFine(uId, bId);
//            if (fine > 0) {
//                String fineStr = String.format("%,d VNĐ", fine);
//                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
//                confirmAlert.setTitle(LanguageManager.getText("msg.fine_warning"));
//                confirmAlert.setHeaderText(null);
//                confirmAlert.setContentText(String.format(LanguageManager.getText("msg.fine_content"), fineStr));
//
//                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
//                    return;
//                }
//            }
//
//            String message = quanLyMuonTra.traSach(uId, bId);
//
//            // Cập nhật dữ liệu
//            bookData.setAll(library.getBooks());
//            userData.setAll(library.getListUsers());
//            bookGalleryTab.updateBookGallery();
//            homeTab.refreshData();
//            refreshBorrowedTable();
//
//            boolean isSuccess = message.contains("Thành công") || message.contains("Success");
//
//            if (isSuccess) {
//                // NẾU THÀNH CÔNG -> XÓA SẠCH MỌI Ô
//                userIdField.clear();
//                bookIdField.clear();
//                barcodeField.clear();
//
//                String displayMsg = LanguageManager.getText("msg.return_success");
//                if (fine > 0) {
//                    displayMsg += "\n" + String.format(LanguageManager.getText("msg.fine_collected"), String.format("%,d VNĐ", fine));
//                }
//                showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("title.return_dialog"), displayMsg);
//
//                statusLabel.setText("Trả sách thành công. Đã xóa form.");
//                statusLabel.setStyle("-fx-text-fill: blue;");
//                userIdField.requestFocus();
//            } else {
//                showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), message);
//            }
//        });
    // ========================================================================================

    /**
     * Hàm helper để tải lại dữ liệu bảng sách đang mượn
     */
    private void refreshBorrowedTable() {
        if (borrowedTable != null) {
            List<BackEnd.Histories.CurrentTransaction> list = library.getTransactionDAO().getCurrentlyBorrowedBooks();
            borrowedTable.setItems(FXCollections.observableArrayList(list));
            borrowedTable.refresh();
        }
    }

    private Pane createSearchPane() {
        searchGalleryPane.setHgap(15);
        searchGalleryPane.setVgap(15);
        searchGalleryPane.setPadding(new Insets(10));
        searchGalleryPane.setStyle("-fx-alignment: top-left;");

        ScrollPane galleryScrollPane = new ScrollPane(searchGalleryPane);
        galleryScrollPane.setFitToWidth(true);
        galleryScrollPane.setPrefHeight(600);

        TextField searchField = new TextField();
        searchField.setPromptText(LanguageManager.getText("field.search_general"));
        searchField.setPrefWidth(300);

        Label resultLabel = new Label(LanguageManager.getText("label.search_guide"));
        resultLabel.setStyle("-fx-font-style: italic;");

        Button searchByIdBtn = new Button(LanguageManager.getText("btn.search_id_exact"));
        searchByIdBtn.setOnAction(e -> handleSearch(searchField.getText(), "ID", resultLabel));

        Button searchCombinedBtn = new Button(LanguageManager.getText("btn.search_combined"));
        searchCombinedBtn.setOnAction(e -> handleSearch(searchField.getText(), "COMBINED", resultLabel));

        Button searchByNameBtn = new Button(LanguageManager.getText("btn.search_name_partial"));
        searchByNameBtn.setOnAction(e -> handleSearch(searchField.getText(), "NAME", resultLabel));

        Button searchByAuthorBtn = new Button(LanguageManager.getText("btn.search_author"));
        searchByAuthorBtn.setOnAction(e -> handleSearch(searchField.getText(), "AUTHOR", resultLabel));

        Button resetBtn = new Button(LanguageManager.getText("btn.clear_result"));
        resetBtn.setOnAction(e -> {
            searchGalleryPane.getChildren().clear();
            searchField.clear();
            resultLabel.setText(LanguageManager.getText("label.search_ready"));
        });

        HBox controls = new HBox(10, searchField, searchByIdBtn, searchCombinedBtn, searchByNameBtn, searchByAuthorBtn, resetBtn);
        controls.setPadding(new Insets(10));

        VBox pane = new VBox(10, resultLabel, controls, galleryScrollPane);
        pane.setPadding(new Insets(10));
        return pane;
    }

    private void handleSearch(String query, String searchType, Label resultLabel) {
        if (query.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.search_input_required"));
            return;
        }

        List<Book> results = switch (searchType) {
            case "ID" -> {
                Book found = library.getBookDAO().getBookById(query.trim());
                yield found != null ? List.of(found) : List.of();
            }
            case "COMBINED" -> searchService.searchCombined(query);
            case "NAME" -> searchService.searchByName(query);
            case "AUTHOR" -> searchService.searchByAuthor(query);
            default -> List.of();
        };

        updateSearchGallery(results);

        String typeText = switch (searchType) {
            case "ID" -> LanguageManager.getText("type.id");
            case "COMBINED" -> LanguageManager.getText("type.combined");
            case "NAME" -> LanguageManager.getText("type.name");
            case "AUTHOR" -> LanguageManager.getText("type.author");
            default -> "";
        };

        resultLabel.setText(String.format(LanguageManager.getText("msg.search_result"), results.size(), typeText));

        if (results.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.no_match"));
        }
    }

    private void updateSearchGallery(List<Book> books) {
        searchGalleryPane.getChildren().clear();

        URL defaultUrl = getClass().getResource("/resources/default_cover.png");
        if (defaultUrl == null) {
            defaultUrl = getClass().getResource("/default_cover.png");
        }
        final String DEFAULT_IMAGE_URL = (defaultUrl != null) ? defaultUrl.toExternalForm() : bookGalleryTab.getPlaceholderBase64Url();

        for (Book book : books) {
            VBox bookBox = new VBox(5);
            bookBox.setPrefWidth(150);
            bookBox.getStyleClass().add("gallery-book-box");
            bookBox.setPadding(new Insets(10));

            Image image;
            String path = book.getImagePath();
            final int TARGET_WIDTH = 120;
            final int TARGET_HEIGHT = 160;

            // --- LOGIC ÁP DỤNG CACHE ---
            try {
                // 1. Check Cache
                if (path != null && BackEnd.Utils.ImageCache.contains(path)) {
                    image = BackEnd.Utils.ImageCache.get(path);
                }
                // 2. Load File
                else if (path != null && !path.isEmpty()) {
                    File imageFile = BackEnd.Utils.FileUtil.getLocalFile(path);
                    if (imageFile.exists()) {
                        BufferedImage originalAWTImage = ImageIO.read(imageFile);
                        image = convertAndResize(originalAWTImage);
                        if (image == null) throw new IOException("Resize failed");

                        // 3. Lưu vào Cache
                        BackEnd.Utils.ImageCache.put(path, image);
                    } else {
                        throw new IOException("File not found");
                    }
                } else {
                    image = new Image(DEFAULT_IMAGE_URL, TARGET_WIDTH, TARGET_HEIGHT, true, true);
                }
            } catch (Exception e) {
                image = new Image(DEFAULT_IMAGE_URL, TARGET_WIDTH, TARGET_HEIGHT, true, true);
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(TARGET_WIDTH);
            imageView.setFitHeight(TARGET_HEIGHT);
            imageView.setPreserveRatio(true);

            Label idLabel = new Label("ID: " + book.getId());
            Label nameLabel = new Label(book.getName());
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(140);
            nameLabel.getStyleClass().add("book-name-label");

            Label authorLabel = new Label(book.getAuthor());
            authorLabel.getStyleClass().add("book-author-label");

            Label statusLabel = new Label(book.isStatus() ? LanguageManager.getText("status.available") : LanguageManager.getText("status.borrowed"));
            statusLabel.getStyleClass().add(book.isStatus() ? "available-status" : "borrowed-status");

            bookBox.getChildren().addAll(imageView, new Separator(), idLabel, nameLabel, authorLabel, statusLabel);
            searchGalleryPane.getChildren().add(bookBox);
        }
    }

    private Pane createHistoryPane() {
        TextArea historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setPrefHeight(500);
        historyArea.setPromptText(LanguageManager.getText("msg.loading"));

        Button refreshBtn = new Button(LanguageManager.getText("btn.refresh"));
        Button exportBtn = new Button(LanguageManager.getText("btn.export"));
        exportBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        exportBtn.setDisable(true);

        final java.util.concurrent.atomic.AtomicReference<List<UserInUserHistory>> currentData =
                new java.util.concurrent.atomic.AtomicReference<>(new java.util.ArrayList<>());

        Runnable loadDataLogic = () -> {
            StringBuilder sb = new StringBuilder("--- " + LanguageManager.getText("tab.history") + " ---\n");
            sb.append(String.format("%-20s | %-15s | %-25s | %s\n",
                    LanguageManager.getText("col.time"),
                    LanguageManager.getText("col.user_name"),
                    LanguageManager.getText("col.name"),
                    LanguageManager.getText("col.action")));
            sb.append("------------------------------------------------------------------------------------------------\n");

            List<UserInUserHistory> allHistory = library.getTransactionDAO().getAllTransactionsHistory();
            currentData.set(allHistory);

            if (allHistory == null || allHistory.isEmpty()) {
                sb.append("\n").append(LanguageManager.getText("msg.no_transaction"));
                exportBtn.setDisable(true);
            } else {
                exportBtn.setDisable(false);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (UserInUserHistory h : allHistory) {
                    String timeStr = h.getLocalDateTime() != null ? h.getLocalDateTime().format(formatter) : "N/A";
                    String userNameDisplay = (h.getName() != null && !h.getName().isEmpty()) ? h.getName() : h.getId();
                    sb.append(String.format("%-20s | %-15s | %-25s | %s\n",
                            timeStr, userNameDisplay, h.getBookName(), h.getTrangThai()));
                }
            }
            historyArea.setText(sb.toString());
        };

        refreshBtn.setOnAction(e -> loadDataLogic.run());

        exportBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle(LanguageManager.getText("title.save_history"));
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fc.setInitialFileName("LichSu_TongHop.csv");
            File file = fc.showSaveDialog(null);

            if (file != null) {
                boolean ok = BackEnd.Utils.ExportUtil.exportHistoryToCSV(currentData.get(), file);
                if (ok) showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.file_saved") + file.getAbsolutePath());
                else showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.export_error"));
            }
        });

        HBox controls = new HBox(10, refreshBtn, exportBtn);
        controls.setPadding(new Insets(0, 0, 10, 0));
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox pane = new VBox(10, controls, historyArea);
        pane.setPadding(new Insets(10));

        Platform.runLater(loadDataLogic);
        return pane;
    }

    private ScrollPane createScrollableNavBar(TabPane tabPane) {
        HBox navBox = new HBox(10);
        navBox.setPadding(new Insets(10, 20, 10, 20));
        navBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup toggleGroup = new ToggleGroup();
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            ToggleButton tabBtn = new ToggleButton(tab.getText());
            tabBtn.getStyleClass().add("nav-tab-button");
            tabBtn.setToggleGroup(toggleGroup);
            final int index = i;
            tabBtn.setOnAction(e -> tabPane.getSelectionModel().select(index));
            navBox.getChildren().add(tabBtn);
            if (i == 0) tabBtn.setSelected(true);
        }

        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() >= 0 && newVal.intValue() < navBox.getChildren().size()) {
                ToggleButton btn = (ToggleButton) navBox.getChildren().get(newVal.intValue());
                btn.setSelected(true);
            }
        });

        ScrollPane scrollPane = new ScrollPane(navBox);
        scrollPane.getStyleClass().add("nav-scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, evt -> {
            if (evt.getDeltaY() != 0) {
                double delta = evt.getDeltaY();
                scrollPane.setHvalue(scrollPane.getHvalue() - delta / navBox.getWidth() * 3);
                evt.consume();
            }
        });
        return scrollPane;
    }

    private void handleExportReport() {
        Alert typeAlert = new Alert(Alert.AlertType.CONFIRMATION);
        typeAlert.setTitle(LanguageManager.getText("title.select_report"));
        typeAlert.setHeaderText(LanguageManager.getText("header.export_question"));
        typeAlert.setContentText(LanguageManager.getText("content.export_select"));

        ButtonType btnBooks = new ButtonType(LanguageManager.getText("btn.report_books"));
        ButtonType btnUsers = new ButtonType(LanguageManager.getText("btn.report_users"));
        ButtonType btnCancel = new ButtonType(LanguageManager.getText("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        typeAlert.getButtonTypes().setAll(btnBooks, btnUsers, btnCancel);

        java.util.Optional<ButtonType> result = typeAlert.showAndWait();

        if (result.isPresent() && result.get() != btnCancel) {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(LanguageManager.getText("title.save_report"));
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

            String defaultName = (result.get() == btnBooks) ? "BaoCao_Sach.csv" : "BaoCao_NguoiDung.csv";
            fileChooser.setInitialFileName(defaultName);

            File destFile = fileChooser.showSaveDialog(null);

            if (destFile != null) {
                boolean success;
                if (result.get() == btnBooks) {
                    List<Book> data = bookStatistic.getTopBook();
                    success = BackEnd.Utils.ExportUtil.exportBooksToCSV(data, destFile);
                } else {
                    List<User> data = userStatistic.danhSachNguoiDung();
                    success = BackEnd.Utils.ExportUtil.exportUsersToCSV(data, destFile);
                }

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.export_success"),
                            LanguageManager.getText("msg.file_saved") + destFile.getAbsolutePath());
                } else {
                    showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.export_error"));
                }
            }
        }
    }
    /**
     * Xử lý logic Reset toàn bộ dữ liệu
     */
    private void handleFactoryReset() {
        // 1. Tạo hộp thoại nhập mật khẩu
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle(LanguageManager.getText("title.reset_dialog"));
        passwordDialog.setHeaderText(LanguageManager.getText("header.reset_dialog"));
        passwordDialog.setContentText(LanguageManager.getText("content.reset_pass"));

        // Tùy chỉnh icon cảnh báo (Optional)
        passwordDialog.getDialogPane().setGraphic(new Label("⚠️"));

        java.util.Optional<String> result = passwordDialog.showAndWait();

        if (result.isPresent()) {
            String inputPass = result.get();

            // 2. Kiểm tra mật khẩu
            if (ADMIN_PASSWORD.equals(inputPass)) {

                // 3. Thực hiện Reset
                // A. Xóa DB
                DatabaseManager.resetDatabase();

                // B. Xóa ảnh
                BackEnd.Utils.FileUtil.clearAllImages();
                BackEnd.Utils.ImageCache.clear(); // Xóa sạch bộ nhớ đệm

                // C. Xóa dữ liệu trên RAM (ObservableList)
                bookData.clear();
                userData.clear();

                // D. Cập nhật lại giao diện (Gallery, Home, Table...)
                // Vì list rỗng nên UI sẽ tự xóa trắng
                bookGalleryTab.updateBookGallery();
                homeTab.refreshData();

                // E. Thông báo thành công
                showAlert(Alert.AlertType.INFORMATION,
                        LanguageManager.getText("msg.success"),
                        LanguageManager.getText("msg.reset_success"));

            } else {
                // Mật khẩu sai
                showAlert(Alert.AlertType.ERROR,
                        LanguageManager.getText("msg.error"),
                        LanguageManager.getText("msg.wrong_pass"));
            }
        }
    }
    private void handleEditUser(TableView<User> userTable, TextField idField, TextField nameField, TextField emailField) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) return; // Không có gì để sửa

        // ---  KIỂM TRA AN TOÀN (DEFENSIVE PROGRAMMING) ---
        // Sử dụng String.valueOf() để đảm bảo getText() không bao giờ là null
        String newName = (nameField != null) ? String.valueOf(nameField.getText()).trim() : "";
        String newEmail = (emailField != null) ? String.valueOf(emailField.getText()).trim() : "";

        if (newName.isEmpty()) {
            LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), "Tên người dùng không được để trống.");
            return;
        }

        // 2. Cập nhật thuộc tính của đối tượng User đã chọn
        selectedUser.setName(newName);
        selectedUser.setEmail(newEmail);

        // 3. Xử lý cập nhật Avatar (Nếu có logic chọn ảnh mới)
        //selectedUser.setAvatarPath(handleUpdateAvatar());

        // 4. Gọi DAO để cập nhật DB
        boolean success = library.getUserDAO().updateUser(selectedUser);

        if (success) {
            // 5. Cập nhật UI
            userTable.refresh();
            userTable.getSelectionModel().clearSelection();
            idField.setEditable(true); // Mở khóa ID

            LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), "Cập nhật thông tin người dùng thành công.");
        } else {
            LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Lỗi: Không thể cập nhật thông tin người dùng.");
        }
    }
    /**
     * Hàm làm sạch các trường nhập liệu và reset bảng về trạng thái Thêm mới
     */
    private void clearUserFields(
            TableView<User> userTable,
            TextField idField,
            TextField nameField,
            TextField emailField) {

        // 1. Xóa nội dung trong các ô nhập
        idField.clear();
        nameField.clear();
        emailField.clear();

        // 2. Mở khóa ô ID để có thể nhập ID mới
        idField.setEditable(true);

        // 3. Bỏ chọn dòng trong bảng (thoát chế độ Sửa)
        userTable.getSelectionModel().clearSelection();

        // 4. Đảm bảo các nút trở lại trạng thái ban đầu (nếu bạn có logic bật/tắt nút)

    }
    /**
     * Hàm tiện ích để làm mới toàn bộ dữ liệu hiển thị
     * Hàm này PHẢI được gọi trong Platform.runLater() nếu đang ở luồng phụ
     */
    private void refreshAllUI() {
        bookData.setAll(library.getBooks());
        userData.setAll(library.getListUsers());
        bookGalleryTab.updateBookGallery();
        if (homeTab != null) homeTab.refreshData();
        refreshBorrowedTable();
    }

    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void initializeData() {
        if (library.getListUsers().isEmpty() && library.getBooks().isEmpty()) {
            System.out.println("-> Khởi tạo dữ liệu giả lần đầu.");
            library.addBook(new Book("B001", "Dune", "Frank Herbert", "1965", "images/img_2bcef25c-d9d1-461d-8427-cc2e48e475f0.png"));
            library.addBook(new Book("B002", "1984", "George Orwell", "1949"));
            library.addUser(new User("U001", "Nguyễn Văn Hiếu"));
            library.addUser(new User("U002", "Trần Thị Quỳnh"));
        } else {
            System.out.println("-> DB đã có dữ liệu, bỏ qua khởi tạo giả.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}