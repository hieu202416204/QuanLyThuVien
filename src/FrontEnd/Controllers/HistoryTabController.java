package FrontEnd.Controllers;

import BackEnd.Histories.UserInUserHistory;
import BackEnd.LibraryQ.Library;
import FrontEnd.Views.HistoryTabView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.List;

public class HistoryTabController {
    private final Library library;
    private final HistoryTabView view;

    private final ObservableList<UserInUserHistory> historyData = FXCollections.observableArrayList();
    private final int PAGE_SIZE = 300;

    public HistoryTabController(Library library, HistoryTabView view) {
        this.library = library;
        this.view = view;

        // Gắn sự kiện (Thay vì viết lambda trực tiếp trong giao diện)
        this.view.getRefreshBtn().setOnAction(e -> reloadHistoryFromDB());
        this.view.getHistoryPagination().setPageFactory(this::createPage);

        // Load dữ liệu lần đầu
        reloadHistoryFromDB();
    }

    public void reloadHistoryFromDB() {
        if (library == null) return;
        List<UserInUserHistory> list = library.getTransactionDAO().getAllTransactionsHistory();
        historyData.setAll(list);

        int pageCount = (int) Math.ceil((double) list.size() / PAGE_SIZE);
        view.getHistoryPagination().setPageCount(pageCount > 0 ? pageCount : 1);
        view.getHistoryPagination().setCurrentPageIndex(0);
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
            view.getHistoryTable().setItems(FXCollections.observableArrayList(historyData.subList(from, to)));
        } else {
            view.getHistoryTable().getItems().clear();
        }
        view.getHistoryTable().refresh();
    }
}