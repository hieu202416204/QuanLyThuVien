package FrontEnd.Controllers;

import BackEnd.LibraryQ.Library;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import FrontEnd.Views.UserTabView;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

import static FrontEnd.LibraryApp.ADMIN_PASSWORD;

/**
 * CONTROLLER: Quản lý Người Dùng (TỐI ƯU HIỆU SUẤT CAO - SERVER SIDE PAGINATION)
 */
public class UserTabController {

    private final Library library;
    private final UserTabView view;

    // Bỏ list userData để tiết kiệm RAM. Chỉ lưu số dòng/trang.
    private static final int USER_PAGE_SIZE = 100;
    private File selectedUserAvatar = null;

    public UserTabController(Library library, UserTabView view) {
        this.library = library;
        this.view = view;

        attachEvents();
        refreshData();
    }

    private void attachEvents() {
        // Nút chức năng
        view.getAddBtn().setOnAction(e -> handleAddUser());
        view.getEditBtn().setOnAction(e -> handleEditUser());
        view.getDeleteBtn().setOnAction(e -> handleDeleteUser());
        view.getClearBtn().setOnAction(e -> clearFields());
        view.getImportUserBtn().setOnAction(e -> handleImportUsers());
        view.getBtnCreateCard().setOnAction(e -> handleCreateCard());

        // Avatar
        view.getBtnAvatar().setOnAction(e -> handleSelectAvatar());

        // Tìm kiếm
        view.getSearchIdBtn().setOnAction(e -> handleSearchById());
        view.getSearchNameBtn().setOnAction(e -> handleSearchByName());
        view.getClearSearchBtn().setOnAction(e -> {
            view.getSearchField().clear();
            refreshData();
        });

        // Phân trang
        view.getUserPagination().setPageFactory(this::createPage);

        // Lắng nghe chọn dòng trong Table
        view.getUserTable().getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSel = newVal != null;
            view.getEditBtn().setDisable(!hasSel);
            view.getDeleteBtn().setDisable(!hasSel);
            view.getAddBtn().setDisable(hasSel);
            view.getBtnCreateCard().setDisable(!hasSel);

            if (hasSel) {
                view.getIdField().setText(newVal.getId());
                view.getNameField().setText(newVal.getName());
                view.getEmailField().setText(newVal.getEmail());
                view.getPersonalIdField().setText(newVal.getPersonalIdNumber());
                view.getIdField().setEditable(false);
            } else {
                clearFields();
            }
        });
    }

    // ========================================================
    // LOGIC TẢI DỮ LIỆU TỐI ƯU (SERVER-SIDE PAGINATION)
    // ========================================================

    public void refreshData() {
        // 1. Chỉ lấy TỔNG SỐ để tính trang (Mất 0.001s)
        int totalUsers = library.getUserDAO().getTotalUserCount();
        int pageCount = (int) Math.ceil((double) totalUsers / USER_PAGE_SIZE);
        view.getUserPagination().setPageCount(pageCount > 0 ? pageCount : 1);

        // 2. Load trang hiện tại
        int currentPage = view.getUserPagination().getCurrentPageIndex();
        if (currentPage >= pageCount) {
            view.getUserPagination().setCurrentPageIndex(0);
        } else {
            updateTablePage(currentPage);
        }
    }

    private Node createPage(int pageIndex) {
        updateTablePage(pageIndex);
        return new VBox();
    }

    private void updateTablePage(int pageIndex) {
        // Hiển thị trạng thái đang tải
        view.getUserTable().getItems().clear();
        view.getUserTable().setPlaceholder(new Label(LanguageManager.getText("msg.loading")));

        int offset = pageIndex * USER_PAGE_SIZE;

        // Chạy ngầm việc query Database để không làm đơ giao diện
        Task<List<User>> loadTask = new Task<>() {
            @Override
            protected List<User> call() {
                // Chỉ lấy đúng 100 User cho trang này
                return library.getUserDAO().getUsersByPage(offset, USER_PAGE_SIZE);
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<User> users = loadTask.getValue();
            if (users.isEmpty()) {
                view.getUserTable().setPlaceholder(new Label("No users found."));
            } else {
                view.getUserTable().setItems(FXCollections.observableArrayList(users));
            }
        });

        new Thread(loadTask).start();
    }

    // ========================================================
    // TÌM KIẾM BẰNG DATABASE (KHÔNG DÙNG RAM)
    // ========================================================

    private void handleSearchById() {
        String id = view.getSearchField().getText().trim();
        if(id.isEmpty()) return;

        User u = library.getUserDAO().getUserById(id);
        if (u != null) {
            view.getUserTable().setItems(FXCollections.observableArrayList(u));
            view.getUserPagination().setPageCount(1); // Ẩn phân trang khi đang search
        } else {
            view.getUserTable().getItems().clear();
            view.getUserTable().setPlaceholder(new Label("User not found."));
            view.getUserPagination().setPageCount(1);
        }
    }

    private void handleSearchByName() {
        String name = view.getSearchField().getText().trim();
        if(name.isEmpty()) return;

        view.getUserTable().getItems().clear();
        view.getUserTable().setPlaceholder(new Label(LanguageManager.getText("msg.searching")));

        Task<List<User>> searchTask = new Task<>() {
            @Override
            protected List<User> call() {
                return library.getUserDAO().searchUsersByName(name);
            }
        };

        searchTask.setOnSucceeded(e -> {
            List<User> result = searchTask.getValue();
            view.getUserTable().setItems(FXCollections.observableArrayList(result));
            view.getUserPagination().setPageCount(1); // Ẩn phân trang khi đang search
            if (result.isEmpty()) view.getUserTable().setPlaceholder(new Label("No match found."));
        });

        new Thread(searchTask).start();
    }

    // ========================================================
    // CÁC HÀM CRUD KHÁC (GIỮ NGUYÊN LOGIC)
    // ========================================================

    private void handleAddUser() {
        String uId = view.getIdField().getText().trim();
        String uName = view.getNameField().getText().trim();

        if (uId.isEmpty() || uName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, LanguageManager.getText("msg.error"), LanguageManager.getText("msg.missing_input"));
            return;
        }

        User newUser = new User(uId, uName, view.getPersonalIdField().getText().trim(), view.getEmailField().getText().trim(), null);

        if (selectedUserAvatar != null) {
            try {
                String savedName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUserAvatar);
                newUser.setAvatarPath(savedName);
            } catch (Exception e) {}
        }

        if (library.getUserDAO().addUser(newUser)) {
            refreshData();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), "User added.");
        } else {
            showAlert(Alert.AlertType.ERROR, LanguageManager.getText("msg.error"), "Duplicate ID or Database Error.");
        }
    }

    private void handleEditUser() {
        User selected = view.getUserTable().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        selected.setName(view.getNameField().getText().trim());
        selected.setEmail(view.getEmailField().getText().trim());
        selected.setPersonalIdNumber(view.getPersonalIdField().getText().trim());

        if (selectedUserAvatar != null) {
            try {
                String newName = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUserAvatar);
                if (selected.getAvatarPath() != null) BackEnd.Utils.ImageCache.remove(selected.getAvatarPath());
                selected.setAvatarPath(newName);
            } catch (Exception e) {}
        }

        if (library.getUserDAO().updateUser(selected)) {
            refreshData();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.update_success"));
        }
    }

    private void handleDeleteUser() {
        User selected = view.getUserTable().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.getSoSachDaMuon() > 0) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Cannot delete user who is currently borrowing books.");
            return;
        }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Admin Password Required");
        dlg.setContentText("Enter Admin Password to delete user:");

        if (dlg.showAndWait().orElse("").equals(ADMIN_PASSWORD)) {
            if (library.getUserDAO().deleteUser(selected.getId())) {
                refreshData();
                clearFields();
                showAlert(Alert.AlertType.INFORMATION, LanguageManager.getText("msg.success"), LanguageManager.getText("msg.delete_success"));
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Wrong password.");
        }
    }

    private void handleSelectAvatar() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            selectedUserAvatar = f;
            view.getLblAvatarStatus().setText("Selected: " + f.getName());
        }
    }

    private void handleImportUsers() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            List<User> users = BackEnd.Utils.ImportUtil.importUsersFromCSV(f);
            int count = 0;
            for (User u : users) {
                if (library.getUserDAO().addUser(u)) count++;
            }
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Import", "Imported: " + count + " users.");
        }
    }

    private void handleCreateCard() {
        User u = view.getUserTable().getSelectionModel().getSelectedItem();
        if (u == null) return;
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("LibraryCard_" + u.getId() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(null);
        if (f != null) {
            FrontEnd.CardGenerator.saveCardToPDF(u, f, selectedUserAvatar);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Card saved.");
        }
    }

    private void clearFields() {
        view.getIdField().clear();
        view.getNameField().clear();
        view.getEmailField().clear();
        view.getPersonalIdField().clear();
        view.getIdField().setEditable(true);
        selectedUserAvatar = null;
        view.getLblAvatarStatus().setText(LanguageManager.getText("label.not_selected"));
        view.getUserTable().getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}