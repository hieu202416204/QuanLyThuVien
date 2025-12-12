package FrontEnd;

import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static FrontEnd.LibraryApp.ADMIN_PASSWORD;

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
    //========================= khai bao email nha phat trien ==========================
    private static final String DEVELOPER_SUPPORT_EMAIL ="hieuvan2206@gmail.com";
    // ////////////////////////khai bao email nha phat trien/////////////////////////////////////////
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
        VBox supportSection = createSupportSection();

        // Thêm vào layout chính (Thêm rulesSection vào giữa)
        mainLayout.getChildren().addAll(pageTitle, generalSection,
                new Separator(), rulesSection
                , new Separator(),dataSection, new Separator(),emailSection,
                new Separator(),
                supportSection);
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
        // KHỐI 2: CẤU HÌNH BARCODE & AUTO-ADD
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
        btnSave.setOnAction(e -> handleSaveEmailSettings());

        // Load dữ liệu cũ
        String savedEmail = library.getSettingsDAO().getSetting("admin_email");
        String savedPass = library.getSettingsDAO().getSetting("app_password");
        emailField.setText(savedEmail);
        passField.setText(savedPass);

        box.getChildren().addAll(header, lblEmail, emailField, lblPass, passField, guide, btnSave);
        return box;
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

        Label lblNote = new Label("Note: Back up your data regularly to avoid loss.");
        lblNote.getStyleClass().add("label-note-italic");

        box.getChildren().addAll(header, btnBackup, lblNote);
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

    /**
     * Lưu cấu hình Email (CÓ BẢO MẬT ADMIN)
     */
    private void handleSaveEmailSettings() {
        // 1. Lấy dữ liệu và làm sạch
        String email = emailField.getText().trim();
        // Xóa khoảng trắng trong mật khẩu (đề phòng copy paste thừa)
        String pass = passField.getText().trim().replace(" ", "");

        // 2. Kiểm tra dữ liệu rỗng
        if (email.isEmpty() || pass.isEmpty()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING,
                    LanguageManager.getText("msg.error"),
                    "Please enter your full email address and application password.");
            return;
        }

        // --- 3. YÊU CẦU MẬT KHẨU ADMIN ---
        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle(LanguageManager.getText("title.admin_required"));
        passDialog.setHeaderText(LanguageManager.getText("header.save_email")); // "Xác nhận thay đổi..."
        passDialog.setContentText(LanguageManager.getText("content.enter_admin_pass"));
        passDialog.getDialogPane().setGraphic(new javafx.scene.control.Label("🔒"));

        java.util.Optional<String> result = passDialog.showAndWait();

        if (result.isPresent()) {
            String inputPass = result.get();

            // Kiểm tra mật khẩu (Mặc định là "admin")
            if (ADMIN_PASSWORD.equals(inputPass)) {

                // 4. MẬT KHẨU ĐÚNG -> TIẾN HÀNH LƯU VÀO DB
                boolean save1 = library.getSettingsDAO().saveSetting("admin_email", email);
                boolean save2 = library.getSettingsDAO().saveSetting("app_password", pass);

                if (save1 && save2) {
                    FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                            LanguageManager.getText("msg.success"),
                            "Email configuration saved successfully.");
                } else {
                    FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR,
                            LanguageManager.getText("msg.error"),
                            "Error saving Database.");
                }

            } else {
                // 5. MẬT KHẨU SAI -> BÁO LỖI
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR,
                        LanguageManager.getText("msg.error"),
                        LanguageManager.getText("msg.wrong_pass"));
            }
        }
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
                    "The barcode configuration has been updated..");
        } else {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Error saving Database.");
        }
    }
    private void handleSaveFinancialRules() {
        String days = maxDaysField.getText().trim();
        String fine = fineAmountField.getText().trim();
        String curr = currencyField.getText().trim();

        if (days.isEmpty() || fine.isEmpty() || curr.isEmpty()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), "Please enter all the required information.");
            return;
        }

        // Hộp thoại mật khẩu
        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle(LanguageManager.getText("title.admin_required"));
        passDialog.setHeaderText(LanguageManager.getText("header.save_rules"));
        passDialog.setContentText(LanguageManager.getText("content.enter_admin_pass"));
        passDialog.getDialogPane().setGraphic(new Label("🔒"));

        java.util.Optional<String> result = passDialog.showAndWait();
        if (result.isPresent() && ADMIN_PASSWORD.equals(result.get())) {
            // Lưu DB
            library.getSettingsDAO().saveSetting("max_borrow_days", days);
            library.getSettingsDAO().saveSetting("fine_per_day", fine);
            library.getSettingsDAO().saveSetting("currency_unit", curr);

            FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                    LanguageManager.getText("msg.success"),
                    "Financial regulations have been successfully saved!");
        } else {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR,
                    LanguageManager.getText("msg.error"),
                    LanguageManager.getText("msg.wrong_pass"));
        }
    }


    // ---  XỬ LÝ SỰ KIỆN BACKUP ---

    private void handleBackup() {
        // 1. Sử dụng DirectoryChooser để chọn THƯ MỤC lưu trữ
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle(LanguageManager.getText("title.save_backup")); // "Chọn thư mục sao lưu"

        // Lấy cửa sổ hiện tại để hiện dialog
        java.io.File selectedDirectory = directoryChooser.showDialog(this.getScene().getWindow());

        if (selectedDirectory != null) {
            try {
                // 2. Gọi BackupService mới (Truyền Folder và Library)
                // Lưu ý: BackupService trả về "true" hoặc chuỗi lỗi
                String result = BackEnd.Utils.BackupService.backupDataToCSV(selectedDirectory, library);

                if ("true".equals(result)) {
                    FrontEnd.LibraryApp.showAlert(
                            javafx.scene.control.Alert.AlertType.INFORMATION,
                            LanguageManager.getText("msg.success"),
                            LanguageManager.getText("msg.backup_success") + "\n📂 " + selectedDirectory.getAbsolutePath()
                    );
                } else {
                    // Trường hợp có lỗi (trả về chuỗi ERROR...)
                    FrontEnd.LibraryApp.showAlert(
                            javafx.scene.control.Alert.AlertType.WARNING,
                            "Kết quả chi tiết", // Tiêu đề
                            result.replace("ERROR:\n", "") // Nội dung lỗi
                    );
                }
            } catch (Exception ex) {
                FrontEnd.LibraryApp.showAlert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        LanguageManager.getText("msg.error"),
                        "Lỗi hệ thống: " + ex.getMessage()
                );
                ex.printStackTrace();
            }
        }
    }
    // --- PHẦN 3: TẠO GIAO DIỆN BÁO CÁO/GÓP Ý ---
    private VBox createSupportSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        Label header = new Label(LanguageManager.getText("header.support"));
        header.getStyleClass().add("section-title");

        Button btnSendReport = new Button("📧 " + LanguageManager.getText("btn.send_report"));
        btnSendReport.getStyleClass().addAll("action-btn", "btn-gray"); // Màu xám cho tính năng phụ
        btnSendReport.setOnAction(e -> handleSendReport());

        Label lblDesc = new Label("Use this feature to send feedback or bug reports directly to the support team.");
        lblDesc.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        box.getChildren().addAll(header, lblDesc, btnSendReport);
        return box;
    }

    // --- XỬ LÝ LOGIC GỬI BÁO CÁO ---
    private void handleSendReport() {
        // 1. Kiểm tra cấu hình Email bắt buộc
        String senderEmail = library.getSettingsDAO().getSetting("admin_email");
        String appPassword = library.getSettingsDAO().getSetting("app_password");

        if (senderEmail.isEmpty() || appPassword.isEmpty() || ! BackEnd.Utils.EmailService.isValidEmail(senderEmail)) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR,
                    LanguageManager.getText("msg.error"),
                    "Please configure your email and password for the application before submitting your report.");
            return;
        }

        // 2. Tạo Hộp thoại tùy chỉnh cho Báo cáo
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.getText("title.send_report"));
        dialog.setHeaderText(LanguageManager.getText("title.send_report"));

        // Cấu trúc UI trong Dialog
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField recipientField = new TextField(DEVELOPER_SUPPORT_EMAIL);
        recipientField.setEditable(false); // Developer email là cố định

        TextField subjectField = new TextField("REPORT BUGS / PROVIDE FEEDBACK FROM THE APP");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Provide a detailed description of the problem, error, or your suggestion...");
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(200);

        grid.add(new Label(LanguageManager.getText("label.recipient") + ":"), 0, 0);
        grid.add(recipientField, 1, 0);
        grid.add(new Label(LanguageManager.getText("label.subject_report") + ":"), 0, 1);
        grid.add(subjectField, 1, 1);
        grid.add(new Label(LanguageManager.getText("label.message") + ":"), 0, 2);
        grid.add(messageArea, 1, 2);
        GridPane.setHgrow(messageArea, Priority.ALWAYS);
        GridPane.setHgrow(subjectField, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);

        // Thêm nút OK và Cancel
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 3. Xử lý kết quả khi bấm OK
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                // Chạy tác vụ gửi mail trong Thread nền
                Task<Boolean> sendTask = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        String subject = subjectField.getText().trim();
                        String body = messageArea.getText().trim() +
                                "\n\n--- Infomation of Sender ---\nEmail: " + senderEmail + "\n";

                        // Gọi EmailService để gửi
                        return BackEnd.Utils.EmailService.sendEmail(
                                DEVELOPER_SUPPORT_EMAIL,
                                subject,
                                body,
                                senderEmail,
                                appPassword
                        );
                    }
                };

                sendTask.setOnRunning(e -> {
                    // Hiện Progress Indicator nếu cần
                });

                sendTask.setOnSucceeded(e -> {
                    if (sendTask.getValue()) {
                        FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                                LanguageManager.getText("msg.success"),
                                LanguageManager.getText("msg.report_sent"));
                    } else {
                        FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR,
                                LanguageManager.getText("msg.error"),
                                LanguageManager.getText("msg.report_fail"));
                    }
                });

                sendTask.setOnFailed(e -> {
                    FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR,
                            LanguageManager.getText("msg.error"),
                            LanguageManager.getText("msg.report_fail") + "\nChi tiết: " + sendTask.getException().getMessage());
                });

                new Thread(sendTask).start();
            }
            return null;
        });

        dialog.showAndWait();
    }

}