package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.Histories.CurrentTransaction;
import BackEnd.LibraryQ.Library;
import BackEnd.LibraryQ.QuanLyMuonTra;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class BorrowReturnTab extends HBox {
    private final Library library;
    private final QuanLyMuonTra quanLyMuonTra;
    private final Runnable globalRefreshCallback; // Callback để báo cho LibraryApp update các tab khác

    // UI Components
    private TableView<CurrentTransaction> borrowedTable;
    private Pagination borrowedPagination;
    private final ObservableList<CurrentTransaction> borrowedData = FXCollections.observableArrayList();
    private final int BORROW_PAGE_SIZE = 100;

    // Input Fields
    private TextField userIdField;
    private TextField bookIdField;
    private TextField barcodeField;
    private Label statusLabel;

    public BorrowReturnTab(Library library, Runnable globalRefreshCallback) {
        this.library = library;
        this.quanLyMuonTra = new QuanLyMuonTra(library);
        this.globalRefreshCallback = globalRefreshCallback;

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

        // Load dữ liệu lần đầu
        refreshData();
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

        // --- SMART SCAN EVENT ---
        // Sử dụng setOnKeyPressed để bắt sự kiện sớm hơn và chặn nó lại
        barcodeField.setOnKeyPressed(event -> {
            // Chỉ xử lý khi nhấn Enter (máy quét thường gửi Enter ở cuối chuỗi)
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleSmartScan();
                event.consume();
            }
        });

        VBox scanBox = new VBox(5, new Label(LanguageManager.getText("label.scan_title")), barcodeField, statusLabel);
        scanBox.getStyleClass().add("scan-box");

        Button borrowBtn = new Button(LanguageManager.getText("btn.action_borrow"));
        borrowBtn.setMaxWidth(Double.MAX_VALUE);
        borrowBtn.setOnAction(e -> performBorrow());

        Button returnBtn = new Button(LanguageManager.getText("btn.action_return"));
        returnBtn.setMaxWidth(Double.MAX_VALUE);
        returnBtn.setOnAction(e -> performReturn());

        box.getChildren().addAll(
                title,
                scanBox,
                new Separator(),
                new Label(LanguageManager.getText("label.manual_info")),
                userIdField,
                bookIdField,
                new Separator(),
                borrowBtn,
                returnBtn
        );
        return box;
    }

    private VBox createTableSection() {
        borrowedTable = new TableView<>();

        TableColumn<CurrentTransaction, String> colUser = new TableColumn<>(LanguageManager.getText("col.user_info"));
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserId() + " - " + d.getValue().getUserName()));

        TableColumn<CurrentTransaction, String> colBook = new TableColumn<>(LanguageManager.getText("col.book_info"));
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookId() + " - " + d.getValue().getBookName()));

        TableColumn<CurrentTransaction, String> colStatus = new TableColumn<>(LanguageManager.getText("col.time_status"));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDaysElapsed() + " " + LanguageManager.getText("text.days")));

        // Logic tô màu trạng thái (Quá hạn -> Đỏ, Bình thường -> Xanh)
        colStatus.setCellFactory(column -> new TableCell<>() {
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

        borrowedTable.getColumns().addAll(colUser, colBook, colStatus);
        borrowedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(borrowedTable, Priority.ALWAYS);

        borrowedPagination = new Pagination();
        borrowedPagination.setPageFactory(this::createPage);

        return new VBox(10, borrowedTable, borrowedPagination);
    }

    // ========================================================================
    // LOGIC HANDLING
    // ========================================================================

    private void handleSmartScan() {
        String scannedCode = barcodeField.getText().trim();
        if (scannedCode.isEmpty()) return;

        boolean isHandled = false;

        // 1. Check if it is a BOOK
        Book b = library.findBookById(scannedCode);
        if (b != null) {
            bookIdField.setText(b.getId());
            statusLabel.setText("📖 Book: " + b.getName());
            updateStatusStyle("scan-status-success");
            isHandled = true;
        }
        // 2. If not, check if it is a USER
        else {
            User u = library.searchUserById(scannedCode);
            if (u != null) {
                userIdField.setText(u.getId());
                statusLabel.setText("👤 User: " + u.getName());
                updateStatusStyle("scan-status-success");
                isHandled = true;
            }
        }

        if (!isHandled) {
            statusLabel.setText("❌ Unknown Code: " + scannedCode);
            updateStatusStyle("scan-status-error");
            barcodeField.selectAll();
            return;
        }

        barcodeField.clear();
        java.awt.Toolkit.getDefaultToolkit().beep();

        // 3. TỰ ĐỘNG KÍCH HOẠT HÀNH ĐỘNG NẾU ĐỦ 2 TRƯỜNG
        String uId = userIdField.getText().trim();
        String bId = bookIdField.getText().trim();

        if (!uId.isEmpty() && !bId.isEmpty()) {
            handleSmartAction(uId, bId);
        } else {
            barcodeField.requestFocus();
        }
    }

    private void handleSmartAction(String uId, String bId) {
        Book book = library.findBookById(bId);
        if (book == null) return;

        if (book.isStatus()) {
            // Sách có sẵn -> MƯỢN
            performBorrow();
        } else {
            // Sách đang được mượn -> TRẢ
            String realBorrowerId = getActiveBorrowerId(bId);

            if (realBorrowerId != null && !realBorrowerId.equals(uId)) {
                // Sai người trả -> Cảnh báo
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Return Warning");
                alert.setHeaderText("Borrower Mismatch");
                alert.setContentText("Book borrowed by: " + realBorrowerId + "\n" +
                        "Current user: " + uId + "\n\nConfirm return for " + realBorrowerId + "?");

                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    // Trả giúp người kia
                    userIdField.setText(realBorrowerId); // Cập nhật lại field để hàm return lấy đúng ID
                    performReturn();
                } else {
                    statusLabel.setText("Return canceled.");
                    updateStatusStyle("scan-status-error");
                }
            } else {
                // Đúng người -> Trả luôn
                performReturn();
            }
        }
    }

    private void performBorrow() {
        String uId = userIdField.getText().trim();
        String bId = bookIdField.getText().trim();

        if (uId.isEmpty() || bId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Missing Info");
            return;
        }

        String msg = quanLyMuonTra.choMuonSach(uId, bId);

        // Cập nhật giao diện toàn cục (Gọi về LibraryApp)
        globalRefreshCallback.run();
        boolean success = msg.contains("Success") || msg.contains("Thành công");

        if (success) {
//            java.awt.Toolkit.getDefaultToolkit().beep();
            statusLabel.setText("✅ Borrowed: " + bId);
            updateStatusStyle("scan-status-success");
            userIdField.clear();
            bookIdField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Borrow Error", msg);
            statusLabel.setText(msg);
            updateStatusStyle("scan-status-error");
        }
    }

    private void performReturn() {
        String uId = userIdField.getText().trim();
        String bId = bookIdField.getText().trim();

        if (uId.isEmpty() || bId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Missing Info");
            return;
        }

        // 1. Tính phạt
        long fine = quanLyMuonTra.calculateFine(uId, bId);

        // 2. Xử lý nộp phạt (nếu có)
        if (fine > 0) {
            String fineStr = String.format("%,d", fine);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(LanguageManager.getText("msg.fine_warning"));
            confirm.setContentText(String.format(LanguageManager.getText("msg.fine_content"), fineStr));

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                statusLabel.setText("Return canceled due to fine.");
                updateStatusStyle("scan-status-error");
                return; // Dừng lại nếu không chịu nộp phạt
            }
            library.getFinancialDAO().recordFine("Overdue: " + uId + "-" + bId, fine);
        }

        // 3. Thực hiện trả sách (LUÔN CHẠY nếu đã qua bước phạt)
        String msg = quanLyMuonTra.traSach(uId, bId);

        // 4. Cập nhật giao diện toàn cục
        globalRefreshCallback.run();

        boolean success = msg.contains("Success") || msg.contains("Thành công");

        if (success) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            String statusText = "✅ Returned: " + bId;
            if (fine > 0) statusText += " (Fine collected)";

            statusLabel.setText(statusText);
            updateStatusStyle("scan-status-success");
            userIdField.clear();
            bookIdField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Return Error", msg);
            statusLabel.setText(msg);
            updateStatusStyle("scan-status-error");
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    // Gọi hàm này từ LibraryApp khi cần refresh
    public void refreshData() {
        List<CurrentTransaction> list = library.getTransactionDAO().getCurrentlyBorrowedBooks();
        borrowedData.setAll(list);

        // Update Pagination
        int pageCount = (int) Math.ceil((double) list.size() / BORROW_PAGE_SIZE);
        borrowedPagination.setPageCount(pageCount > 0 ? pageCount : 1);

        // Reset về trang 0 và ép vẽ lại
        borrowedPagination.setCurrentPageIndex(0);
        updateTablePage(0);
    }

    private Node createPage(int pageIndex) {
        updateTablePage(pageIndex);
        return new VBox();
    }

    private void updateTablePage(int pageIndex) {
        int from = pageIndex * BORROW_PAGE_SIZE;
        int to = Math.min(from + BORROW_PAGE_SIZE, borrowedData.size());

        if (from > to) from = 0; // Fix lỗi index

        if (from <= to && !borrowedData.isEmpty()) {
            borrowedTable.setItems(FXCollections.observableArrayList(borrowedData.subList(from, to)));
        } else {
            borrowedTable.getItems().clear();
        }
        borrowedTable.refresh();
    }

    private String getActiveBorrowerId(String bookId) {
        for (CurrentTransaction t : borrowedData) {
            if (t.getBookId().equals(bookId)) {
                return t.getUserId();
            }
        }
        return null;
    }

    private void updateStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll("scan-status-default", "scan-status-success", "scan-status-error");
        statusLabel.getStyleClass().add(styleClass);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}