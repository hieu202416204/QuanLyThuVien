package FrontEnd.Views;

import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * VIEW: Cấu trúc toàn bộ giao diện Cài đặt (Không chứa logic tải dữ liệu/sự kiện)
 */
public class SettingsTabView extends ScrollPane {

    // --- CÁC THÀNH PHẦN TƯƠNG TÁC (CẦN GETTER) ---
    // 1. General
    private Button btnTheme;
    private Button btnLang;

    // 2. Rules
    private TextField maxDaysField;
    private TextField fineAmountField;
    private TextField currencyField;
    private Button btnSaveFinance;
    private CheckBox autoAddCheckBox;
    private Button btnSaveAuto;

    // 3. Email
    private TextField emailField;
    private PasswordField passField;
    private Button btnSaveEmail;

    // 4. Data
    private Button btnBackup;

    // 5. Support
    private Button btnSendReport;

    // 6. License
    private TextField licenseField;
    private Button btnActivate;
    private Label lblCurrentLevel;
    private Label lblCurrentMac;
    private Label lblStoredMac;

    public SettingsTabView() {
        initUI();
    }

    private void initUI() {
        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox mainLayout = new VBox(25);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setAlignment(Pos.TOP_LEFT);

        Label pageTitle = new Label(LanguageManager.getText("tab.settings"));
        pageTitle.getStyleClass().add("page-title");

        mainLayout.getChildren().addAll(
                pageTitle,
                createGeneralSection(), new Separator(),
                createRulesSection(), new Separator(),
                createDataSection(), new Separator(),
                createEmailSection(), new Separator(),
                createSupportSection(), new Separator(),
                createLicenseSection()
        );
        this.setContent(mainLayout);
    }

    private VBox createGeneralSection() {
        VBox box = new VBox(15);
        Label header = new Label(LanguageManager.getText("header.general_settings"));
        header.getStyleClass().add("section-title");
        header.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980b9;");

        HBox themeBox = new HBox(15);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        btnTheme = new Button(); // Text sẽ được Controller set
        themeBox.getChildren().addAll(new Label(LanguageManager.getText("label.theme")), btnTheme);

        HBox langBox = new HBox(15);
        langBox.setAlignment(Pos.CENTER_LEFT);
        btnLang = new Button(); // Text sẽ được Controller set
        langBox.getChildren().addAll(new Label(LanguageManager.getText("label.language")), btnLang);

        box.getChildren().addAll(header, themeBox, langBox);
        return box;
    }

    private VBox createRulesSection() {
        VBox mainRulesContainer = new VBox(20);

        // Khối 1: Finance
        VBox financeBox = new VBox(10);
        financeBox.getStyleClass().add("input-panel");
        Label headerFinance = new Label(LanguageManager.getText("header.finance_rules"));
        headerFinance.getStyleClass().add("section-title");
        headerFinance.setStyle("-fx-text-fill: #e67e22;");

        maxDaysField = new TextField(); maxDaysField.setPrefWidth(100);
        fineAmountField = new TextField(); fineAmountField.setPrefWidth(150);
        currencyField = new TextField(); currencyField.setPrefWidth(80);

        // Cấm nhập chữ cho ngày và tiền
        maxDaysField.textProperty().addListener((obs, old, val) -> { if (!val.matches("\\d*")) maxDaysField.setText(val.replaceAll("[^\\d]", "")); });
        fineAmountField.textProperty().addListener((obs, old, val) -> { if (!val.matches("\\d*")) fineAmountField.setText(val.replaceAll("[^\\d]", "")); });

        btnSaveFinance = new Button(LanguageManager.getText("btn.save_finance"));
        btnSaveFinance.setStyle("-fx-background-color: #d35400; -fx-text-fill: white; -fx-font-weight: bold;");

        financeBox.getChildren().addAll(
                headerFinance,
                new Label(LanguageManager.getText("label.max_days")), maxDaysField,
                new Label(LanguageManager.getText("label.fine_amount")), fineAmountField,
                new Label(LanguageManager.getText("label.currency")), currencyField,
                new Separator(), btnSaveFinance
        );

        // Khối 2: Automation
        VBox automationBox = new VBox(10);
        automationBox.getStyleClass().add("input-panel");
        Label headerAuto = new Label(LanguageManager.getText("header.automation_rules"));
        headerAuto.getStyleClass().add("section-title");
        headerAuto.setStyle("-fx-text-fill: #2980b9;");

        autoAddCheckBox = new CheckBox(LanguageManager.getText("label.auto_add_book"));
        Label lblDesc = new Label(LanguageManager.getText("desc.auto_add_book"));
        lblDesc.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        btnSaveAuto = new Button(LanguageManager.getText("btn.save_auto"));
        btnSaveAuto.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");

        automationBox.getChildren().addAll(headerAuto, autoAddCheckBox, lblDesc, new Separator(), btnSaveAuto);
        mainRulesContainer.getChildren().addAll(financeBox, automationBox);
        return mainRulesContainer;
    }

