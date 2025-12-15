package FrontEnd;

import BackEnd.LibraryQ.Library;
import BackEnd.Utils.BarcodeScannerHandler;
import BackEnd.Utils.LanguageManager;
import Database.DatabaseManager;
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

    // --- CORE SYSTEM ---
    private Library library;
    private Stage primaryStage;
    private Scene scene; // Lưu scene để tái sử dụng
    private BorderPane rootLayout;
    private boolean isDarkMode = false;

    // --- MODULES (TABS) ---
    // Khai báo biến toàn cục để có thể gọi refresh từ bên ngoài
    private HomeTab homeTab;
    private BookGalleryTab bookGalleryTab;
    private BookManagementTab bookManagementTab;
    private UserTab userTab;
    private BorrowReturnTab borrowReturnTab;
    private SearchTab searchTab;
    private HistoryTab historyTab;
    private StatisticTab statisticTab;
    private SettingsTab settingsTab;

    // Shared UI Component
    private final FlowPane bookFlowPane = new FlowPane();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showSplashScreen();
    }

    // ========================================================================
    // 1. KHỞI ĐỘNG & SPLASH SCREEN
    // ========================================================================
    private void showSplashScreen() {
        ProgressIndicator spinner = new ProgressIndicator();
        Label statusLabel = new Label("Đang khởi động hệ thống...");
        statusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 14px;");

        VBox splashLayout = new VBox(20, spinner, statusLabel);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        Scene splashScene = new Scene(splashLayout, 400, 300);
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setScene(splashScene);
        splashStage.show();

        // Tác vụ nền: Kết nối DB & Load Cache
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Kết nối cơ sở dữ liệu...");
                DatabaseManager.initializeDatabase();

                updateMessage("Khởi động Core System...");
                library = new Library();
                BackEnd.Utils.LicenseManager.init(library);

                updateMessage("Đang tải dữ liệu...");
                library.getBookDAO().reloadCache(); // Pre-load cache

                Thread.sleep(800); // Delay giả lập (nếu cần)
                return null;
            }
        };

        statusLabel.textProperty().bind(initTask.messageProperty());

        initTask.setOnSucceeded(e -> {
            splashStage.close();
            showMainUI(); // Vào màn hình chính
            new Thread(this::runAutoEmailCheck).start(); // Chạy check email ngầm
        });

        initTask.setOnFailed(e -> {
            splashStage.close();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Critical Error");
            alert.setHeaderText("Lỗi khởi động");
            alert.setContentText(initTask.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(initTask).start();
    }

    // ========================================================================
    // 2. GIAO DIỆN CHÍNH (MAIN UI)
    // ========================================================================
    private void showMainUI() {
        // Tạo Scene một lần duy nhất
        if (this.scene == null) {
            this.scene = new Scene(new Region(), 1150, 750);
        }

        // Kiểm tra cài đặt Theme từ Database
        String savedTheme = library.getSettingsDAO().getSetting("theme");
        this.isDarkMode = "dark".equals(savedTheme);

        // Dựng giao diện (Hàm này tách riêng để gọi lại khi đổi ngôn ngữ)
        initLayout();

        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/iconLib.ico")));
        primaryStage.show();

        // Setup Scanner toàn cục
        BarcodeScannerHandler scannerHandler = new BarcodeScannerHandler(library);
        scannerHandler.attachToScene(scene);
    }

    /**
     * Hàm dựng lại toàn bộ giao diện (Gọi khi khởi động HOẶC khi đổi ngôn ngữ)
     */
    private void initLayout() {
        rootLayout = new BorderPane();

        // Cập nhật Title theo ngôn ngữ mới
        primaryStage.setTitle("📚 " + LanguageManager.getText("app_title"));

        // 1. Áp dụng CSS
        scene.getStylesheets().clear();
        java.net.URL cssUrl = getClass().getResource("/styles/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        // Áp dụng Dark Mode nếu đang bật
        rootLayout.getStyleClass().remove("dark-mode");
        if (this.isDarkMode) rootLayout.getStyleClass().add("dark-mode");

        // 2. Khởi tạo các Tab (New lại để cập nhật Text ngôn ngữ)
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("hidden-header-tab-pane");

        homeTab = new HomeTab(library);
        bookGalleryTab = new BookGalleryTab(library, bookFlowPane, this::convertAndResize);
        bookManagementTab = new BookManagementTab(library, bookFlowPane, bookGalleryTab);
        userTab = new UserTab(library);

        // Truyền callback refreshAllUIComponents vào BorrowReturnTab
        borrowReturnTab = new BorrowReturnTab(library, this::refreshAllUIComponents);

        searchTab = new SearchTab(library);
        historyTab = new HistoryTab(library);
        statisticTab = new StatisticTab(library, this::refreshAllUIComponents);

        // Settings Tab: Callback khi đổi ngôn ngữ là gọi lại initLayout()
        settingsTab = new SettingsTab(library, scene, () -> {
            System.out.println("Language changed -> Rebuilding Layout...");
            initLayout();
        }, isDarkMode);

        // 3. Thêm vào TabPane
        tabPane.getTabs().addAll(
                new Tab(LanguageManager.getText("tab.home"), homeTab),
                new Tab(LanguageManager.getText("tab.gallery"), bookGalleryTab),
                new Tab(LanguageManager.getText("tab.manage_book"), bookManagementTab),
                new Tab(LanguageManager.getText("tab.users"), userTab),
                new Tab(LanguageManager.getText("tab.borrow"), borrowReturnTab),
                new Tab(LanguageManager.getText("tab.search"), searchTab),
                new Tab(LanguageManager.getText("tab.history"), historyTab),
                new Tab(LanguageManager.getText("tab.stats"), statisticTab),
                new Tab(LanguageManager.getText("tab.settings"), settingsTab)
        );

        // 4. Tạo Navigation Bar
        ScrollPane navBar = createScrollableNavBar(tabPane);

        rootLayout.setTop(navBar);
        rootLayout.setCenter(tabPane);

        // Gán root mới cho Scene (Thay thế giao diện cũ)
        scene.setRoot(rootLayout);

        // Load dữ liệu
        refreshAllUIComponents();
    }

    // ========================================================================
    // 3. ORCHESTRATOR (HÀM ĐIỀU PHỐI)
    // ========================================================================
    private void refreshAllUIComponents() {
        Platform.runLater(() -> {
            // 1. Refresh Backend Cache
            if (library != null) library.getBookDAO().reloadCache();

            // 2. Refresh từng Tab
            if (homeTab != null) homeTab.refreshData();
            if (bookGalleryTab != null) bookGalleryTab.updateBookGallery();
            if (bookManagementTab != null) bookManagementTab.refreshTable();
            if (userTab != null) userTab.refreshData();
            if (borrowReturnTab != null) borrowReturnTab.refreshData();
            if (statisticTab != null) statisticTab.refreshData();

            // Cập nhật Lịch sử (Để thấy ngay khi trả sách)
            if (historyTab != null) historyTab.reloadHistoryFromDB();
        });
    }

    // ========================================================================
    // 4. HELPER METHODS & UI COMPONENTS
    // ========================================================================

    private ScrollPane createScrollableNavBar(TabPane tabPane) {
        HBox navBox = new HBox();
        navBox.getStyleClass().add("nav-bar-container"); // Dùng class CSS thay vì setStyle cứng

        ToggleGroup tg = new ToggleGroup();
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            ToggleButton btn = new ToggleButton(tabPane.getTabs().get(i).getText());
            btn.setToggleGroup(tg);
            btn.getStyleClass().add("nav-tab-button");

            final int idx = i;
            btn.setOnAction(e -> tabPane.getSelectionModel().select(idx));
            if (i == 0) btn.setSelected(true);

            navBox.getChildren().add(btn);
        }

        Button helpBtn = new Button("❓ Help");
        helpBtn.getStyleClass().add("nav-tab-button");
        helpBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        helpBtn.setOnAction(e -> BackEnd.Utils.HelpService.openUserGuide());

        navBox.getChildren().addAll( helpBtn);

        ScrollPane sp = new ScrollPane(navBox);

        sp.setFitToWidth(true); // Ép dãn chiều ngang
        // ------------------------------------------------

        sp.setFitToHeight(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("nav-scroll-pane"); // CSS nền tối

        return sp;
    }

    // Tiện ích hiển thị thông báo (Để các class con gọi)
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
            BufferedImage resizedImage = ImageResizer.resizeImage(originalImage, 120, 160);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", os);
            return new Image(new ByteArrayInputStream(os.toByteArray()));
        } catch (IOException e) { return null; }
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

    public static void main(String[] args) {
        launch(args);
    }
}