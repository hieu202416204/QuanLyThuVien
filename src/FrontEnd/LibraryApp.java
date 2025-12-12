package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.Histories.CurrentTransaction;
import BackEnd.Histories.UserInUserHistory;
import BackEnd.LibraryQ.Library;
import BackEnd.LibraryQ.QuanLyMuonTra;
import BackEnd.LibraryQ.SearchService;
import BackEnd.Sattistics.BookStatistic;
import BackEnd.Sattistics.UserStatistic;
import BackEnd.User.User;
import BackEnd.Utils.BarcodeScannerHandler;
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
import javafx.scene.Node;
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
import java.util.ArrayList;
import java.util.List;

public class LibraryApp extends Application {
    public static final String ADMIN_PASSWORD = "admin";

    // 1. BACKEND CORE
    private Library library;
    private QuanLyMuonTra quanLyMuonTra;
    private SearchService searchService;
    private UserStatistic userStatistic;
    private BookStatistic bookStatistic;

    // 2. DATA HOLDER
    private final ObservableList<Book> bookData = FXCollections.observableArrayList();
    private final ObservableList<User> userData = FXCollections.observableArrayList();

    // 3. UI COMPONENTS
    private final TableView<User> userTable = new TableView<>();
    private final FlowPane bookFlowPane = new FlowPane();
    private BookGalleryTab bookGalleryTab;
    private HomeTab homeTab;

    // UI COMPONENTS CHO TÌM KIẾM CÓ PHÂN TRANG
    private Pagination searchPagination;
    private List<Book> currentSearchResults = new ArrayList<>();
    private final FlowPane searchGalleryPane = new FlowPane();

    // UI CORE
    private boolean isDarkMode = false;
    private Stage primaryStage;
    private BorderPane rootLayout;

    // TableView cho Mượn/Trả có phân trang
    private TableView<CurrentTransaction> borrowedTable;
    private Pagination borrowedPagination;
    private ObservableList<CurrentTransaction> borrowedData = FXCollections.observableArrayList();

    // Phương thức resize ảnh
    public javafx.scene.image.Image convertAndResize(BufferedImage originalImage) {
        if (originalImage == null) return null;
        try {
            final int TARGET_WIDTH = 120;
            final int TARGET_HEIGHT = 160;
            BufferedImage resizedImage = ImageResizer.resizeImage(originalImage, TARGET_WIDTH, TARGET_HEIGHT);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", os);
            return new javafx.scene.image.Image(new ByteArrayInputStream(os.toByteArray()));
        } catch (IOException e) {
            System.err.println("Lỗi resize ảnh: " + e.getMessage());
            return null;
        }
    }
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // 1. MÀN HÌNH CHỜ (Splash Screen)
        ProgressIndicator spinner = new ProgressIndicator();
        Label statusLabel = new Label("Khởi động hệ thống...");
        statusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        VBox splashLayout = new VBox(20, spinner, statusLabel);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-background-color: white; -fx-border-color: #ccc;");
        Scene splashScene = new Scene(splashLayout, 400, 300);


        primaryStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        primaryStage.setScene(splashScene);
        primaryStage.show();

        // 2. BACKGROUND TASK
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Kết nối cơ sở dữ liệu...");
                DatabaseManager.initializeDatabase();

                updateMessage("Khởi động Core System...");
                // Khởi tạo Library TRƯỚC KHI gọi các hàm khác
                library = new Library();

                // Khởi tạo các Service phụ thuộc
                quanLyMuonTra = new QuanLyMuonTra(library);
                searchService = new SearchService(library);
                userStatistic = new UserStatistic(library);
                bookStatistic = new BookStatistic(library);

                updateMessage("Kiểm tra dữ liệu...");
                initializeData(); // Kiểm tra và tạo data mẫu nếu cần

                updateMessage("Tải bộ nhớ đệm...");
                // Kích hoạt load cache
                library.getBooks();
                library.getListUsers();

