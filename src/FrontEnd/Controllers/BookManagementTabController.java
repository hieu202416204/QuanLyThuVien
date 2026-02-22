package FrontEnd.Controllers;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.BookManagementTabView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * CONTROLLER: Xử lý logic Thêm/Sửa/Xóa, Load dữ liệu, Tìm kiếm ISBN.
 */
public class BookManagementTabController {

    private final Library library;
    private final BookManagementTabView view;
    private final BookGalleryTabController galleryController;

    // Data State
    private final ObservableList<Book> masterData = FXCollections.observableArrayList();
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
        // Nút chức năng
        view.getAddBtn().setOnAction(e -> handleAddBook(false));
        view.getEditBtn().setOnAction(e -> handleEditBook());
        view.getDeleteBtn().setOnAction(e -> handleDeleteBook());
        view.getResetBtn().setOnAction(e -> clearFieldsAndImageStatus());
        view.getImportBtn().setOnAction(e -> handleImportBooks());
        view.getViewHistoryBtn().setOnAction(e -> handleViewBookHistory());
        view.getPrintBarcodeBtn().setOnAction(e -> handlePrintBookBarcode());

        // Hình ảnh & ISBN
        view.getSelectImageBtn().setOnAction(e -> handleSelectImage());
        view.getIsbnField().setOnAction(e -> handleAutoFillBook());
        view.getAutoFillBtn().setOnAction(e -> handleAutoFillBook());

        // Phân trang & Cập nhật danh sách
        view.getPagination().setPageFactory(this::createPage);
        masterData.addListener((javafx.collections.ListChangeListener<Book>) c -> updatePagination());

        // Lắng nghe chọn dòng trong Bảng
        initializeSelectionListener();
    }

    public void refreshTable() {
        masterData.setAll(library.getBooks());
        refreshCategoryList();
        updatePagination();
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) masterData.size() / ROWS_PER_PAGE);
        view.getPagination().setPageCount(pageCount > 0 ? pageCount : 1);

        int currentPage = view.getPagination().getCurrentPageIndex();
        if (currentPage >= pageCount) view.getPagination().setCurrentPageIndex(0);
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
            view.getBookTable().setItems(FXCollections.observableArrayList(masterData.subList(from, to)));
        } else {
            view.getBookTable().getItems().clear();
        }
    }

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

    // --- CÁC HÀM LOGIC CHÍNH ---

    private void handleAddBook(boolean isSilent) {
        if (!BackEnd.Utils.LicenseManager.canAddMoreBooks(library.getBooks().size())) {
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
        view.getCategoryBox().setItems(FXCollections.observableArrayList(cats));
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}