    private VBox createDataSection() {
        VBox box = new VBox(15);
        Label header = new Label(LanguageManager.getText("header.data_management"));
        header.getStyleClass().add("section-header-label");

        btnBackup = new Button("💾 " + LanguageManager.getText("btn.backup"));
        btnBackup.getStyleClass().add("button-backup");

        Label lblNote = new Label("Note: Back up your data regularly to avoid loss.");
        lblNote.getStyleClass().add("label-note-italic");

        box.getChildren().addAll(header, btnBackup, lblNote);
        return box;
    }

    private VBox createEmailSection() {
        VBox box = new VBox(15);
        Label header = new Label(LanguageManager.getText("header.email_settings"));
        header.getStyleClass().add("section-title");
        header.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980b9;");

        emailField = new TextField(); emailField.setPromptText("example@gmail.com"); emailField.setMaxWidth(400);
        passField = new PasswordField(); passField.setPromptText("xxyy zzaa bbcc ddee"); passField.setMaxWidth(400);

        Label guide = new Label(LanguageManager.getText("desc.app_pass_guide"));
        guide.setStyle("-fx-font-style: italic; -fx-text-fill: #666; -fx-font-size: 11px;");

        btnSaveEmail = new Button(LanguageManager.getText("btn.save_settings"));
        btnSaveEmail.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");

        box.getChildren().addAll(header, new Label(LanguageManager.getText("label.admin_email")), emailField,
                new Label(LanguageManager.getText("label.app_password")), passField, guide, btnSaveEmail);
        return box;
    }

    private VBox createSupportSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));
        Label header = new Label(LanguageManager.getText("header.support"));
        header.getStyleClass().add("section-title");

        btnSendReport = new Button("📧 " + LanguageManager.getText("btn.send_report"));
        btnSendReport.getStyleClass().addAll("action-btn", "btn-gray");

        Label lblDesc = new Label("Use this feature to send feedback or bug reports directly to the support team.");
        lblDesc.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        box.getChildren().addAll(header, lblDesc, btnSendReport);
        return box;
    }

    private VBox createLicenseSection() {
        VBox box = new VBox(15);
        box.setStyle("-fx-background-color: #fcf3cf; -fx-border-color: #f1c40f; -fx-border-radius: 5; -fx-padding: 15;");

        Label header = new Label("Copyright Status (License)");
        header.getStyleClass().add("section-title");
        header.setStyle("-fx-text-fill: #d35400;");

        lblCurrentLevel = new Label("Current level: ...");
        lblCurrentLevel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        licenseField = new TextField(); licenseField.setPromptText("KEY-PRO-2025"); licenseField.setPrefWidth(250);
        btnActivate = new Button("Activate");
        btnActivate.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox inputGroup = new HBox(10, new Label("License Key:"), licenseField, btnActivate);
        inputGroup.setAlignment(Pos.CENTER_LEFT);

        Label lblNote = new Label("Free: MAXIMUM 100 books.\nPlus: Email + 1000 books.\nPro: Full options + Backup + UNLIMITED.");
        lblNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        lblCurrentMac = new Label("Current Device ID: ...");
        lblCurrentMac.setStyle("-fx-font-size: 11px; -fx-text-fill: #34495e;");

        lblStoredMac = new Label("Regidtered Device ID: ...");
        lblStoredMac.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");

        box.getChildren().addAll(header, lblCurrentLevel, inputGroup, lblNote, lblCurrentMac, lblStoredMac);
        return box;
    }

    // ==========================================
    // GETTERS CHO CONTROLLER
    // ==========================================
    public Button getBtnTheme() { return btnTheme; }
    public Button getBtnLang() { return btnLang; }
    public TextField getMaxDaysField() { return maxDaysField; }
    public TextField getFineAmountField() { return fineAmountField; }
    public TextField getCurrencyField() { return currencyField; }
    public Button getBtnSaveFinance() { return btnSaveFinance; }
    public CheckBox getAutoAddCheckBox() { return autoAddCheckBox; }
    public Button getBtnSaveAuto() { return btnSaveAuto; }
    public TextField getEmailField() { return emailField; }
    public PasswordField getPassField() { return passField; }
    public Button getBtnSaveEmail() { return btnSaveEmail; }
    public Button getBtnBackup() { return btnBackup; }
    public Button getBtnSendReport() { return btnSendReport; }
    public TextField getLicenseField() { return licenseField; }
    public Button getBtnActivate() { return btnActivate; }
    public Label getLblCurrentLevel() { return lblCurrentLevel; }
    public Label getLblCurrentMac() { return lblCurrentMac; }
    public Label getLblStoredMac() { return lblStoredMac; }
}