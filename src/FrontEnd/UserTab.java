package FrontEnd;

import BackEnd.LibraryQ.Library;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

import static FrontEnd.LibraryApp.ADMIN_PASSWORD;


public class UserTab extends VBox {
    private final Library library;
    private final TableView<User> userTable;
    private Pagination userPagination;
    private final ObservableList<User> userData = FXCollections.observableArrayList();
    private final int USER_PAGE_SIZE = 100;

    // Input Fields
    private TextField idField, nameField, emailField, personalIdField;
    private File selectedUserAvatar = null;
    private Label lblAvatarStatus;

    // Constants
    public UserTab(Library library) {
        this.library = library;
        this.userTable = new TableView<>();
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(15));
        this.setSpacing(10);

        // 1. Search Bar
        HBox searchBar = createSearchBar();

        // 2. Table & Pagination
        setupTableColumns();
        userPagination = new Pagination();
        userPagination.setPageFactory(this::createPage);

        // 3. Input Panel (Add/Edit)
        FlowPane inputPane = createInputPanel();

        // 4. Action Buttons
        HBox actionBar = createActionBar();

        this.getChildren().addAll(searchBar, userTable, userPagination, inputPane, actionBar);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        // Load data initial
        refreshData();
    }

    private HBox createSearchBar() {
        TextField searchUserField = new TextField();
        searchUserField.setPromptText("Search...");

        Button searchIdBtn = new Button(LanguageManager.getText("btn.search_id"));
        searchIdBtn.setOnAction(e -> handleSearchUserById(searchUserField.getText()));

        Button searchNameBtn = new Button(LanguageManager.getText("btn.search_name"));
        searchNameBtn.setOnAction(e -> handleSearchUserByName(searchUserField.getText()));

        Button clearSearchBtn = new Button(LanguageManager.getText("btn.clear_search"));
        clearSearchBtn.setOnAction(e -> {
            refreshData();
            searchUserField.clear();
        });

        HBox box = new HBox(10, searchUserField, searchIdBtn, searchNameBtn, clearSearchBtn);
        box.getStyleClass().add("input-panel");
        return box;
    }

    private void setupTableColumns() {
        TableColumn<User, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colId.setPrefWidth(100);

        TableColumn<User, String> colName = new TableColumn<>(LanguageManager.getText("col.user_name"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<User, String> colEmail = new TableColumn<>(LanguageManager.getText("col.email"));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colEmail.setPrefWidth(250);
        TableColumn<User, String> colPersonalIdNumber = new TableColumn<>(LanguageManager.getText("col.personalIdNumber"));
        colPersonalIdNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPersonalIdNumber()));
        colPersonalIdNumber.setPrefWidth(250);

        userTable.getColumns().setAll(colId, colName, colEmail, colPersonalIdNumber);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private FlowPane createInputPanel() {
        idField = new TextField(); idField.setPromptText(LanguageManager.getText("col.id")); idField.setPrefWidth(120);
        nameField = new TextField(); nameField.setPromptText(LanguageManager.getText("col.name.user")); nameField.setPrefWidth(200);
        emailField = new TextField(); emailField.setPromptText(LanguageManager.getText("col.email")); emailField.setPrefWidth(200);
        personalIdField = new TextField(); personalIdField.setPromptText(LanguageManager.getText("col.personalIdNumber")); personalIdField.setPrefWidth(200);
        Button btnAvatar = new Button("📷 Avatar");
        lblAvatarStatus = new Label(LanguageManager.getText("label.not_selected"));
        btnAvatar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
            File f = fc.showOpenDialog(null);
            if (f != null) { selectedUserAvatar = f; lblAvatarStatus.setText(LanguageManager.getText("label.selected")); }
        });

        FlowPane pane = new FlowPane(15, 10);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("input-panel");
        pane.getChildren().addAll(idField, nameField, emailField,personalIdField, new HBox(5, btnAvatar, lblAvatarStatus));
        return pane;
    }

    private HBox createActionBar() {
        Button addBtn = new Button(LanguageManager.getText("btn.add_user"));
        addBtn.setStyle("-fx-base: #27ae60; -fx-text-fill: white;");
        addBtn.setOnAction(e -> handleAddUser());

        Button editBtn = new Button("✏️ " + LanguageManager.getText("btn.edit"));
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> handleEditUser());

        Button deleteBtn = new Button(LanguageManager.getText("btn.delete_user"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> handleDeleteUser());

        Button clearBtn = new Button("🔄 " + LanguageManager.getText("btn.refresh"));
        clearBtn.setOnAction(e -> clearFields());

        Button importUserBtn = new Button("📥 Import CSV");
        importUserBtn.setOnAction(e -> handleImportUsers());

        Button btnCreateCard = new Button(LanguageManager.getText("btn.create_card"));
        btnCreateCard.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
        btnCreateCard.setDisable(true);
        btnCreateCard.setOnAction(e -> handleCreateCard());

        // Listener để bật/tắt nút khi chọn bảng
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSel = newVal != null;
            editBtn.setDisable(!hasSel);
            deleteBtn.setDisable(!hasSel);
            addBtn.setDisable(hasSel); // Tắt nút Add khi đang chọn (để tránh sửa ID)
            btnCreateCard.setDisable(!hasSel);

            if(hasSel) {
                idField.setText(newVal.getId());
                nameField.setText(newVal.getName());
                emailField.setText(newVal.getEmail());
                personalIdField.setText(newVal.getPersonalIdNumber());
                idField.setEditable(false);
            }
        });

        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(addBtn, importUserBtn, editBtn, deleteBtn, clearBtn, new Separator(javafx.geometry.Orientation.VERTICAL), btnCreateCard);
        return bar;
    }

    // --- LOGIC HANDLERS ---

    public void refreshData() {
        userData.setAll(library.getListUsers());
        updatePagination();
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) userData.size() / USER_PAGE_SIZE);
        userPagination.setPageCount(pageCount > 0 ? pageCount : 1);

        // Fix lỗi index nếu xóa hết data trang cuối
        int currentPage = userPagination.getCurrentPageIndex();
        if (currentPage >= pageCount) {
            userPagination.setCurrentPageIndex(0);
        } else {
            // Force update trang hiện tại
            updateTablePage(currentPage);
        }
    }

    private Node createPage(int pageIndex) {
        updateTablePage(pageIndex);
        return new VBox();
    }

    private void updateTablePage(int pageIndex) {
        int from = pageIndex * USER_PAGE_SIZE;
        int to = Math.min(from + USER_PAGE_SIZE, userData.size());
        if (from <= to && !userData.isEmpty()) {
            userTable.setItems(FXCollections.observableArrayList(userData.subList(from, to)));
        } else {
            userTable.getItems().clear();
        }
    }

    private void handleAddUser() {
        if (idField.getText().isEmpty() || nameField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", LanguageManager.getText("msg.user_missing_info"));
            return;
        }
        String uId = idField.getText().trim();
        String uName = nameField.getText().trim();
        String uEmail = emailField.getText().trim();
        String uPersonalId = personalIdField.getText().trim();
        String avatarPath = null;

        if (selectedUserAvatar != null) {
            try { avatarPath = BackEnd.Utils.FileUtil.saveImageToLocal(selectedUserAvatar); }
            catch (Exception ex) { ex.printStackTrace(); }
        }

        if (library.getUserDAO().addUser(new User(uId, uName, uEmail, avatarPath, uPersonalId))) {
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Success", LanguageManager.getText("msg.user_added"));
            if (!uEmail.isEmpty()) {
                new Thread(() -> BackEnd.Utils.EmailService.sendWelcomeEmail(library, uEmail, uName, uId)).start();
            }
            clearFields();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "ID Exists or DB Error.");
        }
    }

    private void handleEditUser() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u != null) {
            u.setName(nameField.getText());
            u.setEmail(emailField.getText());
            u.setPersonalIdNumber(personalIdField.getText());
            if (library.getUserDAO().updateUser(u)) {
                userTable.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User Updated");
                clearFields();
            }
        }
    }

    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if(selected == null) return;

        TextInputDialog passDlg = new TextInputDialog();
        passDlg.setTitle(LanguageManager.getText("title.confirm_delete"));
        passDlg.setContentText(LanguageManager.getText("content.enter_pass"));

        if (passDlg.showAndWait().orElse("").equals(ADMIN_PASSWORD)) {
            if(library.deleteUser(selected.getId())) {
                refreshData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User Deleted");
                clearFields();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", LanguageManager.getText("msg.wrong_pass"));
        }
    }

    private void handleSearchUserById(String id) {
        if(id.trim().isEmpty()) return;
        User u = library.searchUserById(id);
        if(u != null) {
            userTable.getSelectionModel().select(u);
            userTable.scrollTo(u);
        } else {
            showAlert(Alert.AlertType.WARNING, "Not Found", "User ID not found: " + id);
        }
    }

    private void handleSearchUserByName(String name) {
        if(name.trim().isEmpty()) return;
        List<User> list = library.searchUserByName(name);
        if(!list.isEmpty()) {
            userData.setAll(list);
            updatePagination();
        } else {
            showAlert(Alert.AlertType.WARNING, "Not Found", "No user matches: " + name);
        }
    }

    private void handleImportUsers() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showOpenDialog(null);
        if(f != null) {
            List<User> users = BackEnd.Utils.ImportUtil.importUsersFromCSV(f);
            int count = 0;
            for(User u : users) if(library.getUserDAO().addUser(u)) count++;
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Import", "Imported: " + count + " users.");
        }
    }

    private void handleCreateCard() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if(u == null) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(null);
        if(f != null) {
            FrontEnd.CardGenerator.saveCardToPDF(u, f, null);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Card saved.");
        }
    }

    private void clearFields() {
        idField.clear(); nameField.clear(); emailField.clear(); personalIdField.clear();
        idField.setEditable(true);
        selectedUserAvatar = null;
        lblAvatarStatus.setText(LanguageManager.getText("label.not_selected"));
        userTable.getSelectionModel().clearSelection();
        refreshData();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}