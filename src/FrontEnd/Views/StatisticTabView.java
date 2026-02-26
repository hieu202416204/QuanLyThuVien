package FrontEnd.Views;

import BackEnd.Book.Book;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
    private TableView<Book> topBookRateTable; // day la bang hien thi cac quyen sach co muc do hu hong cao
    private Label titleUser;
    private Label titleBook;
    private Label titleBookRate;

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
        GridPane statsLayout = createStatsLayout();

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

    private GridPane createStatsLayout() {
        // --- Table Top Users ---
        topUserTable = new TableView<>();

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
        vUser.setAlignment(Pos.CENTER);
        HBox.setHgrow(vUser, Priority.ALWAYS);


        // --- Table Top Books ---
        topBookTable = new TableView<>();

        TableColumn<Book, String> colBRank = new TableColumn<>(LanguageManager.getText("col.rank"));
        colBRank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topBookTable.getItems().indexOf(data.getValue()) + 1)));

        TableColumn<Book, String> colBName = new TableColumn<>(LanguageManager.getText("col.name"));
        colBName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Book, String> colBCount = new TableColumn<>(LanguageManager.getText("col.borrow_count"));
        colBCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoLuotMuon())));

        topBookTable.getColumns().addAll(colBRank, colBName, colBCount);
        topBookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


        titleBook = new Label(LanguageManager.getText("title.top_books"));
        titleBook.getStyleClass().add("page-title");
        VBox vBook = new VBox(5, titleBook, topBookTable);
        vBook.setAlignment(Pos.CENTER);
        HBox.setHgrow(vBook, Priority.ALWAYS);

        // top sach co ti le hu hong cao nhat

        topBookRateTable = new TableView<>();

        TableColumn<Book, String> colBRRank = new TableColumn<>(LanguageManager.getText("col.rank"));
        colBRRank.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(topBookRateTable.getItems().indexOf(data.getValue()) + 1)));
        TableColumn<Book, String> colBRName = new TableColumn<>(LanguageManager.getText("col.name"));
        colBRName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        TableColumn<Book, String> colBRCount = new TableColumn<>("Tỉ lệ hư hỏng");
        colBRCount.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getDamagePercent())));

        topBookRateTable.getColumns().addAll(colBRRank, colBRName, colBRCount);
        topBookRateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        titleBookRate = new Label("TOP SÁCH CÓ TỈ LỆ HƯ HỎNG CAO NHẤT");
        titleBookRate.getStyleClass().add("page-title");
        VBox vBookRate = new VBox(5, titleBookRate, topBookRateTable);
        vBookRate.setAlignment(Pos.CENTER);
        HBox.setHgrow(vBookRate, Priority.ALWAYS);

        // bố cục trang
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(20);

        grid.add(vUser,0,0);
        grid.add(vBook, 1, 0);
        grid.add(vBookRate, 0, 1);

        GridPane.setHgrow(vUser, Priority.ALWAYS);
        GridPane.setHgrow(vBook, Priority.ALWAYS);
        GridPane.setHgrow(vBookRate, Priority.ALWAYS);

        GridPane.setVgrow(vUser, Priority.ALWAYS);
        GridPane.setVgrow(vBook, Priority.ALWAYS);
        GridPane.setVgrow(vBookRate, Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setPercentWidth(50);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setPercentWidth(50);

        grid.getColumnConstraints().addAll(c1, c2);
        RowConstraints r1 = new RowConstraints();
        r1.setVgrow(Priority.ALWAYS);

        RowConstraints r2 = new RowConstraints();
        r2.setVgrow(Priority.ALWAYS);

        grid.getRowConstraints().addAll(r1, r2);

        return grid;
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
    public TableView<Book> getTopBookRateTable(){ return topBookRateTable; }
    public Label getTitleBookRate(){ return titleBookRate; }
}