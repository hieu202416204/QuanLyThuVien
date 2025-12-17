package FrontEnd;

import BackEnd.Histories.UserInUserHistory;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoryTab extends VBox {
    private final Library library;
    private TableView<UserInUserHistory> historyTable;
    private Pagination historyPagination;
    private ObservableList<UserInUserHistory> historyData = FXCollections.observableArrayList();
    private final int PAGE_SIZE = 300;

    public HistoryTab(Library library) {
        this.library = library;
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);

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
        historyPagination.setPageFactory(this::createPage);

        Button refreshBtn = new Button(LanguageManager.getText("btn.refresh"));
        refreshBtn.setOnAction(e -> reloadHistoryFromDB());

        this.getChildren().addAll(refreshBtn, historyTable, historyPagination);

        // Load lần đầu
        reloadHistoryFromDB();
    }

    public void reloadHistoryFromDB() {
        if (library == null) return;
        List<UserInUserHistory> list = library.getTransactionDAO().getAllTransactionsHistory();
        historyData.setAll(list);

        int pageCount = (int) Math.ceil((double) list.size() / PAGE_SIZE);
        historyPagination.setPageCount(pageCount > 0 ? pageCount : 1);
        historyPagination.setCurrentPageIndex(0);
        updateTable(0);
    }

    private Node createPage(int pageIndex) {
        updateTable(pageIndex);
        return new VBox();
    }

    private void updateTable(int pageIndex) {
        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, historyData.size());
        if (from <= to) {
            historyTable.setItems(FXCollections.observableArrayList(historyData.subList(from, to)));
        } else {
            historyTable.getItems().clear();
        }
        historyTable.refresh();
    }
}