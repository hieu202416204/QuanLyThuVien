package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Function; // Thêm import Function

public class BookGalleryTab extends ScrollPane {

    private final Library library;
    private final FlowPane bookFlowPane;
    // Thêm Function để nhận hàm resize từ LibraryApp
    private final Function<BufferedImage, Image> resizeConverter;

    // Kích thước cố định
    private static final int TARGET_WIDTH = 120;
    private static final int TARGET_HEIGHT = 160;

    // Khai báo Base64 Placeholder cho ảnh mặc định an toàn
    private static final String PLACEHOLDER_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";
    private static final String PLACEHOLDER_URL = "data:image/png;base64," + PLACEHOLDER_BASE64;


    public BookGalleryTab(Library library, FlowPane flowPane, Function<BufferedImage, Image> resizeConverter) {
        this.library = library;
        this.bookFlowPane = flowPane;
        this.resizeConverter = resizeConverter; // Nhận hàm resize

        bookFlowPane.setHgap(15);
        bookFlowPane.setVgap(15);
        bookFlowPane.setPadding(new Insets(10));
        bookFlowPane.setPrefWidth(950);
        bookFlowPane.setStyle("-fx-alignment: top-center;");

        this.setContent(bookFlowPane);
        this.setFitToWidth(true);
        this.setPrefHeight(600);

        updateBookGallery();
    }
    // Thêm vào BookGalleryTab.java
    public String getPlaceholderBase64Url() {
        return PLACEHOLDER_URL;
    }

    public void updateBookGallery() {
        bookFlowPane.getChildren().clear();

        // Xác định URL ảnh mặc định an toàn
        java.net.URL defaultUrl = getClass().getResource("/resources/default_cover.png");
        if (defaultUrl == null) {
            defaultUrl = getClass().getResource("/default_cover.png");
        }
        final String DEFAULT_IMAGE_URL = (defaultUrl != null) ? defaultUrl.toExternalForm() : PLACEHOLDER_URL;

        for (Book book : library.getBooks()) {
            VBox bookBox = new VBox(5);
            // Kích thước cố định cho thẻ sách (Sẽ được chỉnh qua CSS để đẹp hơn)
            bookBox.setPrefWidth(150);
            bookBox.getStyleClass().add("gallery-book-box");

            // --- 1. LOGIC TẢI ẢNH BÌA VÀ RESIZE CHẤT LƯỢNG CAO ---
            Image image;
            String path = book.getImagePath();

            try {
                if (path != null && !path.isEmpty()) {
                    File imageFile = new File(path);
                    if (imageFile.exists()) {
                        BufferedImage originalAWTImage = ImageIO.read(imageFile);
                        // GỌI HÀM RESIZE VÀ CHUYỂN ĐỔI (TRUYỀN TỪ LIBRARYAPP)
                        image = resizeConverter.apply(originalAWTImage);

                        if (image == null) throw new IOException("Resize thất bại hoặc file rỗng.");
                    } else {
                        throw new IOException("File ảnh không tồn tại: " + path);
                    }
                } else {
                    // Nếu không có path, dùng ảnh mặc định (FX Image)
                    image = new Image(DEFAULT_IMAGE_URL, TARGET_WIDTH, TARGET_HEIGHT, true, true);
                }

            } catch (Exception e) {
                System.err.println("Lỗi tải/resize ảnh cho sách " + book.getId() + ": " + e.getMessage());
                // Dùng ảnh mặc định/placeholder
                image = new Image(DEFAULT_IMAGE_URL, TARGET_WIDTH, TARGET_HEIGHT, true, true);
            }

            ImageView imageView = new ImageView(image);
            // Thiết lập kích thước ImageView cố định để phù hợp với kích thước resize
            imageView.setFitWidth(TARGET_WIDTH);
            imageView.setFitHeight(TARGET_HEIGHT);
            imageView.setPreserveRatio(true);

            // --- 2. THÔNG TIN SÁCH (LABELS) ---
            Label idLabel = new Label("ID: " + book.getId());
            idLabel.getStyleClass().add("id-label");

            Label nameLabel = new Label(book.getName());
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(140);
            nameLabel.getStyleClass().add("book-name-label");

            Label authorLabel = new Label("Tác giả: " + book.getAuthor());
            authorLabel.getStyleClass().add("book-author-label");

            Label yearLabel = new Label("Năm: " + book.getYear());
            yearLabel.getStyleClass().add("year-label");

            // 3. Trạng thái sách
            Label statusLabel = new Label(book.isStatus() ? "CÓ SẴN" : "ĐÃ MƯỢN");
            statusLabel.getStyleClass().add(book.isStatus() ? "available-status" : "borrowed-status");

            // SẮP XẾP: Ảnh bìa lên trên cùng
            bookBox.getChildren().addAll(
                    imageView,
                    new Separator(),
                    idLabel,
                    nameLabel,
                    authorLabel,
                    yearLabel,
                    statusLabel
            );
            bookFlowPane.getChildren().add(bookBox);
        }
    }
}