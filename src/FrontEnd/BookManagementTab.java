package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class BookManagementTab extends VBox {

    private final Library library;
    private final ObservableList<Book> bookData;
    private final TableView<Book> bookTable = new TableView<>();
    private final BookGalleryTab galleryTab;

    // --- KHAI BÁO CÁC FIELDS ---
    // Nút bấm
    private final Button viewHistoryBtn = new Button();
    private final Button editBtn = new Button();
    private final Button addBtn = new Button();
    private final Button deleteBtn = new Button();
    private final Button printBarcodeBtn = new Button();

    // Ô nhập liệu
    private final TextField idField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField authorField = new TextField();
    private final TextField yearField = new TextField();
    private final TextField isbnField = new TextField(); // Ô nhập ISBN

    // Xử lý ảnh
    private File selectedUploadFile = null;
    private String existingFileName = null;
    private final Label imagePathLabel = new Label();

    // Layout chứa các nút
    private VBox controlsLayout;
    private final Button resetBtn = new Button("🔄 Làm mới");

    public BookManagementTab(Library library, ObservableList<Book> bookData, FlowPane flowPane, BookGalleryTab galleryTab) {
        this.library = library;
        this.bookData = bookData;
        this.galleryTab = galleryTab;

        this.setPadding(new Insets(10));
        this.setSpacing(10);

        initializeTable();      // 1. Tạo bảng
        initializeControls();   // 2. Tạo nút và ô nhập (Khởi tạo controlsLayout ở đây)
        initializeSelectionListener(); // 3. Sự kiện chọn dòng

        // [QUAN TRỌNG] SỬA LỖI NULL POINTER Ở ĐÂY
        // Chỉ thêm controlsLayout (đã chứa tất cả nút) và bảng sách
        // Đảm bảo controlsLayout không null vì đã gọi initializeControls() ở trên
        this.getChildren().addAll(controlsLayout, bookTable);
    }

    // --- 1. KHỞI TẠO BẢNG ---
    private void initializeTable() {
        TableColumn<Book, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(80);

        TableColumn<Book, String> colName = new TableColumn<>(LanguageManager.getText("col.name"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<Book, String> colAuthor = new TableColumn<>(LanguageManager.getText("col.author"));
        colAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        colAuthor.setPrefWidth(150);

        TableColumn<Book, String> colYear = new TableColumn<>(LanguageManager.getText("col.year"));
        colYear.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getYear()));
        colYear.setPrefWidth(80);

        TableColumn<Book, String> colStatus = new TableColumn<>(LanguageManager.getText("col.status"));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isStatus()
                        ? LanguageManager.getText("status.available")
                        : LanguageManager.getText("status.borrowed")
        ));
        colStatus.setPrefWidth(100);

        bookTable.getColumns().addAll(colId, colName, colAuthor, colYear, colStatus);
        bookTable.setItems(bookData);
    }

    // --- 2. KHỞI TẠO NÚT BẤM & LAYOUT (ĐÃ TÁI CẤU TRÚC) ---
    private void initializeControls() {
        // --- A. CẤU HÌNH VÀ GOM NHÓM INPUT FIELDS ---

        // 1. Áp dụng CSS Class cho các ô nhập liệu (Đã định nghĩa trong bài Dark Mode)
        idField.getStyleClass().add("modern-textfield");
        nameField.getStyleClass().add("modern-textfield");
        authorField.getStyleClass().add("modern-textfield");
        yearField.getStyleClass().add("modern-textfield");
        isbnField.getStyleClass().add("modern-textfield");

        // 2. Cấu hình hành vi (Behavior)
        isbnField.setPromptText(LanguageManager.getText("field.scan_isbn"));
        isbnField.setPrefWidth(120);
        isbnField.setOnAction(e -> handleAutoFillBook()); // False: Scanner/Enter -> Dùng Setting

        Button autoFillBtn = new Button("🔍");
        autoFillBtn.getStyleClass().add("icon-button"); // Nút tròn xanh
        autoFillBtn.setTooltip(new Tooltip(LanguageManager.getText("btn.autofill")));
        autoFillBtn.setOnAction(e -> handleAutoFillBook()); // True: Click -> Luôn xem trước (Thủ công)

        // 3. Gom nhóm ISBN (Label + Field + Button)
        HBox isbnGroup = new HBox(5, new Label(LanguageManager.getText("label.isbn")), isbnField, autoFillBtn);
        isbnGroup.setAlignment(Pos.CENTER_LEFT);

        // 4. Gom nhóm Form Nhập (FlowPane)
        FlowPane inputPane = new FlowPane(10, 10);
        inputPane.getStyleClass().add("input-panel"); // Class chung cho panel nhập liệu

        // Thêm các trường nhập liệu
        inputPane.getChildren().addAll(
                isbnGroup,
                createInputGroup(LanguageManager.getText("col.id") + ":", idField, 100),
                createInputGroup(LanguageManager.getText("col.name") + ":", nameField, 200),
                createInputGroup(LanguageManager.getText("col.author") + ":", authorField, 150),
                createInputGroup(LanguageManager.getText("col.year") + ":", yearField, 80)
        );
        inputPane.setAlignment(Pos.CENTER_LEFT);
        inputPane.setPadding(new Insets(10));
        inputPane.getStyleClass().add("input-panel");
        // --- B. CẤU HÌNH IMAGE BOX ---
        Button selectImageBtn = new Button(LanguageManager.getText("btn.select_img"));
        selectImageBtn.setOnAction(e -> handleSelectImage());

        imagePathLabel.setText(LanguageManager.getText("label.no_cover"));
        imagePathLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");

        HBox imageBox = new HBox(10, selectImageBtn, imagePathLabel, new Label(LanguageManager.getText("msg.scan_hint")));
        imageBox.setAlignment(Pos.CENTER_LEFT);
        imageBox.setPadding(new Insets(5, 0, 5, 0));

        // --- C. THANH CÔNG CỤ (ACTION TOOLBAR) ---

        // 1. Cấu hình Nút
        addBtn.setText(LanguageManager.getText("btn.add"));
        addBtn.getStyleClass().addAll("action-btn", "btn-green");
        addBtn.setOnAction(e -> handleAddBook(false)); // False: Bấm nút -> Luôn là chế độ thủ công

        editBtn.setText(LanguageManager.getText("btn.edit"));
        editBtn.getStyleClass().addAll("action-btn", "btn-blue");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> handleEditBook());

        deleteBtn.setText(LanguageManager.getText("btn.delete"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> handleDeleteBook());

        viewHistoryBtn.setText(LanguageManager.getText("btn.history_book"));
        viewHistoryBtn.getStyleClass().addAll("action-btn", "btn-gray");
        viewHistoryBtn.setDisable(true);
        viewHistoryBtn.setOnAction(e -> handleViewBookHistory());

        printBarcodeBtn.setText("🖨️ " + LanguageManager.getText("btn.print_barcode"));
        printBarcodeBtn.getStyleClass().addAll("action-btn", "btn-purple");
        printBarcodeBtn.setDisable(true);
        printBarcodeBtn.setOnAction(e -> handlePrintBookBarcode());

        // 2. Nút Reset (Làm mới)
        resetBtn.setText(LanguageManager.getText("btn.reset"));
        resetBtn.getStyleClass().addAll("action-btn", "btn-blue");
        resetBtn.setTooltip(new Tooltip(LanguageManager.getText("tooltip.reset")));
        resetBtn.setOnAction(e -> clearFieldsAndImageStatus()); // Gọi hàm dọn dẹp

        // 3. Layout Toolbar (Action HBox)
        HBox actionToolbar = new HBox(15);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);
        actionToolbar.setPadding(new Insets(10, 0, 10, 0));
        actionToolbar.getChildren().addAll(
                resetBtn,
                addBtn, editBtn, deleteBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                viewHistoryBtn, printBarcodeBtn
        );

        // --- D. TỔNG HỢP (CLASS LEVEL) ---
        controlsLayout = new VBox(10); // controlsLayout là VBox đã khai báo ở class level
        controlsLayout.getChildren().addAll(inputPane, imageBox, actionToolbar);

        // Listener bật tắt nút (Giữ nguyên)
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSel = newVal != null;
            viewHistoryBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
            printBarcodeBtn.setDisable(!hasSel);
            deleteBtn.setDisable(!hasSel);
            addBtn.setDisable(hasSel);
        });
    }

    /**
     * Hàm phụ trợ: Tạo một nhóm gồm Label + TextField có Style chuẩn
     * (Cần có nếu bạn chưa định nghĩa nó ở nơi khác)
     */
    private HBox createInputGroup(String labelText, TextField field, double width) {
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");

        field.setPrefWidth(width);
        // field đã được thêm class .modern-textfield ở initializeControls()

        HBox box = new HBox(5, label, field);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // --- 3. CÁC CHỨC NĂNG CHÍNH ---

    /**
     * @param isSilent: Nếu true -> Không hiện thông báo thành công (dùng cho Auto-Add)
     */
    private void handleAddBook(boolean isSilent) {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();

        if (!id.isEmpty() && !name.isEmpty()) {
            // Kiểm tra trùng ID
            if (library.findBookById(id) != null) {
                LibraryApp.showAlert(Alert.AlertType.ERROR,
                        LanguageManager.getText("msg.error"),
                        String.format(LanguageManager.getText("msg.id_duplicate_fmt"), id));
                return;
            }

            try {
                Book newBook = new Book(id, name, authorField.getText().trim(), yearField.getText().trim());

                if (selectedUploadFile != null) {
                    String savedName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                    newBook.setImagePath(savedName);
                }

                library.addBook(newBook);
                updateView();

                // Dọn dẹp form ngay lập tức để sẵn sàng cho cuốn sau
                clearFieldsAndImageStatus();

                if (isSilent) {
                    // CHẾ ĐỘ TỰ ĐỘNG: Không hiện Alert, chỉ báo nhỏ ở góc hoặc Beep
                    java.awt.Toolkit.getDefaultToolkit().beep(); // Phát tiếng tít
                    imagePathLabel.setText(String.format(LanguageManager.getText("msg.auto_save_success"), name));
                    imagePathLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    // CHẾ ĐỘ THỦ CÔNG: Hiện thông báo chúc mừng
                    LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                            LanguageManager.getText("msg.success"),
                            LanguageManager.getText("msg.book_added"));
                }

            } catch (Exception e) {
                LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), e.getMessage());
            }
        } else {
            LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.missing_input"));
        }
    }
    private void handleEditBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!nameField.getText().isEmpty()) {
            selected.setName(nameField.getText());
            selected.setAuthor(authorField.getText());
            selected.setYear(yearField.getText());

            if (selectedUploadFile != null) {
                try {
                    String newName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                    if (selected.getImagePath() != null) BackEnd.Utils.ImageCache.remove(selected.getImagePath());
                    selected.setImagePath(newName);
                } catch (Exception e) { e.printStackTrace(); }
            }

            library.getBookDAO().updateBook(selected);
            updateView();
            clearFieldsAndImageStatus();
            LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                    LanguageManager.getText("msg.success"),
                    LanguageManager.getText("msg.update_success"));
        }
    }

    @FXML
    private void handleDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LibraryApp.showAlert(Alert.AlertType.WARNING,
                    LanguageManager.getText("msg.error"),
                    LanguageManager.getText("msg.select_delete"));
            return;
        }

        if (!selected.isStatus()) {
            LibraryApp.showAlert(Alert.AlertType.WARNING,
                    LanguageManager.getText("msg.error"),
                    LanguageManager.getText("msg.book_borrowed"));
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(LanguageManager.getText("title.confirm_delete"));
        confirmAlert.setHeaderText(null);

        // Format nội dung xác nhận với Tên sách
        String content = String.format(LanguageManager.getText("msg.confirm_delete_fmt"), selected.getName());
        confirmAlert.setContentText(content);

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean success = library.deleteBook(selected.getId());

            if (success) {
                if (selected.getImagePath() != null) {
                    BackEnd.Utils.ImageCache.remove(selected.getImagePath());
                }
                updateView();
                clearFieldsAndImageStatus();

                LibraryApp.showAlert(Alert.AlertType.INFORMATION,
                        LanguageManager.getText("msg.success"),
                        LanguageManager.getText("msg.delete_success"));
            } else {
                LibraryApp.showAlert(Alert.AlertType.ERROR,
                        LanguageManager.getText("msg.error"),
                        LanguageManager.getText("msg.delete_error"));
            }
        }
    }

    private void handleAutoFillBook() {
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) return;

        imagePathLabel.setText(LanguageManager.getText("msg.fetching_info"));
        imagePathLabel.setStyle("-fx-text-fill: black;"); // Reset màu

        new Thread(() -> {
            Book fetched = BackEnd.Utils.BookInfoHelper.fetchBookDetails(isbn);

            javafx.application.Platform.runLater(() -> {
                if (fetched != null) {
                    // 1. Điền thông tin
                    nameField.setText(fetched.getName());
                    authorField.setText(fetched.getAuthor());
                    yearField.setText(fetched.getYear());

                    // Tải ảnh tạm
                    if (fetched.getImagePath() != null) {
                        try {
                            File temp = BackEnd.Utils.BookInfoHelper.downloadCoverImage(fetched.getImagePath());
                            if (temp != null) {
                                this.selectedUploadFile = temp;
                                imagePathLabel.setText(LanguageManager.getText("msg.img_downloaded"));
                            }
                        } catch(Exception e) {}
                    }

                    // 2. Sinh ID quản lý (Bxxx) nếu chưa nhập
                    if (idField.getText().isEmpty()) {
                        String newId = generateNextBookId();
                        idField.setText(newId);
                    }

                    // 3. KIỂM TRA CHẾ ĐỘ TỰ ĐỘNG
                    String autoAdd = library.getSettingsDAO().getSetting("auto_add_enabled");

                    if ("true".equals(autoAdd)) {
                        // TỰ ĐỘNG LƯU (Chế độ im lặng)
                        handleAddBook(true);
                    } else {
                        // THỦ CÔNG: Chỉ điền thông tin, chờ người dùng sửa/bấm thêm
                        idField.requestFocus();
                        // Có thể bỏ Alert "Thành công" ở đây để đỡ phiền, chỉ cần thấy chữ hiện ra là được
                    }

                } else {
                    // [YÊU CẦU CỦA BẠN] KHÔNG TÌM THẤY -> HIỆN LỖI LÊN MÀN HÌNH
                    imagePathLabel.setText(LanguageManager.getText("msg.fill_not_found"));
                    imagePathLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

                    LibraryApp.showAlert(Alert.AlertType.ERROR,
                            LanguageManager.getText("msg.error"),
                            LanguageManager.getText("msg.fill_not_found") + "\n(ISBN: " + isbn + ")");

                    isbnField.selectAll(); // Bôi đen để quét lại mã khác
                }
            });
        }).start();
    }

    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fileChooser.showOpenDialog(null);
        if (f != null) {
            try {
                BufferedImage original = ImageIO.read(f);
                if (original == null) return;

                File finalFile = f;
                if (original.getWidth() > 600) {
                    int newHeight = (int) ((double) original.getHeight() / original.getWidth() * 600);
                    BufferedImage resized = XuLiAnh.ImageResizer.resizeImage(original, 600, newHeight);
                    File temp = File.createTempFile("cover_opt_", ".png");
                    ImageIO.write(resized, "png", temp);
                    finalFile = temp;
                    imagePathLabel.setText(LanguageManager.getText("label.img_optimized") + " " + f.getName());
                } else {
                    imagePathLabel.setText(LanguageManager.getText("label.selected") + " " + f.getName());                }
                this.selectedUploadFile = finalFile;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void handlePrintBookBarcode() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setInitialFileName("Barcode_" + selected.getId() + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File dest = fc.showSaveDialog(null);
        if (dest != null) {
            FrontEnd.CardGenerator.saveBookBarcodeToPDF(selected, dest);
            LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.barcode_saved"));
        }
    }

    private void handleViewBookHistory() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) return;

        Alert historyAlert = new Alert(Alert.AlertType.INFORMATION);
        historyAlert.setTitle("Lịch sử");
        historyAlert.setHeaderText("Lịch sử: " + selectedBook.getName());

        TextArea area = new TextArea();
        area.setEditable(false);
        List<String[]> list = library.getTransactionDAO().getBookHistory(selectedBook.getId());
        StringBuilder sb = new StringBuilder();
        for(String[] s : list) sb.append(s[0]).append(" | ").append(s[1]).append(" | ").append(s[2]).append("\n");
        if(list.isEmpty()) sb.append("Chưa có giao dịch.");
        area.setText(sb.toString());

        historyAlert.getDialogPane().setContent(area);
        historyAlert.showAndWait();
    }

    private void initializeSelectionListener() {
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                idField.setText(newSelection.getId());
                nameField.setText(newSelection.getName());
                authorField.setText(newSelection.getAuthor());
                yearField.setText(newSelection.getYear());

                this.existingFileName = newSelection.getImagePath();
                this.selectedUploadFile = null;

                imagePathLabel.setText(existingFileName != null && !existingFileName.isEmpty()
                        ? "Ảnh hiện tại: " + new File(existingFileName).getName()
                        : LanguageManager.getText("label.no_cover"));

                idField.setEditable(false);
            } else {
                clearFieldsAndImageStatus();
                idField.setEditable(true);
            }
        });
    }

    // ========================== Hàm này dùng để thực hiện nhiệm vụ hoàn tác ===========
    private void clearFieldsAndImageStatus() {
        // 1. Xóa nội dung
        isbnField.clear();
        idField.clear();
        nameField.clear();
        authorField.clear();
        yearField.clear();

        // 2. Reset biến ảnh
        selectedUploadFile = null;
        existingFileName = null;
        imagePathLabel.setText(LanguageManager.getText("label.no_cover"));

        // 3. QUAN TRỌNG: Bỏ chọn dòng trong bảng
        bookTable.getSelectionModel().clearSelection();

        // 4. Mở khóa ô ID (vì khi edit nó bị khóa)
        idField.setEditable(true);

        // 5. Focus lại
        isbnField.requestFocus(); // Focus vào ô quét ISBN để sẵn sàng làm việc tiếp
    }

    private void updateView() {
        bookData.setAll(library.getBooks());
        bookTable.refresh();
        galleryTab.updateBookGallery();
    }

    public VBox getPane() { return this; }
    /**
     * Thuật toán sinh ID tự động: B001, B002, ... B999
     */
    private String generateNextBookId() {
        List<Book> books = library.getBooks();
        int maxId = 0;

        for (Book b : books) {
            String id = b.getId();
            // Chỉ xét các ID bắt đầu bằng "B" và theo sau là số (VD: B001)
            if (id.matches("^B\\d+$")) {
                try {
                    // Cắt bỏ chữ "B", lấy phần số
                    int numberPart = Integer.parseInt(id.substring(1));
                    if (numberPart > maxId) {
                        maxId = numberPart;
                    }
                } catch (NumberFormatException e) {
                    // Bỏ qua nếu ID không đúng định dạng
                }
            }
        }

        // Sinh ID tiếp theo: B + (max + 1) được format 3 chữ số (001, 010...)
        return String.format("B%03d", maxId + 1);
    }
}