                Thread.sleep(500);
                return null;
            }
        };

        initTask.setOnSucceeded(e -> {
            primaryStage.hide(); // Ẩn Splash

            Stage mainStage = new Stage();
            this.primaryStage = mainStage;

            // Dựng giao diện
            rebuildUI();

            // Đổ dữ liệu vào UI (sau khi giao diện hiện để mượt)
            Platform.runLater(this::loadDataToUI);

            // Chạy kiểm tra email quá hạn ngầm
            runAutoEmailCheck();
        });

        initTask.setOnFailed(e -> {
            initTask.getException().printStackTrace();
            statusLabel.setText("Lỗi: " + initTask.getException().getMessage());
        });


        new Thread(initTask).start();
    }

    private void loadDataToUI() {
        Task<Void> dataTask = new Task<>() {
            @Override
            protected Void call() {
                List<Book> books = library.getBooks();
                List<User> users = library.getListUsers();
                Platform.runLater(() -> {
                    bookData.setAll(books);
                    userData.setAll(users);
                    if (homeTab != null) homeTab.refreshData();
                });
                return null;
            }
        };
        new Thread(dataTask).start();
    }

    private void runAutoEmailCheck() {
        Task<Void> emailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Kiểm tra email quá hạn...");
                String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
                int maxDays = 60;
                try {
                    if (maxDaysStr != null && !maxDaysStr.isEmpty()) maxDays = Integer.parseInt(maxDaysStr);
                } catch (NumberFormatException ignored) {}

                List<String[]> overdueList = library.getTransactionDAO().getOverdueTransactionsWithEmail(maxDays);
                if (overdueList.isEmpty()) return null;

                for (String[] record : overdueList) {
                    String email = record[0];
                    String userName = record[1];
                    String bookName = record[2];
                    long days = Long.parseLong(record[3]);
                    BackEnd.Utils.EmailService.sendOverdueNotification(library, email, userName, bookName, days);
                    Thread.sleep(2000); // Tránh spam
                }
                return null;
            }
        };
        Thread thread = new Thread(emailTask);
        thread.setDaemon(true);
        thread.start();
    }
    /**
     * Hàm dựng lại toàn bộ giao diện (Gọi khi khởi động hoặc khi đổi ngôn ngữ)
     */
    private void rebuildUI() {
        rootLayout = new BorderPane();
        Scene scene = new Scene(rootLayout, 1100, 750);
        if (this.isDarkMode) scene.getRoot().getStyleClass().add("dark-mode");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("hidden-header-tab-pane");

        // 1. HOME TAB
        homeTab = new HomeTab(library);
        Tab tabHome = new Tab(LanguageManager.getText("tab.home"), homeTab);

        // 2. GALLERY TAB (Lazy)
        Tab tabGallery = new Tab(LanguageManager.getText("tab.gallery"));
        tabGallery.setOnSelectionChanged(e -> {
            if (tabGallery.isSelected() && tabGallery.getContent() == null) {
                if (bookGalleryTab == null) bookGalleryTab = new BookGalleryTab(library, bookFlowPane, this::convertAndResize);
                tabGallery.setContent(bookGalleryTab);
            }
        });

        // 3. BOOK MANAGEMENT TAB (Lazy)
        Tab tabBooks = new Tab(LanguageManager.getText("tab.manage_book"));
        tabBooks.setOnSelectionChanged(e -> {
            if (tabBooks.isSelected() && tabBooks.getContent() == null) {
                if (bookGalleryTab == null) bookGalleryTab = new BookGalleryTab(library, bookFlowPane, this::convertAndResize);
                tabBooks.setContent(new BookManagementTab(library, bookData, bookFlowPane, bookGalleryTab));
            }
        });

        // 4. USER TAB (Tạo ngay để bind pagination)
        Tab tabUsers = new Tab(LanguageManager.getText("tab.users"));
        tabUsers.setContent(createUserPane());

        // 5. BORROW/RETURN TAB
        Tab tabBorrowReturn = new Tab(LanguageManager.getText("tab.borrow"));
        tabBorrowReturn.setContent(createBorrowReturnPane());

        // 6. SEARCH TAB
        Tab tabSearch = new Tab(LanguageManager.getText("tab.search"));
        tabSearch.setContent(createSearchPane());

        // 7. HISTORY TAB
        Tab tabHistory = new Tab(LanguageManager.getText("tab.history"));
        tabHistory.setContent(createHistoryPane());

        // 8. STATISTICS TAB
        Tab tabStatistics = new Tab(LanguageManager.getText("tab.stats"));
        tabStatistics.setContent(createStatisticsPane());

        // 9. SETTINGS TAB
        Runnable onLanguageChangeCallback = () -> {
            this.isDarkMode = scene.getRoot().getStyleClass().contains("dark-mode");
            rebuildUI();
        };
        Tab tabSettings = new Tab(LanguageManager.getText("tab.settings"), new SettingsTab(library, scene, onLanguageChangeCallback, this.isDarkMode));

        tabPane.getTabs().addAll(tabHome, tabGallery, tabBooks, tabUsers, tabBorrowReturn, tabSearch, tabHistory, tabStatistics, tabSettings);

        ScrollPane navBar = createScrollableNavBar(tabPane);
        rootLayout.setTop(navBar);
        rootLayout.setCenter(tabPane);

        java.net.URL cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        BarcodeScannerHandler scannerHandler = new BarcodeScannerHandler(library);
        scannerHandler.attachToScene(scene);
        primaryStage.setScene(scene);
        primaryStage.setTitle("📚 " + LanguageManager.getText("tab.manage_book"));
        primaryStage.show();
    }

    private void refreshAllUIComponents() {
        // Tải lại Cache
        library.getBookDAO().reloadCache();

        // Cập nhật List
        bookData.setAll(library.getBooks());
        userData.setAll(library.getListUsers());

        // Refresh các Tab
        if (homeTab != null) homeTab.refreshData();
        if (bookGalleryTab != null) bookGalleryTab.updateBookGallery();
        refreshBorrowedTable();
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

    // ========================================================================
    // 1. NGƯỜI DÙNG: PHÂN TRANG (100 dòng/trang)
    // ========================================================================
    private Pagination userPagination;
    private static final int USER_PAGE_SIZE = 100;
    private File selectedUserAvatar = null;

    private Pane createUserPane() {
        // Table
        TableColumn<User, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(100);

        TableColumn<User, String> colName = new TableColumn<>(LanguageManager.getText("col.user_name"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<User, String> colEmail = new TableColumn<>(LanguageManager.getText("col.email"));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colEmail.setPrefWidth(250);

        userTable.getColumns().setAll(colId, colName, colEmail);
        userTable.setFixedCellSize(30);
        userTable.prefHeightProperty().bind(userTable.fixedCellSizeProperty().multiply(12).add(30));

        // Pagination
        userPagination = new Pagination();
        userData.addListener((javafx.collections.ListChangeListener<User>) c -> updateUserPaginationCount());

        userPagination.setPageFactory(pageIndex -> {
            int from = pageIndex * USER_PAGE_SIZE;
            int to = Math.min(from + USER_PAGE_SIZE, userData.size());
            if (from > to) from = to;
            List<User> pageUsers = userData.subList(from, to);
            userTable.setItems(FXCollections.observableArrayList(pageUsers));
            return new VBox();
        });
        updateUserPaginationCount();

        // Inputs
        TextField idField = new TextField(); idField.setPromptText(LanguageManager.getText("col.id")); idField.setPrefWidth(120);
        TextField nameField = new TextField(); nameField.setPromptText(LanguageManager.getText("col.name.user")); nameField.setPrefWidth(200);
        TextField emailField = new TextField(); emailField.setPromptText(LanguageManager.getText("col.email")); emailField.setPrefWidth(200);

        Button btnAvatar = new Button("📷");
        Label lblAvatarStatus = new Label(LanguageManager.getText("label.not_selected"));
        btnAvatar.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
            File f = fc.showOpenDialog(null);
            if (f != null) { selectedUserAvatar = f; lblAvatarStatus.setText(LanguageManager.getText("label.selected")); }
        });

        FlowPane inputPane = new FlowPane(15, 10);
        inputPane.setPadding(new Insets(10));
        inputPane.getStyleClass().add("input-panel");
        inputPane.getChildren().addAll(idField, nameField, emailField, new HBox(5, btnAvatar, lblAvatarStatus));

        // Buttons
        Button addBtn = new Button(LanguageManager.getText("btn.add_user"));
        addBtn.setStyle("-fx-base: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            if (!idField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                String avatarFileName = null;
                if (selectedUserAvatar != null) {
                    try { avatarFileName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUserAvatar); } catch (Exception ex) {}
                }
                String uId = idField.getText().trim();
                String uName = nameField.getText().trim();
                String uEmail = emailField.getText().trim();

                if(library.getUserDAO().addUser(new User(uId, uName, uEmail, avatarFileName))) {
                    userData.setAll(library.getListUsers());
                    showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.user_added"));

                    // Gửi email chào mừng
                    if (!uEmail.isEmpty()) {
                        new Thread(() -> BackEnd.Utils.EmailService.sendWelcomeEmail(library, uEmail, uName, uId)).start();
                    }

                    idField.clear(); nameField.clear(); emailField.clear(); selectedUserAvatar = null;
                } else showAlert(Alert.AlertType.ERROR, "Lỗi", "Trùng ID hoặc lỗi DB.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", LanguageManager.getText("msg.user_missing_info"));
            }
        });

        Button importUserBtn = new Button("📥 Import Excel");
        importUserBtn.getStyleClass().addAll("action-btn", "btn-green");
        importUserBtn.setOnAction(e -> handleImportUsers());

        Button deleteBtn = new Button(LanguageManager.getText("btn.delete_user"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if(selected == null) return;
            TextInputDialog passDlg = new TextInputDialog();
            passDlg.setTitle(LanguageManager.getText("title.confirm_delete"));
            passDlg.setContentText(LanguageManager.getText("content.enter_pass"));
            if (passDlg.showAndWait().orElse("").equals(ADMIN_PASSWORD)) {
                if(library.deleteUser(selected.getId())) {
                    userData.setAll(library.getListUsers());
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Deleted");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", LanguageManager.getText("msg.wrong_pass"));
            }
        });

        Button editBtn = new Button("✏️ " + LanguageManager.getText("btn.edit"));
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> handleEditUser(userTable, idField, nameField, emailField));

        Button clearBtn = new Button("🔄 " + LanguageManager.getText("btn.refresh"));
        clearBtn.setOnAction(e -> clearUserFields(userTable, idField, nameField, emailField));

        Button viewUserHistoryBtn = new Button(LanguageManager.getText("btn.view_history_user"));
        viewUserHistoryBtn.setDisable(true);
        viewUserHistoryBtn.setOnAction(e -> handleViewUserHistory(userTable.getSelectionModel().getSelectedItem()));

        Button btnCreateCard = new Button(LanguageManager.getText("btn.create_card"));
        btnCreateCard.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
        btnCreateCard.setOnAction(e -> handleCreateCard());

        HBox actionBar = new HBox(15);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.getChildren().addAll(addBtn, importUserBtn, editBtn, deleteBtn, clearBtn, new Separator(javafx.geometry.Orientation.VERTICAL), viewUserHistoryBtn, btnCreateCard);

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSel = newVal != null;
            editBtn.setDisable(!hasSel);
            deleteBtn.setDisable(!hasSel);
            addBtn.setDisable(hasSel);
            viewUserHistoryBtn.setDisable(!hasSel);
            if(hasSel) {
                idField.setText(newVal.getId()); nameField.setText(newVal.getName()); emailField.setText(newVal.getEmail());
                idField.setEditable(false);
            }
        });

        // Search
        TextField searchUserField = new TextField();
        Button searchIdBtn = new Button(LanguageManager.getText("btn.search_id"));
        searchIdBtn.setOnAction(e -> handleSearchUserById(searchUserField.getText()));
        Button searchNameBtn = new Button(LanguageManager.getText("btn.search_name"));
        searchNameBtn.setOnAction(e -> handleSearchUserByName(searchUserField.getText()));
        Button clearSearchBtn = new Button(LanguageManager.getText("btn.clear_search"));
        clearSearchBtn.setOnAction(e -> { userData.setAll(library.getListUsers()); searchUserField.clear(); });
        HBox searchBar = new HBox(10, searchUserField, searchIdBtn, searchNameBtn, clearSearchBtn);
        searchBar.getStyleClass().add("input-panel");

        VBox layout = new VBox(10, searchBar, userTable, userPagination, inputPane, actionBar);
        layout.setPadding(new Insets(15));
        return layout;
    }

    private void updateUserPaginationCount() {
        int size = userData.size();
        int pageCount = size > 0 ? (int) Math.ceil((double) size / USER_PAGE_SIZE) : 1;
        userPagination.setPageCount(pageCount);

        // --- ĐOẠN CODE CẦN THÊM VÀO ---
        // Ép buộc nạp lại dữ liệu cho bảng ngay lập tức dựa trên trang hiện tại
        int pageIndex = userPagination.getCurrentPageIndex();

        // Tính toán lại chỉ số bắt đầu và kết thúc (như trong PageFactory)
        int from = pageIndex * USER_PAGE_SIZE;
        int to = Math.min(from + USER_PAGE_SIZE, userData.size());

        // Tránh lỗi index nếu dữ liệu bị xóa sạch
        if (from > to) from = to;
        if (from >= userData.size() && !userData.isEmpty()) {
            // Nếu trang hiện tại vượt quá dữ liệu (ví dụ đang ở trang 2 mà xóa hết còn 1 trang)
            // thì lùi về trang 0
            from = 0;
            to = Math.min(USER_PAGE_SIZE, userData.size());
            userPagination.setCurrentPageIndex(0);
        }

        List<User> pageUsers = userData.subList(from, to);
        userTable.setItems(FXCollections.observableArrayList(pageUsers));
        // ------------------------------
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
            fc.setInitialFileName("History_" + selectedUser.getId() + ".csv");
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

    // ========================================================================
    // 2. MƯỢN TRẢ: THÔNG MINH (SMART SCAN & AUTO ACTION)
    // ========================================================================
    private Pane createBorrowReturnPane() {
        final int BORROW_PAGE_SIZE = 100;

        VBox inputSection = new VBox(15);
        inputSection.setPadding(new Insets(10));
        inputSection.setPrefWidth(350);
        inputSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 1px 0 0;");

        Label titleLabel = new Label(LanguageManager.getText("title.action_section"));
        titleLabel.getStyleClass().add("section-title");

        // Khai báo các trường nhập liệu
        TextField userIdField = new TextField(); userIdField.setPromptText(LanguageManager.getText("field.user_id"));
        TextField bookIdField = new TextField(); bookIdField.setPromptText(LanguageManager.getText("field.book_id"));
        TextField barcodeField = new TextField(); barcodeField.setPromptText(LanguageManager.getText("field.scan_hint"));
        Label statusLabel = new Label(LanguageManager.getText("msg.scan_ready"));

        // --- SMART SCAN HANDLING ---
        barcodeField.setOnAction(e -> {
            String scannedCode = barcodeField.getText().trim();
            if (scannedCode.isEmpty()) return;

            boolean isHandled = false;

            // 1. Check if it is a BOOK
            Book b = library.findBookById(scannedCode);
            if (b != null) {
                bookIdField.setText(b.getId());
                statusLabel.setText("📖 Book: " + b.getName());
                updateStatusStyle(statusLabel, "scan-status-success");
                isHandled = true;
            }
            // 2. If not, check if it is a USER
            else {
                User u = library.searchUserById(scannedCode);
                if (u != null) {
                    userIdField.setText(u.getId());
                    statusLabel.setText("👤 User: " + u.getName());
                    updateStatusStyle(statusLabel, "scan-status-success");
                    isHandled = true;
                }
            }

            if (!isHandled) {
                statusLabel.setText("❌ Code does not exist: " + scannedCode);
                updateStatusStyle(statusLabel, "scan-status-error");
                barcodeField.selectAll();
                return; // Stop if the code is invalid
            }


            // Dọn ô quét để sẵn sàng quét tiếp
            barcodeField.clear();
            java.awt.Toolkit.getDefaultToolkit().beep();

            // 3. TỰ ĐỘNG KÍCH HOẠT HÀNH ĐỘNG NẾU ĐỦ 2 TRƯỜNG
            String uId = userIdField.getText().trim();
            String bId = bookIdField.getText().trim();

            if (!uId.isEmpty() && !bId.isEmpty()) {
                // Gọi hàm xử lý thông minh tách biệt
                handleSmartAction(uId, bId, userIdField, bookIdField, statusLabel);
            } else {
                // Nếu chưa đủ, focus vào ô quét để quét tiếp cái còn thiếu
                barcodeField.requestFocus();
            }
        });

        VBox scanBox = new VBox(5, new Label(LanguageManager.getText("label.scan_title")), barcodeField, statusLabel);
        scanBox.getStyleClass().add("scan-box");

        // Các nút thủ công (vẫn giữ lại để dùng khi cần)
        Button borrowBtn = new Button(LanguageManager.getText("btn.action_borrow"));
        borrowBtn.setMaxWidth(Double.MAX_VALUE);
        borrowBtn.setOnAction(e -> performBorrow(userIdField.getText(), bookIdField.getText(), userIdField, bookIdField, statusLabel));

        Button returnBtn = new Button(LanguageManager.getText("btn.action_return"));
        returnBtn.setMaxWidth(Double.MAX_VALUE);
        returnBtn.setOnAction(e -> performReturn(userIdField.getText(), bookIdField.getText(), userIdField, bookIdField, statusLabel));

        inputSection.getChildren().addAll(titleLabel, scanBox, new Separator(),
                new Label(LanguageManager.getText("label.manual_info")), userIdField, bookIdField, new Separator(), borrowBtn, returnBtn);

        // --- Bảng phải (Giữ nguyên logic hiển thị) ---
        if (borrowedTable == null) {
            borrowedTable = new TableView<>();
            VBox.setVgrow(borrowedTable, Priority.ALWAYS);
        }
        // --- Table & Pagination ---
        if (borrowedTable == null) {
            borrowedTable = new TableView<>();
            VBox.setVgrow(borrowedTable, Priority.ALWAYS);
        }
        if (borrowedTable.getColumns().isEmpty()) {
            TableColumn<CurrentTransaction, String> colUser = new TableColumn<>(LanguageManager.getText("col.user_info"));
            colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserId() + " - " + d.getValue().getUserName()));
            TableColumn<CurrentTransaction, String> colBook = new TableColumn<>(LanguageManager.getText("col.book_info"));
            colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookId() + " - " + d.getValue().getBookName()));
            TableColumn<CurrentTransaction, String> colStatus = new TableColumn<>(LanguageManager.getText("col.time_status"));

            // Logic tô màu trạng thái
            colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDaysElapsed() + " " + LanguageManager.getText("text.days")));
            colStatus.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null); setStyle("");
                    if (empty || item == null) return;

                    CurrentTransaction t = getTableView().getItems().get(getIndex());
                    long days = t.getDaysElapsed();
                    setText(item);

                    String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
                    int max = (maxDaysStr.isEmpty()) ? 60 : Integer.parseInt(maxDaysStr);

                    if (days > max) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        setText(item + " " + LanguageManager.getText("status.overdue"));
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            });

            borrowedTable.getColumns().addAll(colUser, colBook, colStatus);
        }

        if (borrowedPagination == null) borrowedPagination = new Pagination();

        borrowedData.addListener((javafx.collections.ListChangeListener<CurrentTransaction>) c -> {
            int size = borrowedData.size();
            borrowedPagination.setPageCount(size > 0 ? (int) Math.ceil((double)size / BORROW_PAGE_SIZE) : 1);
        });

        borrowedPagination.setPageFactory(idx -> {
            int from = idx * BORROW_PAGE_SIZE;
            int to = Math.min(from + BORROW_PAGE_SIZE, borrowedData.size());
            if(from > to) from = to;
            borrowedTable.setItems(FXCollections.observableArrayList(borrowedData.subList(from, to)));
            return new VBox();
        });

        refreshBorrowedTable();

        VBox rightSide = new VBox(10, borrowedTable, borrowedPagination);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        HBox mainLayout = new HBox(10, inputSection, rightSide);
        mainLayout.setPadding(new Insets(10));
        return mainLayout;
    }

    private void refreshBorrowedTable() {
        if (library != null) {
            List<CurrentTransaction> list = library.getTransactionDAO().getCurrentlyBorrowedBooks();
            borrowedData.setAll(list);
            if (!list.isEmpty() && borrowedPagination != null) borrowedPagination.setCurrentPageIndex(0);
        }
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

    // ========================================================================
    // 3. TÌM KIẾM: PHÂN TRANG (100 dòng/trang)
    // ========================================================================
    private Pane createSearchPane() {
        searchGalleryPane.setHgap(15);
        searchGalleryPane.setVgap(15);
        searchGalleryPane.setPadding(new Insets(10));
        searchGalleryPane.setStyle("-fx-alignment: top-left;");
        searchGalleryPane.setPrefWrapLength(1000);

        if (searchPagination == null) {
            searchPagination = new Pagination();
            searchPagination.setVisible(false);
        }
        searchPagination.setPageFactory(this::createSearchPage);
        searchPagination.setMinHeight(500);

        TextField searchField = new TextField();
        searchField.setPrefWidth(300);
        Label resultLabel = new Label(LanguageManager.getText("label.search_guide"));

        Button searchCombinedBtn = new Button(LanguageManager.getText("btn.search_combined"));
        searchCombinedBtn.setOnAction(e -> handleSearch(searchField.getText(), "COMBINED", resultLabel));

        Button searchIdBtn = new Button(LanguageManager.getText("btn.search_id_exact"));
        searchIdBtn.setOnAction(e -> handleSearch(searchField.getText(), "ID", resultLabel));

        Button resetBtn = new Button(LanguageManager.getText("btn.clear_result"));
        resetBtn.setOnAction(e -> {
            currentSearchResults.clear();
            searchPagination.setVisible(false);
            searchField.clear();
            resultLabel.setText(LanguageManager.getText("label.search_ready"));
        });

        HBox controls = new HBox(10, searchField, searchIdBtn, searchCombinedBtn, resetBtn);
        controls.setPadding(new Insets(10));

        VBox pane = new VBox(10, resultLabel, controls, searchPagination);
        pane.setPadding(new Insets(10));
        return pane;
    }


    private Node createSearchPage(int pageIndex) {
        int from = pageIndex * 100;
        int to = Math.min(from + 100, currentSearchResults.size());
        searchGalleryPane.getChildren().clear();
        updateSearchGallery(currentSearchResults.subList(from, to));
        ScrollPane sp = new ScrollPane(searchGalleryPane);
        sp.setFitToWidth(true);
        return sp;
    }

    private void handleSearch(String query, String searchType, Label resultLabel) {
        if (query.trim().isEmpty()) return;

        List<Book> results;

        // Nếu người dùng chọn "Tìm kiếm Tổng hợp" (COMBINED)
        // Gọi hàm searchByKeyword mới siêu mạnh mẽ
        if ("COMBINED".equals(searchType)) {
            results = library.getBookDAO().searchByKeyword(query);
        } else if ("ID".equals(searchType)) {
            Book b = library.getBookDAO().getBookById(query);
            results = (b != null) ? List.of(b) : List.of();
        } else if ("AUTHOR".equals(searchType)) {
            // Bạn có thể dùng hàm searchByKeyword luôn cũng được vì nó bao gồm cả Author
            results = library.getBookDAO().searchByKeyword(query);
        } else {
            // NAME
            results = library.getBookDAO().searchBookByPartialName(query);
        }

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
    private static final String PLACEHOLDER_IMG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";
    private void updateSearchGallery(List<Book> books) {
        searchGalleryPane.getChildren().clear();

        URL defaultUrl = getClass().getResource("/resources/default_cover.png");
        if (defaultUrl == null) {
            defaultUrl = getClass().getResource("/default_cover.png");
        }
        final String DEFAULT_IMAGE_URL = (defaultUrl != null) ? defaultUrl.toExternalForm() : PLACEHOLDER_IMG;
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

    // ========================================================================
    // 4. LỊCH SỬ: TABLEVIEW + PHÂN TRANG (300 dòng/trang)
    // ========================================================================
    private Pagination historyPagination;
    private ObservableList<UserInUserHistory> historyData = FXCollections.observableArrayList();

    private Pane createHistoryPane() {
        TableView<UserInUserHistory> historyTable = new TableView<>();

        TableColumn<UserInUserHistory, String> colTime = new TableColumn<>(LanguageManager.getText("col.time"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocalDateTime() != null ? d.getValue().getLocalDateTime().format(formatter) : ""));

        TableColumn<UserInUserHistory, String> colUser = new TableColumn<>(LanguageManager.getText("col.user_name"));
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName() != null ? d.getValue().getName() : d.getValue().getId()));

        TableColumn<UserInUserHistory, String> colBook = new TableColumn<>(LanguageManager.getText("col.name"));
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookName()));

        TableColumn<UserInUserHistory, String> colAction = new TableColumn<>(LanguageManager.getText("col.action"));
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTrangThai()));

        historyTable.getColumns().addAll(colTime, colUser, colBook, colAction);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        historyPagination = new Pagination();
        historyData.addListener((javafx.collections.ListChangeListener<UserInUserHistory>) c -> {
            int size = historyData.size();
            historyPagination.setPageCount(size > 0 ? (int)Math.ceil((double)size / 300) : 1);
        });

        historyPagination.setPageFactory(idx -> {
            int from = idx * 300;
            int to = Math.min(from + 300, historyData.size());
            if(from > to) from = to;
            historyTable.setItems(FXCollections.observableArrayList(historyData.subList(from, to)));
            return new VBox();
        });

        Button refreshBtn = new Button(LanguageManager.getText("btn.refresh"));
        refreshBtn.setOnAction(e -> {
            List<UserInUserHistory> all = library.getTransactionDAO().getAllTransactionsHistory();
            historyData.setAll(all);
        });

        // Load lần đầu
        refreshBtn.fire();

        VBox pane = new VBox(10, refreshBtn, historyTable, historyPagination);
        pane.setPadding(new Insets(10));
        return pane;
    }

    private ScrollPane createScrollableNavBar(TabPane tabPane) {
        HBox navBox = new HBox(10);
        navBox.setPadding(new Insets(10));
        navBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup tg = new ToggleGroup();
        for(int i=0; i<tabPane.getTabs().size(); i++) {
            ToggleButton btn = new ToggleButton(tabPane.getTabs().get(i).getText());
            btn.setToggleGroup(tg);
            btn.getStyleClass().add("nav-tab-button");
            int idx = i;
            btn.setOnAction(e -> tabPane.getSelectionModel().select(idx));
            navBox.getChildren().add(btn);
        }

        // Nút Hướng dẫn
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button helpBtn = new Button("❓ Guide");
        helpBtn.getStyleClass().add("nav-tab-button");
        helpBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        helpBtn.setOnAction(e -> BackEnd.Utils.HelpService.openUserGuide());

        navBox.getChildren().addAll(spacer, helpBtn);

        ScrollPane sp = new ScrollPane(navBox);
        sp.getStyleClass().add("nav-scroll-pane");
        return sp;
    }
