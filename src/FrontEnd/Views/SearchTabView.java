package FrontEnd.Views;

import BackEnd.Book.Book;
import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;

/**
 * VIEW: Chỉ đảm nhận việc vẽ giao diện tìm kiếm và hiển thị lưới sách.
 */
public class SearchTabView extends VBox {

    // --- CÁC THÀNH PHẦN GIAO DIỆN CHÍNH ---
    private final Pagination pagination;
    private final ScrollPane mainScrollPane;
    private final FlowPane searchGalleryPane;

    // --- THANH TÌM KIẾM ---
    private TextField searchField;
    private Label resultLabel;
    private Button btnSearch;
    private Button resetBtn;

    // --- TÀI NGUYÊN ẢNH ---
    private static final String PLACEHOLDER_IMG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAQAAAB1z2qZAAABV0lEQVR42u3TQQ0AIAwEsf/uRgzI+HAKW97bA0BMA0D8gEAAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAASBAmIAEQAIgERgIhARAYAMAgP4V70XgWqQAAAAASUVORK5CYII=";
    private String defaultImgUrl;

    public SearchTabView() {
        this.searchGalleryPane = new FlowPane();
        this.mainScrollPane = new ScrollPane();
        this.pagination = new Pagination();

        // Thiết lập URL mặc định
        URL defaultUrl = getClass().getResource("/resources/default_cover.png");
        this.defaultImgUrl = defaultUrl != null ? defaultUrl.toExternalForm() : PLACEHOLDER_IMG;

        initUI();
    }

    private void initUI() {
        setPadding(new Insets(10));
        setSpacing(10);

        // --- 1. SEARCH BAR ---
        searchField = new TextField();
        searchField.setPrefWidth(300);
        searchField.setPromptText(LanguageManager.getText("label.search_ready"));

        resultLabel = new Label("");

        btnSearch = new Button(LanguageManager.getText("btn.search_combined"));
        resetBtn = new Button(LanguageManager.getText("btn.clear_result"));

        ToolBar bar = new ToolBar(searchField, btnSearch, resetBtn);

        // --- 2. MAIN CONTENT AREA (SCROLL PANE) ---
        searchGalleryPane.setHgap(15);
        searchGalleryPane.setVgap(15);
        searchGalleryPane.setPadding(new Insets(10));

        // BINDING CHIỀU RỘNG: Khắc phục lỗi trượt ngang
        searchGalleryPane.prefWrapLengthProperty().bind(mainScrollPane.widthProperty().subtract(25));

        mainScrollPane.setContent(searchGalleryPane);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Chặn trượt ngang
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox.setVgrow(mainScrollPane, Priority.ALWAYS);

        // --- 3. PAGINATION ---
        pagination.setVisible(false);
        pagination.setMaxHeight(60);

        // Add tất cả vào VBox chính
        getChildren().addAll(resultLabel, bar, mainScrollPane, pagination);
    }

    /**
     * Hàm vẽ 1 ô sách (Truyền ảnh đã xử lý từ Controller vào)
     */
    public VBox createBookBox(Book book, Image image) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setPrefWidth(150);
        box.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-background-radius: 5;");

        // Nếu Controller không lấy được ảnh, dùng ảnh mặc định
        if (image == null) image = new Image(defaultImgUrl);

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

    // ==========================================
    // GETTERS CHO CONTROLLER
    // ==========================================
    public Pagination getPagination() { return pagination; }
    public ScrollPane getMainScrollPane() { return mainScrollPane; }
    public FlowPane getSearchGalleryPane() { return searchGalleryPane; }
    public TextField getSearchField() { return searchField; }
    public Label getResultLabel() { return resultLabel; }
    public Button getBtnSearch() { return btnSearch; }
    public Button getResetBtn() { return resetBtn; }
    public String getDefaultImgUrl() { return defaultImgUrl; }
}