package FrontEnd.Views;

import BackEnd.Book.Book;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * VIEW: Chỉ đảm nhận việc vẽ giao diện thống kê (Bảng xếp hạng, Nút bấm).
 */
public class StatisticTabView extends VBox {

    // --- CÁC THÀNH PHẦN GIAO DIỆN (CẦN GETTER) ---
    private Button btnRefresh;
    private Button btnExport;
    private Button btnReset;

    private TableView<User> topUserTable;
    private TableView<Book> topBookTable;
    private Label titleUser;
    private Label titleBook;

    public StatisticTabView() {
        initUI();
    }

    private void initUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(15);
        this.setAlignment(Pos.TOP_CENTER);

        // 1. Controls (Buttons)
        HBox buttonBar = createButtonBar();

        // 2. Statistics Layout (2 Tables side by side)
        HBox statsLayout = createStatsLayout();

        this.getChildren().addAll(buttonBar, statsLayout);
    }

    private HBox createButtonBar() {
        btnRefresh = new Button(LanguageManager.getText("btn.refresh"));

        btnExport = new Button(LanguageManager.getText("btn.export"));
        btnExport.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");

        btnReset = new Button(LanguageManager.getText("btn.factory_reset"));
        btnReset.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox box = new HBox(10, btnRefresh, btnExport, btnReset);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private HBox createStatsLayout() {
        // --- Table Top Users ---
        topUserTable = new TableView<>();
        topUserTable.setMinWidth(400);
        topUserTable.setMinHeight(500);

        TableColumn<User, String> colURank = new TableColumn<>(LanguageManager.getText("col.rank"));
        colURank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topUserTable.getItems().indexOf(data.getValue()) + 1)));

        TableColumn<User, String> colUName = new TableColumn<>(LanguageManager.getText("col.user_name"));
        colUName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<User, String> colUCount = new TableColumn<>(LanguageManager.getText("col.borrowed_books"));
        colUCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoSachDaMuon())));

        topUserTable.getColumns().addAll(colURank, colUName, colUCount);
        topUserTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        titleUser = new Label(LanguageManager.getText("title.top_users"));
        titleUser.getStyleClass().add("page-title");
        VBox vUser = new VBox(5, titleUser, topUserTable);
        HBox.setHgrow(vUser, Priority.ALWAYS);


        // --- Table Top Books ---
        topBookTable = new TableView<>();
        topBookTable.setMinWidth(400);

        TableColumn<Book, String> colBRank = new TableColumn<>(LanguageManager.getText("col.rank"));
        colBRank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topBookTable.getItems().indexOf(data.getValue()) + 1)));

        TableColumn<Book, String> colBName = new TableColumn<>(LanguageManager.getText("col.name"));
        colBName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Book, String> colBCount = new TableColumn<>(LanguageManager.getText("col.borrow_count"));
        colBCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoLuotMuon())));

        topBookTable.getColumns().addAll(colBRank, colBName, colBCount);
        topBookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        topBookTable.setMinHeight(500);


        titleBook = new Label(LanguageManager.getText("title.top_books"));
        titleBook.getStyleClass().add("page-title");
        VBox vBook = new VBox(5, titleBook, topBookTable);
        HBox.setHgrow(vBook, Priority.ALWAYS);

        return new HBox(20, vUser, vBook);
    }

    // ==========================================
    // GETTERS CHO CONTROLLER
    // ==========================================
    public Button getBtnRefresh() { return btnRefresh; }
    public Button getBtnExport() { return btnExport; }
    public Button getBtnReset() { return btnReset; }
    public TableView<User> getTopUserTable() { return topUserTable; }
    public TableView<Book> getTopBookTable() { return topBookTable; }
    public Label getTitleUser() { return titleUser; }
    public Label getTitleBook() { return titleBook; }
}