// ========================================================================
    // HELPERS & EVENT HANDLERS
    // ========================================================================


    private void handleCreateCard() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Warning", "Select a user first."); return; }

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(null);
        if(f != null) {
            FrontEnd.CardGenerator.saveCardToPDF(selected, f, null);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Card Saved.");
        }
    }

    private void handleImportUsers() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showOpenDialog(null);
        if(f != null) {
            List<User> users = BackEnd.Utils.ImportUtil.importUsersFromCSV(f);
            int success = 0;
            for(User u : users) {
                if(library.getUserDAO().addUser(u)) success++;
            }
            userData.setAll(library.getListUsers());
            showAlert(Alert.AlertType.INFORMATION, "Result", "Imported: " + success);
        }
    }

    private void handleExportReport() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(null);
        if(f != null) {
            BackEnd.Utils.ExportUtil.exportBooksToCSV(bookStatistic.getTopBook(), f);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Exported.");
        }
    }

    private void handleFactoryReset() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setContentText("Enter Admin Password:");
        if(dlg.showAndWait().orElse("").equals(ADMIN_PASSWORD)) {
            DatabaseManager.resetDatabase();
            BackEnd.Utils.FileUtil.clearAllImages();
            BackEnd.Utils.ImageCache.clear();
            bookData.clear(); userData.clear();
            homeTab.refreshData();
            bookGalleryTab.updateBookGallery();
            showAlert(Alert.AlertType.INFORMATION, "Reset", "System Reset Successful.");
        }
    }



    private void handleEditUser(TableView<User> t, TextField i, TextField n, TextField e) {
        User u = t.getSelectionModel().getSelectedItem();
        if(u != null && !n.getText().isEmpty()) {
            u.setName(n.getText()); u.setEmail(e.getText());
            if(library.getUserDAO().updateUser(u)) {
                t.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User Updated.");
                clearUserFields(t, i, n, e);
            }
        }
    }

    private void clearUserFields(TableView<User> t, TextField i, TextField n, TextField e) {
        i.clear(); n.clear(); e.clear();
        t.getSelectionModel().clearSelection();
        i.setEditable(true);
        userData.setAll(library.getListUsers());
    }
    // --- HÀM XỬ LÝ THÔNG MINH (LOGIC CỐT LÕI) ---
    private void handleSmartAction(String uId, String bId, TextField uField, TextField bField, Label statusLbl) {
        Book book = library.findBookById(bId);

        if (book == null) return; // Không tìm thấy sách

        if (book.isStatus()) {
            // TRƯỜNG HỢP 1: Sách đang CÓ SẴN (Status = true) -> User muốn MƯỢN
            performBorrow(uId, bId, uField, bField, statusLbl);
        } else {
            // TRƯỜNG HỢP 2: Sách ĐANG ĐƯỢC MƯỢN (Status = false) -> User đến TRẢ

            // Tìm xem ai là người mượn thực sự của cuốn sách này
            String realBorrowerId = getActiveBorrowerId(bId);

            if (realBorrowerId != null && realBorrowerId.equals(uId)) {
                // A. Đúng người -> Trả luôn
                performReturn(uId, bId, uField, bField, statusLbl);
            } else {
                // B. Sai người (Người B đi trả sách cho Người A) -> Cảnh báo
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Return Book Warning");
                alert.setHeaderText("The returner does not match the borrower!");
                alert.setContentText("This book was borrowed by: " + realBorrowerId + "\n" +
                        "But the current user is: " + uId + "\n\n" +
                        "Do you want to CONFIRM returning the book for " + realBorrowerId + "?");

                ButtonType btnYes = new ButtonType("Confirm Return", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNo = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnYes, btnNo);


                if (alert.showAndWait().orElse(btnNo) == btnYes) {
                    // Nếu đồng ý: Thực hiện trả sách dưới danh nghĩa người mượn thực sự (realBorrowerId)
                    // để DB đóng đúng giao dịch
                    performReturn(realBorrowerId, bId, uField, bField, statusLbl);
                } else {
                    statusLbl.setText("Return operation has been canceled.");
                    updateStatusStyle(statusLbl, "scan-status-error");
                }
            }
        }
    }

    // --- HÀM THỰC HIỆN MƯỢN (Tách ra để tái sử dụng) ---
    private void performBorrow(String uId, String bId, TextField uField, TextField bField, Label statusLbl) {
        String msg = quanLyMuonTra.choMuonSach(uId, bId);
        refreshAllUIComponents();

        boolean success = msg.contains("Thành công") || msg.contains("Success");

        // Chỉ hiện Dialog nếu lỗi, còn thành công thì chỉ cần beep và update status (cho nhanh)
        if (!success) {
            showAlert(Alert.AlertType.ERROR, "Borrow Error", msg);
            statusLbl.setText(msg);
            updateStatusStyle(statusLbl, "scan-status-error");
        } else {
            java.awt.Toolkit.getDefaultToolkit().beep();
            statusLbl.setText("✅ Borrowed successfully: " + bId);
            updateStatusStyle(statusLbl, "scan-status-success");
            // Xóa trường để sẵn sàng cho giao dịch tiếp theo
            uField.clear();
            bField.clear();
        }
    }

    // --- HÀM THỰC HIỆN TRẢ (Tách ra để tái sử dụng) ---
    private void performReturn(String uId, String bId, TextField uField, TextField bField, Label statusLbl) {
        // 1. Tính phạt trước
        long fine = quanLyMuonTra.calculateFine(uId, bId);
        if (fine > 0) {
            String fineStr = String.format("%,d", fine);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(LanguageManager.getText("msg.fine_warning"));
            confirm.setContentText(String.format(LanguageManager.getText("msg.fine_content"), fineStr));

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                statusLbl.setText("Return canceled due to unpaid fine.");
                return; // Cancel if the user does not agree to pay the fine
            }
// Record revenue
            library.getFinancialDAO().recordFine("Overdue fine: " + uId + "-" + bId, fine);

// 2. Process return
            String msg = quanLyMuonTra.traSach(uId, bId);
            refreshAllUIComponents();

            boolean success = msg.contains("Success") || msg.contains("Thành công");

            if (success) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                String statusText = "✅ Returned successfully: " + bId;
                if (fine > 0) statusText += " (Fine collected)";

                statusLbl.setText(statusText);
                updateStatusStyle(statusLbl, "scan-status-success");

                uField.clear();
                bField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Return Error", msg);
                statusLbl.setText(msg);
                updateStatusStyle(statusLbl, "scan-status-error");
            }
        }
    }

            /**
             * Hàm helper: Tìm User ID của người đang mượn cuốn sách này.
             * Dùng borrowedData (đã load sẵn) để tra cứu nhanh, tránh query DB.
             */
    private String getActiveBorrowerId(String bookId) {
        for (CurrentTransaction t : borrowedData) {
            if (t.getBookId().equals(bookId)) {
                return t.getUserId();
            }
        }
        return null; // Không tìm thấy (Lỗi dữ liệu hoặc sách không được mượn)
    }


    private void initializeData() {
        if (library.getListUsers().isEmpty() || library.getBooks().isEmpty()) {
            System.out.println("-> Init Demo Data.");
            // --- THÊM 20 CUỐN SÁCH (CNTT & Đời Sống) ---
            library.addBook(new Book("B001", "Clean Code", "Robert C. Martin", "2008"));
            library.addBook(new Book("B002", "Effective Java", "Joshua Bloch", "2018"));
            library.addBook(new Book("B003", "Head First Design Patterns", "Eric Freeman", "2004"));
            library.addBook(new Book("B004", "The Pragmatic Programmer", "Andrew Hunt", "1999"));
            library.addBook(new Book("B005", "Introduction to Algorithms", "Thomas H. Cormen", "2009"));
            library.addBook(new Book("B006", "Code Complete", "Steve McConnell", "2004"));
            library.addBook(new Book("B007", "Refactoring", "Martin Fowler", "1999"));
            library.addBook(new Book("B008", "Design Patterns", "Erich Gamma", "1994"));
            library.addBook(new Book("B009", "The Mythical Man-Month", "Frederick Brooks", "1975"));
            library.addBook(new Book("B010", "Java Concurrency in Practice", "Brian Goetz", "2006"));
            library.addBook(new Book("B011", "Đắc Nhân Tâm", "Dale Carnegie", "2019"));
            library.addBook(new Book("B012", "Nhà Giả Kim", "Paulo Coelho", "2017"));
            library.addBook(new Book("B013", "Tuổi Trẻ Đáng Giá Bao Nhiêu", "Rosie Nguyễn", "2016"));
            library.addBook(new Book("B014", "Cà Phê Cùng Tony", "Tony Buổi Sáng", "2015"));
            library.addBook(new Book("B015", "Mắt Biếc", "Nguyễn Nhật Ánh", "2018"));
            library.addBook(new Book("B016", "Dế Mèn Phiêu Lưu Ký", "Tô Hoài", "2020"));
            library.addBook(new Book("B017", "Harry Potter và Hòn Đá Phù Thủy", "J.K. Rowling", "2015"));
            library.addBook(new Book("B018", "Sherlock Holmes Toàn Tập", "Arthur Conan Doyle", "2018"));
            library.addBook(new Book("B019", "Rừng Na Uy", "Haruki Murakami", "2019"));
            library.addBook(new Book("B020", "Sapiens: Lược Sử Loài Người", "Yuval Noah Harari", "2017"));

// --- THÊM 20 NGƯỜI DÙNG (Sinh viên) ---
            library.addUser(new User("U001", "Nguyễn Văn An"));
            library.addUser(new User("U002", "Trần Thị Bích"));
            library.addUser(new User("U003", "Lê Hoàng Cường"));
            library.addUser(new User("U004", "Phạm Minh Duy"));
            library.addUser(new User("U005", "Hoàng Thị Em"));
            library.addUser(new User("U006", "Vũ Văn Dũng"));
            library.addUser(new User("U007", "Đặng Thị Gấm"));
            library.addUser(new User("U008", "Bùi Văn Hùng"));
            library.addUser(new User("U009", "Đỗ Thị Inh"));
            library.addUser(new User("U010", "Hồ Văn Khoa"));
            library.addUser(new User("U011", "Ngô Thị Lan"));
            library.addUser(new User("U012", "Dương Văn Mạnh"));
            library.addUser(new User("U013", "Lý Thị Ngọc"));
            library.addUser(new User("U014", "Trương Văn Oanh"));
            library.addUser(new User("U015", "Võ Thị Phương"));
            library.addUser(new User("U016", "Đinh Văn Quân"));
            library.addUser(new User("U017", "Mai Thị Quỳnh"));
            library.addUser(new User("U018", "Cao Văn Sơn"));
            library.addUser(new User("U019", "Phan Thị Trang"));
            library.addUser(new User("U020", "Lâm Văn Uy"));
        }
    }


    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}