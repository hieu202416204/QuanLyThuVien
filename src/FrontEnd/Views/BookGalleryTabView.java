package FrontEnd.Views;

import BackEnd.Book.Book;
import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;

/**
 * VIEW: Chỉ vẽ giao diện lưới và cấu trúc thẻ sách.
 */
public class BookGalleryTabView extends VBox {
    // --- CÁC THÀNH PHẦN UI CỐ ĐỊNH ---
    private ScrollPane mainScrollPane;
    private FlowPane galleryFlowPane;
    private Pagination pagination;

    // Image Resources
    private static final String PLACEHOLDER_URL = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";
    private String defaultImageUrl;

    public BookGalleryTabView() {
        initUI();
    }

    private void initUI() {
        // Chuẩn bị URL ảnh mặc định
        URL resource = getClass().getResource("/resources/default_cover.png");
        if (resource == null) resource = getClass().getResource("/default_cover.png");
        this.defaultImageUrl = (resource != null) ? resource.toExternalForm() : PLACEHOLDER_URL;

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
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Ràng buộc chiều rộng để FlowPane xuống dòng đúng lúc
        galleryFlowPane.prefWrapLengthProperty().bind(mainScrollPane.widthProperty().subtract(40));

        VBox.setVgrow(mainScrollPane, Priority.ALWAYS);

        // 3. Pagination (Thanh điều hướng)
        pagination = new Pagination();
        pagination.setMaxHeight(60);

        // Add vào layout chính
        this.getChildren().addAll(mainScrollPane, pagination);
    }

    /**
     * Phương thức này thuần túy là vẽ UI cho 1 cuốn sách
     */
    public VBox createBookCard(Book book, Image image) {
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

    // --- GETTERS ---
    public FlowPane getGalleryFlowPane() { return galleryFlowPane; }
    public ScrollPane getMainScrollPane() { return mainScrollPane; }
    public Pagination getPagination() { return pagination; }
    public String getDefaultImageUrl() { return defaultImageUrl; }
}