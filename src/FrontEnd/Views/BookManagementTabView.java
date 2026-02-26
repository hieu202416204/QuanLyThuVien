package FrontEnd.Views;

import BackEnd.Book.Book;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * VIEW: Chỉ đảm nhận việc vẽ giao diện quản lý sách.
 */
public class BookManagementTabView extends VBox {

    // --- Table & Pagination ---
    private TableView<Book> bookTable;
    private Pagination pagination;

    // --- Input Fields ---
    private TextField idField, nameField, authorField, yearField, isbnField;
    private ComboBox<String> categoryBox;
    private Label imagePathLabel;
    private Button inspectBtn; // Nút Kiểm kê
    private TableColumn<Book, Book> colCondition; // Cột tình trạng

    // --- Buttons ---
    private Button autoFillBtn, selectImageBtn;
    private Button addBtn, editBtn, deleteBtn, resetBtn, importBtn, viewHistoryBtn, printBarcodeBtn;

    public BookManagementTabView() {
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // Khởi tạo các thành phần
        bookTable = new TableView<>();
        pagination = new Pagination();

        // 1. Setup Input Panel
        VBox inputSection = createInputSection();

        // 2. Setup Action Bar
        HBox actionBar = createActionBar();

        // 3. Setup Table Columns
        setupTableColumns();

        // 4. Thêm vào Layout
        this.getChildren().addAll(inputSection, actionBar, bookTable, pagination);
        VBox.setVgrow(bookTable, Priority.ALWAYS);
    }

    private VBox createInputSection() {
        idField = createStyledTextField("col.id");
        nameField = createStyledTextField("col.name");
        authorField = createStyledTextField("col.author");
        yearField = createStyledTextField("col.year");

        // ISBN Group
        isbnField = createStyledTextField("field.scan_isbn");
        isbnField.setPrefWidth(120);
        autoFillBtn = new Button("🔍");
        autoFillBtn.setTooltip(new Tooltip(LanguageManager.getText("btn.autofill")));
        HBox isbnGroup = new HBox(5, new Label(LanguageManager.getText("label.isbn")), isbnField, autoFillBtn);
        isbnGroup.setAlignment(Pos.CENTER_LEFT);

        // Category
        categoryBox = new ComboBox<>();
        categoryBox.setEditable(true);
        categoryBox.setPromptText(LanguageManager.getText("field.category"));
        categoryBox.setPrefWidth(150);

        // Layout các Input
        FlowPane inputPane = new FlowPane(15, 10);
        inputPane.getStyleClass().add("input-panel");
        inputPane.setPadding(new Insets(10));
        inputPane.getChildren().addAll(
                isbnGroup,
                createInputGroup(LanguageManager.getText("col.id"), idField, 100),
                createInputGroup(LanguageManager.getText("col.name"), nameField, 200),
                createInputGroup(LanguageManager.getText("col.author"), authorField, 150),
                createInputGroup(LanguageManager.getText("label.category"), categoryBox, 150),
                createInputGroup(LanguageManager.getText("col.year"), yearField, 80)
        );

        // Image Section
        selectImageBtn = new Button(LanguageManager.getText("btn.select_img"));
        imagePathLabel = new Label(LanguageManager.getText("label.no_cover"));
        imagePathLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");

        HBox imageBox = new HBox(10, selectImageBtn, imagePathLabel, new Label(LanguageManager.getText("msg.scan_hint")));
        imageBox.setAlignment(Pos.CENTER_LEFT);
        imageBox.setPadding(new Insets(0, 0, 0, 10));

        return new VBox(10, inputPane, imageBox);
    }

    private HBox createActionBar() {
        addBtn = new Button(LanguageManager.getText("btn.add"));
        addBtn.getStyleClass().addAll("action-btn", "btn-green");

        editBtn = new Button(LanguageManager.getText("btn.edit"));
        editBtn.getStyleClass().addAll("action-btn", "btn-blue");
        editBtn.setDisable(true);

        deleteBtn = new Button(LanguageManager.getText("btn.delete"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setDisable(true);

        resetBtn = new Button("🔄 " + LanguageManager.getText("btn.reset"));

        importBtn = new Button("Import Excel");
        importBtn.getStyleClass().addAll("action-btn", "btn-green");

        viewHistoryBtn = new Button(LanguageManager.getText("btn.history_book"));
        viewHistoryBtn.setDisable(true);

        printBarcodeBtn = new Button("🖨️ " + LanguageManager.getText("btn.print_barcode"));
        printBarcodeBtn.setDisable(true);

        inspectBtn = new Button(LanguageManager.getText("status.kiemke"));
        inspectBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold;");

        // Thêm inspectBtn vào HBox bar
        HBox box = new HBox(10, resetBtn, addBtn, editBtn, deleteBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                importBtn, inspectBtn, viewHistoryBtn, printBarcodeBtn); // <--- Đã chèn inspectBtn vào đây
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 0, 5, 0));
        return box;
    }

    private void setupTableColumns() {
        TableColumn<Book, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(80);

        TableColumn<Book, String> colName = new TableColumn<>(LanguageManager.getText("col.name"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<Book, String> colAuthor = new TableColumn<>(LanguageManager.getText("col.author"));
        colAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        colAuthor.setPrefWidth(150);

        TableColumn<Book, String> colCategory = new TableColumn<>(LanguageManager.getText("label.category"));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        colCategory.setPrefWidth(100);

        TableColumn<Book, String> colStatus = new TableColumn<>(LanguageManager.getText("col.status"));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isStatus() ? LanguageManager.getText("status.available") : LanguageManager.getText("status.borrowed")
        ));
        colCondition = new TableColumn<>(LanguageManager.getText("status.tinhtrang"));
        colCondition.setPrefWidth(180);

        bookTable.getColumns().addAll(colId, colName, colAuthor, colCategory, colStatus, colCondition);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private TextField createStyledTextField(String key) {
        TextField tf = new TextField();
        tf.setPromptText(LanguageManager.getText(key));
        tf.getStyleClass().add("modern-textfield");
        return tf;
    }

    private HBox createInputGroup(String labelText, Node field, double width) {
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");
        if (field instanceof Region) ((Region) field).setPrefWidth(width);
        HBox box = new HBox(5, label, field);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ==========================================
    // GETTERS CHO CONTROLLER
    // ==========================================
    public TableView<Book> getBookTable() { return bookTable; }
    public Pagination getPagination() { return pagination; }
    public TextField getIdField() { return idField; }
    public TextField getNameField() { return nameField; }
    public TextField getAuthorField() { return authorField; }
    public TextField getYearField() { return yearField; }
    public TextField getIsbnField() { return isbnField; }
    public ComboBox<String> getCategoryBox() { return categoryBox; }
    public Label getImagePathLabel() { return imagePathLabel; }

    public Button getAutoFillBtn() { return autoFillBtn; }
    public Button getSelectImageBtn() { return selectImageBtn; }
    public Button getAddBtn() { return addBtn; }
    public Button getEditBtn() { return editBtn; }
    public Button getDeleteBtn() { return deleteBtn; }
    public Button getResetBtn() { return resetBtn; }
    public Button getImportBtn() { return importBtn; }
    public Button getViewHistoryBtn() { return viewHistoryBtn; }
    public Button getPrintBarcodeBtn() { return printBarcodeBtn; }
    public Button getInspectBtn() { return inspectBtn; }
    public TableColumn<Book, Book> getColCondition() { return colCondition; }
}