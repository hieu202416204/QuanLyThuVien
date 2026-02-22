package FrontEnd.Controllers;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.BookManagementTabView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * CONTROLLER (TỐI ƯU HIỆU SUẤT CAO - SERVER SIDE PAGINATION)
 */
public class BookManagementTabController {

    private final Library library;
    private final BookManagementTabView view;
    private final BookGalleryTabController galleryController;

    private static final int ROWS_PER_PAGE = 100;
    private File selectedUploadFile = null;
    private String existingFileName = null;

    public BookManagementTabController(Library library, BookManagementTabView view, BookGalleryTabController galleryController) {
        this.library = library;
        this.view = view;
        this.galleryController = galleryController;

        attachEvents();
        refreshTable();
    }

    private void attachEvents() {
        view.getAddBtn().setOnAction(e -> handleAddBook(false));
        view.getEditBtn().setOnAction(e -> handleEditBook());
        view.getDeleteBtn().setOnAction(e -> handleDeleteBook());
        view.getResetBtn().setOnAction(e -> clearFieldsAndImageStatus());
        view.getImportBtn().setOnAction(e -> handleImportBooks());
        view.getViewHistoryBtn().setOnAction(e -> handleViewBookHistory());
        view.getPrintBarcodeBtn().setOnAction(e -> handlePrintBookBarcode());

        view.getSelectImageBtn().setOnAction(e -> handleSelectImage());
        view.getIsbnField().setOnAction(e -> handleAutoFillBook());
        view.getAutoFillBtn().setOnAction(e -> handleAutoFillBook());

        // Phân trang
        view.getPagination().setPageFactory(this::createPage);

        initializeSelectionListener();
    }

    // ========================================================
    // LOGIC TẢI DỮ LIỆU TỐI ƯU (SERVER-SIDE PAGINATION)
    // ========================================================

    public void refreshTable() {
        // 1. Chỉ lấy TỔNG SỐ để tính trang (Mất 0.001s)
        int totalBooks = library.getBookDAO().getTotalBookCount();
        int pageCount = (int) Math.ceil((double) totalBooks / ROWS_PER_PAGE);
        view.getPagination().setPageCount(pageCount > 0 ? pageCount : 1);

        refreshCategoryList();

        // 2. Load trang hiện tại
        int currentPage = view.getPagination().getCurrentPageIndex();
        if (currentPage >= pageCount) {
            view.getPagination().setCurrentPageIndex(0); // Tự động trigger createPage
        } else {
            updateTablePage(currentPage);
        }
    }

    private Node createPage(int pageIndex) {
        updateTablePage(pageIndex);
        return new VBox();
    }

    private void updateTablePage(int pageIndex) {
        // Hiển thị trạng thái đang tải
        view.getBookTable().getItems().clear();
        view.getBookTable().setPlaceholder(new Label(LanguageManager.getText("msg.loading")));

        int offset = pageIndex * ROWS_PER_PAGE;

        // Chạy ngầm việc query Database để không làm đơ giao diện
        Task<List<Book>> loadTask = new Task<>() {
            @Override
            protected List<Book> call() {
                // Chỉ lấy đúng 100 cuốn sách cho trang này
                return library.getBookDAO().getBooksByPage(offset, ROWS_PER_PAGE);
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Book> books = loadTask.getValue();
            if (books.isEmpty()) {
                view.getBookTable().setPlaceholder(new Label(LanguageManager.getText("msg.library_empty")));
            } else {
                view.getBookTable().setItems(FXCollections.observableArrayList(books));
            }
        });

        new Thread(loadTask).start();
    }

    // ========================================================
    // CÁC HÀM XỬ LÝ (CRUD)
    // ========================================================

