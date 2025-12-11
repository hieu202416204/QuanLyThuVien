package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

public class BookGalleryTab extends StackPane { // Đổi sang StackPane để canh giữa tốt hơn

    private final Library library;
    private final Function<BufferedImage, Image> resizeConverter;

    // Cấu hình phân trang
    private static final int ITEMS_PER_PAGE = 24; // Tăng số lượng lên chút cho đẹp
    private Pagination pagination;
    private List<Book> cachedBooks;

    private static final String PLACEHOLDER_URL = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";
    private String defaultImageUrl;

    public BookGalleryTab(Library library, FlowPane dummy, Function<BufferedImage, Image> resizeConverter) {
        this.library = library;
        this.resizeConverter = resizeConverter;

        this.setPadding(new Insets(10));

        // Chuẩn bị URL ảnh mặc định
        URL resource = getClass().getResource("/resources/default_cover.png");
        if (resource == null) resource = getClass().getResource("/default_cover.png");
        this.defaultImageUrl = (resource != null) ? resource.toExternalForm() : PLACEHOLDER_URL;

        updateBookGallery();
    }

    public String getPlaceholderBase64Url() { return PLACEHOLDER_URL; }

    public void updateBookGallery() {
        this.getChildren().clear();

        this.cachedBooks = library.getBooks();

        if (cachedBooks.isEmpty()) {
            this.getChildren().add(new Label(LanguageManager.getText("msg.library_empty")));
            return;
        }

        int pageCount = (int) Math.ceil((double) cachedBooks.size() / ITEMS_PER_PAGE);

        // Khởi tạo Pagination
        pagination = new Pagination(pageCount, 0);
        pagination.setPageFactory(this::createPage);

        // Quan trọng: Pagination phải giãn full màn hình
        pagination.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        this.getChildren().add(pagination);
    }

    /**
     * Tạo nội dung trang (SỬA LỖI LAYOUT TẠI ĐÂY)
     */
    private Node createPage(int pageIndex) {
        // 1. FlowPane chứa các thẻ sách
        FlowPane pageBox = new FlowPane();
        pageBox.setHgap(20); // Khoảng cách ngang giữa các sách
        pageBox.setVgap(20); // Khoảng cách dọc
        pageBox.setPadding(new Insets(20));
        pageBox.setAlignment(Pos.TOP_LEFT);

        // QUAN TRỌNG: Để FlowPane tự động xuống dòng khi hết chỗ
        pageBox.setPrefWrapLength(100); // Giá trị nhỏ để nó phụ thuộc vào container cha

        // 2. ScrollPane bao bên ngoài (Để cuộn nếu nhiều sách)
        ScrollPane scrollPane = new ScrollPane(pageBox);
        scrollPane.setFitToWidth(true); // <--- CHÌA KHÓA: Bắt buộc FlowPane giãn theo chiều ngang của màn hình
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Logic tạo thẻ sách (Giữ nguyên)
        int start = pageIndex * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, cachedBooks.size());

        for (int i = start; i < end; i++) {
            Book book = cachedBooks.get(i);
            VBox card = createBookCard(book, null);
            pageBox.getChildren().add(card);
        }

        // Logic load ảnh bất đồng bộ (Giữ nguyên)
        Task<Void> loadImagesTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < pageBox.getChildren().size(); i++) {
                    int bookIndex = start + i;
                    if (bookIndex >= cachedBooks.size()) break;

                    Book book = cachedBooks.get(bookIndex);
                    Node node = pageBox.getChildren().get(i);

                    if (node instanceof VBox card) {
                        Image img = loadImageForBook(book, defaultImageUrl);
                        Platform.runLater(() -> {
                            if (!card.getChildren().isEmpty() && card.getChildren().get(0) instanceof ImageView view) {
                                view.setImage(img);
                            }
                        });
                        Thread.sleep(10); // Nghỉ ít hơn để load nhanh hơn
                    }
                }
                return null;
            }
        };

        Thread thread = new Thread(loadImagesTask);
        thread.setDaemon(true);
        thread.start();

        return scrollPane;
    }

    private Image loadImageForBook(Book book, String defaultUrl) {
        String fileName = book.getImagePath();
        if (fileName != null && BackEnd.Utils.ImageCache.contains(fileName)) {
            return BackEnd.Utils.ImageCache.get(fileName);
        }
        try {
            File localFile = BackEnd.Utils.FileUtil.getLocalFile(fileName);
            if (localFile != null && localFile.exists()) {
                BufferedImage originalAWTImage = ImageIO.read(localFile);
                Image resized = resizeConverter.apply(originalAWTImage);
                if (resized != null) {
                    BackEnd.Utils.ImageCache.put(fileName, resized);
                    return resized;
                }
            }
        } catch (Exception e) { }
        return new Image(defaultUrl, 120, 160, true, true);
    }

    private VBox createBookCard(Book book, Image image) {
        VBox bookBox = new VBox(5);
        bookBox.setPrefWidth(140); // Chiều rộng cố định cho mỗi thẻ
        bookBox.getStyleClass().add("gallery-book-box");

        if (image == null) image = new Image(defaultImageUrl, 120, 160, true, true);

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(120);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(book.getName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(130);
        nameLabel.getStyleClass().add("book-name-label");

        // Tooltip để xem tên đầy đủ nếu bị cắt
        Tooltip tooltip = new Tooltip(book.getName());
        Tooltip.install(bookBox, tooltip);

        Label authorLabel = new Label(book.getAuthor());
        authorLabel.getStyleClass().add("book-author-label");
        authorLabel.setMaxWidth(130);

        Label statusLabel = new Label(book.isStatus()
                ? LanguageManager.getText("status.available")
                : LanguageManager.getText("status.borrowed"));
        statusLabel.getStyleClass().add(book.isStatus() ? "available-status" : "borrowed-status");

        bookBox.getChildren().addAll(imageView, new Separator(), nameLabel, authorLabel, statusLabel);
        return bookBox;
    }
}