package FrontEnd.Views;

import BackEnd.Histories.UserInUserHistory;
import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import java.time.format.DateTimeFormatter;

public class HistoryTabView extends VBox {

    private TableView<UserInUserHistory> historyTable;
    private Pagination historyPagination;

    // Các nút chức năng mới
    private DatePicker datePicker;
    private Button searchBtn;
    private Button clearBtn;
    private Button refreshBtn;
    private Button exportBtn;
    private Button deleteBtn;

    public HistoryTabView() {
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // --- 1. TẠO THANH CÔNG CỤ TÌM KIẾM & CHỨC NĂNG ---
        datePicker = new DatePicker();
        datePicker.setPromptText(LanguageManager.getText("prompt.select_date"));
        datePicker.setPrefWidth(150);

        searchBtn = new Button(LanguageManager.getText("btn.lookup"));
        searchBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        clearBtn = new Button(LanguageManager.getText("btn.clear_filter"));

        refreshBtn = new Button("🔄 " + LanguageManager.getText("btn.refresh"));

        exportBtn = new Button("📥 " + LanguageManager.getText("btn.export"));
        exportBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        deleteBtn = new Button("🗑 " + LanguageManager.getText("btn.delete"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setDisable(true); // Bị khóa cho đến khi click vào dòng lịch sử

        HBox topBar = new HBox(10);
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topBar.getChildren().addAll(
                new Label(LanguageManager.getText("label.search_date")), datePicker, searchBtn, clearBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                refreshBtn, exportBtn
        );
        // --- 2. KHỞI TẠO BẢNG ---
        historyTable = new TableView<>();
        TableColumn<UserInUserHistory, String> colTime = new TableColumn<>(LanguageManager.getText("col.time"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocalDateTime() != null ? d.getValue().getLocalDateTime().format(formatter) : ""));

        TableColumn<UserInUserHistory, String> colUser = new TableColumn<>(LanguageManager.getText("col.user_name"));
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName() != null ? d.getValue().getName() : d.getValue().getId()));

        TableColumn<UserInUserHistory, String> colBook = new TableColumn<>(LanguageManager.getText("col.name"));
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookName()));

        TableColumn<UserInUserHistory, String> colAction = new TableColumn<>(LanguageManager.getText("col.action"));
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTrangThai()));

        historyTable.getColumns().setAll(colTime, colUser, colBook, colAction);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setMinHeight(500);

        historyPagination = new Pagination();

        // 3. THÊM TẤT CẢ VÀO LAYOUT
        this.getChildren().addAll(topBar, historyTable, historyPagination);
    }

    // --- Cung cấp Getter cho Controller ---
    public TableView<UserInUserHistory> getHistoryTable() { return historyTable; }
    public Pagination getHistoryPagination() { return historyPagination; }
    public DatePicker getDatePicker() { return datePicker; }
    public Button getSearchBtn() { return searchBtn; }
    public Button getClearBtn() { return clearBtn; }
    public Button getRefreshBtn() { return refreshBtn; }
    public Button getExportBtn() { return exportBtn; }
    public Button getDeleteBtn() { return deleteBtn; }
}