    private void initializeSelectionListener() {
        view.getBookTable().getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSel = newVal != null;
            view.getEditBtn().setDisable(!hasSel);
            view.getDeleteBtn().setDisable(!hasSel);
            view.getViewHistoryBtn().setDisable(!hasSel);
            view.getPrintBarcodeBtn().setDisable(!hasSel);
            view.getAddBtn().setDisable(hasSel);

            if (hasSel) {
                view.getIdField().setText(newVal.getId());
                view.getNameField().setText(newVal.getName());
                view.getAuthorField().setText(newVal.getAuthor());
                view.getYearField().setText(newVal.getYear());
                view.getCategoryBox().setValue(newVal.getCategory());
                view.getIdField().setEditable(false);

                existingFileName = newVal.getImagePath();
                selectedUploadFile = null;
                view.getImagePathLabel().setText(existingFileName != null ? "Current: " + new File(existingFileName).getName() : "No Cover");
            } else {
                clearFieldsAndImageStatus();
            }
        });
    }

    private void handleAddBook(boolean isSilent) {
        int totalBooks = library.getBookDAO().getTotalBookCount();
        if (!BackEnd.Utils.LicenseManager.canAddMoreBooks(totalBooks)) {
            if (!isSilent) showAlert(Alert.AlertType.WARNING, "License Limit", "Upgrade required.");
            return;
        }

        String id = view.getIdField().getText().trim();
        String name = view.getNameField().getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            if (!isSilent) showAlert(Alert.AlertType.ERROR, "Error", LanguageManager.getText("msg.missing_input"));
            return;
        }

        if (library.findBookById(id) != null) {
            if (!isSilent) showAlert(Alert.AlertType.ERROR, "Error", "Duplicate ID: " + id);
            return;
        }

        try {
            String cat = view.getCategoryBox().getValue();
            if (cat == null || cat.trim().isEmpty()) cat = "General";

            Book newBook = new Book(id, name, view.getAuthorField().getText().trim(), view.getYearField().getText().trim(), cat.trim());

            if (selectedUploadFile != null) {
                String savedName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                newBook.setImagePath(savedName);
            }

            library.addBook(newBook);

            refreshTable();
            if (galleryController != null) galleryController.updateBookGallery();

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
        Book selected = view.getBookTable().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!view.getNameField().getText().isEmpty()) {
            selected.setName(view.getNameField().getText());
            selected.setAuthor(view.getAuthorField().getText());
            selected.setYear(view.getYearField().getText());

            if (selectedUploadFile != null) {
                try {
                    String newName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUploadFile);
                    if (selected.getImagePath() != null) BackEnd.Utils.ImageCache.remove(selected.getImagePath());
                    selected.setImagePath(newName);
                } catch (Exception e) { e.printStackTrace(); }
            }

            String cat = view.getCategoryBox().getValue();
            if (cat != null) selected.setCategory(cat);

            library.getBookDAO().updateBook(selected);

            refreshTable();
            if (galleryController != null) galleryController.updateBookGallery();
            clearFieldsAndImageStatus();

            showAlert(Alert.AlertType.INFORMATION, "Success", LanguageManager.getText("msg.update_success"));
        }
    }

    private void handleDeleteBook() {
        Book selected = view.getBookTable().getSelectionModel().getSelectedItem();
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
                if (galleryController != null) galleryController.updateBookGallery();
                clearFieldsAndImageStatus();
                showAlert(Alert.AlertType.INFORMATION, "Success", LanguageManager.getText("msg.delete_success"));
            }
        }
    }

    private void handleAutoFillBook() {
        String isbn = view.getIsbnField().getText().trim();
        if (isbn.isEmpty()) return;

        view.getImagePathLabel().setText(LanguageManager.getText("msg.fetching_info"));

        new Thread(() -> {
            Book fetched = BackEnd.Utils.BookInfoHelper.fetchBookDetails(isbn);
            Platform.runLater(() -> {
                if (fetched != null) {
                    view.getNameField().setText(fetched.getName());
                    view.getAuthorField().setText(fetched.getAuthor());
                    view.getYearField().setText(fetched.getYear());

                    if (fetched.getImagePath() != null) {
                        try {
                            File temp = BackEnd.Utils.BookInfoHelper.downloadCoverImage(fetched.getImagePath());
                            if (temp != null) {
                                this.selectedUploadFile = temp;
                                view.getImagePathLabel().setText(LanguageManager.getText("msg.img_downloaded"));
                            }
                        } catch(Exception e) {}
                    }

                    if (view.getIdField().getText().isEmpty()) view.getIdField().setText(generateNextBookId());

                    String autoAdd = library.getSettingsDAO().getSetting("auto_add_enabled");
                    if ("true".equals(autoAdd)) handleAddBook(true);

                } else {
                    view.getImagePathLabel().setText(LanguageManager.getText("msg.fill_not_found"));
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
            if (galleryController != null) galleryController.updateBookGallery();
            showAlert(Alert.AlertType.INFORMATION, "Result", "Imported: " + success + " books.");
        }
    }

    private void handleSelectImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            selectedUploadFile = f;
            view.getImagePathLabel().setText("Selected: " + f.getName());
        }
    }

    private void handlePrintBookBarcode() {
        Book b = view.getBookTable().getSelectionModel().getSelectedItem();
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
        Book b = view.getBookTable().getSelectionModel().getSelectedItem();
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

    private void clearFieldsAndImageStatus() {
        view.getIdField().clear(); view.getNameField().clear(); view.getAuthorField().clear(); view.getYearField().clear(); view.getIsbnField().clear();
        view.getCategoryBox().setValue(null);
        selectedUploadFile = null;
        existingFileName = null;
        view.getImagePathLabel().setText(LanguageManager.getText("label.no_cover"));

        view.getBookTable().getSelectionModel().clearSelection();
        view.getIdField().setEditable(true);
        view.getIdField().setText(generateNextBookId());
        view.getIsbnField().requestFocus();
    }

    /**
     * Tự động sinh ID mới NHANH CHÓNG nhờ dùng lệnh SQL lấy ID lớn nhất
     */
    private String generateNextBookId() {
        String lastId = library.getBookDAO().getLastBookId();
        int nextNum = 1;
        if (lastId != null && lastId.matches("^B\\d+$")) {
            try {
                nextNum = Integer.parseInt(lastId.substring(1)) + 1;
            } catch (Exception ignored) {}
        }
        return String.format("B%03d", nextNum);
    }

    private void refreshCategoryList() {
        Task<List<String>> catTask = new Task<>() {
            @Override
            protected List<String> call() {
                return library.getBookDAO().getUniqueCategories();
            }
        };
        catTask.setOnSucceeded(e -> view.getCategoryBox().setItems(FXCollections.observableArrayList(catTask.getValue())));
        new Thread(catTask).start();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}