package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.User.User;
import BackEnd.Utils.BarcodeScannerHandler;
import BackEnd.Utils.LanguageManager;
import Database.DatabaseManager;
import FrontEnd.Controllers.SettingsTabController;
import FrontEnd.Controllers.StatisticTabController;
import FrontEnd.Views.SettingsTabView;
import FrontEnd.Views.StatisticTabView;
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
    private Scene scene; // Lưu scene để tái sử dụng
    private BorderPane rootLayout;
    private boolean isDarkMode = false;

    // --- MODULES (TABS) ---
    // Khai báo biến toàn cục để có thể gọi refresh từ bên ngoài
    private FrontEnd.Views.HomeTabView homeTabView;
    private FrontEnd.Controllers.HomeTabController homeTabController;    private FrontEnd.Views.BookGalleryTabView bookGalleryTabView;
    private FrontEnd.Controllers.BookGalleryTabController bookGalleryTabController;
    private FrontEnd.Views.BookManagementTabView bookManagementTabView;
    private FrontEnd.Controllers.BookManagementTabController bookManagementTabController;
    private FrontEnd.Views.UserTabView userTabView;
    private FrontEnd.Controllers.UserTabController userTabController;    private FrontEnd.Views.BorrowReturnTabView borrowReturnTabView;
    private FrontEnd.Controllers.BorrowReturnTabController borrowReturnTabController;
    private FrontEnd.Views.SearchTabView searchTabView;
    private FrontEnd.Controllers.SearchTabController searchTabController;    private FrontEnd.Views.HistoryTabView historyTabView;
    private FrontEnd.Controllers.HistoryTabController historyTabController;
    private StatisticTabView statisticTabView;
    private StatisticTabController statisticTabController;
    private SettingsTabView settingsTabView;
    private SettingsTabController settingsTabController;

    // Shared UI Component
    private final FlowPane bookFlowPane = new FlowPane();
    // cờ galerry
    private boolean galleryDirty = true;

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

        // Tác vụ nền: Kết nối DB & Load Cache
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("\n" +
                        "Database connecting...");
                DatabaseManager.initializeDatabase();

                updateMessage("Start the Core System...");
                library = new Library();
                initializeData();
                BackEnd.Utils.LicenseManager.init(library);

                updateMessage("Loading data...");
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
            alert.setHeaderText("ERROR, SORY!");
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

// Khởi tạo MVC cho HomeTab
        homeTabView = new FrontEnd.Views.HomeTabView();
        homeTabController = new FrontEnd.Controllers.HomeTabController(library, homeTabView);// Khởi tạo MVC cho BookGalleryTab
        bookGalleryTabView = new FrontEnd.Views.BookGalleryTabView();
        bookGalleryTabController = new FrontEnd.Controllers.BookGalleryTabController(library, bookGalleryTabView, this::convertAndResize);
// Khởi tạo MVC cho BookManagementTab (Truyền galleryController vào)
        bookManagementTabView = new FrontEnd.Views.BookManagementTabView();
        bookManagementTabController = new FrontEnd.Controllers.BookManagementTabController(library, bookManagementTabView, bookGalleryTabController);
// Khởi tạo MVC cho UserTab
        userTabView = new FrontEnd.Views.UserTabView();
        userTabController = new FrontEnd.Controllers.UserTabController(library, userTabView);
        // Truyền callback refreshAllUIComponents vào BorrowReturnTab
// Khởi tạo MVC cho BorrowReturnTab
        borrowReturnTabView = new FrontEnd.Views.BorrowReturnTabView();
        borrowReturnTabController = new FrontEnd.Controllers.BorrowReturnTabController(library, borrowReturnTabView, this::refreshAllUIComponents);
// Khởi tạo MVC cho SearchTab
        searchTabView = new FrontEnd.Views.SearchTabView();
        searchTabController = new FrontEnd.Controllers.SearchTabController(library, searchTabView);// Khởi tạo MVC cho History Tab
        historyTabView = new FrontEnd.Views.HistoryTabView();
        historyTabController = new FrontEnd.Controllers.HistoryTabController(library, historyTabView);
// Khởi tạo MVC cho StatisticTab
        statisticTabView = new FrontEnd.Views.StatisticTabView();
        statisticTabController = new FrontEnd.Controllers.StatisticTabController(library, statisticTabView, this::refreshAllUIComponents);
        // Settings Tab: Callback khi đổi ngôn ngữ là gọi lại initLayout()
        // Khởi tạo MVC cho SettingsTab
        settingsTabView = new FrontEnd.Views.SettingsTabView();
        settingsTabController = new FrontEnd.Controllers.SettingsTabController(
                library,
                settingsTabView,
                scene,
                this::initLayout, // callback khi đổi ngôn ngữ
                isDarkMode
        );

        // 3. Thêm vào TabPane
        tabPane.getTabs().addAll(
                new Tab(LanguageManager.getText("tab.home"), homeTabView),
                new Tab(LanguageManager.getText("tab.gallery"), bookGalleryTabView),
                new Tab(LanguageManager.getText("tab.manage_book"), bookManagementTabView),
                new Tab(LanguageManager.getText("tab.users"), userTabView),
                new Tab(LanguageManager.getText("tab.borrow"), borrowReturnTabView),
                new Tab(LanguageManager.getText("tab.search"), searchTabView),
                new Tab(LanguageManager.getText("tab.history"), historyTabView),
                new Tab(LanguageManager.getText("tab.stats"), statisticTabView),
                new Tab(LanguageManager.getText("tab.settings"), settingsTabView)
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

    // Bổ sung cache cho réize ảnh
    private final java.util.Map<String, Image> imageCache = new java.util.HashMap<>();
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

    //===========================================================================================
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

    public static void main(String[] args) {
        launch(args);
    }
}