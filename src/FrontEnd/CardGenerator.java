package FrontEnd;

import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class CardGenerator {

    private static final double CARD_WIDTH = 600;
    private static final double CARD_HEIGHT = 380;

    /**
     * Tạo giao diện thẻ (Node)
     */
    public static Pane createCardView(User user, String libraryName, File overrideAvatar) {
        // 1. Nền thẻ
        StackPane cardRoot = new StackPane();
        cardRoot.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        cardRoot.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        cardRoot.setMinSize(CARD_WIDTH, CARD_HEIGHT); // Bắt buộc kích thước tối thiểu để không bị co

        cardRoot.setStyle("-fx-background-color: linear-gradient(to bottom right, #004e92, #000428); " +
                "-fx-background-radius: 20; -fx-border-radius: 20; " +
                "-fx-border-color: #ffffff55; -fx-border-width: 2;");

        // 2. Nội dung chính
        HBox contentBox = new HBox(25);
        contentBox.setPadding(new Insets(25));
        contentBox.setAlignment(Pos.CENTER_LEFT);

        // --- CỘT TRÁI: ẢNH ---
        VBox leftBox = new VBox();
        leftBox.setAlignment(Pos.CENTER);

        StackPane photoFrame = new StackPane();
        photoFrame.setPrefSize(160, 210);
        photoFrame.setMaxSize(160, 210);
        photoFrame.setStyle("-fx-border-color: white; -fx-border-width: 4; -fx-background-color: #ecf0f1; -fx-border-radius: 5;");

        ImageView avatarView = new ImageView();

        // Xử lý ảnh (Dùng FileInputStream để tránh lỗi đường dẫn và cache)
        try {
            File fileToLoad = null;
            if (overrideAvatar != null && overrideAvatar.exists()) {
                fileToLoad = overrideAvatar;
            } else {
                String dbPath = user.getAvatarPath();
                if (dbPath != null && !dbPath.isEmpty()) {
                    File checkFile = new File(dbPath);
                    if (checkFile.isAbsolute() && checkFile.exists()) fileToLoad = checkFile;
                    else fileToLoad = BackEnd.Utils.FileUtil.getLocalFile(dbPath);
                }
            }

            if (fileToLoad != null && fileToLoad.exists()) {
                try (FileInputStream fis = new FileInputStream(fileToLoad)) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(fis, 150, 200, true, true);
                    if (!image.isError()) avatarView.setImage(image);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi load ảnh: " + e.getMessage());
        }

        avatarView.setFitWidth(150);
        avatarView.setFitHeight(200);
        avatarView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(150, 200);
        clip.setArcWidth(5);
        clip.setArcHeight(5);
        avatarView.setClip(clip);

        photoFrame.getChildren().add(avatarView);
        leftBox.getChildren().add(photoFrame);

        // --- CỘT PHẢI: THÔNG TIN ---
        VBox rightBox = new VBox(8);
        rightBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        Label lblLibName = new Label(libraryName.toUpperCase());
        lblLibName.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblLibName.setTextFill(Color.LIGHTBLUE);

        Separator sep = new Separator();
        sep.setMaxWidth(250);

        Text txtName = new Text(user.getName().toUpperCase());
        txtName.setFont(Font.font("Arial", FontWeight.BOLD, 34));
        txtName.setFill(Color.WHITE);
        txtName.setWrappingWidth(350);

        Label lblRole = new Label(LanguageManager.getText("label.member_role"));
        lblRole.setStyle("-fx-background-color: #f39c12; -fx-text-fill: #ffffff; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");

        Label lblIdTitle = new Label("MEMBER ID:");
        lblIdTitle.setTextFill(Color.LIGHTGRAY);
        lblIdTitle.setFont(Font.font(12));

        Label lblId = new Label(user.getId().toUpperCase());
        lblId.setFont(Font.font("Monospaced", FontWeight.BOLD, 28));
        lblId.setTextFill(Color.WHITE);
        lblId.setStyle("-fx-border-color: #ffffff55; -fx-border-width: 1; -fx-padding: 2 10; -fx-border-radius: 5;");

        Label lblEmail = new Label(user.getEmail() != null ? user.getEmail() : "");
        lblEmail.setFont(Font.font("Arial", 14));
        lblEmail.setTextFill(Color.LIGHTGRAY);
        WritableImage barcodeImage = BarcodeHelper.createBarcode(user.getId(), 300, 60);

        ImageView barcodeView = new ImageView(barcodeImage);

        // Vì barcode màu đen mà nền thẻ tối, ta cần lót một tấm nền trắng bên dưới barcode
        StackPane barcodeContainer = new StackPane(barcodeView);
        barcodeContainer.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-background-radius: 5;");
        barcodeContainer.setMaxWidth(310); // Lớn hơn ảnh 1 chút để tạo viền
        barcodeContainer.setAlignment(Pos.CENTER_LEFT); // Căn lề

        // Thêm vào box bên phải
        rightBox.getChildren().addAll(lblLibName, sep, txtName, lblRole, new Label(""), lblIdTitle, lblId, lblEmail,
                new Label(""), barcodeContainer); // Thêm barcodeContainer vào cuối

        contentBox.getChildren().addAll(leftBox, rightBox);
        cardRoot.getChildren().add(contentBox);

        return cardRoot;
    }

    /**
     * Xuất thẻ ra file PDF (Sửa lỗi nền đen & Đổi định dạng)
     */
    public static void saveCardToPDF(User user, File outputFile, File overrideAvatar) {
        String libName = LanguageManager.getText("label.library_name");

        // 1. Tạo Node thẻ
        Pane cardView = createCardView(user, libName, overrideAvatar);

        // JavaFX Node cần được nằm trong Scene và "layout" thì mới hiển thị đúng CSS/Màu sắc
        Scene dummyScene = new Scene(cardView);
        cardView.applyCss();
        cardView.layout();

        // 2. Chụp ảnh thẻ thành Image JavaFX
        WritableImage fxImage = cardView.snapshot(new SnapshotParameters(), null);

        // 3. Tạo file PDF và nhúng ảnh vào
        try {
            // Chuyển đổi JavaFX Image -> BufferedImage -> Byte Array (PNG)
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(fxImage, null), "png", byteOutput);

            // Tạo PDF
            Document document = new Document(PageSize.A4); // Khổ giấy A4
            PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            // Tạo ảnh PDF từ Byte Array
            Image pdfImage = Image.getInstance(byteOutput.toByteArray());

            // Căn chỉnh ảnh vào giữa trang PDF
            // Tỷ lệ thẻ tín dụng chuẩn (8.6cm x 5.4cm) -> scale cho vừa đẹp
            pdfImage.scaleToFit(350, 250);
            pdfImage.setAlignment(Image.ALIGN_CENTER);

            document.add(pdfImage);
            document.close();

            System.out.println("Đã lưu PDF tại: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi xuất PDF: " + e.getMessage());
        }
    }
}