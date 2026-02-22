package FrontEnd.Controllers;

import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.SettingsTabView;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import static FrontEnd.LibraryApp.ADMIN_PASSWORD;

/**
 * CONTROLLER: Quản lý logic Cài đặt, Lưu DB, Đổi giao diện/ngôn ngữ, và kết nối Backup/Email Service.
 */
public class SettingsTabController {

    private final Library library;
    private final SettingsTabView view;
    private final Scene scene;
    private final Runnable onLanguageChange;
    private static final String DEVELOPER_SUPPORT_EMAIL = "hieuvan2206@gmail.com";

    public SettingsTabController(Library library, SettingsTabView view, Scene scene, Runnable onLanguageChange, boolean isDarkMode) {
        this.library = library;
        this.view = view;
        this.scene = scene;
        this.onLanguageChange = onLanguageChange;

        initializeData(isDarkMode);
        attachEvents();
    }

    private void initializeData(boolean isDarkMode) {
        // Cập nhật text nút Theme & Ngôn ngữ
        updateThemeButtonText(isDarkMode);
        updateLangButtonText();

        // Load Finance
        String maxD = library.getSettingsDAO().getSetting("max_borrow_days");
        String fineA = library.getSettingsDAO().getSetting("fine_per_day");
        String curr = library.getSettingsDAO().getSetting("currency_unit");
        view.getMaxDaysField().setText(maxD != null && !maxD.isEmpty() ? maxD : "60");
        view.getFineAmountField().setText(fineA != null && !fineA.isEmpty() ? fineA : "2000");
        view.getCurrencyField().setText(curr != null && !curr.isEmpty() ? curr : "VNĐ");

        // Load Automation
        view.getAutoAddCheckBox().setSelected("true".equals(library.getSettingsDAO().getSetting("auto_add_enabled")));

        // Load Email
        view.getEmailField().setText(library.getSettingsDAO().getSetting("admin_email"));
        view.getPassField().setText(library.getSettingsDAO().getSetting("app_password"));

        // Load License & Device
        view.getLicenseField().setText(library.getSettingsDAO().getSetting("license_key"));
        view.getLblCurrentLevel().setText("Current level: " + BackEnd.Utils.LicenseManager.getLevelName());

        String currentMac = BackEnd.Utils.HardwareID.getMacAddress();
        String storedMac = library.getSettingsDAO().getSetting("hardware_id");
        view.getLblCurrentMac().setText("Current Device ID: " + currentMac);
        view.getLblStoredMac().setText("Regidtered Device ID: " + (storedMac != null ? storedMac : "Chưa khóa"));
    }

    private void attachEvents() {
        view.getBtnTheme().setOnAction(e -> handleToggleTheme());
        view.getBtnLang().setOnAction(e -> handleSwitchLanguage());
        view.getBtnSaveFinance().setOnAction(e -> handleSaveFinancialRules());
        view.getBtnSaveAuto().setOnAction(e -> handleSaveAutoSettings());

        view.getBtnSaveEmail().setOnAction(e -> {
            if (!BackEnd.Utils.LicenseManager.isProFeature()) {
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, "Tính năng PRO", "Gửi email chỉ dành cho bản PRO.");
                return;
            }
            handleSaveEmailSettings();
        });

