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

        // Gọi hàm vẽ giao diện lần đầu
        rebuildUI();
    }

    /**
     * Hàm dựng lại toàn bộ giao diện (Gọi khi khởi động hoặc khi đổi ngôn ngữ)
     */
    private void rebuildUI() {
        // 1. Tải dữ liệu mới nhất
        bookData.setAll(library.getBooks());
        userData.setAll(library.getListUsers());

        // 2. Tạo TabPane và các Tab (SỬ DỤNG LANGUAGE MANAGER)
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("hidden-header-tab-pane");

        // --- KHỞI TẠO CÁC TAB ---
        homeTab = new HomeTab(library);
        bookGalleryTab = new BookGalleryTab(library, bookFlowPane, this::convertAndResize);

        Tab tabHome = new Tab(LanguageManager.getText("tab.home"), homeTab);
        Tab tabGallery = new Tab(LanguageManager.getText("tab.gallery"), bookGalleryTab);
        Tab tabBooks = new Tab(LanguageManager.getText("tab.manage_book"), new BookManagementTab(library, bookData, bookFlowPane, bookGalleryTab).getPane());
        Tab tabUsers = new Tab(LanguageManager.getText("tab.users"), createUserPane());
        Tab tabBorrowReturn = new Tab(LanguageManager.getText("tab.borrow"), createBorrowReturnPane());
        Tab tabSearch = new Tab(LanguageManager.getText("tab.search"), createSearchPane());
        Tab tabHistory = new Tab(LanguageManager.getText("tab.history"), createHistoryPane());
        Tab tabStatistics = new Tab(LanguageManager.getText("tab.stats"), createStatisticsPane());

        tabPane.getTabs().addAll(tabHome, tabGallery, tabBooks, tabUsers, tabBorrowReturn, tabSearch, tabHistory, tabStatistics);

        // 3. Thanh Navigation (Tạo lại để cập nhật tên Tab)
        ScrollPane navBar = createScrollableNavBar(tabPane);

        // 4. Layout chính
        rootLayout = new BorderPane();
        rootLayout.setTop(navBar);
        rootLayout.setCenter(tabPane);

        // --- BOTTOM BAR: Nút Theme + Nút Ngôn ngữ ---
        Button themeToggleBtn = new Button();
        updateThemeButtonText(themeToggleBtn);
        themeToggleBtn.setOnAction(e -> toggleTheme(rootLayout.getScene(), themeToggleBtn));

        // Nút đổi ngôn ngữ
        Button langBtn = new Button();
        updateLangButtonText(langBtn);
        langBtn.setOnAction(e -> handleLanguageSwitch(langBtn));

        HBox bottomBar = new HBox(10, langBtn, themeToggleBtn);
        bottomBar.setAlignment(Pos.BOTTOM_RIGHT);
        bottomBar.setPadding(new Insets(10));
        rootLayout.setBottom(bottomBar);

        // 5. Tạo Scene
        Scene scene = new Scene(rootLayout, 1100, 750);

        // Load CSS
        java.net.URL cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl == null) cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        if (isDarkMode) {
            scene.getRoot().getStyleClass().add("dark-mode");
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle("📚 " + LanguageManager.getText("tab.manage_book"));
        primaryStage.show();
    }

    // --- CÁC HÀM HỖ TRỢ NGÔN NGỮ ---

    private void handleLanguageSwitch(Button btn) {
        if (LanguageManager.getCurrentLang().equals("vi")) {
            LanguageManager.setLanguage("en");
        } else {
            LanguageManager.setLanguage("vi");
        }
        rebuildUI(); // Vẽ lại giao diện
    }

    private void updateLangButtonText(Button btn) {
        if (LanguageManager.getCurrentLang().equals("vi")) {
            btn.setText(LanguageManager.getText("btn.lang_en"));
        } else {
            btn.setText(LanguageManager.getText("btn.lang_vi"));
        }
    }

    private void updateThemeButtonText(Button btn) {
        if (isDarkMode) {
            btn.setText(LanguageManager.getText("btn.theme_light"));
        } else {
            btn.setText(LanguageManager.getText("btn.theme_dark"));
        }
    }

    private void toggleTheme(Scene scene, Button button) {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            if (!scene.getRoot().getStyleClass().contains("dark-mode")) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark-mode");
        }
        updateThemeButtonText(button);
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
    private Pane createUserPane() {
        TableColumn<User, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(150);

        TableColumn<User, String> colName = new TableColumn<>(LanguageManager.getText("col.user_name"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(300);

        userTable.getColumns().clear();
        userTable.getColumns().addAll(colId, colName);
        userTable.setItems(userData);

        TextField idField = new TextField();
        idField.setPromptText(LanguageManager.getText("col.id"));

        TextField nameField = new TextField();
        nameField.setPromptText(LanguageManager.getText("col.name"));

        Button addBtn = new Button(LanguageManager.getText("btn.add_user"));
        addBtn.setOnAction(e -> {
            if (!idField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                User user = new User(idField.getText(), nameField.getText());
                library.addUser(user);
                userData.setAll(library.getListUsers());
                showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.user_added"));
                idField.clear(); nameField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.user_missing_info"));
            }
        });

        Button deleteBtn = new Button(LanguageManager.getText("btn.delete_user"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                library.deleteUser(selected.getId());
                userData.setAll(library.getListUsers());
                showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.user_deleted"));
            } else {
                showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.select_user_delete"));
            }
        });

        Button viewUserHistoryBtn = new Button(LanguageManager.getText("btn.view_history_user"));
        viewUserHistoryBtn.setDisable(true);
        viewUserHistoryBtn.setOnAction(e -> handleViewUserHistory(userTable.getSelectionModel().getSelectedItem()));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            viewUserHistoryBtn.setDisable(newSelection == null);
        });

        TextField searchUserField = new TextField();
        searchUserField.setPromptText(LanguageManager.getText("field.search_user"));
        searchUserField.setPrefWidth(250);

        Button searchIdBtn = new Button(LanguageManager.getText("btn.search_id"));
        searchIdBtn.setOnAction(e -> handleSearchUserById(searchUserField.getText()));

        Button searchNameBtn = new Button(LanguageManager.getText("btn.search_name"));
        searchNameBtn.setOnAction(e -> handleSearchUserByName(searchUserField.getText()));

        Button clearSearchBtn = new Button(LanguageManager.getText("btn.clear_search"));
        clearSearchBtn.setOnAction(e -> {
            userData.setAll(library.getListUsers());
            searchUserField.clear();
            userTable.getSelectionModel().clearSelection();
        });

        HBox searchControls = new HBox(10, searchUserField, searchIdBtn, searchNameBtn, clearSearchBtn);
        searchControls.setPadding(new Insets(10, 0, 10, 0));
        searchControls.setAlignment(Pos.CENTER_LEFT);

        HBox crudControls = new HBox(10, idField, nameField, addBtn, deleteBtn, viewUserHistoryBtn);
        crudControls.setPadding(new Insets(10, 0, 0, 0));
        crudControls.setAlignment(Pos.CENTER_LEFT);

        VBox pane = new VBox(10, searchControls, userTable, crudControls);
        pane.setPadding(new Insets(10));

        return pane;
    }

    private void handleSearchUserById(String userId) {
        if (userId.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.search_input_required"));
            return;
        }
        User foundUser = library.searchUserById(userId);

        if (foundUser != null) {
            userTable.getSelectionModel().select(foundUser);
            userTable.scrollTo(foundUser);
            showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.user_found") + ": " + userId);
        } else {
            userTable.getSelectionModel().clearSelection();
            showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.user_not_found") + ": " + userId);
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

    private Pane createBorrowReturnPane() {
        TextField userIdField = new TextField();
        userIdField.setPromptText(LanguageManager.getText("field.user_id"));
        TextField bookIdField = new TextField();
        bookIdField.setPromptText(LanguageManager.getText("field.book_id"));

        Button borrowBtn = new Button(LanguageManager.getText("btn.action_borrow"));
        borrowBtn.setOnAction(e -> {
            String message = quanLyMuonTra.choMuonSach(userIdField.getText(), bookIdField.getText());
            bookData.setAll(library.getBooks());
            userData.setAll(library.getListUsers());
            bookGalleryTab.updateBookGallery();

            Alert.AlertType type = message.contains("Thành công") || message.contains("Success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
            showAlert(type, LanguageManager.getText("title.borrow_dialog"), message);
            userIdField.clear(); bookIdField.clear();
        });

        Button returnBtn = new Button(LanguageManager.getText("btn.action_return"));
        returnBtn.setOnAction(e -> {
            String uId = userIdField.getText().trim();
            String bId = bookIdField.getText().trim();

            if (uId.isEmpty() || bId.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.user_missing_info"));
                return;
            }

            long fine = quanLyMuonTra.calculateFine(uId, bId);
            if (fine > 0) {
                String fineStr = String.format("%,d VNĐ", fine);
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle(LanguageManager.getText("msg.fine_warning"));
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText(String.format(LanguageManager.getText("msg.fine_content"), fineStr));

                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }

            String message = quanLyMuonTra.traSach(uId, bId);
            bookData.setAll(library.getBooks());
            userData.setAll(library.getListUsers());
            bookGalleryTab.updateBookGallery();
            homeTab.refreshData();

            boolean isSuccess = message.contains("Thành công") || message.contains("Success");
            if (isSuccess) {
                userIdField.clear(); bookIdField.clear();
                String displayMsg = LanguageManager.getText("msg.return_success");
                if (fine > 0) {
                    displayMsg += "\n" + String.format(LanguageManager.getText("msg.fine_collected"), String.format("%,d VNĐ", fine));
                }
                showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("title.return_dialog"), displayMsg);
            } else {
                showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), message);
            }
        });

        HBox pane = new HBox(10, userIdField, bookIdField, borrowBtn, returnBtn);
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-alignment: center-left;");
        return pane;
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

            try {
                if (path != null && !path.isEmpty()) {
                    File imageFile = BackEnd.Utils.FileUtil.getLocalFile(path);
                    if (imageFile.exists()) {
                        BufferedImage originalAWTImage = ImageIO.read(imageFile);
                        image = convertAndResize(originalAWTImage);
                        if (image == null) throw new IOException("Resize failed");
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
            library.addBook(new Book("B001", "Dune", "Frank Herbert", "1965"));
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