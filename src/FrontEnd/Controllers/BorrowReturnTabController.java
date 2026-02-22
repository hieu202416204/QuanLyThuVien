package FrontEnd.Controllers;

import BackEnd.Book.Book;
import BackEnd.Histories.CurrentTransaction;
import BackEnd.LibraryQ.Library;
import BackEnd.LibraryQ.QuanLyMuonTra;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.BorrowReturnTabView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * CONTROLLER: Xử lý nghiệp vụ Mượn/Trả, Smart Scan, Phạt tiền.
 */
public class BorrowReturnTabController {

    private final Library library;
    private final BorrowReturnTabView view;
    private final QuanLyMuonTra quanLyMuonTra;
    private final Runnable globalRefreshCallback;

    private final ObservableList<CurrentTransaction> borrowedData = FXCollections.observableArrayList();
    private final int BORROW_PAGE_SIZE = 100;

    public BorrowReturnTabController(Library library, BorrowReturnTabView view, Runnable globalRefreshCallback) {
        this.library = library;
        this.view = view;
        this.quanLyMuonTra = new QuanLyMuonTra(library);
        this.globalRefreshCallback = globalRefreshCallback;

        attachEvents();
        setupStatusColoring();
        refreshData();
    }

    private void attachEvents() {
        // Sự kiện nút bấm
        view.getBorrowBtn().setOnAction(e -> performBorrow());
        view.getReturnBtn().setOnAction(e -> performReturn());

        // Bắt sự kiện phím Enter trên thanh mã vạch (Smart Scan)
        view.getBarcodeField().setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleSmartScan();
                event.consume();
            }
        });

        // Phân trang
        view.getBorrowedPagination().setPageFactory(this::createPage);
    }

    // Logic tô màu trạng thái quá hạn (Chuyển từ View sang Controller)
    private void setupStatusColoring() {
        view.getColStatus().setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setStyle("");
                if (empty || item == null) return;

                CurrentTransaction t = getTableView().getItems().get(getIndex());
                long days = t.getDaysElapsed();
                setText(item);

                String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
                int max = (maxDaysStr == null || maxDaysStr.isEmpty()) ? 60 : Integer.parseInt(maxDaysStr);

                if (days > max) {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    setText(item + " " + LanguageManager.getText("status.overdue"));
                } else {
                    setStyle("-fx-text-fill: green;");
                }
            }
        });
    }

    // --- LOGIC XỬ LÝ ---

    private void handleSmartScan() {
        String scannedCode = view.getBarcodeField().getText().trim();
        if (scannedCode.isEmpty()) return;

        boolean isHandled = false;

        Book b = library.findBookById(scannedCode);
        if (b != null) {
            view.getBookIdField().setText(b.getId());
            view.getStatusLabel().setText("📖 Book: " + b.getName());
            updateStatusStyle("scan-status-success");
            isHandled = true;
        } else {
            User u = library.searchUserById(scannedCode);
            if (u != null) {
                view.getUserIdField().setText(u.getId());
                view.getStatusLabel().setText("👤 User: " + u.getName());
                updateStatusStyle("scan-status-success");
                isHandled = true;
            }
        }

        if (!isHandled) {
            view.getStatusLabel().setText("❌ Unknown Code: " + scannedCode);
            updateStatusStyle("scan-status-error");
            view.getBarcodeField().selectAll();
            return;
        }

        view.getBarcodeField().clear();
        java.awt.Toolkit.getDefaultToolkit().beep();

        String uId = view.getUserIdField().getText().trim();
        String bId = view.getBookIdField().getText().trim();

        if (!uId.isEmpty() && !bId.isEmpty()) {
            handleSmartAction(uId, bId);
        } else {
            view.getBarcodeField().requestFocus();
        }
    }

    private void handleSmartAction(String uId, String bId) {
        Book book = library.findBookById(bId);
        if (book == null) return;

        if (book.isStatus()) {
            performBorrow();
        } else {
            String realBorrowerId = getActiveBorrowerId(bId);
            if (realBorrowerId != null && !realBorrowerId.equals(uId)) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Return Warning");
                alert.setHeaderText("Borrower Mismatch");
                alert.setContentText("Book borrowed by: " + realBorrowerId + "\n" +
                        "Current user: " + uId + "\n\nConfirm return for " + realBorrowerId + "?");

                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    view.getUserIdField().setText(realBorrowerId);
                    performReturn();
                } else {
                    view.getStatusLabel().setText("Return canceled.");
                    updateStatusStyle("scan-status-error");
                }
            } else {
                performReturn();
            }
        }
    }

    private void performBorrow() {
        String uId = view.getUserIdField().getText().trim();
        String bId = view.getBookIdField().getText().trim();

        if (uId.isEmpty() || bId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Missing Info");
            return;
        }

        String msg = quanLyMuonTra.choMuonSach(uId, bId);
        globalRefreshCallback.run();
        boolean success = msg.contains("Success") || msg.contains("Thành công");

        if (success) {
            view.getStatusLabel().setText("✅ Borrowed: " + bId);
            updateStatusStyle("scan-status-success");
            view.getUserIdField().clear();
            view.getBookIdField().clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Borrow Error", msg);
            view.getStatusLabel().setText(msg);
            updateStatusStyle("scan-status-error");
        }
    }

    private void performReturn() {
        String uId = view.getUserIdField().getText().trim();
        String bId = view.getBookIdField().getText().trim();

        if (uId.isEmpty() || bId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Missing Info");
            return;
        }

        long fine = quanLyMuonTra.calculateFine(uId, bId);
        if (fine > 0) {
            String fineStr = String.format("%,d", fine);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(LanguageManager.getText("msg.fine_warning"));
            confirm.setContentText(String.format(LanguageManager.getText("msg.fine_content"), fineStr));

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                view.getStatusLabel().setText("Return canceled due to fine.");
                updateStatusStyle("scan-status-error");
                return;
            }
            library.getFinancialDAO().recordFine("Overdue: " + uId + "-" + bId, fine);
        }

        String msg = quanLyMuonTra.traSach(uId, bId);
        globalRefreshCallback.run();
        boolean success = msg.contains("Success") || msg.contains("Thành công");

        if (success) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            String statusText = "✅ Returned: " + bId + (fine > 0 ? " (Fine collected)" : "");
            view.getStatusLabel().setText(statusText);
            updateStatusStyle("scan-status-success");
            view.getUserIdField().clear();
            view.getBookIdField().clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Return Error", msg);
            view.getStatusLabel().setText(msg);
            updateStatusStyle("scan-status-error");
        }
    }

    public void refreshData() {
        List<CurrentTransaction> list = library.getTransactionDAO().getCurrentlyBorrowedBooks();
        borrowedData.setAll(list);

        int pageCount = (int) Math.ceil((double) list.size() / BORROW_PAGE_SIZE);
        view.getBorrowedPagination().setPageCount(pageCount > 0 ? pageCount : 1);
        view.getBorrowedPagination().setCurrentPageIndex(0);
        updateTablePage(0);
    }

    private Node createPage(int pageIndex) {
        updateTablePage(pageIndex);
        return new VBox();
    }

    private void updateTablePage(int pageIndex) {
        int from = pageIndex * BORROW_PAGE_SIZE;
        int to = Math.min(from + BORROW_PAGE_SIZE, borrowedData.size());

        if (from > to) from = 0;

        if (from <= to && !borrowedData.isEmpty()) {
            view.getBorrowedTable().setItems(FXCollections.observableArrayList(borrowedData.subList(from, to)));
        } else {
            view.getBorrowedTable().getItems().clear();
        }
        view.getBorrowedTable().refresh();
    }

    private String getActiveBorrowerId(String bookId) {
        for (CurrentTransaction t : borrowedData) {
            if (t.getBookId().equals(bookId)) return t.getUserId();
        }
        return null;
    }

    private void updateStatusStyle(String styleClass) {
        view.getStatusLabel().getStyleClass().removeAll("scan-status-default", "scan-status-success", "scan-status-error");
        view.getStatusLabel().getStyleClass().add(styleClass);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}