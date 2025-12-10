package FrontEnd;

import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsTab extends ScrollPane { // Dùng ScrollPane để tránh bị che khi màn hình nhỏ

    // Rules Controls
    private TextField maxDaysField;
    private TextField fineAmountField;
    private TextField currencyField;
    private final Library library;
    private final Scene scene;
    private final Runnable onLanguageChange; // Hàm callback để báo cho App biết cần vẽ lại

    // Email Controls
    private TextField emailField;
    private PasswordField passField;

    public SettingsTab(Library library, Scene scene, Runnable onLanguageChange, boolean isDarkMode) {
        this.library = library;
        this.scene = scene;
        this.onLanguageChange = onLanguageChange;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        // Layout chính
        VBox mainLayout = new VBox(25);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setAlignment(Pos.TOP_LEFT);

        // Tiêu đề trang
        Label pageTitle = new Label(LanguageManager.getText("tab.settings"));
        pageTitle.getStyleClass().add("page-title");

        // --- PHẦN 1: GIAO DIỆN & NGÔN NGỮ ---
        VBox generalSection = createGeneralSection(isDarkMode);

        // --- PHẦN 2: QUY ĐỊNH MƯỢN TRẢ (MỚI) ---
        VBox rulesSection = createRulesSection();

        // --- PHẦN 3: CẤU HÌNH EMAIL ---
        VBox emailSection = createEmailSection();

        // Thêm vào layout chính (Thêm rulesSection vào giữa)
        mainLayout.getChildren().addAll(pageTitle, generalSection, new Separator(), rulesSection, new Separator(), emailSection);
        this.setContent(mainLayout);
    }
    // chỉnh sửa số ngày mượn tối đa cũng như quy định mức phạt
//==============================  ============== =========== =============
    private VBox createRulesSection() {
        VBox box = new VBox(15);

        Label header = new Label(LanguageManager.getText("header.rules_settings"));
        header.getStyleClass().add("section-title");
        header.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980b9;");

        // 1. Số ngày tối đa
        Label lblDays = new Label(LanguageManager.getText("label.max_days"));
        maxDaysField = new TextField();
        maxDaysField.setPromptText("60"); // Mặc định cũ
        maxDaysField.setMaxWidth(150);
        // Chỉ cho nhập số
        maxDaysField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                maxDaysField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // 2. Tiền phạt
        Label lblFine = new Label(LanguageManager.getText("label.fine_amount"));
        fineAmountField = new TextField();
        fineAmountField.setPromptText("2000"); // Mặc định cũ
        fineAmountField.setMaxWidth(150);
        // Chỉ cho nhập số
        fineAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                fineAmountField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // 3. Đơn vị tiền tệ
        Label lblCurrency = new Label(LanguageManager.getText("label.currency"));
        currencyField = new TextField();
        currencyField.setPromptText("VNĐ");
        currencyField.setMaxWidth(100);

        // Nút Lưu riêng cho phần này (hoặc dùng chung nút lưu cuối cùng cũng được, ở đây tôi tạo nút riêng cho từng phần để rõ ràng)
        Button btnSaveRules = new Button(LanguageManager.getText("btn.save_settings"));
        btnSaveRules.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSaveRules.setOnAction(e -> handleSaveRules());

        // Load dữ liệu cũ
        String savedDays = library.getSettingsDAO().getSetting("max_borrow_days");
        String savedFine = library.getSettingsDAO().getSetting("fine_per_day");
        String savedCurr = library.getSettingsDAO().getSetting("currency_unit");

        // Nếu chưa có trong DB thì hiển thị mặc định
        maxDaysField.setText(savedDays.isEmpty() ? "60" : savedDays);
        fineAmountField.setText(savedFine.isEmpty() ? "2000" : savedFine);
        currencyField.setText(savedCurr.isEmpty() ? "VNĐ" : savedCurr);

        box.getChildren().addAll(header, lblDays, maxDaysField, lblFine, fineAmountField, lblCurrency, currencyField, btnSaveRules);
        return box;
    }
    // phương thức xử lý tiền phạt
    private void handleSaveRules() {
        String days = maxDaysField.getText().trim();
        String fine = fineAmountField.getText().trim();
        String curr = currencyField.getText().trim();

        if (days.isEmpty() || fine.isEmpty() || curr.isEmpty()) {
            LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        library.getSettingsDAO().saveSetting("max_borrow_days", days);
        library.getSettingsDAO().saveSetting("fine_per_day", fine);
        library.getSettingsDAO().saveSetting("currency_unit", curr);

        LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.settings_saved"));
    }

    private VBox createGeneralSection(boolean isDarkMode) {
        VBox box = new VBox(15);

        Label header = new Label(LanguageManager.getText("header.general_settings"));
        header.getStyleClass().add("section-title");
        header.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980b9;");

        // 1. Dòng chuyển đổi Theme
        HBox themeBox = new HBox(15);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        Label lblTheme = new Label(LanguageManager.getText("label.theme"));

        // Nút Toggle Theme
        Button btnTheme = new Button();
        updateThemeButtonText(btnTheme, isDarkMode); // Set chữ ban đầu
        btnTheme.setOnAction(e -> handleToggleTheme(btnTheme));

        themeBox.getChildren().addAll(lblTheme, btnTheme);

        // 2. Dòng chuyển đổi Ngôn ngữ
        HBox langBox = new HBox(15);
        langBox.setAlignment(Pos.CENTER_LEFT);
        Label lblLang = new Label(LanguageManager.getText("label.language"));

        // Nút Toggle Ngôn ngữ
        Button btnLang = new Button();
        updateLangButtonText(btnLang); // Set chữ ban đầu
        btnLang.setOnAction(e -> handleSwitchLanguage());

        langBox.getChildren().addAll(lblLang, btnLang);

        box.getChildren().addAll(header, themeBox, langBox);
        return box;
    }

    private VBox createEmailSection() {
        VBox box = new VBox(15);

        Label header = new Label(LanguageManager.getText("header.email_settings"));
        header.getStyleClass().add("section-title");
        header.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980b9;");

        // Email
        Label lblEmail = new Label(LanguageManager.getText("label.admin_email"));
        emailField = new TextField();
        emailField.setPromptText("example@gmail.com");
        emailField.setMaxWidth(400);

        // Password
        Label lblPass = new Label(LanguageManager.getText("label.app_password"));
        passField = new PasswordField();
        passField.setPromptText("xxyy zzaa bbcc ddee");
        passField.setMaxWidth(400);

        // Guide
        Label guide = new Label(LanguageManager.getText("desc.app_pass_guide"));
        guide.setStyle("-fx-font-style: italic; -fx-text-fill: #666; -fx-font-size: 11px;");

        // Button Save
        Button btnSave = new Button(LanguageManager.getText("btn.save_settings"));
        btnSave.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSave.setOnAction(e -> handleSaveSettings());

        // Load dữ liệu cũ
        String savedEmail = library.getSettingsDAO().getSetting("admin_email");
        String savedPass = library.getSettingsDAO().getSetting("app_password");
        emailField.setText(savedEmail);
        passField.setText(savedPass);

        box.getChildren().addAll(header, lblEmail, emailField, lblPass, passField, guide, btnSave);
        return box;
    }

    // --- LOGIC XỬ LÝ ---

    private void handleToggleTheme(Button btn) {
        // Kiểm tra xem Scene đã có class dark-mode chưa
        boolean isCurrentlyDark = scene.getRoot().getStyleClass().contains("dark-mode");

        if (isCurrentlyDark) {
            scene.getRoot().getStyleClass().remove("dark-mode");
            updateThemeButtonText(btn, false);
        } else {
            scene.getRoot().getStyleClass().add("dark-mode");
            updateThemeButtonText(btn, true);
        }
    }

    private void updateThemeButtonText(Button btn, boolean isDark) {
        if (isDark) {
            btn.setText(LanguageManager.getText("btn.theme_light")); // "Chuyển sang Sáng"
        } else {
            btn.setText(LanguageManager.getText("btn.theme_dark"));  // "Chuyển sang Tối"
        }
    }

    private void handleSwitchLanguage() {
        // 1. Đổi ngôn ngữ trong Manager
        if (LanguageManager.getCurrentLang().equals("vi")) {
            LanguageManager.setLanguage("en");
        } else {
            LanguageManager.setLanguage("vi");
        }

        // 2. GỌI CALLBACK ĐỂ VẼ LẠI TOÀN BỘ APP
        if (onLanguageChange != null) {
            onLanguageChange.run();
        }
    }

    private void updateLangButtonText(Button btn) {
        if (LanguageManager.getCurrentLang().equals("vi")) {
            btn.setText(LanguageManager.getText("btn.lang_en"));
        } else {
            btn.setText(LanguageManager.getText("btn.lang_vi"));
        }
    }

    private void handleSaveSettings() {
        String email = emailField.getText().trim();
        String pass = passField.getText().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), "Vui lòng nhập đủ thông tin.");
            return;
        }

        boolean save1 = library.getSettingsDAO().saveSetting("admin_email", email);
        boolean save2 = library.getSettingsDAO().saveSetting("app_password", pass);

        if (save1 && save2) {
            LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.settings_saved"));
        } else {
            LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Lỗi Database.");
        }
    }
}