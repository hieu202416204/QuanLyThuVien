package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import XuLiAnh.ImageResizer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SearchTab extends VBox {
    private final Library library;
    private Pagination searchPagination;
    private final FlowPane searchGalleryPane;
    private List<Book> currentSearchResults = new ArrayList<>();
    private final int PAGE_SIZE = 100;

    private static final String PLACEHOLDER_IMG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";

    public SearchTab(Library library) {
        this.library = library;
        this.searchGalleryPane = new FlowPane();
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // UI Setup
        searchGalleryPane.setHgap(15);
        searchGalleryPane.setVgap(15);
        searchGalleryPane.setPadding(new Insets(10));
        searchGalleryPane.setPrefWrapLength(1000);

        searchPagination = new Pagination();
        searchPagination.setVisible(false); // Ẩn lúc đầu
        searchPagination.setPageFactory(this::createSearchPage);
        searchPagination.setMinHeight(500);

        // Search Controls
        TextField searchField = new TextField();
        searchField.setPrefWidth(300);
        searchField.setPromptText("Enter keyword...");

        Label resultLabel = new Label(LanguageManager.getText("label.search_guide"));

        Button searchCombinedBtn = new Button(LanguageManager.getText("btn.search_combined"));
        searchCombinedBtn.setOnAction(e -> handleSearch(searchField.getText(), "COMBINED", resultLabel));

        Button searchIdBtn = new Button(LanguageManager.getText("btn.search_id_exact"));
        searchIdBtn.setOnAction(e -> handleSearch(searchField.getText(), "ID", resultLabel));

        Button searchAuthorBtn = new Button(LanguageManager.getText("btn.search_author")); // Thêm nút tìm tác giả nếu muốn
        searchAuthorBtn.setOnAction(e -> handleSearch(searchField.getText(), "AUTHOR", resultLabel));

        Button resetBtn = new Button(LanguageManager.getText("btn.clear_result"));
        resetBtn.setOnAction(e -> {
            currentSearchResults.clear();
            searchPagination.setVisible(false);
            searchField.clear();
            searchGalleryPane.getChildren().clear();
            resultLabel.setText(LanguageManager.getText("label.search_ready"));
        });

        javafx.scene.layout.HBox controls = new javafx.scene.layout.HBox(10, searchField, searchIdBtn, searchCombinedBtn, searchAuthorBtn, resetBtn);
        controls.setPadding(new Insets(10));

        this.getChildren().addAll(resultLabel, controls, searchPagination);
    }

    private void handleSearch(String query, String searchType, Label resultLabel) {
        if (query.trim().isEmpty()) return;

        List<Book> results;
        if ("COMBINED".equals(searchType)) {
            results = library.getBookDAO().searchByKeyword(query);
        } else if ("ID".equals(searchType)) {
            Book b = library.getBookDAO().getBookById(query);
            results = (b != null) ? List.of(b) : List.of();
        } else if ("AUTHOR".equals(searchType)) {
            results = library.getBookDAO().searchByKeyword(query); // Hoặc hàm search author riêng
        } else {
            results = library.getBookDAO().searchBookByPartialName(query);
        }

        // Cập nhật dữ liệu
        this.currentSearchResults = results;

        // Cập nhật Label
        String typeText = searchType; // Có thể map sang language manager
        resultLabel.setText(String.format(LanguageManager.getText("msg.search_result"), results.size(), typeText));

        // Logic hiển thị Pagination
        if (results.isEmpty()) {
            searchPagination.setVisible(false);
            searchGalleryPane.getChildren().clear();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText(LanguageManager.getText("msg.no_match"));
            alert.showAndWait();
        } else {
            searchPagination.setVisible(true);
            int pageCount = (int) Math.ceil((double) results.size() / PAGE_SIZE);
            searchPagination.setPageCount(pageCount);
            searchPagination.setCurrentPageIndex(0);

            // Force update trang 0
            updateGallery(0);
        }
    }

    private Node createSearchPage(int pageIndex) {
        updateGallery(pageIndex);
        // Trả về ScrollPane chứa FlowPane để có thể cuộn
        ScrollPane sp = new ScrollPane(searchGalleryPane);
        sp.setFitToWidth(true);
        return sp;
    }

    private void updateGallery(int pageIndex) {
        searchGalleryPane.getChildren().clear();

        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, currentSearchResults.size());

        if (from >= to) return;

        List<Book> books = currentSearchResults.subList(from, to);

        URL defaultUrl = getClass().getResource("/resources/default_cover.png");
        final String DEFAULT_IMG = (defaultUrl != null) ? defaultUrl.toExternalForm() : PLACEHOLDER_IMG;

        for (Book book : books) {
            VBox bookBox = new VBox(5);
            bookBox.setPrefWidth(150);
            bookBox.getStyleClass().add("gallery-book-box");
            bookBox.setPadding(new Insets(10));

            // Image Handling (Self-contained)
            Image image;
            try {
                if (book.getImagePath() != null && !book.getImagePath().isEmpty()) {
                    // Check Cache inside BackEnd Utils if available, or load manually
                    File imgFile = BackEnd.Utils.FileUtil.getLocalFile(book.getImagePath());
                    if (imgFile.exists()) {
                        BufferedImage buf = ImageIO.read(imgFile);
                        image = convertAndResize(buf);
                    } else {
                        image = new Image(DEFAULT_IMG);
                    }
                } else {
                    image = new Image(DEFAULT_IMG);
                }
            } catch (Exception e) {
                image = new Image(DEFAULT_IMG);
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(120);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(true);

            Label idLabel = new Label("ID: " + book.getId());
            Label nameLabel = new Label(book.getName());
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(140);
            nameLabel.getStyleClass().add("book-name-label");

            Label statusLabel = new Label(book.isStatus() ? LanguageManager.getText("status.available") : LanguageManager.getText("status.borrowed"));
            statusLabel.getStyleClass().add(book.isStatus() ? "available-status" : "borrowed-status");

            bookBox.getChildren().addAll(imageView, new Separator(), idLabel, nameLabel, statusLabel);
            searchGalleryPane.getChildren().add(bookBox);
        }
    }

    // Hàm resize ảnh nội bộ (private helper)
    private Image convertAndResize(BufferedImage original) {
        if(original == null) return null;
        try {
            BufferedImage resized = ImageResizer.resizeImage(original, 120, 160);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", os);
            return new Image(new ByteArrayInputStream(os.toByteArray()));
        } catch (IOException e) { return null; }
    }
}