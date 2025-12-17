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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

public class BookGalleryTab extends VBox {

    private final Library library;
    private final Function<BufferedImage, Image> resizeConverter;

    // --- CÁC THÀNH PHẦN UI CỐ ĐỊNH (STATIC) ---
    private final ScrollPane mainScrollPane;
    private final FlowPane galleryFlowPane;
    private final Pagination pagination;

    // Data Cache
    private List<Book> cachedBooks;
    private static final int ITEMS_PER_PAGE = 24;

    // Image Resources
    private static final String PLACEHOLDER_URL = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";
    private String defaultImageUrl;

    public BookGalleryTab(Library library, FlowPane dummy, Function<BufferedImage, Image> resizeConverter) {
        this.library = library;
        this.resizeConverter = resizeConverter;

        // Chuẩn bị URL ảnh mặc định
        URL resource = getClass().getResource("/resources/default_cover.png");
        if (resource == null) resource = getClass().getResource("/default_cover.png");
        this.defaultImageUrl = (resource != null) ? resource.toExternalForm() : PLACEHOLDER_URL;

        // --- KHỞI TẠO UI MỘT LẦN DUY NHẤT ---
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // 1. FlowPane (Nơi chứa sách)
        galleryFlowPane = new FlowPane();
        galleryFlowPane.setHgap(20);
        galleryFlowPane.setVgap(20);
        galleryFlowPane.setPadding(new Insets(20));
        galleryFlowPane.setAlignment(Pos.TOP_LEFT);

        // 2. ScrollPane (Khung cuộn cố định)
        mainScrollPane = new ScrollPane(galleryFlowPane);
        mainScrollPane.setFitToWidth(true); //
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Chặn trượt ngang tuyệt đối
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Ràng buộc chiều rộng để FlowPane xuống dòng đúng lúc
        galleryFlowPane.prefWrapLengthProperty().bind(mainScrollPane.widthProperty().subtract(40)); // Trừ hao padding

        // Để ScrollPane chiếm hết không gian dọc còn lại
        VBox.setVgrow(mainScrollPane, Priority.ALWAYS);

        // 3. Pagination (Thanh điều hướng)
        pagination = new Pagination();
        pagination.setMaxHeight(60); // Giới hạn chiều cao
        pagination.setPageFactory(this::handlePageChange); // Sự kiện đổi trang

        // Add vào layout chính
        this.getChildren().addAll(mainScrollPane, pagination);

        // Load dữ liệu lần đầu
        updateBookGallery();
    }

    public String getPlaceholderBase64Url() { return PLACEHOLDER_URL; }

    // =============================================================
    // LOGIC CẬP NHẬT DỮ LIỆU
    // =============================================================
    public void updateBookGallery() {
        this.cachedBooks = library.getBooks();

        if (cachedBooks == null || cachedBooks.isEmpty()) {
            galleryFlowPane.getChildren().clear();
            galleryFlowPane.getChildren().add(new Label(LanguageManager.getText("msg.library_empty")));
            pagination.setVisible(false);
            return;
        }

        // Tính toán số trang
        int pageCount = (int) Math.ceil((double) cachedBooks.size() / ITEMS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setVisible(true);

        // Reset về trang 0 (nó sẽ tự gọi handlePageChange)
        pagination.setCurrentPageIndex(0);
        // Nếu đang ở trang 0 rồi thì set lại để force update
        if (pagination.getCurrentPageIndex() == 0) {
            updateGalleryView(0);
        }
    }

    /**
     * Hàm này được Pagination gọi khi người dùng bấm số trang.
     * Trả về VBox rỗng vì ta không dùng vùng hiển thị mặc định của Pagination.
     */
    private Node handlePageChange(int pageIndex) {
        if (cachedBooks != null && !cachedBooks.isEmpty()) {
            updateGalleryView(pageIndex);
        }
        return new VBox(); // Trả về node rỗng (Dummy)
    }

    /**
     * Logic vẽ lại các cuốn sách vào FlowPane cố định
     */
    private void updateGalleryView(int pageIndex) {
        // Đảm bảo chạy trên UI Thread
        Platform.runLater(() -> {
            galleryFlowPane.getChildren().clear();
            mainScrollPane.setVvalue(0); // Cuộn lên đầu trang

            int start = pageIndex * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, cachedBooks.size());

            // 1. Tạo khung xương (Skeleton) với ảnh Placeholder trước cho mượt
            for (int i = start; i < end; i++) {
                Book book = cachedBooks.get(i);
                VBox card = createBookCard(book, null); // Ảnh null sẽ dùng placeholder
                galleryFlowPane.getChildren().add(card);
            }

            // 2. Load ảnh thật ở chế độ nền (Background)
            startImageLoadingTask(start, end);
        });
    }

    // =============================================================
    // LOGIC LOAD ẢNH BẤT ĐỒNG BỘ (Background Thread)
    // =============================================================
    private void startImageLoadingTask(int start, int end) {
        Task<Void> loadImagesTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Duyệt qua các node vừa được thêm vào FlowPane
                // Lưu ý: galleryFlowPane.getChildren() truy cập từ thread khác có thể nguy hiểm
                // nhưng vì ta chỉ đọc index tương ứng nên tạm ổn.
                // Cách an toàn hơn là load ảnh xong mới update UI.

                List<Node> currentCards = galleryFlowPane.getChildren();
                int size = currentCards.size();

                for (int i = 0; i < size; i++) {
                    if (isCancelled()) break;

                    int bookIndex = start + i;
                    if (bookIndex >= cachedBooks.size()) break;

                    Book book = cachedBooks.get(bookIndex);

                    // Kiểm tra cache hoặc load từ file
                    Image img = loadImageForBook(book, defaultImageUrl);

                    // Cập nhật lại UI khi đã có ảnh
                    int finalI = i;
                    Platform.runLater(() -> {
                        if (finalI < currentCards.size()) {
                            Node node = currentCards.get(finalI);
                            if (node instanceof VBox card && !card.getChildren().isEmpty()) {
                                if (card.getChildren().get(0) instanceof ImageView view) {
                                    view.setImage(img); // Thay thế placeholder bằng ảnh thật
                                }
                            }
                        }
                    });

                    Thread.sleep(5); // Nghỉ cực ngắn để không chiếm dụng CPU
                }
                return null;
            }
        };

        Thread thread = new Thread(loadImagesTask);
        thread.setDaemon(true);
        thread.start();
    }

    // =============================================================
    // HELPERS (Giữ nguyên logic cũ)
    // =============================================================
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
        bookBox.setPrefWidth(140);
        bookBox.getStyleClass().add("gallery-book-box");

        // Nếu chưa có ảnh, dùng Placeholder
        if (image == null) image = new Image(defaultImageUrl, 120, 160, true, true);

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(120);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(book.getName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(130);
        nameLabel.getStyleClass().add("book-name-label");

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