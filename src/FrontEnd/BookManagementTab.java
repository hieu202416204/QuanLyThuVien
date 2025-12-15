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
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class BookManagementTab extends VBox {

    private final Library library;
    private final BookGalleryTab galleryTab; // Để update tab Gallery khi thêm/sửa sách

    // Data
    private final ObservableList<Book> masterData = FXCollections.observableArrayList();
    private final TableView<Book> bookTable = new TableView<>();
    private Pagination pagination;
    private static final int ROWS_PER_PAGE = 100;

    // Input Fields
    private TextField idField, nameField, authorField, yearField, isbnField;
    private ComboBox<String> categoryBox;
    private Label imagePathLabel;

    // Variables
    private File selectedUploadFile = null;
    private String existingFileName = null;

    // Buttons
    private Button addBtn, editBtn, deleteBtn, viewHistoryBtn, printBarcodeBtn, resetBtn;

    public BookManagementTab(Library library, FlowPane unusedFlowPane, BookGalleryTab galleryTab) {
        this.library = library;
        this.galleryTab = galleryTab;

        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // 1. Setup Input Panel (Fields & Image)
        VBox inputSection = createInputSection();

        // 2. Setup Action Bar (Buttons)
        HBox actionBar = createActionBar();

        // 3. Setup Table & Pagination
        setupTableColumns();
        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);

        // 4. Add to Layout
        this.getChildren().addAll(inputSection, actionBar, bookTable, pagination);
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        // 5. Load Data
        refreshTable();

        // Listener
        initializeSelectionListener();

        // Auto Update Pagination when data changes
        masterData.addListener((javafx.collections.ListChangeListener<Book>) c -> updatePagination());
    }

    // ========================================================================
    // UI CREATION METHODS
    // ========================================================================

    private VBox createInputSection() {
        // --- Fields ---
        idField = createStyledTextField("col.id");
        nameField = createStyledTextField("col.name");
        authorField = createStyledTextField("col.author");
        yearField = createStyledTextField("col.year");

        // ISBN Special Handling
        isbnField = createStyledTextField("field.scan_isbn");
        isbnField.setPrefWidth(120);
        isbnField.setOnAction(e -> handleAutoFillBook());
        Button autoFillBtn = new Button("🔍");
        autoFillBtn.setTooltip(new Tooltip(LanguageManager.getText("btn.autofill")));
        autoFillBtn.setOnAction(e -> handleAutoFillBook());
        HBox isbnGroup = new HBox(5, new Label(LanguageManager.getText("label.isbn")), isbnField, autoFillBtn);
        isbnGroup.setAlignment(Pos.CENTER_LEFT);

        // Category ComboBox
        categoryBox = new ComboBox<>();
        categoryBox.setEditable(true);
        categoryBox.setPromptText(LanguageManager.getText("field.category"));
        categoryBox.setPrefWidth(150);
        refreshCategoryList();

        // --- Flow Layout for Inputs ---
        FlowPane inputPane = new FlowPane(15, 10);
        inputPane.getStyleClass().add("input-panel");
        inputPane.setPadding(new Insets(10));
        inputPane.getChildren().addAll(
                isbnGroup,
                createInputGroup(LanguageManager.getText("col.id"), idField, 100),
                createInputGroup(LanguageManager.getText("col.name"), nameField, 200),
                createInputGroup(LanguageManager.getText("col.author"), authorField, 150),
                createInputGroup(LanguageManager.getText("label.category"), categoryBox, 150),
                createInputGroup(LanguageManager.getText("col.year"), yearField, 80)
        );

        // --- Image Selection ---
        Button selectImageBtn = new Button(LanguageManager.getText("btn.select_img"));
        selectImageBtn.setOnAction(e -> handleSelectImage());
        imagePathLabel = new Label(LanguageManager.getText("label.no_cover"));
        imagePathLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");

        HBox imageBox = new HBox(10, selectImageBtn, imagePathLabel, new Label(LanguageManager.getText("msg.scan_hint")));
        imageBox.setAlignment(Pos.CENTER_LEFT);
        imageBox.setPadding(new Insets(0, 0, 0, 10));

        return new VBox(10, inputPane, imageBox);
    }

    private HBox createActionBar() {
        addBtn = new Button(LanguageManager.getText("btn.add"));
        addBtn.getStyleClass().addAll("action-btn", "btn-green");
        addBtn.setOnAction(e -> handleAddBook(false));

        editBtn = new Button(LanguageManager.getText("btn.edit"));
        editBtn.getStyleClass().addAll("action-btn", "btn-blue");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> handleEditBook());

        deleteBtn = new Button(LanguageManager.getText("btn.delete"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> handleDeleteBook());

        resetBtn = new Button("🔄 " + LanguageManager.getText("btn.reset"));
        resetBtn.setOnAction(e -> clearFieldsAndImageStatus());

        Button importBtn = new Button("Import Excel");
        importBtn.getStyleClass().addAll("action-btn", "btn-green");
        importBtn.setOnAction(e -> handleImportBooks());

        viewHistoryBtn = new Button(LanguageManager.getText("btn.history_book"));
        viewHistoryBtn.setDisable(true);
        viewHistoryBtn.setOnAction(e -> handleViewBookHistory());

        printBarcodeBtn = new Button("🖨️ " + LanguageManager.getText("btn.print_barcode"));
        printBarcodeBtn.setDisable(true);
        printBarcodeBtn.setOnAction(e -> handlePrintBookBarcode());

        HBox box = new HBox(10, resetBtn, addBtn, editBtn, deleteBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                importBtn, viewHistoryBtn, printBarcodeBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 0, 5, 0));
        return box;
    }

    private void setupTableColumns() {
        TableColumn<Book, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(80);

        TableColumn<Book, String> colName = new TableColumn<>(LanguageManager.getText("col.name"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<Book, String> colAuthor = new TableColumn<>(LanguageManager.getText("col.author"));
        colAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        colAuthor.setPrefWidth(150);

        TableColumn<Book, String> colCategory = new TableColumn<>(LanguageManager.getText("label.category"));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        colCategory.setPrefWidth(100);

        TableColumn<Book, String> colStatus = new TableColumn<>(LanguageManager.getText("col.status"));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isStatus() ? LanguageManager.getText("status.available") : LanguageManager.getText("status.borrowed")
        ));

        bookTable.getColumns().addAll(colId, colName, colAuthor, colCategory, colStatus);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // ========================================================================
    // LOGIC & HANDLERS
    // ========================================================================

    // Gọi từ LibraryApp
    public void refreshTable() {
        // Tải lại dữ liệu từ Library (đã được cập nhật từ DB)
        masterData.setAll(library.getBooks());
        refreshCategoryList();
        updatePagination();
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) masterData.size() / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);

        // Refresh page hiện tại
        int currentPage = pagination.getCurrentPageIndex();
        if (currentPage >= pageCount) pagination.setCurrentPageIndex(0);
        else updateTablePage(currentPage);
    }

    private Node createPage(int pageIndex) {
        updateTablePage(pageIndex);
        return new VBox();
    }

    private void updateTablePage(int pageIndex) {
        int from = pageIndex * ROWS_PER_PAGE;
        int to = Math.min(from + ROWS_PER_PAGE, masterData.size());

        if (from <= to && !masterData.isEmpty()) {
            bookTable.setItems(FXCollections.observableArrayList(masterData.subList(from, to)));
        } else {
            bookTable.getItems().clear();
        }
    }

    // --- Action Handlers ---

    private void handleAddBook(boolean isSilent) {
        // Check License
        if (!BackEnd.Utils.LicenseManager.canAddMoreBooks(library.getBooks().size())) {
            if (!isSilent) showAlert(Alert.AlertType.WARNING, "License Limit", "Upgrade required.");
            return;
        }

        String id = idField.getText().trim();
        String name = nameField.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            if (!isSilent) showAlert(Alert.AlertType.ERROR, "Error", LanguageManager.getText("msg.missing_input"));
            return;
        }

        if (library.findBookById(id) != null) {
            if (!isSilent) showAlert(Alert.AlertType.ERROR, "Error", "Duplicate ID: " + id);
            return;
        }

        try {
            String cat = categoryBox.getValue();
            if (cat == null || cat.trim().isEmpty()) cat = "General";

            Book newBook = new Book(id, name, authorField.getText().trim(), yearField.getText().trim(), cat.trim());

            if (selectedUploadFile != null) {
                String savedName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                newBook.setImagePath(savedName);
            }

            library.addBook(newBook);

            // Update UI
            refreshTable();
            if (galleryTab != null) galleryTab.updateBookGallery();

            clearFieldsAndImageStatus();

            if (isSilent) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Success", LanguageManager.getText("msg.book_added"));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void handleEditBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!nameField.getText().isEmpty()) {
            selected.setName(nameField.getText());
            selected.setAuthor(authorField.getText());
            selected.setYear(yearField.getText());

            // Update Image
            if (selectedUploadFile != null) {
                try {
                    String newName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                    if (selected.getImagePath() != null) BackEnd.Utils.ImageCache.remove(selected.getImagePath());
                    selected.setImagePath(newName);
                } catch (Exception e) { e.printStackTrace(); }
            }

            String cat = categoryBox.getValue();
            if (cat != null) selected.setCategory(cat);

            library.getBookDAO().updateBook(selected);

            refreshTable();
            if (galleryTab != null) galleryTab.updateBookGallery();
            clearFieldsAndImageStatus();

            showAlert(Alert.AlertType.INFORMATION, "Success", LanguageManager.getText("msg.update_success"));
        }
    }

    private void handleDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!selected.isStatus()) {
            showAlert(Alert.AlertType.WARNING, "Warning", LanguageManager.getText("msg.book_borrowed"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText(String.format(LanguageManager.getText("msg.confirm_delete_fmt"), selected.getName()));

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (library.deleteBook(selected.getId())) {
                refreshTable();
                if (galleryTab != null) galleryTab.updateBookGallery();
                clearFieldsAndImageStatus();
                showAlert(Alert.AlertType.INFORMATION, "Success", LanguageManager.getText("msg.delete_success"));
            }
        }
    }

    private void handleAutoFillBook() {
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) return;

        imagePathLabel.setText(LanguageManager.getText("msg.fetching_info"));

        new Thread(() -> {
            Book fetched = BackEnd.Utils.BookInfoHelper.fetchBookDetails(isbn);
            Platform.runLater(() -> {
                if (fetched != null) {
                    nameField.setText(fetched.getName());
                    authorField.setText(fetched.getAuthor());
                    yearField.setText(fetched.getYear());

                    // Download Cover
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

                    // Auto Add if enabled setting
                    String autoAdd = library.getSettingsDAO().getSetting("auto_add_enabled");
                    if ("true".equals(autoAdd)) handleAddBook(true);

                } else {
                    imagePathLabel.setText(LanguageManager.getText("msg.fill_not_found"));
                }
            });
        }).start();
    }

    private void handleImportBooks() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            List<Book> books = BackEnd.Utils.ImportUtil.importBooksFromCSV(f);
            if(books.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Error", "Empty or Invalid CSV");
                return;
            }

            int success = 0;
            for (Book b : books) {
                if (library.getBookDAO().getBookById(b.getId()) == null) {
                    if (library.addBook(b)) success++;
                }
            }
            refreshTable();
            if (galleryTab != null) galleryTab.updateBookGallery();
            showAlert(Alert.AlertType.INFORMATION, "Result", "Imported: " + success + " books.");
        }
    }

    // --- Helper Methods ---

    private void initializeSelectionListener() {
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSel = newVal != null;
            editBtn.setDisable(!hasSel);
            deleteBtn.setDisable(!hasSel);
            viewHistoryBtn.setDisable(!hasSel);
            printBarcodeBtn.setDisable(!hasSel);
            addBtn.setDisable(hasSel);

            if (hasSel) {
                idField.setText(newVal.getId());
                nameField.setText(newVal.getName());
                authorField.setText(newVal.getAuthor());
                yearField.setText(newVal.getYear());
                categoryBox.setValue(newVal.getCategory());
                idField.setEditable(false);

                existingFileName = newVal.getImagePath();
                selectedUploadFile = null;
                imagePathLabel.setText(existingFileName != null ? "Current: " + new File(existingFileName).getName() : "No Cover");
            } else {
                clearFieldsAndImageStatus();
            }
        });
    }

    private void clearFieldsAndImageStatus() {
        idField.clear(); nameField.clear(); authorField.clear(); yearField.clear(); isbnField.clear();
        categoryBox.setValue(null);
        selectedUploadFile = null;
        existingFileName = null;
        imagePathLabel.setText(LanguageManager.getText("label.no_cover"));

        bookTable.getSelectionModel().clearSelection();
        idField.setEditable(true);
        idField.setText(generateNextBookId());
        isbnField.requestFocus();
    }

    private String generateNextBookId() {
        List<Book> books = library.getBooks();
        int maxId = 0;
        for (Book b : books) {
            if (b.getId().matches("^B\\d+$")) {
                try {
                    int num = Integer.parseInt(b.getId().substring(1));
                    if (num > maxId) maxId = num;
                } catch (Exception e) {}
            }
        }
        return String.format("B%03d", maxId + 1);
    }

    private void refreshCategoryList() {
        List<String> cats = library.getBookDAO().getUniqueCategories();
        categoryBox.setItems(FXCollections.observableArrayList(cats));
    }

    private TextField createStyledTextField(String key) {
        TextField tf = new TextField();
        tf.setPromptText(LanguageManager.getText(key));
        tf.getStyleClass().add("modern-textfield");
        return tf;
    }

    private HBox createInputGroup(String labelText, Node field, double width) {
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");
        if (field instanceof Region) ((Region) field).setPrefWidth(width);
        HBox box = new HBox(5, label, field);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // --- STUB HANDLERS FOR MISSING FEATURES IN SNIPPET ---
    private void handleSelectImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            selectedUploadFile = f;
            imagePathLabel.setText("Selected: " + f.getName());
        }
    }

    private void handlePrintBookBarcode() {
        Book b = bookTable.getSelectionModel().getSelectedItem();
        if(b == null) return;
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("Barcode_" + b.getId() + ".pdf");
        File f = fc.showSaveDialog(null);
        if(f != null) {
            FrontEnd.CardGenerator.saveBookBarcodeToPDF(b, f);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Saved.");
        }
    }

    private void handleViewBookHistory() {
        Book b = bookTable.getSelectionModel().getSelectedItem();
        if(b == null) return;
        List<String[]> hist = library.getTransactionDAO().getBookHistory(b.getId());
        StringBuilder sb = new StringBuilder();
        for(String[] s : hist) sb.append(s[0]).append(" | ").append(s[1]).append(" | ").append(s[2]).append("\n");
        if(hist.isEmpty()) sb.append("No transactions.");

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Book History");
        a.setHeaderText("History: " + b.getName());
        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        a.getDialogPane().setContent(area);
        a.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}