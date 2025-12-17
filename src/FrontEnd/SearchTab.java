package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import XuLiAnh.ImageResizer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SearchTab extends VBox {

    private final Library library;

    // 1. CÁC THÀNH PHẦN CỐ ĐỊNH (STATIC UI)
    private final Pagination pagination;
    private final ScrollPane mainScrollPane;
    private final FlowPane searchGalleryPane;

    private List<Book> currentSearchResults = new ArrayList<>();
    private static final int PAGE_SIZE = 50; // Giảm xuống 50 để load nhanh hơn

    // Cache ảnh
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final String PLACEHOLDER_IMG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";

    public SearchTab(Library library) {
        this.library = library;

        // Khởi tạo các thành phần UI một lần duy nhất
        this.searchGalleryPane = new FlowPane();
        this.mainScrollPane = new ScrollPane();
        this.pagination = new Pagination();

        initUI();
    }

    private void initUI() {
        setPadding(new Insets(10));
        setSpacing(10);

        // --- 1. SEARCH BAR ---
        TextField searchField = new TextField();
        searchField.setPrefWidth(300);
        searchField.setPromptText(LanguageManager.getText("label.search_ready")); // Placeholder

        Label resultLabel = new Label("");

        Button btnSearch = new Button(LanguageManager.getText("btn.search_combined"));
        btnSearch.setOnAction(e -> handleSearch(searchField.getText(), resultLabel));

        Button resetBtn = new Button(LanguageManager.getText("btn.clear_result"));
        resetBtn.setOnAction(e -> reset(resultLabel, searchField));

        ToolBar bar = new ToolBar(searchField, btnSearch, resetBtn);

        // --- 2. MAIN CONTENT AREA (SCROLL PANE) ---
        // Cấu hình FlowPane
        searchGalleryPane.setHgap(15);
        searchGalleryPane.setVgap(15);
        searchGalleryPane.setPadding(new Insets(10));

        // BINDING CHIỀU RỘNG: Khắc phục lỗi trượt ngang
        searchGalleryPane.prefWrapLengthProperty().bind(mainScrollPane.widthProperty().subtract(25));

        // Cấu hình ScrollPane
        mainScrollPane.setContent(searchGalleryPane);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Chặn trượt ngang
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Quan trọng: Để ScrollPane chiếm hết không gian còn lại
        VBox.setVgrow(mainScrollPane, Priority.ALWAYS);

        // --- 3. PAGINATION (CHỈ LÀM THANH ĐIỀU HƯỚNG) ---
        pagination.setVisible(false);
        pagination.setMaxHeight(60); // Giới hạn chiều cao để không chiếm chỗ

        // Đây là mẹo: PageFactory trả về null hoặc node rỗng vì ta không dùng vùng hiển thị của Pagination
        // Ta chỉ dùng số trang của nó thôi.
        pagination.setPageFactory(pageIndex -> {
            if (currentSearchResults.isEmpty()) return null;
            updateGallery(pageIndex); // Gọi hàm update thủ công
            return new VBox(); // Trả về box rỗng để Pagination không báo lỗi
        });

        // Add tất cả vào VBox chính
        getChildren().addAll(resultLabel, bar, mainScrollPane, pagination);
    }

    // =====================================================
    // LOGIC TÌM KIẾM
    // =====================================================
    private void handleSearch(String query, Label resultLabel) {
        if (query == null || query.trim().isEmpty()) return;

        resultLabel.setText(LanguageManager.getText("msg.searching"));
        pagination.setVisible(false);
        searchGalleryPane.getChildren().clear(); // Xóa cũ ngay lập tức

        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() {
                // Giả lập delay nhỏ nếu cần test loading
                // try { Thread.sleep(500); } catch (InterruptedException e) {}
                return library.getBookDAO().searchByKeyword(query);
            }
        };

        searchTask.setOnSucceeded(e -> {
            currentSearchResults = searchTask.getValue();
            if (currentSearchResults.isEmpty()) {
                resultLabel.setText(LanguageManager.getText("msg.no_match"));
                pagination.setVisible(false);
            } else {
                resultLabel.setText(
                        String.format(LanguageManager.getText("msg.search_result"),
                                currentSearchResults.size(), query)
                );

                // Tính số trang
                int pageCount = (int) Math.ceil((double) currentSearchResults.size() / PAGE_SIZE);
                pagination.setPageCount(pageCount);
                pagination.setCurrentPageIndex(0); // Sẽ tự trigger updateGallery(0)
                pagination.setVisible(true);
            }
        });

        new Thread(searchTask).start();
    }

    // =====================================================
    // UPDATE GALLERY (KHÔNG TẠO MỚI SCROLLPANE)
    // =====================================================
    private void updateGallery(int pageIndex) {
        // Chạy trên UI Thread để an toàn
        Platform.runLater(() -> {
            searchGalleryPane.getChildren().clear(); // Xóa sách cũ trong FlowPane

            // Kéo ScrollPane lên đầu mỗi khi chuyển trang
            mainScrollPane.setVvalue(0);

            int from = pageIndex * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, currentSearchResults.size());

            if (from >= currentSearchResults.size()) return;

            URL defaultUrl = getClass().getResource("/resources/default_cover.png");
            final String DEFAULT_IMG = defaultUrl != null ? defaultUrl.toExternalForm() : PLACEHOLDER_IMG;

            List<Node> newNodes = new ArrayList<>();
            for (Book book : currentSearchResults.subList(from, to)) {
                newNodes.add(createBookBox(book, DEFAULT_IMG));
            }

            // Add một lần cho hiệu năng cao
            searchGalleryPane.getChildren().addAll(newNodes);
        });
    }

    // =====================================================
    // UI BOOK ITEM (GIỮ NGUYÊN)
    // =====================================================
    private VBox createBookBox(Book book, String defaultImg) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setPrefWidth(150);
        box.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-background-radius: 5;");

        // Lấy ảnh từ Cache
        Image image = imageCache.computeIfAbsent(book.getId(), id -> loadImage(book, defaultImg));

        ImageView iv = new ImageView(image);
        iv.setFitWidth(120);
        iv.setFitHeight(160);
        iv.setPreserveRatio(true);

        Label id = new Label("ID: " + book.getId());
        id.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

        Label name = new Label(book.getName());
        name.setWrapText(true);
        name.setMaxWidth(130);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        name.setMinHeight(35); // Cố định chiều cao tên để thẳng hàng

        Label status = new Label(book.isStatus() ? LanguageManager.getText("status.available") : LanguageManager.getText("status.borrowed"));
        status.setStyle(book.isStatus() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

        box.getChildren().addAll(iv, new Separator(), id, name, status);
        return box;
    }

    // =====================================================
    // IMAGE LOADING HELPER
    // =====================================================
    private Image loadImage(Book book, String defaultImg) {
        try {
            if (book.getImagePath() != null) {
                File f = BackEnd.Utils.FileUtil.getLocalFile(book.getImagePath());
                if (f.exists()) {
                    BufferedImage buf = ImageIO.read(f);
                    return convertAndResize(buf);
                }
            }
        } catch (Exception ignored) {}
        return new Image(defaultImg);
    }

    private Image convertAndResize(BufferedImage original) {
        try {
            BufferedImage resized = ImageResizer.resizeImage(original, 120, 160);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", os);
            return new Image(new ByteArrayInputStream(os.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    private void reset(Label resultLabel, TextField field) {
        currentSearchResults.clear();
        searchGalleryPane.getChildren().clear();
        pagination.setVisible(false);
        field.clear();
        resultLabel.setText("");
    }
}