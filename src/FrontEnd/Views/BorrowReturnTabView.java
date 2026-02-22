package FrontEnd.Views;

import BackEnd.Histories.CurrentTransaction;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * VIEW: Giao diện Mượn/Trả sách. Không chứa logic xử lý mượn trả.
 */
public class BorrowReturnTabView extends HBox {

    // --- UI Components ---
    private TableView<CurrentTransaction> borrowedTable;
    private Pagination borrowedPagination;
    private TableColumn<CurrentTransaction, String> colStatus; // Cần lấy ra để Controller tô màu

    // --- Input Fields ---
    private TextField userIdField;
    private TextField bookIdField;
    private TextField barcodeField;
    private Label statusLabel;

    // --- Buttons ---
    private Button borrowBtn;
    private Button returnBtn;

    public BorrowReturnTabView() {
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // --- LEFT SIDE: INPUT FORM ---
        VBox inputSection = createInputSection();

        // --- RIGHT SIDE: TABLE ---
        VBox rightSide = createTableSection();

        this.getChildren().addAll(inputSection, rightSide);
        HBox.setHgrow(rightSide, Priority.ALWAYS);
    }

    private VBox createInputSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));
        box.setPrefWidth(350);
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 1px 0 0;");

        Label title = new Label(LanguageManager.getText("title.action_section"));
        title.getStyleClass().add("section-title");

        userIdField = new TextField();
        userIdField.setPromptText(LanguageManager.getText("field.user_id"));

        bookIdField = new TextField();
        bookIdField.setPromptText(LanguageManager.getText("field.book_id"));

        barcodeField = new TextField();
        barcodeField.setPromptText(LanguageManager.getText("field.scan_hint"));

        statusLabel = new Label(LanguageManager.getText("msg.scan_ready"));
        statusLabel.setWrapText(true);

        VBox scanBox = new VBox(5, new Label(LanguageManager.getText("label.scan_title")), barcodeField, statusLabel);
        scanBox.getStyleClass().add("scan-box");

        borrowBtn = new Button(LanguageManager.getText("btn.action_borrow"));
        borrowBtn.setMaxWidth(Double.MAX_VALUE);

        returnBtn = new Button(LanguageManager.getText("btn.action_return"));
        returnBtn.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(
                title, scanBox, new Separator(),
                new Label(LanguageManager.getText("label.manual_info")),
                userIdField, bookIdField, new Separator(),
                borrowBtn, returnBtn
        );
        return box;
    }

    private VBox createTableSection() {
        borrowedTable = new TableView<>();

        TableColumn<CurrentTransaction, String> colUser = new TableColumn<>(LanguageManager.getText("col.user_info"));
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserId() + " - " + d.getValue().getUserName()));

        TableColumn<CurrentTransaction, String> colBook = new TableColumn<>(LanguageManager.getText("col.book_info"));
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookId() + " - " + d.getValue().getBookName()));

        colStatus = new TableColumn<>(LanguageManager.getText("col.time_status"));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDaysElapsed() + " " + LanguageManager.getText("text.days")));

        borrowedTable.getColumns().addAll(colUser, colBook, colStatus);
        borrowedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(borrowedTable, Priority.ALWAYS);

        borrowedPagination = new Pagination();

        return new VBox(10, borrowedTable, borrowedPagination);
    }

    // ==========================================
    // GETTERS CHO CONTROLLER
    // ==========================================
    public TableView<CurrentTransaction> getBorrowedTable() { return borrowedTable; }
    public Pagination getBorrowedPagination() { return borrowedPagination; }
    public TableColumn<CurrentTransaction, String> getColStatus() { return colStatus; }

    public TextField getUserIdField() { return userIdField; }
    public TextField getBookIdField() { return bookIdField; }
    public TextField getBarcodeField() { return barcodeField; }
    public Label getStatusLabel() { return statusLabel; }

    public Button getBorrowBtn() { return borrowBtn; }
    public Button getReturnBtn() { return returnBtn; }
}