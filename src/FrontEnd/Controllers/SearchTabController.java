package FrontEnd.Controllers;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.SearchTabView;
import XuLiAnh.ImageResizer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CONTROLLER: Quản lý logic tìm kiếm ngầm, phân trang, và xử lý ảnh đệm (Cache).
 */
public class SearchTabController {

    private final Library library;
    private final SearchTabView view;

    // --- STATE & CACHE ---
    private List<Book> currentSearchResults = new ArrayList<>();
    private static final int PAGE_SIZE = 50;
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    public SearchTabController(Library library, SearchTabView view) {
        this.library = library;
        this.view = view;

        attachEvents();
    }

    private void attachEvents() {
        // Nút Tìm kiếm
        view.getBtnSearch().setOnAction(e -> handleSearch(view.getSearchField().getText()));

        // Nút Xóa/Reset
        view.getResetBtn().setOnAction(e -> handleReset());

        // Thanh phân trang (Mẹo trả về dummy node)
        view.getPagination().setPageFactory(pageIndex -> {
            if (currentSearchResults.isEmpty()) return null;
            updateGallery(pageIndex);
            return new VBox();
        });
    }

    // =====================================================
    // LOGIC TÌM KIẾM
    // =====================================================
    private void handleSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;

        view.getResultLabel().setText(LanguageManager.getText("msg.searching"));
        view.getPagination().setVisible(false);
        view.getSearchGalleryPane().getChildren().clear();

        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() {
                return library.getBookDAO().searchByKeyword(query);
            }
        };

        searchTask.setOnSucceeded(e -> {
            currentSearchResults = searchTask.getValue();
            if (currentSearchResults.isEmpty()) {
                view.getResultLabel().setText(LanguageManager.getText("msg.no_match"));
                view.getPagination().setVisible(false);
            } else {
                view.getResultLabel().setText(
                        String.format(LanguageManager.getText("msg.search_result"),
                                currentSearchResults.size(), query)
                );

                int pageCount = (int) Math.ceil((double) currentSearchResults.size() / PAGE_SIZE);
                view.getPagination().setPageCount(pageCount);
                view.getPagination().setCurrentPageIndex(0);
                view.getPagination().setVisible(true);
            }
        });

        new Thread(searchTask).start();
    }

    // =====================================================
    // LOGIC CẬP NHẬT GIAO DIỆN
    // =====================================================
    private void updateGallery(int pageIndex) {
        Platform.runLater(() -> {
            view.getSearchGalleryPane().getChildren().clear();
            view.getMainScrollPane().setVvalue(0); // Cuộn lên đầu

            int from = pageIndex * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, currentSearchResults.size());

            if (from >= currentSearchResults.size()) return;

            List<Node> newNodes = new ArrayList<>();
            for (Book book : currentSearchResults.subList(from, to)) {
                // Lấy ảnh từ Cache hoặc tải mới
                Image image = imageCache.computeIfAbsent(book.getId(), id -> loadImage(book, view.getDefaultImgUrl()));
                // Gọi View vẽ thẻ sách
                newNodes.add(view.createBookBox(book, image));
            }

            view.getSearchGalleryPane().getChildren().addAll(newNodes);
        });
    }

    private void handleReset() {
        currentSearchResults.clear();
        view.getSearchGalleryPane().getChildren().clear();
        view.getPagination().setVisible(false);
        view.getSearchField().clear();
        view.getResultLabel().setText("");
    }

    // =====================================================
    // IMAGE LOADING HELPER
    // =====================================================
    private Image loadImage(Book book, String defaultImgUrl) {
        try {
            if (book.getImagePath() != null) {
                File f = BackEnd.Utils.FileUtil.getLocalFile(book.getImagePath());
                if (f.exists()) {
                    BufferedImage buf = ImageIO.read(f);
                    return convertAndResize(buf);
                }
            }
        } catch (Exception ignored) {}
        return new Image(defaultImgUrl);
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
}