package FrontEnd.Controllers;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Sattistics.BookStatistic;
import BackEnd.Sattistics.UserStatistic;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import Database.DatabaseManager;
import FrontEnd.Views.StatisticTabView;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.util.List;

import static FrontEnd.LibraryApp.ADMIN_PASSWORD;

/**
 * CONTROLLER: Xử lý logic tải dữ liệu thống kê, xuất file CSV và Reset Database.
 */
public class StatisticTabController {

    private final Library library;
    private final StatisticTabView view;

    private final UserStatistic userStatistic;
    private final BookStatistic bookStatistic;
    private final Runnable globalRefreshCallback;

    public StatisticTabController(Library library, StatisticTabView view, Runnable globalRefreshCallback) {
        this.library = library;
        this.view = view;
        this.globalRefreshCallback = globalRefreshCallback;

        this.userStatistic = new UserStatistic(library);
        this.bookStatistic = new BookStatistic(library);

        attachEvents();
        refreshData();
    }

    private void attachEvents() {
        view.getBtnRefresh().setOnAction(e -> refreshData());
        view.getBtnExport().setOnAction(e -> handleExportReport());
        view.getBtnReset().setOnAction(e -> handleFactoryReset());
    }

    // ========================================================
    // LOGIC XỬ LÝ
    // ========================================================

    public void refreshData() {
        // Cập nhật Top Users
        List<User> allUsers = userStatistic.danhSachNguoiDung();
        int uLimit = Math.min(40, allUsers.size());
        view.getTopUserTable().setItems(FXCollections.observableArrayList(allUsers.subList(0, uLimit)));
        view.getTitleUser().setText(LanguageManager.getText("title.top_users"));

        // Cập nhật Top Books
        List<Book> allBooks = bookStatistic.getTopBook();
        int bLimit = Math.min(40, allBooks.size());
        view.getTopBookTable().setItems(FXCollections.observableArrayList(allBooks.subList(0, bLimit)));
        view.getTitleBook().setText(LanguageManager.getText("title.top_books"));

        // Cập nhật top tỉ lệ sách hu hỏng
        List<Book> allBooksRate = bookStatistic.getTopBookRate();
        int brLimit = Math.min(40, allBooksRate.size());
        view.getTopBookRateTable().setItems(FXCollections.observableArrayList(allBooksRate.subList(0, brLimit)));
        view.getTitleBookRate().setText("XẾP HẠNG SÁCH CÓ TỈ LỆ HƯ HỎNG CAO NHẤT");
    }

    private void handleExportReport() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File f = fc.showSaveDialog(view.getScene() != null ? view.getScene().getWindow() : null);

        if (f != null) {
            BackEnd.Utils.ExportUtil.exportBooksToCSV(bookStatistic.getTopBook(), f);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setContentText("Exported successfully.");
            a.show();
        }
    }

    private void handleFactoryReset() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Factory Reset");
        dlg.setContentText("Enter Admin Password to delete ALL DATA:");

        if (dlg.showAndWait().orElse("").equals(ADMIN_PASSWORD)) {
            // 1. Xóa Database
            DatabaseManager.resetDatabase();

            // 2. Xóa Cache ảnh
            BackEnd.Utils.FileUtil.clearAllImages();
            BackEnd.Utils.ImageCache.clear();

            // 3. Kích hoạt cập nhật lại toàn bộ App
            if (globalRefreshCallback != null) {
                globalRefreshCallback.run();
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setContentText("System has been reset completely.");
            a.show();
        }
    }
}