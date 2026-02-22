package FrontEnd.Controllers;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.BookGalleryTabView;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.Function;

/**
 * CONTROLLER: Quản lý logic tính toán trang, tải ảnh bất đồng bộ (Thread).
 */
public class BookGalleryTabController {

    private final Library library;
    private final BookGalleryTabView view;
    private final Function<BufferedImage, Image> resizeConverter;

    // Data Cache
    private List<Book> cachedBooks;
    private static final int ITEMS_PER_PAGE = 24;

    public BookGalleryTabController(Library library, BookGalleryTabView view, Function<BufferedImage, Image> resizeConverter) {
        this.library = library;
        this.view = view;
        this.resizeConverter = resizeConverter;

        // 1. Gắn sự kiện đổi trang
        this.view.getPagination().setPageFactory(this::handlePageChange);

        // 2. Tải dữ liệu lần đầu
        updateBookGallery();
    }

    // =============================================================
    // LOGIC CẬP NHẬT DỮ LIỆU CHÍNH
    // =============================================================
    public void updateBookGallery() {
        this.cachedBooks = library.getBooks();

        if (cachedBooks == null || cachedBooks.isEmpty()) {
            view.getGalleryFlowPane().getChildren().clear();
            view.getGalleryFlowPane().getChildren().add(new Label(LanguageManager.getText("msg.library_empty")));
            view.getPagination().setVisible(false);
            return;
        }

        int pageCount = (int) Math.ceil((double) cachedBooks.size() / ITEMS_PER_PAGE);
        view.getPagination().setPageCount(pageCount);
        view.getPagination().setVisible(true);

        view.getPagination().setCurrentPageIndex(0);
        if (view.getPagination().getCurrentPageIndex() == 0) {
            updateGalleryView(0);
        }
    }

    private Node handlePageChange(int pageIndex) {
        if (cachedBooks != null && !cachedBooks.isEmpty()) {
            updateGalleryView(pageIndex);
        }
        return new VBox(); // Dummy node
    }

    private void updateGalleryView(int pageIndex) {
        Platform.runLater(() -> {
            view.getGalleryFlowPane().getChildren().clear();
            view.getMainScrollPane().setVvalue(0); // Cuộn lên đầu trang

            int start = pageIndex * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, cachedBooks.size());

            // 1. Tạo khung xương (Skeleton) với ảnh Placeholder
            for (int i = start; i < end; i++) {
                Book book = cachedBooks.get(i);
                VBox card = view.createBookCard(book, null); // Ảnh null sẽ dùng placeholder
                view.getGalleryFlowPane().getChildren().add(card);
            }

            // 2. Load ảnh thật ở chế độ nền
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
                List<Node> currentCards = view.getGalleryFlowPane().getChildren();
                int size = currentCards.size();

                for (int i = 0; i < size; i++) {
                    if (isCancelled()) break;

                    int bookIndex = start + i;
                    if (bookIndex >= cachedBooks.size()) break;

                    Book book = cachedBooks.get(bookIndex);

                    // Lấy ảnh thật (hoặc ảnh mặc định nếu lỗi)
                    Image img = loadImageForBook(book, view.getDefaultImageUrl());

                    // Cập nhật lại giao diện (Đẩy vào UI Thread)
                    int finalI = i;
                    Platform.runLater(() -> {
                        if (finalI < currentCards.size()) {
                            Node node = currentCards.get(finalI);
                            if (node instanceof VBox card && !card.getChildren().isEmpty()) {
                                if (card.getChildren().get(0) instanceof ImageView viewImg) {
                                    viewImg.setImage(img); // Thay ảnh
                                }
                            }
                        }
                    });

                    Thread.sleep(5); // Nghỉ siêu ngắn để CPU không bị treo
                }
                return null;
            }
        };

        Thread thread = new Thread(loadImagesTask);
        thread.setDaemon(true);
        thread.start();
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
}