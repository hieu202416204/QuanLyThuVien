package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.User.User;
import BackEnd.Utils.BarcodeScannerHandler;
import BackEnd.Utils.LanguageManager;
import Database.DatabaseManager;
import FrontEnd.Controllers.*;
import FrontEnd.Views.*;
import XuLiAnh.ImageResizer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LibraryApp extends Application {
    public static final String ADMIN_PASSWORD = "admin";

    // --- CORE SYSTEM ---
    private Library library;
    private Stage primaryStage;
    private Scene scene;
    private BorderPane rootLayout;
    private boolean isDarkMode = false;
    private TabPane tabPane;

    // --- MODULES (TABS) MVC ---
    private HomeTabView homeTabView;
    private HomeTabController homeTabController;
    private BookGalleryTabView bookGalleryTabView;
    private BookGalleryTabController bookGalleryTabController;
    private BookManagementTabView bookManagementTabView;
    private BookManagementTabController bookManagementTabController;
    private UserTabView userTabView;
    private UserTabController userTabController;
    private BorrowReturnTabView borrowReturnTabView;
    private BorrowReturnTabController borrowReturnTabController;
    private SearchTabView searchTabView;
    private SearchTabController searchTabController;
    private HistoryTabView historyTabView;
    private HistoryTabController historyTabController;
    private StatisticTabView statisticTabView;
    private StatisticTabController statisticTabController;
    private SettingsTabView settingsTabView;
    private SettingsTabController settingsTabController;

    private boolean galleryDirty = true;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showSplashScreen();
    }

    // ========================================================================
    // 1. KHỞI ĐỘNG & SPLASH SCREEN LÀM VIỆC THẬT
    // ========================================================================
    private void showSplashScreen() {
        ProgressIndicator spinner = new ProgressIndicator();
        Label statusLabel = new Label("Starting system...");
        statusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 14px;");

        VBox splashLayout = new VBox(20, spinner, statusLabel);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        Scene splashScene = new Scene(splashLayout, 400, 300);
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setScene(splashScene);
        splashStage.show();

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Kết nối Database
                updateMessage("Connecting to Database...");
                DatabaseManager.initializeDatabase();
                Thread.sleep(200);

                // 2. Khởi tạo Library và Core
                updateMessage("Initializing Core Systems...");
                library = new Library();
                initializeData();
                BackEnd.Utils.LicenseManager.init(library);
                Thread.sleep(200);

                // 3. Tải cài đặt (Settings)
                updateMessage("Loading System Settings...");
                String savedTheme = library.getSettingsDAO().getSetting("theme");
                isDarkMode = "dark".equals(savedTheme);
                Thread.sleep(200);

                updateMessage("Counting total data...");
                library.getBookDAO().getTotalBookCount();
                library.getUserDAO().getTotalUserCount();

                return null;
            }
        };

        statusLabel.textProperty().bind(initTask.messageProperty());

        initTask.setOnSucceeded(e -> {
            splashStage.close();
            showMainUI();
            new Thread(this::runAutoEmailCheck).start();
        });

        initTask.setOnFailed(e -> {
            splashStage.close();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Critical Error");
            alert.setHeaderText("Initialization Failed");
            alert.setContentText(initTask.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(initTask).start();
    }

    // ========================================================================
    // 2. GIAO DIỆN CHÍNH & LAZY LOADING
    // ========================================================================
    private void showMainUI() {
        if (this.scene == null) {
            this.scene = new Scene(new Region(), 1150, 750);
        }

        initLayout();

        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/iconLib.ico")));
        primaryStage.show();

        BarcodeScannerHandler scannerHandler = new BarcodeScannerHandler(library);
        scannerHandler.attachToScene(scene);
    }

    private void initLayout() {
        // --- Dùng StackPane để NavBar có thể đè lên TabPane ---
        StackPane mainStack = new StackPane();
        primaryStage.setTitle("📚 " + LanguageManager.getText("app_title"));

        scene.getStylesheets().clear();
        java.net.URL cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        mainStack.getStyleClass().remove("dark-mode");
        if (this.isDarkMode) mainStack.getStyleClass().add("dark-mode");

        tabPane = new TabPane();
        tabPane.getStyleClass().add("hidden-header-tab-pane");

        // KHỞI TẠO DUY NHẤT HOMETAB (Để hiển thị ngay lập tức)
        homeTabView = new HomeTabView();
        homeTabController = new HomeTabController(library, homeTabView);

        // Tạo các Tab rỗng
        Tab tHome = new Tab(LanguageManager.getText("tab.home"), homeTabView);
        Tab tGallery = new Tab(LanguageManager.getText("tab.gallery"), new Label("Loading..."));
        Tab tManageBook = new Tab(LanguageManager.getText("tab.manage_book"), new Label("Loading..."));
        Tab tUsers = new Tab(LanguageManager.getText("tab.users"), new Label("Loading..."));
        Tab tBorrow = new Tab(LanguageManager.getText("tab.borrow"), new Label("Loading..."));
        Tab tSearch = new Tab(LanguageManager.getText("tab.search"), new Label("Loading..."));
        Tab tHistory = new Tab(LanguageManager.getText("tab.history"), new Label("Loading..."));
        Tab tStats = new Tab(LanguageManager.getText("tab.stats"), new Label("Loading..."));
        Tab tSettings = new Tab(LanguageManager.getText("tab.settings"), new Label("Loading..."));

        tabPane.getTabs().addAll(tHome, tGallery, tManageBook, tUsers, tBorrow, tSearch, tHistory, tStats, tSettings);

        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            loadTabContent(newVal.intValue());
        });

        // Tạo Navbar (thanh chứa các nút Tab)
        ScrollPane navBar = createScrollableNavBar(tabPane);

        // --- THIẾT LẬP HIỆU ỨNG ẨN/HIỆN NAVBAR ---
//        // 1. Một "vùng cảm ứng" (Hover Zone) trong suốt nằm ở sát cạnh trên
//        Region hoverZone = new Region();
//        hoverZone.setPrefHeight(1); // Vùng cảm ứng
//        hoverZone.setMaxHeight(1);
//        hoverZone.setStyle("-fx-background-color: transparent;");
//        StackPane.setAlignment(hoverZone, Pos.TOP_CENTER);

        // 2. Định vị Navbar ở trên cùng và ẩn nó lên trên ngoài màn hình (-60px) ban đầu
        StackPane.setAlignment(navBar, Pos.TOP_CENTER);
        navBar.setMaxHeight(70);
        navBar.setTranslateY(-60); // Đẩy Navbar lên khỏi màn hình 60px

        // 3. Tạo hiệu ứng Animation trượt
        javafx.animation.TranslateTransition slideDown = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), navBar);
        slideDown.setToY(0); // Trượt xuống vị trí 0 (hiện ra)

        javafx.animation.TranslateTransition slideUp = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), navBar);
        slideUp.setToY(-60); // Trượt lên -60 (ẩn đi)

        // 4. Bắt sự kiện chuột
        // Khi chuột chạm vào vùng cảm ứng trên cùng -> Đổ Navbar xuống
