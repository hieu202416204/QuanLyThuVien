package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
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
    private final Button viewHistoryBtn = new Button("📜 Xem Lịch sử Sách");
    private final Button editBtn = new Button("✏️ Cập nhật sách");
    private final Button addBtn = new Button("➕ Thêm sách");
    private final Button deleteBtn = new Button("❌ Xóa sách");

    private final TextField idField = createTextField("ID");
    private final TextField nameField = createTextField("Name");
    private final TextField authorField = createTextField("Author");
    private final TextField yearField = createTextField("Year");

    // currentImagePath lưu trữ đường dẫn tuyệt đối (từ FileChooser)
    private String currentImagePath = "";
    private final Label imagePathLabel = new Label("Chưa có ảnh bìa");

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
        TableColumn<Book, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(80);

        TableColumn<Book, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<Book, String> colAuthor = new TableColumn<>("Author");
        colAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        colAuthor.setPrefWidth(150);

        TableColumn<Book, String> colYear = new TableColumn<>("Year");
        colYear.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getYear()));
        colYear.setPrefWidth(80);

        TableColumn<Book, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isStatus() ? "CÓ SẴN" : "ĐÃ MƯỢN"));
        colStatus.setPrefWidth(100);

        bookTable.getColumns().addAll(colId, colName, colAuthor, colYear, colStatus);
        bookTable.setItems(bookData);
    }

    // --- 2. INITIALIZE CONTROLS ---
    private void initializeControls() {
        addBtn.setOnAction(e -> handleAddBook(idField, nameField, authorField, yearField));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setOnAction(e -> handleDeleteBook());
        viewHistoryBtn.setDisable(true);
        viewHistoryBtn.setOnAction(e -> handleViewBookHistory()); 
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> handleEditBook()); 

        controls = new HBox(10, idField, nameField, authorField, yearField, addBtn, deleteBtn, editBtn, viewHistoryBtn);
        controls.setPadding(new Insets(10));

        Button selectImageBtn = new Button("🖼️ Chọn Ảnh Bìa");
        selectImageBtn.setOnAction(e -> handleSelectImage());

        imageControls = new HBox(10, selectImageBtn, imagePathLabel);
        imageControls.setPadding(new Insets(10, 0, 0, 0));
    }

    // --- 3. LOGIC XỬ LÝ SỰ KIỆN ---

    /**
     * Thêm sách: Ghi vào DB và cập nhật ObservableList/UI.
     */
    private void handleAddBook(TextField idField, TextField nameField, TextField authorField, TextField yearField) {
        if (!idField.getText().isEmpty() && !nameField.getText().isEmpty()) {

            Book newBook = new Book(
                    idField.getText(),
                    nameField.getText(),
                    authorField.getText(),
                    yearField.getText()
            );

            if (!currentImagePath.isEmpty()) {
                newBook.setImagePath(currentImagePath);
            }

            // 1. Ghi vào DB
            library.addBook(newBook);

            // 2. Cập nhật ObservableList và UI
            // bookData.add(newBook); // Thêm trực tiếp để cập nhật UI nhanh
            updateView(); // Tải lại toàn bộ từ DB để đồng bộ hoàn toàn

            // Reset các trường sau khi thêm
            clearFieldsAndImageStatus();

            LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sách mới đã được thêm vào thư viện.");

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
        if (selectedBook == null) {
            LibraryApp.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn sách cần cập nhật.");
            return;
        }

        if (!nameField.getText().isEmpty()) {

            // 1. Cập nhật đối tượng trong bộ nhớ (sẽ dùng để ghi vào DB)
            selectedBook.setName(nameField.getText());
            selectedBook.setAuthor(authorField.getText());
            selectedBook.setYear(yearField.getText());

            // ĐẢM BẢO CẬP NHẬT IMAGEPATH, ngay cả khi người dùng không chọn file mới.
            // currentImagePath đã được gán giá trị cũ (hoặc giá trị mới) từ listener/chooser
            selectedBook.setImagePath(currentImagePath);

            // 2. GHI VÀO DB qua BookDAO
            boolean success = library.getBookDAO().updateBook(selectedBook);

            if (success) {
                updateView(); // Tải lại từ DB và cập nhật Gallery
                clearFieldsAndImageStatus();
                LibraryApp.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thông tin sách đã được cập nhật.");
            } else {
                LibraryApp.showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", "Không thể ghi thông tin sách vào DB.");
            }

        } else {
            LibraryApp.showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", "Tên sách không được để trống.");
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
            // LƯU ĐƯỜNG DẪN TUYỆT ĐỐI (C:\...)
            currentImagePath = selectedFile.getAbsolutePath();
            imagePathLabel.setText("Đã chọn: " + selectedFile.getName());
        } else {
            // Nếu hủy chọn, reset về rỗng
            currentImagePath = "";
            imagePathLabel.setText("Chưa có ảnh bìa.");
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
        historyAlert.getDialogPane().setContent(dialogContent);
        historyAlert.showAndWait();
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
                currentImagePath = newSelection.getImagePath() != null ? newSelection.getImagePath() : "";
                imagePathLabel.setText(newSelection.getImagePath() != null && !newSelection.getImagePath().isEmpty()
                        ? "Ảnh hiện tại: " + new File(newSelection.getImagePath()).getName()
                        : "Chọn ảnh mới.");

                idField.setEditable(false);
            } else {
                // Xóa nội dung và reset trạng thái khi không chọn sách
                clearFieldsAndImageStatus();
                idField.setEditable(true);
            }
        });
    }

    /**
     * Xóa nội dung và reset đường dẫn ảnh đã chọn về rỗng.
     */
    private void clearFieldsAndImageStatus() {
        idField.clear(); nameField.clear(); authorField.clear(); yearField.clear();
        currentImagePath = ""; // Reset đường dẫn ảnh về rỗng
        imagePathLabel.setText("Chưa có ảnh bìa.");
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
