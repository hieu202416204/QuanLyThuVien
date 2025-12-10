package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager; // Import LanguageManager
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

public class BookGalleryTab extends ScrollPane {

    private final Library library;
    private final FlowPane bookFlowPane;
    private final Function<BufferedImage, Image> resizeConverter;

    // Kích thước cố định
    private static final int TARGET_WIDTH = 120;
    private static final int TARGET_HEIGHT = 160;

    // Placeholder mặc định
    private static final String PLACEHOLDER_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";
    private static final String PLACEHOLDER_URL = "data:image/png;base64," + PLACEHOLDER_BASE64;

    public BookGalleryTab(Library library, FlowPane flowPane, Function<BufferedImage, Image> resizeConverter) {
        this.library = library;
        this.bookFlowPane = flowPane;
        this.resizeConverter = resizeConverter;

        // Cấu hình FlowPane
        bookFlowPane.setHgap(15);
        bookFlowPane.setVgap(15);
        bookFlowPane.setPadding(new Insets(10));
        bookFlowPane.setPrefWidth(950);
        bookFlowPane.setAlignment(Pos.TOP_LEFT); // Căn lề trái cho đẹp khi ít sách

        this.setContent(bookFlowPane);
        this.setFitToWidth(true);
        this.setPrefHeight(600);

        // Gọi cập nhật lần đầu
        updateBookGallery();
    }

    public String getPlaceholderBase64Url() {
        return PLACEHOLDER_URL;
    }

    /**
     * Phương thức chính: Tải sách và ảnh trên luồng riêng (Background Thread)
     */
    public void updateBookGallery() {
        // 1. Xóa nội dung cũ và hiển thị Loading Indicator
        bookFlowPane.getChildren().clear();

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);

        // Dùng LanguageManager cho chữ "Đang tải..."
        VBox loadingBox = new VBox(10, spinner, new Label(LanguageManager.getText("msg.loading")));
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(50));

        bookFlowPane.getChildren().add(loadingBox);
        bookFlowPane.setAlignment(Pos.CENTER); // Căn giữa spinner

        // 2. Tạo Task để chạy ngầm
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Lấy danh sách sách từ DB
                List<Book> books = library.getBooks();

                // Chuẩn bị URL mặc định một lần để dùng lại
                URL defaultUrl = getClass().getResource("/resources/default_cover.png");
                if (defaultUrl == null) {
                    defaultUrl = getClass().getResource("/default_cover.png");
                }
                final String DEFAULT_IMAGE_URL = (defaultUrl != null) ? defaultUrl.toExternalForm() : PLACEHOLDER_URL;

                // Duyệt qua từng cuốn sách
                for (Book book : books) {
                    // --- A. XỬ LÝ ẢNH NẶNG (Chạy ở Background) ---
                    Image finalImage = loadImageForBook(book, DEFAULT_IMAGE_URL);

                    // --- B. CẬP NHẬT UI (Phải chạy ở JavaFX Thread) ---
                    Platform.runLater(() -> {
                        // Nếu đây là cuốn sách đầu tiên tải xong, xóa cái Spinner đi
                        if (bookFlowPane.getChildren().contains(loadingBox)) {
                            bookFlowPane.getChildren().remove(loadingBox);
                            bookFlowPane.setAlignment(Pos.TOP_LEFT); // Trả lại căn lề
                        }

                        // Tạo thẻ sách và thêm vào FlowPane
                        VBox card = createBookCard(book, finalImage);
                        bookFlowPane.getChildren().add(card);
                    });

                    // Ngủ một xíu (10ms) để UI mượt hơn
                    Thread.sleep(10);
                }

                // Nếu danh sách rỗng sau khi chạy xong
                if (books.isEmpty()) {
                    Platform.runLater(() -> {
                        bookFlowPane.getChildren().clear();
                        bookFlowPane.setAlignment(Pos.CENTER);
                        // Dùng LanguageManager cho thông báo thư viện rỗng
                        bookFlowPane.getChildren().add(new Label(LanguageManager.getText("msg.library_empty")));
                    });
                }

                return null;
            }
        };

        // 3. Bắt đầu luồng chạy Task
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true); // Để luồng tự tắt khi tắt app
        thread.start();
    }

    /**
     * Logic tải và resize ảnh (CÓ SỬ DỤNG CACHE)
     */
    private Image loadImageForBook(Book book, String defaultUrl) {
        String fileName = book.getImagePath();

        // 1. KIỂM TRA CACHE TRƯỚC (Siêu nhanh)
        // Nếu tên file này đã từng được load, lấy ngay lập tức
        if (fileName != null && BackEnd.Utils.ImageCache.contains(fileName)) {
            return BackEnd.Utils.ImageCache.get(fileName);
        }

        // 2. NẾU CHƯA CÓ TRONG CACHE THÌ MỚI ĐỌC FILE (Chậm)
        try {
            File localFile = BackEnd.Utils.FileUtil.getLocalFile(fileName);

            if (localFile != null && localFile.exists()) {
                BufferedImage originalAWTImage = ImageIO.read(localFile);
                Image resized = resizeConverter.apply(originalAWTImage);

                if (resized != null) {
                    // 3. ĐỌC XONG THÌ LƯU VÀO CACHE NGAY
                    BackEnd.Utils.ImageCache.put(fileName, resized);
                    return resized;
                }
            }
        } catch (Exception e) {
            // Lỗi đọc file thì bỏ qua
        }

        // Trả về ảnh mặc định (Không cần cache ảnh mặc định vì nó nhẹ và load từ Resource)
        return new Image(defaultUrl, TARGET_WIDTH, TARGET_HEIGHT, true, true);
    }

    /**
     * Tạo giao diện thẻ sách (VBox)
     */
    private VBox createBookCard(Book book, Image image) {
        VBox bookBox = new VBox(5);
        bookBox.setPrefWidth(150);
        bookBox.getStyleClass().add("gallery-book-box");

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(TARGET_WIDTH);
        imageView.setFitHeight(TARGET_HEIGHT);
        imageView.setPreserveRatio(true);

        // ID
        Label idLabel = new Label(LanguageManager.getText("col.id") + ": " + book.getId());
        idLabel.getStyleClass().add("id-label");

        // Tên sách
        Label nameLabel = new Label(book.getName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(140);
        nameLabel.getStyleClass().add("book-name-label");

        // Tác giả (Dùng key col.author thay vì chữ cứng "TG")
        Label authorLabel = new Label(LanguageManager.getText("col.author") + ": " + book.getAuthor());
        authorLabel.getStyleClass().add("book-author-label");

        // Năm (Dùng key col.year)
        Label yearLabel = new Label(LanguageManager.getText("col.year") + ": " + book.getYear());
        yearLabel.getStyleClass().add("year-label");

        // Trạng thái (CÓ SẴN / ĐÃ MƯỢN)
        Label statusLabel = new Label(book.isStatus()
                ? LanguageManager.getText("status.available")
                : LanguageManager.getText("status.borrowed"));
        statusLabel.getStyleClass().add(book.isStatus() ? "available-status" : "borrowed-status");

        bookBox.getChildren().addAll(
                imageView,
                new Separator(),
                idLabel,
                nameLabel,
                authorLabel,
                yearLabel,
                statusLabel
        );
        return bookBox;
    }
}