//        hoverZone.setOnMouseEntered(e -> {
//            slideUp.stop();
//            slideDown.play();
//        });

        // Để giữ Navbar không bị cuộn lên khi chuột đang nằm trên chính Navbar đó
        navBar.setOnMouseEntered(e -> {
            slideUp.stop();
            slideDown.play();
        });

        // Khi chuột rời khỏi Navbar (đi xuống khu vực bảng) -> Cuộn Navbar lên
        navBar.setOnMouseEntered(e -> {
            slideUp.stop();
            slideDown.play();
        });
        navBar.setOnMouseExited(e -> {
            slideDown.stop();
            slideUp.play();
        });
        scene.setOnMouseMoved(e -> {
            if (e.getSceneY() <= 3) { // chỉ khi sát mép trên (3px)
                slideUp.stop();
                slideDown.play();
            }
        });
        // --- RÁP CÁC LỚP LẠI ---
        // Lớp 1: Nội dung chính (TabPane) chiếm trọn màn hình
        // Lớp 2: Navbar (ẩn ở trên)
        mainStack.getChildren().addAll(tabPane, navBar);

        scene.setRoot(mainStack);
    }
    /**
     * Khởi tạo Tab khi người dùng bấm vào (Lazy Loading)
     */
    private void loadTabContent(int tabIndex) {
        Tab targetTab = tabPane.getTabs().get(tabIndex);

        // Nếu tab đã được load (không phải là Label "Loading...") thì bỏ qua
        if (!(targetTab.getContent() instanceof Label)) {
            refreshSpecificTab(tabIndex); // Chỉ làm mới dữ liệu
            return;
        }

        // Tải nội dung thực sự tùy theo index
        switch (tabIndex) {
            case 1: // Gallery
                bookGalleryTabView = new BookGalleryTabView();
                bookGalleryTabController = new BookGalleryTabController(library, bookGalleryTabView, this::convertAndResize);
                targetTab.setContent(bookGalleryTabView);
                break;
            case 2: // Manage Book
                // Phải load GalleryController trước nếu nó null (vì ManageBook cần gọi Gallery update)
                if (bookGalleryTabController == null) {
                    bookGalleryTabView = new BookGalleryTabView();
                    bookGalleryTabController = new BookGalleryTabController(library, bookGalleryTabView, this::convertAndResize);
                    tabPane.getTabs().get(1).setContent(bookGalleryTabView);
                }
                bookManagementTabView = new BookManagementTabView();
                bookManagementTabController = new BookManagementTabController(library, bookManagementTabView, bookGalleryTabController);
                targetTab.setContent(bookManagementTabView);
                break;
            case 3: // Users
                userTabView = new UserTabView();
                userTabController = new UserTabController(library, userTabView);
                targetTab.setContent(userTabView);
                break;
            case 4: // Borrow/Return
                borrowReturnTabView = new BorrowReturnTabView();
                borrowReturnTabController = new BorrowReturnTabController(library, borrowReturnTabView, this::refreshAllUIComponents);
                targetTab.setContent(borrowReturnTabView);
                break;
            case 5: // Search
                searchTabView = new SearchTabView();
                searchTabController = new SearchTabController(library, searchTabView);
                targetTab.setContent(searchTabView);
                break;
            case 6: // History
                historyTabView = new HistoryTabView();
                historyTabController = new HistoryTabController(library, historyTabView);
                targetTab.setContent(historyTabView);
                break;
            case 7: // Stats
                statisticTabView = new StatisticTabView();
                statisticTabController = new StatisticTabController(library, statisticTabView, this::refreshAllUIComponents);
                targetTab.setContent(statisticTabView);
                break;
            case 8: // Settings
                settingsTabView = new SettingsTabView();
                settingsTabController = new SettingsTabController(library, settingsTabView, scene, this::initLayout, isDarkMode);
                targetTab.setContent(settingsTabView);
                break;
        }
    }

    // ========================================================================
    // 3. ORCHESTRATOR (ĐIỀU PHỐI DỮ LIỆU)
    // ========================================================================

    // Refresh toàn cục (Dùng khi Thêm/Xóa/Sửa quan trọng)
    private void refreshAllUIComponents() {
        Platform.runLater(() -> {
            if (homeTabController != null) homeTabController.refreshData();
            if (bookGalleryTabController != null && galleryDirty) {
                bookGalleryTabController.updateBookGallery();
                galleryDirty = false;
            }
            if (bookManagementTabController != null) bookManagementTabController.refreshTable();
            if (userTabController != null) userTabController.refreshData();
            if (borrowReturnTabController != null){
                borrowReturnTabController.refreshData();
                galleryDirty = true;
            }
            if (statisticTabController != null) statisticTabController.refreshData();
            if (historyTabController != null) historyTabController.reloadHistoryFromDB();
        });
    }

    // Refresh riêng biệt khi chuyển Tab
    private void refreshSpecificTab(int tabIndex) {
        Platform.runLater(() -> {
            switch (tabIndex) {
                case 0: if (homeTabController != null) homeTabController.refreshData(); break;
                case 1: if (bookGalleryTabController != null && galleryDirty) { bookGalleryTabController.updateBookGallery(); galleryDirty = false; } break;
                case 2: if (bookManagementTabController != null) bookManagementTabController.refreshTable(); break;
                case 3: if (userTabController != null) userTabController.refreshData(); break;
                case 4: if (borrowReturnTabController != null) borrowReturnTabController.refreshData(); break;
            }
        });
    }

    // ========================================================================
    // 4. HELPER METHODS & OTHERS
    // ========================================================================

    private ScrollPane createScrollableNavBar(TabPane tabPane) {
        HBox navBox = new HBox();
        navBox.getStyleClass().add("nav-bar-container");
        navBox.setAlignment(Pos.CENTER);
        navBox.setPadding(new javafx.geometry.Insets(10, 20, 10, 20));

        ToggleGroup tg = new ToggleGroup();
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            ToggleButton btn = new ToggleButton(tabPane.getTabs().get(i).getText());
            btn.setToggleGroup(tg);
            btn.getStyleClass().add("nav-tab-button");

            final int idx = i;
            btn.setOnAction(e -> tabPane.getSelectionModel().select(idx));

            tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.intValue() == idx) btn.setSelected(true);
            });

            if (i == 0) btn.setSelected(true);
            navBox.getChildren().add(btn);
        }

        Button helpBtn = new Button("❓ Help");
        helpBtn.getStyleClass().add("nav-tab-button");
        helpBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        helpBtn.setOnAction(e -> BackEnd.Utils.HelpService.openUserGuide());

        navBox.getChildren().addAll(helpBtn);

        ScrollPane sp = new ScrollPane(navBox);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("nav-scroll-pane");

        // Tạo đổ bóng để khi slide down nhìn giống như một cái khay rớt xuống
        sp.setStyle("-fx-background-color: transparent; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);");

        return sp;
    }

    public static void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public Image convertAndResize(BufferedImage originalImage) {
        if (originalImage == null) return null;
        try {
            BufferedImage resized = ImageResizer.resizeImage(originalImage, 120, 160);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", os);
            return new Image(new ByteArrayInputStream(os.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }

    private void runAutoEmailCheck() {
        try {
            String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
            int maxDays = (maxDaysStr != null && !maxDaysStr.isEmpty()) ? Integer.parseInt(maxDaysStr) : 60;
            var overdueList = library.getTransactionDAO().getOverdueTransactionsWithEmail(maxDays);
            for (String[] record : overdueList) {
                BackEnd.Utils.EmailService.sendOverdueNotification(library, record[0], record[1], record[2], Long.parseLong(record[3]));
                Thread.sleep(2000);
            }
        } catch (Exception ignored) {}
    }

    private void initializeData() {
        if (library.getListUsers().isEmpty() || library.getBooks().isEmpty()) {
            System.out.println("-> Init Demo Data.");
            library.addBook(new Book("B001", "Clean Code", "Robert C. Martin", "2008", "CNTT"));
            library.addUser(new User("U001", "Nguyễn Văn An"));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}