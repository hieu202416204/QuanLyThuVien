package FrontEnd.Views;

import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * VIEW: Chỉ đảm nhận việc vẽ giao diện quản lý người dùng.
 */
public class UserTabView extends VBox {

    // --- CÁC THÀNH PHẦN GIAO DIỆN (Cần cung cấp getter cho Controller) ---
    private TableView<User> userTable;
    private Pagination userPagination;

    private TextField searchField;
    private Button searchIdBtn, searchNameBtn, clearSearchBtn;

    private TextField idField, nameField, emailField, personalIdField;
    private Button btnAvatar;
    private Label lblAvatarStatus;

    private Button addBtn, editBtn, deleteBtn, clearBtn, importUserBtn, btnCreateCard;

    public UserTabView() {
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(15));
        this.setSpacing(10);

        HBox searchBar = createSearchBar();
        setupTable();
        FlowPane inputPane = createInputPanel();
        HBox actionBar = createActionBar();

        this.getChildren().addAll(searchBar, userTable, userPagination, inputPane, actionBar);
        VBox.setVgrow(userTable, Priority.ALWAYS);
    }

    private HBox createSearchBar() {
        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchIdBtn = new Button(LanguageManager.getText("btn.search_id"));
        searchNameBtn = new Button(LanguageManager.getText("btn.search_name"));
        clearSearchBtn = new Button(LanguageManager.getText("btn.clear_search"));

        HBox box = new HBox(10, searchField, searchIdBtn, searchNameBtn, clearSearchBtn);
        box.getStyleClass().add("input-panel");
        return box;
    }

    private void setupTable() {
        userTable = new TableView<>();

        TableColumn<User, String> colId = new TableColumn<>(LanguageManager.getText("col.id"));
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));

        TableColumn<User, String> colName = new TableColumn<>(LanguageManager.getText("col.name.user"));
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

        TableColumn<User, String> colEmail = new TableColumn<>(LanguageManager.getText("col.email"));
        colEmail.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<User, String> colPersonalId = new TableColumn<>(LanguageManager.getText("col.personalIdNumber"));
        colPersonalId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPersonalIdNumber()));

        userTable.getColumns().addAll(colId, colName, colEmail, colPersonalId);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        userPagination = new Pagination();
    }

    private FlowPane createInputPanel() {
        idField = new TextField(); idField.setPromptText(LanguageManager.getText("col.id"));
        nameField = new TextField(); nameField.setPromptText(LanguageManager.getText("col.name.user"));
        emailField = new TextField(); emailField.setPromptText(LanguageManager.getText("col.email"));
        personalIdField = new TextField(); personalIdField.setPromptText(LanguageManager.getText("col.personalIdNumber"));

        btnAvatar = new Button("📷 Avatar");
        lblAvatarStatus = new Label(LanguageManager.getText("label.not_selected"));

        FlowPane pane = new FlowPane(15, 10);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("input-panel");
        pane.getChildren().addAll(
                new VBox(5, new Label(LanguageManager.getText("col.id")), idField),
                new VBox(5, new Label(LanguageManager.getText("col.name.user")), nameField),
                new VBox(5, new Label(LanguageManager.getText("col.email")), emailField),
                new VBox(5, new Label(LanguageManager.getText("col.personalIdNumber")), personalIdField),
                new HBox(10, btnAvatar, lblAvatarStatus)
        );
        return pane;
    }

    private HBox createActionBar() {
        addBtn = new Button(LanguageManager.getText("btn.add"));
        addBtn.setStyle("-fx-base: #27ae60; -fx-text-fill: white;");

        editBtn = new Button("✏️ " + LanguageManager.getText("btn.edit"));
        editBtn.setDisable(true);

        deleteBtn = new Button(LanguageManager.getText("btn.delete"));
        deleteBtn.getStyleClass().add("button-delete");
        deleteBtn.setDisable(true);

        clearBtn = new Button("🔄 " + LanguageManager.getText("btn.refresh"));
        importUserBtn = new Button("📥 Import CSV");

        btnCreateCard = new Button(LanguageManager.getText("btn.create_card"));
        btnCreateCard.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
        btnCreateCard.setDisable(true);

        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(clearBtn, addBtn, editBtn, deleteBtn, new Separator(javafx.geometry.Orientation.VERTICAL), importUserBtn, btnCreateCard);
        return bar;
    }

    // ===============================================================
    // GETTERS CHO CONTROLLER
    // ===============================================================
    public TableView<User> getUserTable() { return userTable; }
    public Pagination getUserPagination() { return userPagination; }
    public TextField getSearchField() { return searchField; }
    public Button getSearchIdBtn() { return searchIdBtn; }
    public Button getSearchNameBtn() { return searchNameBtn; }
    public Button getClearSearchBtn() { return clearSearchBtn; }
    public TextField getIdField() { return idField; }
    public TextField getNameField() { return nameField; }
    public TextField getEmailField() { return emailField; }
    public TextField getPersonalIdField() { return personalIdField; }
    public Button getBtnAvatar() { return btnAvatar; }
    public Label getLblAvatarStatus() { return lblAvatarStatus; }
    public Button getAddBtn() { return addBtn; }
    public Button getEditBtn() { return editBtn; }
    public Button getDeleteBtn() { return deleteBtn; }
    public Button getClearBtn() { return clearBtn; }
    public Button getImportUserBtn() { return importUserBtn; }
    public Button getBtnCreateCard() { return btnCreateCard; }
}