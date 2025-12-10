package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class BookManagementTab extends VBox {

    private final Library library;
    private final ObservableList<Book> bookData;
    private final TableView<Book> bookTable = new TableView<>();
    private final BookGalleryTab galleryTab;

    // --- KHAI BÁO CÁC FIELDS (Controls và Data) ---
    private final Button viewHistoryBtn = new Button("📜 " + LanguageManager.getText("tab.history"));
    private final Button editBtn = new Button("✏️ " + LanguageManager.getText("btn.edit"));
    private final Button addBtn = new Button("➕ " + LanguageManager.getText("btn.add"));
    private final Button deleteBtn = new Button("❌ " + LanguageManager.getText("btn.delete"));
    private final TextField idField = createTextField("ID");
    private final TextField nameField = createTextField("Name");
    private final TextField authorField = createTextField("Author");
    private final TextField yearField = createTextField("Year");

    // currentImagePath lưu trữ đường dẫn tuyệt đối (từ FileChooser)
    private String currentImagePath = "";
    private final Label imagePathLabel = new Label("Chưa có ảnh bìa");
    private File selectedUploadFile = null; // File người dùng mới chọn từ máy tính
    private String existingFileName = null; // Tên file cũ đang có trong DB (dùng cho chức năng Edit)
    // Khai báo các HBox ở cấp độ class
    private HBox controls;
    private HBox imageControls;

    public BookManagementTab(Library library, ObservableList<Book> bookData, FlowPane flowPane, BookGalleryTab galleryTab) {
        this.library = library;
        this.bookData = bookData;
        this.galleryTab = galleryTab;

        initializeTable();
        initializeControls();
        initializeSelectionListener();

        // Thêm tất cả các khối vào VBox chính
        this.getChildren().addAll(bookTable, controls, imageControls);

        this.setPadding(new Insets(10));
        this.setSpacing(10);
    }

    // --- 1. INITIALIZE TABLE ---
    private void initializeTable() {
        TableColumn<Book, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(80);

        TableColumn<Book, String> colName = new TableColumn<>(LanguageManager.getText("col.name"));        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<Book, String> colAuthor = new TableColumn<>(LanguageManager.getText("col.author"));        colAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        colAuthor.setPrefWidth(150);

        TableColumn<Book, String> colYear = new TableColumn<>(LanguageManager.getText("col.year"));        colYear.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getYear()));
        colYear.setPrefWidth(80);

        TableColumn<Book, String> colStatus = new TableColumn<>(LanguageManager.getText("col.status"));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isStatus()
                        ? LanguageManager.getText("status.available")
                        : LanguageManager.getText("status.borrowed")
        ));        colStatus.setPrefWidth(100);

        bookTable.getColumns().addAll(colId, colName, colAuthor, colYear, colStatus);
        bookTable.setItems(bookData);
    }

    // --- 2. INITIALIZE CONTROLS ---
    private void initializeControls() {
        // ngôn ngữ chuyển đổi
        addBtn.setText(LanguageManager.getText("btn.add"));
        editBtn.setText(LanguageManager.getText("btn.edit"));
        deleteBtn.setText(LanguageManager.getText("btn.delete"));
        viewHistoryBtn.setText(LanguageManager.getText("btn.history_book"));
        //===============================================================
        // Cấu hình ô ID để nhận sự kiện Enter (từ máy quét)
        idField.setPromptText(BackEnd.Utils.LanguageManager.getText("field.id"));
        idField.setOnAction(e -> handleAutoFillBook()); // <--- BẮT SỰ KIỆN ENTER TẠI ĐÂY
        // Tạo nút "Lấy thông tin" nhỏ bên cạnh ô ID (Cho trường hợp nhập tay)
        Button autoFillBtn = new Button(BackEnd.Utils.LanguageManager.getText("btn.autofill"));
        autoFillBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 5;"); // Làm nút nhỏ lại
        autoFillBtn.setOnAction(e -> handleAutoFillBook());
        // HBox chứa ô ID và nút AutoFill
        HBox idBox = new HBox(5, idField, autoFillBtn);
        idBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        //===============================================================
        nameField.setPromptText(LanguageManager.getText("field.name"));
        authorField.setPromptText(LanguageManager.getText("field.author"));
        yearField.setPromptText(LanguageManager.getText("field.year"));
        // kết thúc
        addBtn.setOnAction(e -> handleAddBook(idField, nameField, authorField, yearField));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setOnAction(e -> handleDeleteBook());
        viewHistoryBtn.setDisable(true);
        viewHistoryBtn.setOnAction(e -> handleViewBookHistory()); 
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> handleEditBook()); 

        controls = new HBox(10, idBox , nameField, authorField, yearField, addBtn, deleteBtn, editBtn, viewHistoryBtn);
        controls.setPadding(new Insets(10));
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Phan chon anh cho sach
        Button selectImageBtn = new Button(LanguageManager.getText("btn.select_img"));
        selectImageBtn.setOnAction(e -> handleSelectImage());

        Label hintLabel = new Label(BackEnd.Utils.LanguageManager.getText("msg.scan_hint"));
        hintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888; -fx-font-style: italic;");

        VBox imageBox = new VBox(5, new HBox(10, selectImageBtn, imagePathLabel), hintLabel);
        imageControls = new HBox(10, imageBox);
        imageControls.setPadding(new Insets(10, 0, 0, 0));
    }
    //=====================================================================
    // --- Thêm hàm xử lý Auto Fill ---
    private void handleAutoFillBook() {
        String isbn = idField.getText().trim();
        if (isbn.isEmpty()) return;

        // Hiển thị thông báo đang tải (Vì cần kết nối mạng)
        imagePathLabel.setText(BackEnd.Utils.LanguageManager.getText("msg.fetching_info"));

        // Chạy trên luồng phụ để không đơ giao diện
        new Thread(() -> {
            BackEnd.Book.Book fetchedBook = BackEnd.Utils.BookInfoHelper.fetchBookDetails(isbn);

            javafx.application.Platform.runLater(() -> {
                if (fetchedBook != null) {
                    // 1. Điền thông tin vào các ô
                    nameField.setText(fetchedBook.getName());
                    authorField.setText(fetchedBook.getAuthor());
                    yearField.setText(fetchedBook.getYear());

                    // 2. Xử lý ảnh bìa (Tải về và Copy vào thư mục images)
                    if (fetchedBook.getImagePath() != null) {
                        try {
                            // Tải ảnh từ URL về file tạm
                            File tempCover = BackEnd.Utils.BookInfoHelper.downloadCoverImage(fetchedBook.getImagePath());
                            if (tempCover != null) {
                                // Lưu file tạm vào biến upload để hàm AddBook xử lý sau
                                this.selectedUploadFile = tempCover;
                                imagePathLabel.setText("Đã tải ảnh bìa từ Internet.");
                            }
                        } catch (Exception ex) {
                            imagePathLabel.setText("Lỗi tải ảnh bìa.");
                        }
                    } else {
                        imagePathLabel.setText(BackEnd.Utils.LanguageManager.getText("msg.fill_success"));
                    }

                    LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Auto-Fill", BackEnd.Utils.LanguageManager.getText("msg.fill_success"));

                } else {
                    imagePathLabel.setText(BackEnd.Utils.LanguageManager.getText("msg.fill_not_found"));
                    LibraryApp.showAlert(Alert.AlertType.WARNING, "Auto-Fill", BackEnd.Utils.LanguageManager.getText("msg.fill_not_found"));
                }
            });
        }).start();
    }
    // ========================================================================

    // --- 3. LOGIC XỬ LÝ SỰ KIỆN ---

    /**
     * Thêm sách: Ghi vào DB và cập nhật ObservableList/UI.
     */
    private void handleAddBook(TextField id, TextField name, TextField author, TextField year) {
        if (!id.getText().isEmpty() && !name.getText().isEmpty()) {
            try {
                Book newBook = new Book(id.getText(), name.getText(), author.getText(), year.getText());

                // LOGIC MỚI: Nếu có chọn file ảnh, thực hiện copy
                if (selectedUploadFile != null) {
                    String savedFileName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                    newBook.setImagePath(savedFileName);
                }

                library.addBook(newBook);
                updateView();
                // Dọn dẹp sau khi thêm thành công sách( reset trống)
                clearFieldsAndImageStatus();
                LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm sách.");

            } catch (Exception e) {
                LibraryApp.showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu ảnh: " + e.getMessage());
            }
        } else {
            LibraryApp.showAlert(Alert.AlertType.ERROR, "Lỗi thêm sách", "ID và Tên sách là các trường bắt buộc.");
        }
    }

    /**
     * Xóa sách: Xóa khỏi DB và cập nhật UI.
     */
    private void handleDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean success = library.deleteBook(selected.getId()); // Gọi DAO
            if (success) {
                updateView(); // Cập nhật lại UI từ DB
                clearFieldsAndImageStatus();
                LibraryApp.showAlert(Alert.AlertType.INFORMATION,"Thành công","Sách ID " + selected.getId() + " (" + selected.getName() + ") đã được xóa.");
            } else {
                LibraryApp.showAlert(Alert.AlertType.ERROR,"Lỗi Xóa Sách","Không thể xóa sách khỏi thư viện.");
            }
        } else {
            LibraryApp.showAlert(Alert.AlertType.WARNING,"Cảnh báo","Vui lòng chọn một cuốn sách trong danh sách để xóa.");
        }
    }

    /**
     * Cập nhật sách: Cập nhật đối tượng và ghi vào DB.
     */
    private void handleEditBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) return;

        if (!nameField.getText().isEmpty()) {
            try {
                selectedBook.setName(nameField.getText());
                selectedBook.setAuthor(authorField.getText());
                selectedBook.setYear(yearField.getText());

                // LOGIC MỚI:
                // 1. Nếu người dùng chọn ảnh mới -> Copy ảnh mới, cập nhật tên mới
                if (selectedUploadFile != null) {
                    String newFileName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                    selectedBook.setImagePath(newFileName);
                }
                // 2. Nếu không chọn ảnh mới -> Giữ nguyên tên file cũ (không làm gì cả, vì object selectedBook đã có sẵn imagePath)

                library.getBookDAO().updateBook(selectedBook);
                updateView();
                clearFieldsAndImageStatus();
                LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật sách.");

            } catch (Exception e) {
                LibraryApp.showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi lưu file ảnh: " + e.getMessage());
            }
        }
    }

    /**
     * Chọn file ảnh bìa 
     */
    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn Ảnh Bìa Sách");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) this.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            this.selectedUploadFile = selectedFile; // Lưu file gốc
            imagePathLabel.setText("Đã chọn: " + selectedFile.getName());
        }
    }

    /**
     * Xem lịch sử sách: Sử dụng TransactionDAO thay vì In-memory History.
     */
    private void handleViewBookHistory() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) return;

        Alert historyAlert = new Alert(Alert.AlertType.INFORMATION);
        historyAlert.setTitle("Lịch sử Giao dịch");
        historyAlert.setHeaderText("Lịch sử Mượn/Trả của Sách ID: " + selectedBook.getId() + " - " + selectedBook.getName());
        ButtonType btnExport = new ButtonType("📤 Xuất Excel", ButtonBar.ButtonData.OTHER);
        ButtonType btnClose = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        historyAlert.getButtonTypes().setAll(btnExport, btnClose);
        TextArea historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setPrefRowCount(15);
        historyArea.setPrefColumnCount(50);

        StringBuilder sb = new StringBuilder();

        // 1. GỌI TRANSACTION DAO ĐỂ LẤY DỮ LIỆU TỪ DB
        // Dữ liệu trả về là List<String[]>, mỗi phần tử là {User ID, Ngày Mượn, Ngày Trả}
        List<String[]> bookHistory = library.getTransactionDAO().getBookHistory(selectedBook.getId());

        if (!bookHistory.isEmpty()) {
            sb.append(String.format("%-10s | %-12s | %-12s\n", "User ID", "Ngày Mượn", "Ngày Trả"));
            sb.append("---------------------------------------------------\n");

            for (String[] record : bookHistory) {
                String userId = record[0];
                String muon = record[1];
                String tra = record[2] != null ? record[2] : "ĐANG MƯỢN";

                sb.append(String.format("%-10s | %-12s | %-12s\n", userId, muon, tra));
            }
        } else {
            sb.append("Chưa có lịch sử giao dịch nào cho cuốn sách này.");
        }


        historyArea.setText(sb.toString());
        VBox dialogContent = new VBox(10, new Label("Chi tiết lịch sử:"), historyArea);
        java.util.Optional<ButtonType> result = historyAlert.showAndWait();

        if (result.isPresent() && result.get() == btnExport) {
            if (bookHistory.isEmpty()) {
                LibraryApp.showAlert(Alert.AlertType.WARNING, "Rỗng", "Không có dữ liệu.");
                return;
            }

            FileChooser fc = new FileChooser();
            fc.setTitle("Lưu Lịch sử Sách");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fc.setInitialFileName("LichSu_Sach_" + selectedBook.getId() + ".csv");
            File file = fc.showSaveDialog(null);

            if (file != null) {
                boolean ok = BackEnd.Utils.ExportUtil.exportBookSpecificHistoryToCSV(bookHistory, file);
                if (ok) LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Xong", "Đã xuất file.");
            }
        }
    }

    // --- 4. CÁC PHƯƠNG THỨC HỖ TRỢ ---

    /**
     * Lắng nghe sự kiện chọn hàng: Tải dữ liệu và ImagePath hiện tại.
     */
    private void initializeSelectionListener() {
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            viewHistoryBtn.setDisable(newSelection == null);
            editBtn.setDisable(newSelection == null);
            addBtn.setDisable(newSelection != null);

            if (newSelection != null) {
                // Đổ dữ liệu vào các trường nhập liệu khi chọn sách
                idField.setText(newSelection.getId());
                nameField.setText(newSelection.getName());
                authorField.setText(newSelection.getAuthor());
                yearField.setText(newSelection.getYear());

                // Cập nhật currentImagePath bằng đường dẫn CŨ từ DB
                this.existingFileName = newSelection.getImagePath();
                this.selectedUploadFile = null; // Reset file upload mới

                imagePathLabel.setText(existingFileName != null && !existingFileName.isEmpty()
                        ? "Ảnh hiện tại: " + existingFileName
                        : "Chưa có ảnh.");

                idField.setEditable(false);
            } else {
                // Xóa nội dung và reset trạng thái khi không chọn sách
                clearFieldsAndImageStatus();
                idField.setEditable(true);
            }
        });
    }

    /**
     * Hàm dọn dẹp form, đưa về trạng thái trắng tinh
     */
    private void clearFieldsAndImageStatus() {
        // 1. Xóa nội dung các ô Text
        idField.clear();
        nameField.clear();
        authorField.clear();
        yearField.clear();

        // 2. Reset biến lưu file ảnh (QUAN TRỌNG: Tránh lưu nhầm ảnh cũ cho sách mới)
        this.selectedUploadFile = null;
        this.existingFileName = null;

        // 3. Đặt lại nhãn hiển thị trạng thái ảnh
        // Nếu bạn chưa thêm key vào properties thì dùng chuỗi cứng: "Chưa có ảnh bìa"
        try {
            imagePathLabel.setText(BackEnd.Utils.LanguageManager.getText("label.no_cover"));
        } catch (Exception e) {
            imagePathLabel.setText("Chưa có ảnh bìa");
        }

        // 4. Đặt con trỏ chuột quay lại ô ID để sẵn sàng quét cuốn tiếp theo ngay
        idField.requestFocus();
    }

    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(150);
        return field;
    }

    /**
     * Tải lại dữ liệu từ DB và buộc cập nhật các thành phần UI.
     */
    private void updateView() {
        bookData.setAll(library.getBooks()); // Tải lại từ DB
        bookTable.refresh(); // Buộc TableView phải hiển thị lại
        galleryTab.updateBookGallery(); // Cập nhật Gallery
    }

    public VBox getPane() {
        return this;
    }
}
