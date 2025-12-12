package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class BookManagementTab extends VBox {

    private final Library library;
    // Dữ liệu gốc (Toàn bộ)
    private final ObservableList<Book> masterData;
    private final TableView<Book> bookTable = new TableView<>();
    private final BookGalleryTab galleryTab;

    // --- PHÂN TRANG ---
    private static final int ROWS_PER_PAGE = 100;
    private final Pagination pagination;

    // --- CÁC FIELD NHẬP LIỆU ---
    private final Button viewHistoryBtn = new Button();
    private final Button editBtn = new Button();
    private final Button addBtn = new Button();
    private final Button deleteBtn = new Button();
    private final Button printBarcodeBtn = new Button();
    private final Button importBtn = new Button("IMPORT EXCEL FILE");

    private final TextField idField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField authorField = new TextField();
    private final TextField yearField = new TextField();
    private final TextField isbnField = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private File selectedUploadFile = null;
    private String existingFileName = null;
    private final Label imagePathLabel = new Label();
    private VBox controlsLayout;
    private final Button resetBtn = new Button("🔄 " + LanguageManager.getText("btn.reset"));

    public BookManagementTab(Library library, ObservableList<Book> bookData, FlowPane flowPane, BookGalleryTab galleryTab) {
        this.library = library;
        this.masterData = bookData; // Dữ liệu nguồn
        this.galleryTab = galleryTab;

        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // 1. Khởi tạo Bảng
        initializeTable();

        // 2. Khởi tạo Controls
        initializeControls();

        // 3. Khởi tạo Phân trang (Pagination)
        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        // Cập nhật số trang ban đầu
        updatePaginationCount();

        initializeSelectionListener();

        // 4. Lắng nghe thay đổi dữ liệu gốc để cập nhật phân trang
        // (Ví dụ: khi thêm/xóa sách, số trang sẽ thay đổi)
        this.masterData.addListener((javafx.collections.ListChangeListener<Book>) c -> {
            updatePaginationCount();
            // Refresh lại trang hiện tại
            int currentPage = pagination.getCurrentPageIndex();
            pagination.setPageFactory(this::createPage);
            pagination.setCurrentPageIndex(currentPage);
        });

        // Layout: Controls ở trên, Bảng + Phân trang ở dưới
        this.getChildren().addAll(controlsLayout, bookTable, pagination);
    }

    private void updatePaginationCount() {
        int size = masterData.size();
        int pageCount = (size > 0) ? (int) Math.ceil((double) size / ROWS_PER_PAGE) : 1;
        pagination.setPageCount(pageCount);
    }

    /**
     * Hàm tạo nội dung cho mỗi trang
     */
    private Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, masterData.size());

        if (fromIndex > toIndex) fromIndex = toIndex;

        // Cắt list con từ list gốc
        List<Book> pageData = masterData.subList(fromIndex, toIndex);

        // Đưa vào bảng
        bookTable.setItems(FXCollections.observableArrayList(pageData));

        return new VBox(); // Trả về node rỗng vì ta set items cho bảng bên ngoài
    }

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

        // Quan trọng: Chiều cao bảng cố định để không bị nhảy layout khi chuyển trang
        bookTable.setFixedCellSize(30); // Chiều cao mỗi dòng
        bookTable.prefHeightProperty().bind(bookTable.fixedCellSizeProperty().multiply(15).add(30)); // Hiện khoảng 15 dòng
        bookTable.setMinHeight(400);
    }
    // --- 2. KHỞI TẠO NÚT BẤM & LAYOUT (ĐÃ TÁI CẤU TRÚC) ---
    private void initializeControls() {
        // --- A. CẤU HÌNH VÀ GOM NHÓM INPUT FIELDS ---

        // 1. Áp dụng CSS Class cho các ô nhập liệu
        idField.getStyleClass().add("modern-textfield");
        nameField.getStyleClass().add("modern-textfield");
        authorField.getStyleClass().add("modern-textfield");
        yearField.getStyleClass().add("modern-textfield");
        isbnField.getStyleClass().add("modern-textfield");
        // --- CẤU HÌNH COMBOBOX CHỦ ĐỀ ---
        categoryBox.setEditable(true); // QUAN TRỌNG: Cho phép nhập mới
        categoryBox.setPromptText(LanguageManager.getText("field.category"));
        categoryBox.setPrefWidth(150);
        categoryBox.getStyleClass().add("modern-textfield"); // Tận dụng style cũ

        // Tải danh sách chủ đề ban đầu
        refreshCategoryList();

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
                createInputGroup(LanguageManager.getText("label.category"), categoryBox.getEditor(), 150),
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
        // nút thêm excel file lấy dữ liệu
        importBtn.getStyleClass().addAll("action-btn", "btn-green"); // Dùng màu xanh giống nút Add
        importBtn.setOnAction(e -> handleImportBooks());
        // 3. Layout Toolbar (Action HBox)
        HBox actionToolbar = new HBox(15);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);
        actionToolbar.setPadding(new Insets(10, 0, 10, 0));
        actionToolbar.getChildren().addAll(
                resetBtn,
                addBtn, editBtn, deleteBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                importBtn,
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
            if (library.findBookById(id) != null) {
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), String.format(LanguageManager.getText("msg.id_duplicate_fmt"), id));
                return;
            }
            try {
                String cat = categoryBox.getValue();
                if (cat == null || cat.trim().isEmpty()) cat = "General"; // Mặc định

                Book newBook = new Book(
                        idField.getText().trim(),
                        nameField.getText().trim(),
                        authorField.getText().trim(),
                        yearField.getText().trim(),
                        cat.trim() // Truyền chủ đề vào
                );
                if (selectedUploadFile != null) {
                    String savedName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                    newBook.setImagePath(savedName);
                }
                library.addBook(newBook);
                updateView();
                refreshCategoryList();
                clearFieldsAndImageStatus();
                if (isSilent) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    imagePathLabel.setText(String.format(LanguageManager.getText("msg.auto_save_success"), name));
                    imagePathLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.book_added"));
                }
            } catch (Exception e) {
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), e.getMessage());
            }
        } else {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.missing_input"));
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
            String cat = categoryBox.getValue();
            if (cat != null && !cat.isEmpty()) selected.setCategory(cat);

            library.getBookDAO().updateBook(selected);
            updateView();
            refreshCategoryList();
            clearFieldsAndImageStatus();
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.update_success"));
        }
    }

    private void handleDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!selected.isStatus()) {
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.book_borrowed"));
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(LanguageManager.getText("title.confirm_delete"));
        confirmAlert.setContentText(String.format(LanguageManager.getText("msg.confirm_delete_fmt"), selected.getName()));
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean success = library.deleteBook(selected.getId());
            if (success) {
                if (selected.getImagePath() != null) BackEnd.Utils.ImageCache.remove(selected.getImagePath());
                updateView();
                clearFieldsAndImageStatus();
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.delete_success"));
            } else {
                FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.delete_error"));
            }
        }
    }
    private void handleAutoFillBook() {
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) return;

        imagePathLabel.setText(LanguageManager.getText("msg.fetching_info"));
        imagePathLabel.setStyle("-fx-text-fill: black;");

        new Thread(() -> {
            Book fetched = BackEnd.Utils.BookInfoHelper.fetchBookDetails(isbn);
            Platform.runLater(() -> {
                if (fetched != null) {
                    nameField.setText(fetched.getName());
                    authorField.setText(fetched.getAuthor());
                    yearField.setText(fetched.getYear());
                    if (fetched.getImagePath() != null) {
                        try {
                            File temp = BackEnd.Utils.BookInfoHelper.downloadCoverImage(fetched.getImagePath());
                            if (temp != null) {
                                this.selectedUploadFile = temp;
                                imagePathLabel.setText(LanguageManager.getText("msg.img_downloaded"));
                            }
                        } catch(Exception e) {}
                    }
                    if (idField.getText().isEmpty()) idField.setText(generateNextBookId());
                    String autoAdd = library.getSettingsDAO().getSetting("auto_add_enabled");
                    if ("true".equals(autoAdd)) handleAddBook(true);
                    else idField.requestFocus();
                } else {
                    imagePathLabel.setText(LanguageManager.getText("msg.fill_not_found"));
                    imagePathLabel.setStyle("-fx-text-fill: red;");
                    FrontEnd.LibraryApp.showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.fill_not_found"));
                }
            });
        }).start();
    }

    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
        File f = fileChooser.showOpenDialog(null);
        if (f != null) {
            this.selectedUploadFile = f;
            imagePathLabel.setText(LanguageManager.getText("label.selected") + " " + f.getName());
        }
    }

    private void handlePrintBookBarcode() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("Barcode_" + selected.getId() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File dest = fc.showSaveDialog(null);
        if (dest != null) {
            FrontEnd.CardGenerator.saveBookBarcodeToPDF(selected, dest);
            FrontEnd.LibraryApp.showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.barcode_saved"));
        }
    }

    private void handleViewBookHistory() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) return;
        Alert historyAlert = new Alert(Alert.AlertType.INFORMATION);
        historyAlert.setTitle("History");
        historyAlert.setHeaderText("History: " + selectedBook.getName());
        TextArea area = new TextArea();
        area.setEditable(false);
        List<String[]> list = library.getTransactionDAO().getBookHistory(selectedBook.getId());
        StringBuilder sb = new StringBuilder();
        for(String[] s : list) sb.append(s[0]).append(" | ").append(s[1]).append(" | ").append(s[2]).append("\n");
        if(list.isEmpty()) sb.append("No transactions have been made yet.");
        area.setText(sb.toString());
        historyAlert.getDialogPane().setContent(area);
        historyAlert.showAndWait();
    }

    private void initializeSelectionListener() {
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newSelection) -> {
            if (newSelection != null) {
                idField.setText(newSelection.getId());
                nameField.setText(newSelection.getName());
                authorField.setText(newSelection.getAuthor());
                yearField.setText(newSelection.getYear());
                this.existingFileName = newSelection.getImagePath();
                this.selectedUploadFile = null;
                imagePathLabel.setText(existingFileName != null ? "Current photo: " + new File(existingFileName).getName() : LanguageManager.getText("label.no_cover"));
                idField.setEditable(false);
                categoryBox.setValue(newSelection.getCategory()); // Điền chủ đề
            } else {
                clearFieldsAndImageStatus();
            }
        });
    }

    // ========================== Hàm này dùng để thực hiện nhiệm vụ hoàn tác ===========
    private void clearFieldsAndImageStatus() {
        isbnField.clear();
        idField.clear();
        nameField.clear();
        authorField.clear();
        yearField.clear();
        selectedUploadFile = null;
        existingFileName = null;
        categoryBox.setValue(null); // Xóa chọn chủ đề
        imagePathLabel.setText(LanguageManager.getText("label.no_cover"));
        bookTable.getSelectionModel().clearSelection();
        idField.setEditable(true);
        idField.setText(generateNextBookId());
        isbnField.requestFocus();
    }

    private void updateView() {
        masterData.setAll(library.getBooks()); // Cập nhật dữ liệu gốc -> Listener sẽ tự trigger updatePaginationCount
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
            if (id.matches("^B\\d+$")) {
                try {
                    int numberPart = Integer.parseInt(id.substring(1));
                    if (numberPart > maxId) maxId = numberPart;
                } catch (NumberFormatException e) {}
            }
        }
        return String.format("B%03d", maxId + 1);
    }

    // --- PHƯƠNG THỨC XỬ LÝ NHẬP SÁCH ---
    private void handleImportBooks() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose your data excel (CSV)");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            // 1. Đọc file
            List<Book> booksFromFile = BackEnd.Utils.ImportUtil.importBooksFromCSV(file);

            if (booksFromFile.isEmpty()) {
                LibraryApp.showAlert(Alert.AlertType.WARNING, "Warrning", "The file is empty or incorrectly formatted.");
                return;
            }

            // 2. Thêm vào DB
            int successCount = 0;
            int failCount = 0;

            for (Book b : booksFromFile) {
                // Kiểm tra trùng ID trước khi thêm
                if (library.getBookDAO().getBookById(b.getId()) == null) {
                    boolean ok = library.addBook(b);
                    if (ok) successCount++;
                    else failCount++;
                } else {
                    failCount++; // Trùng ID coi như thất bại (bỏ qua)
                }
            }

            // 3. Cập nhật giao diện
            updateView();

            // 4. Thông báo kết quả
            String msg = "Import successful: " + successCount + " books.\n" +
                    "Skipped (Duplicate ID or Error): " + failCount + " books.";
            LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Data Entry Results", msg);
        }
        /// ================================================================
        //=================================================================
        //=================== Định dạng file excel===========================
       // ID,Tên Sách,Tác Giả,Năm XB
       // B001,Harry Potter,J.K.Rowling,1997
        //B002,"Kinh tế học, Tập 1",Paul A.,2010
        /// ================================================================
        //=================================================================
        //=================== Định dạng file excel===========================
    }
    // --- HÀM LÀM MỚI DANH SÁCH CHỦ ĐỀ ---
    private void refreshCategoryList() {
        // Lấy list chủ đề duy nhất từ DAO và đổ vào ComboBox
        List<String> categories = library.getBookDAO().getUniqueCategories();
        categoryBox.setItems(FXCollections.observableArrayList(categories));
    }
}