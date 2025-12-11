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
    private CheckBox autoAddCheckBox;
    private CheckBox cbAutoAdd;
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

        // --- PHẦN 2: QUY ĐỊNH MƯỢN TRẢ ---
        VBox rulesSection = createRulesSection();

        // --- PHẦN 3: CẤU HÌNH EMAIL ---
        VBox emailSection = createEmailSection();
        VBox dataSection = createDataSection();

        // Thêm vào layout chính (Thêm rulesSection vào giữa)
        mainLayout.getChildren().addAll(pageTitle, generalSection, new Separator(), rulesSection, new Separator(),dataSection, new Separator(), emailSection);
        this.setContent(mainLayout);
    }
    // chỉnh sửa số ngày mượn tối đa cũng như quy định mức phạt
//==============================  ============== =========== =============
    private VBox createRulesSection() {
        // Container chính chứa cả 2 phần
        VBox mainRulesContainer = new VBox(20);

        // =================================================================
        // KHỐI 1: QUY ĐỊNH TÀI CHÍNH & THỜI GIAN (Cần mật khẩu Admin)
        // =================================================================
        VBox financeBox = new VBox(10);
        financeBox.getStyleClass().add("input-panel");
        Label headerFinance = new Label(LanguageManager.getText("header.finance_rules"));
        headerFinance.getStyleClass().add("section-title");
        headerFinance.setStyle("-fx-text-fill: #e67e22;"); // Màu cam cho phần tiền nong

        // 1. Số ngày tối đa
        Label lblDays = new Label(LanguageManager.getText("label.max_days"));
        maxDaysField = new TextField();
        maxDaysField.setPrefWidth(100);
        // Validate số
        maxDaysField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) maxDaysField.setText(val.replaceAll("[^\\d]", ""));
        });

        // 2. Tiền phạt
        Label lblFine = new Label(LanguageManager.getText("label.fine_amount"));
        fineAmountField = new TextField();
        fineAmountField.setPrefWidth(150);
        // Validate số
        fineAmountField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) fineAmountField.setText(val.replaceAll("[^\\d]", ""));
        });

        // 3. Đơn vị tiền
        Label lblCurrency = new Label(LanguageManager.getText("label.currency"));
        currencyField = new TextField();
        currencyField.setPrefWidth(80);

        // Nút Lưu riêng cho phần Tài chính
        Button btnSaveFinance = new Button(LanguageManager.getText("btn.save_finance"));
        btnSaveFinance.setStyle("-fx-background-color: #d35400; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSaveFinance.setOnAction(e -> handleSaveFinancialRules()); // <--- GỌI HÀM XỬ LÝ RIÊNG

        // Load dữ liệu cũ
        maxDaysField.setText(library.getSettingsDAO().getSetting("max_borrow_days"));
        fineAmountField.setText(library.getSettingsDAO().getSetting("fine_per_day"));
        currencyField.setText(library.getSettingsDAO().getSetting("currency_unit"));
        if(maxDaysField.getText().isEmpty()) maxDaysField.setText("60");
        if(fineAmountField.getText().isEmpty()) fineAmountField.setText("2000");
        if(currencyField.getText().isEmpty()) currencyField.setText("VNĐ");

        // Layout phần Finance: Dùng GridPane hoặc VBox lồng nhau
        financeBox.getChildren().addAll(
                headerFinance,
                new Label(LanguageManager.getText("label.max_days")), maxDaysField,
                new Label(LanguageManager.getText("label.fine_amount")), fineAmountField,
                new Label(LanguageManager.getText("label.currency")), currencyField,
                new Separator(),
                btnSaveFinance
        );


        // =================================================================
        // KHỐI 2: CẤU HÌNH BARCODE & AUTO-ADD (Không cần mật khẩu hoặc tùy chọn)
        // =================================================================
        VBox automationBox = new VBox(10);
        automationBox.getStyleClass().add("input-panel");

        Label headerAuto = new Label(LanguageManager.getText("header.automation_rules"));
        headerAuto.getStyleClass().add("section-title");
        headerAuto.setStyle("-fx-text-fill: #2980b9;"); // Màu xanh cho phần kỹ thuật

        autoAddCheckBox = new CheckBox(LanguageManager.getText("label.auto_add_book"));
        String autoAddStatus = library.getSettingsDAO().getSetting("auto_add_enabled");
        autoAddCheckBox.setSelected("true".equals(autoAddStatus));

        Label lblDesc = new Label(LanguageManager.getText("desc.auto_add_book"));
        lblDesc.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        // Nút Lưu riêng cho phần Auto
        Button btnSaveAuto = new Button(LanguageManager.getText("btn.save_auto"));
        btnSaveAuto.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSaveAuto.setOnAction(e -> handleSaveAutoSettings()); // <--- GỌI HÀM XỬ LÝ RIÊNG

        automationBox.getChildren().addAll(headerAuto, autoAddCheckBox, lblDesc, new Separator(), btnSaveAuto);

        // Thêm cả 2 khối vào container chính
        mainRulesContainer.getChildren().addAll(financeBox, automationBox);
        return mainRulesContainer;
    }
    // phương thức xử lý tiền phạt
    /**
     * Xử lý lưu Quy định (Cần mật khẩu Admin)
     */
    /**
     * Lưu quy định tiền/ngày (BẮT BUỘC CÓ MẬT KHẨU ADMIN)
     */
    /**
     * Lưu cấu hình Auto-Add (Lưu trực tiếp cho tiện)
     */
    private void handleSaveAutoSettings() {
        boolean isAuto = autoAddCheckBox.isSelected();

        // Lưu DB
        boolean success = library.getSettingsDAO().saveSetting("auto_add_enabled", String.valueOf(isAuto));

        if (success) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                    LanguageManager.getText("msg.success"),
                    "Cấu hình Barcode đã được cập nhật.");
        } else {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Lỗi lưu Database.");
        }
    }
    private void handleSaveFinancialRules() {
        String days = maxDaysField.getText().trim();
        String fine = fineAmountField.getText().trim();
        String curr = currencyField.getText().trim();

        if (days.isEmpty() || fine.isEmpty() || curr.isEmpty()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), "Vui lòng nhập đủ thông tin.");
            return;
        }

        // Hộp thoại mật khẩu
        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle(LanguageManager.getText("title.admin_required"));
        passDialog.setHeaderText(LanguageManager.getText("header.save_rules"));
        passDialog.setContentText(LanguageManager.getText("content.enter_admin_pass"));
        passDialog.getDialogPane().setGraphic(new Label("🔒"));

        java.util.Optional<String> result = passDialog.showAndWait();
        if (result.isPresent() && "admin".equals(result.get())) {
            // Lưu DB
            library.getSettingsDAO().saveSetting("max_borrow_days", days);
            library.getSettingsDAO().saveSetting("fine_per_day", fine);
            library.getSettingsDAO().saveSetting("currency_unit", curr);

            FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                    LanguageManager.getText("msg.success"),
                    "Đã lưu quy định tài chính thành công!");
        } else {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR,
                    LanguageManager.getText("msg.error"),
                    LanguageManager.getText("msg.wrong_pass"));
        }
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
        String pass = passField.getText().trim().replace(" ", "");

        if (email.isEmpty() || pass.isEmpty()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING,
                    LanguageManager.getText("msg.error"),
                    "Vui lòng nhập Email và Mật khẩu ứng dụng.");
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
    // ---  PHƯƠNG THỨC TẠO GIAO DIỆN BACKUP ---
    private VBox createDataSection() {
        VBox box = new VBox(15);

        Label header = new Label(LanguageManager.getText("header.data_management"));
        header.getStyleClass().add("section-header-label");

        // Nút Sao lưu
        Button btnBackup = new Button("💾 " + LanguageManager.getText("btn.backup"));
        btnBackup.getStyleClass().add("button-backup");
        btnBackup.setOnAction(e -> handleBackup());

        Label lblNote = new Label("Lưu ý: Hãy sao lưu dữ liệu thường xuyên để tránh mất mát.");
        lblNote.getStyleClass().add("label-note-italic");
        // lblNote.setStyle("-fx-font-style: italic; -fx-text-fill: #666; -fx-font-size: 11px;"); // <-- BỎ DÒNG CŨ NÀY

        box.getChildren().addAll(header, btnBackup, lblNote);
        return box;
    }
    // ---  XỬ LÝ SỰ KIỆN BACKUP ---
    private void handleBackup() {
        // 1. Mở hộp thoại chọn nơi lưu file
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(LanguageManager.getText("title.save_backup"));

        // Tạo tên file gợi ý: library_backup_YYYY-MM-DD.db
        String date = java.time.LocalDate.now().toString();
        fileChooser.setInitialFileName("library_backup_" + date + ".db");

        // Chỉ cho lưu file .db
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("SQLite Database", "*.db")
        );

        java.io.File dest = fileChooser.showSaveDialog(null);

        if (dest != null) {
            try {
                // 2. Gọi BackupService
                boolean success = Boolean.parseBoolean(BackEnd.Utils.BackupService.backupDatabase(dest));

                if (success) {
                    FrontEnd.LibraryApp.showAlert(
                            javafx.scene.control.Alert.AlertType.INFORMATION,
                            LanguageManager.getText("msg.success"),
                            LanguageManager.getText("msg.backup_success") + "\n" + dest.getAbsolutePath()
                    );
                }
            } catch (Exception ex) {
                FrontEnd.LibraryApp.showAlert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        LanguageManager.getText("msg.error"),
                        "ERROR " + ex.getMessage()
                );
                ex.printStackTrace();
            }
        }
    }
}