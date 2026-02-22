package FrontEnd.Views;

import BackEnd.Histories.UserInUserHistory;
import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import java.time.format.DateTimeFormatter;

// 1. Kế thừa đúng VBox như file gốc
public class HistoryTabView extends VBox {

    // Khai báo các thành phần UI để Controller gọi được
    private TableView<UserInUserHistory> historyTable;
    private Pagination historyPagination;
    private Button refreshBtn;

    public HistoryTabView() {
        initUI(); // Gọi lại đúng hàm vẽ UI
    }

    private void initUI() {
        // 2. Giữ nguyên toàn bộ CSS, Padding, Spacing
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // Tạo bảng (Giữ nguyên mã cũ của bạn)
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

        refreshBtn = new Button(LanguageManager.getText("btn.refresh"));

        // 3. Giữ nguyên thứ tự add vào VBox
        this.getChildren().addAll(refreshBtn, historyTable, historyPagination);
    }

    // --- Cung cấp Getter cho Controller ---
    public TableView<UserInUserHistory> getHistoryTable() { return historyTable; }
    public Pagination getHistoryPagination() { return historyPagination; }
    public Button getRefreshBtn() { return refreshBtn; }
}