        view.getBtnBackup().setOnAction(e -> {
            if (!BackEnd.Utils.LicenseManager.isProFeature()) {
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, "Tính năng PRO", "Sao lưu dữ liệu chỉ dành cho bản PRO.");
                return;
            }
            handleBackup();
        });

        view.getBtnSendReport().setOnAction(e -> handleSendReport());
        view.getBtnActivate().setOnAction(e -> handleActivateLicense());
    }

    // ========================================================
    // LOGIC XỬ LÝ (GIỮ NGUYÊN TỪ CŨ)
    // ========================================================

    private void handleToggleTheme() {
        boolean isCurrentlyDark = scene.getRoot().getStyleClass().contains("dark-mode");
        if (isCurrentlyDark) {
            scene.getRoot().getStyleClass().remove("dark-mode");
            updateThemeButtonText(false);
        } else {
            scene.getRoot().getStyleClass().add("dark-mode");
            updateThemeButtonText(true);
        }
    }

    private void updateThemeButtonText(boolean isDark) {
        view.getBtnTheme().setText(LanguageManager.getText(isDark ? "btn.theme_light" : "btn.theme_dark"));
    }

    private void handleSwitchLanguage() {
        LanguageManager.setLanguage(LanguageManager.getCurrentLang().equals("vi") ? "en" : "vi");
        if (onLanguageChange != null) onLanguageChange.run();
    }

    private void updateLangButtonText() {
        view.getBtnLang().setText(LanguageManager.getText(LanguageManager.getCurrentLang().equals("vi") ? "btn.lang_en" : "btn.lang_vi"));
    }

    private void handleSaveFinancialRules() {
        String days = view.getMaxDaysField().getText().trim();
        String fine = view.getFineAmountField().getText().trim();
        String curr = view.getCurrencyField().getText().trim();

        if (days.isEmpty() || fine.isEmpty() || curr.isEmpty()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), "Please enter all the required information.");
            return;
        }

        if (promptAdminPassword(LanguageManager.getText("header.save_rules"))) {
            library.getSettingsDAO().saveSetting("max_borrow_days", days);
            library.getSettingsDAO().saveSetting("fine_per_day", fine);
            library.getSettingsDAO().saveSetting("currency_unit", curr);
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), "Financial regulations have been successfully saved!");
        }
    }

    private void handleSaveAutoSettings() {
        boolean success = library.getSettingsDAO().saveSetting("auto_add_enabled", String.valueOf(view.getAutoAddCheckBox().isSelected()));
        if (success) FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), "The barcode configuration has been updated..");
        else FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Error saving Database.");
    }

    private void handleSaveEmailSettings() {
        String email = view.getEmailField().getText().trim();
        String pass = view.getPassField().getText().trim().replace(" ", "");

        if (email.isEmpty() || pass.isEmpty()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), "Please enter your full email address and application password.");
            return;
        }

        if (promptAdminPassword(LanguageManager.getText("header.save_email"))) {
            boolean s1 = library.getSettingsDAO().saveSetting("admin_email", email);
            boolean s2 = library.getSettingsDAO().saveSetting("app_password", pass);
            if (s1 && s2) FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), "Email configuration saved successfully.");
            else FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Error saving Database.");
        }
    }

    private void handleBackup() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle(LanguageManager.getText("title.save_backup"));
        java.io.File dir = chooser.showDialog(view.getScene().getWindow());

        if (dir != null) {
            try {
                String result = BackEnd.Utils.BackupService.backupDataToCSV(dir, library);
                if ("true".equals(result)) {
                    FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.backup_success") + "\n📂 " + dir.getAbsolutePath());
                } else {
                    FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, "Kết quả chi tiết", result.replace("ERROR:\n", ""));
                }
            } catch (Exception ex) {
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Lỗi hệ thống: " + ex.getMessage());
            }
        }
    }

    private void handleSendReport() {
        if (!BackEnd.Utils.LicenseManager.canSendEmail()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, "Tính năng cao cấp", "Tính năng Gửi Email chỉ dành cho bản PLUS và PRO.\nVui lòng nâng cấp.");
            return;
        }

        String senderEmail = library.getSettingsDAO().getSetting("admin_email");
        String appPassword = library.getSettingsDAO().getSetting("app_password");

        if (senderEmail.isEmpty() || appPassword.isEmpty() || !BackEnd.Utils.EmailService.isValidEmail(senderEmail)) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Please configure your email and password for the application before submitting your report.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.getText("title.send_report"));
        dialog.setHeaderText(LanguageManager.getText("title.send_report"));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));

        TextField recipientField = new TextField(DEVELOPER_SUPPORT_EMAIL); recipientField.setEditable(false);
        TextField subjectField = new TextField("REPORT BUGS / PROVIDE FEEDBACK FROM THE APP");
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Provide a detailed description of the problem...");
        messageArea.setWrapText(true); messageArea.setPrefHeight(200);

        grid.add(new Label(LanguageManager.getText("label.recipient") + ":"), 0, 0); grid.add(recipientField, 1, 0);
        grid.add(new Label(LanguageManager.getText("label.subject_report") + ":"), 0, 1); grid.add(subjectField, 1, 1);
        grid.add(new Label(LanguageManager.getText("label.message") + ":"), 0, 2); grid.add(messageArea, 1, 2);
        GridPane.setHgrow(messageArea, Priority.ALWAYS); GridPane.setHgrow(subjectField, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return BackEnd.Utils.EmailService.sendEmail(DEVELOPER_SUPPORT_EMAIL, subjectField.getText().trim(),
                                messageArea.getText().trim() + "\n\n--- Infomation of Sender ---\nEmail: " + senderEmail + "\n",
                                senderEmail, appPassword);
                    }
                };
                task.setOnSucceeded(e -> FrontEnd.LibraryApp.showAlert(task.getValue() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                        LanguageManager.getText(task.getValue() ? "msg.success" : "msg.error"),
                        LanguageManager.getText(task.getValue() ? "msg.report_sent" : "msg.report_fail")));
                task.setOnFailed(e -> FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.report_fail") + "\nChi tiết: " + task.getException().getMessage()));
                new Thread(task).start();
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void handleActivateLicense() {
        String key = view.getLicenseField().getText().trim();
        library.getSettingsDAO().saveSetting("license_key", key);
        BackEnd.Utils.LicenseManager.init(library);
        view.getLblCurrentLevel().setText("Current Level: " + BackEnd.Utils.LicenseManager.getLevelName());
        FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Success!", "Updated Level: " + BackEnd.Utils.LicenseManager.getLevelName() + "\nVui lòng khởi động lại ứng dụng để áp dụng đầy đủ thay đổi.");
    }

    // Tiện ích lấy Admin Password chung
    private boolean promptAdminPassword(String headerText) {
        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle(LanguageManager.getText("title.admin_required"));
        passDialog.setHeaderText(headerText);
        passDialog.setContentText(LanguageManager.getText("content.enter_admin_pass"));
        passDialog.getDialogPane().setGraphic(new Label("🔒"));

        java.util.Optional<String> result = passDialog.showAndWait();
        if (result.isPresent() && ADMIN_PASSWORD.equals(result.get())) return true;

        FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.wrong_pass"));
        return false;
    }
}