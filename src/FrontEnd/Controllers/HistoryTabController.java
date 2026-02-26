package FrontEnd.Controllers;

import BackEnd.Histories.UserInUserHistory;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.HistoryTabView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryTabController {
    private final Library library;
    private final HistoryTabView view;

    // Lưu toàn bộ lịch sử gốc
    private final List<UserInUserHistory> masterHistoryList = new ArrayList<>();
    // Dữ liệu hiển thị hiện tại (Đã bị lọc qua ngày)
    private final ObservableList<UserInUserHistory> historyData = FXCollections.observableArrayList();
    private final int PAGE_SIZE = 300;

    public HistoryTabController(Library library, HistoryTabView view) {
        this.library = library;
        this.view = view;

        attachEvents();
        reloadHistoryFromDB();
    }

    private void attachEvents() {
        view.getRefreshBtn().setOnAction(e -> reloadHistoryFromDB());
        view.getSearchBtn().setOnAction(e -> handleSearchByDate());
        view.getClearBtn().setOnAction(e -> handleClearSearch());
        view.getExportBtn().setOnAction(e -> handleExportExcel());
        view.getDeleteBtn().setOnAction(e -> handleDeleteRecord());

        view.getHistoryPagination().setPageFactory(this::createPage);

        // Lắng nghe chọn dòng: Chỉ bật nút Xóa khi có 1 dòng được chọn
        view.getHistoryTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            view.getDeleteBtn().setDisable(newVal == null);
        });
    }

    // ===============================================
    // LOGIC DỮ LIỆU
    // ===============================================

    public void reloadHistoryFromDB() {
        if (library == null) return;
        masterHistoryList.clear();
        masterHistoryList.addAll(library.getTransactionDAO().getAllTransactionsHistory());

        handleClearSearch(); // Reset bộ lọc ngày
    }

    private void handleSearchByDate() {
        if (view.getDatePicker().getValue() == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, LanguageManager.getText("msg.require_date"));
            a.show();
            return;
        }

        LocalDate searchDate = view.getDatePicker().getValue();

        // Lọc trực tiếp trên RAM những dòng có Ngày trùng với Ngày tra cứu
        List<UserInUserHistory> filtered = masterHistoryList.stream()
                .filter(h -> h.getLocalDateTime() != null && h.getLocalDateTime().toLocalDate().equals(searchDate))
                .toList();

        historyData.setAll(filtered);
        updatePagination();
    }

    private void handleClearSearch() {
        view.getDatePicker().setValue(null);
        historyData.setAll(masterHistoryList);
        updatePagination();
    }

    // ===============================================
    // PHÂN TRANG VÀ RENDER
    // ===============================================

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) historyData.size() / PAGE_SIZE);
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
        if (from <= to && !historyData.isEmpty()) {
            view.getHistoryTable().setItems(FXCollections.observableArrayList(historyData.subList(from, to)));
        } else {
            view.getHistoryTable().getItems().clear();
        }
        view.getHistoryTable().refresh();
    }

    // ===============================================
    // CHỨC NĂNG MỞ RỘNG (XUẤT EXCEL & XÓA)
    // ===============================================

    private void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle(LanguageManager.getText("title.save_excel"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File", "*.csv"));
        fc.setInitialFileName("LichSuGiaoDich_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(null);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.write('\ufeff'); // Thêm BOM chuẩn UTF-8 để Excel đọc tiếng Việt không bị lỗi font
                writer.println(LanguageManager.getText("csv.header.history"));
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (UserInUserHistory h : historyData) {
                    String time = h.getLocalDateTime() != null ? h.getLocalDateTime().format(fmt) : "";
                    String user = h.getName() != null ? h.getName() : h.getId();
                    String book = h.getBookName();
                    String action = h.getTrangThai();
                    // Đóng nháy kép tránh lỗi nếu tên có chứa dấu phẩy
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\"\n", time, user, book, action);
                }
                Alert a = new Alert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.export_success"));                a.show();
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR, LanguageManager.getText("msg.export_error") + "\n" + e.getMessage());                a.show();
            }
        }
    }

    private void handleDeleteRecord() {
        UserInUserHistory selected = view.getHistoryTable().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(LanguageManager.getText("title.security"));
        dlg.setHeaderText(LanguageManager.getText("header.req_admin_delete_hist"));
        if (dlg.showAndWait().orElse("").equals(FrontEnd.LibraryApp.ADMIN_PASSWORD)) {
            // Lệnh xóa DB
            boolean success = library.getTransactionDAO().deleteHistoryRecord(selected.getId(), selected.getBookName(), selected.getLocalDateTime());
            if (success) {
                reloadHistoryFromDB(); // Tải lại bảng sau khi xóa
                Alert a = new Alert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.hist_delete_success"));
                a.show();
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR, LanguageManager.getText("msg.hist_delete_fail"));
                a.show();
            }
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR, LanguageManager.getText("msg.wrong_pass"));
            a.show();
        }
    }
}