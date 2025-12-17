package FrontEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Sattistics.BookStatistic;
import BackEnd.Sattistics.UserStatistic;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import Database.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

import static FrontEnd.LibraryApp.ADMIN_PASSWORD;

public class StatisticTab extends VBox {
    private final Library library;
    private final UserStatistic userStatistic;
    private final BookStatistic bookStatistic;
    private final Runnable globalRefreshCallback; // Để gọi reset toàn bộ app

    private TableView<User> topUserTable;
    private TableView<Book> topBookTable;
    private Label titleUser, titleBook;

    public StatisticTab(Library library, Runnable globalRefreshCallback) {
        this.library = library;
        this.globalRefreshCallback = globalRefreshCallback;
        this.userStatistic = new UserStatistic(library);
        this.bookStatistic = new BookStatistic(library);

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
        Button refreshBtn = new Button(LanguageManager.getText("btn.refresh"));
        refreshBtn.setOnAction(e -> refreshData());

        Button exportBtn = new Button(LanguageManager.getText("btn.export"));
        exportBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        exportBtn.setOnAction(e -> handleExportReport());

        Button resetBtn = new Button(LanguageManager.getText("btn.factory_reset"));
        resetBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        resetBtn.setOnAction(e -> handleFactoryReset());

        HBox box = new HBox(10, refreshBtn, exportBtn, resetBtn);
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

    public void refreshData() {
        // Update Users
        List<User> allUsers = userStatistic.danhSachNguoiDung();
        int uLimit = Math.min(20, allUsers.size());
        topUserTable.setItems(FXCollections.observableArrayList(allUsers.subList(0, uLimit)));
        titleUser.setText(LanguageManager.getText("title.top_users") + " (" + allUsers.size() + ")");

        // Update Books
        List<Book> allBooks = bookStatistic.getTopBook();
        int bLimit = Math.min(20, allBooks.size());
        topBookTable.setItems(FXCollections.observableArrayList(allBooks.subList(0, bLimit)));
        titleBook.setText(LanguageManager.getText("title.top_books") + " (" + allBooks.size() + ")");
    }

    private void handleExportReport() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File f = fc.showSaveDialog(null);
        if(f != null) {
            BackEnd.Utils.ExportUtil.exportBooksToCSV(bookStatistic.getTopBook(), f);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setContentText("Exported successfully.");
            a.show();
        }
    }

    private void handleFactoryReset() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Factory Reset");
        dlg.setContentText("Enter Admin Password to delete ALL DATA:");

        if (dlg.showAndWait().orElse("").equals(ADMIN_PASSWORD)) {
            // 1. Reset DB
            DatabaseManager.resetDatabase();

            // 2. Clear Images Cache
            BackEnd.Utils.FileUtil.clearAllImages();
            BackEnd.Utils.ImageCache.clear();

            // 3. Trigger Global Refresh (Quan trọng)
            if (globalRefreshCallback != null) {
                globalRefreshCallback.run();
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setContentText("System has been reset completely.");
            a.show();
        }
    }
}