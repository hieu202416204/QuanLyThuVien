package FrontEnd.Controllers;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.Utils.LanguageManager;
import Database.VisitDAO;
import FrontEnd.Views.HomeTabView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CONTROLLER: Quản lý Background Thread, lấy dữ liệu thống kê, xử lý logic Dashboard.
 */
public class HomeTabController {

    private final Library library;
    private final HomeTabView view;
    private final VisitDAO visitDAO = new VisitDAO();

    // Điều khiển Background Thread
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicLong refreshInterval = new AtomicLong(60000); // Đổi mặc định thành 60s theo code setting
    private double currentUpperBound = 0;

    public HomeTabController(Library library, HomeTabView view) {
        this.library = library;
        this.view = view;

        attachEvents();
        startBackgroundAutoRefresh();
    }

    private void attachEvents() {
        // Events Biểu đồ Khách Truy Cập (Visit Chart)
        view.getCmbVisitViewType().setOnAction(e -> triggerImmediateVisitUpdate());
        view.getDatePickerVisit().setOnAction(e -> triggerImmediateVisitUpdate());

        // Event Kiểm kê (Inventory)
        view.getBtnAnalyze().setOnAction(e -> handleAnalyzeInventory());

        // Events Floating Menu (Cài đặt làm mới)
        view.getItemRefresh().setOnAction(e -> refreshData());
        view.getR20().setOnAction(e -> refreshInterval.set(60000));     // 1 phút
        view.getR40().setOnAction(e -> refreshInterval.set(60000 * 3)); // 3 phút
        view.getR60().setOnAction(e -> refreshInterval.set(60000 * 5)); // 5 phút
    }

    // =========================================================================
    // DỮ LIỆU & LUỒNG NỀN (BACKGROUND THREAD)
    // =========================================================================

    private static class DashboardData {
        int totalBooks, totalUsers, available, borrowed;
        double revenue;
        List<Book> topBooks;
        Map<String, Integer> userGrowth;
        List<long[]> returnStats;
        String visitTitle;
        XYChart.Series<String, Number> visitSeries;
    }

    private void startBackgroundAutoRefresh() {
        Thread bgThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    if (view.getScene() == null || !view.isVisible()) {
                        Thread.sleep(2000);
                        continue;
                    }

                    final LocalDate[] selectedDate = {LocalDate.now()};
                    final String[] selectedView = {"Week View"};

                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    Platform.runLater(() -> {
                        if (view.getDatePickerVisit().getValue() != null)
                            selectedDate[0] = view.getDatePickerVisit().getValue();
                        if (view.getCmbVisitViewType().getValue() != null)
                            selectedView[0] = view.getCmbVisitViewType().getValue();
                        latch.countDown();
                    });
                    latch.await();

                    DashboardData data = loadDashboardData(selectedDate[0], selectedView[0]);
                    Platform.runLater(() -> updateUI(data));

                    Thread.sleep(refreshInterval.get());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    try { Thread.sleep(5000); } catch (InterruptedException ex) {}
                }
            }
        });
        bgThread.setDaemon(true);
        bgThread.setName("HomeTab-AutoRefresh");
        bgThread.start();
    }

    private DashboardData loadDashboardData(LocalDate visitDate, String visitViewType) {
        DashboardData data = new DashboardData();
        List<Book> allBooks = library.getBooks();

        data.totalBooks = allBooks.size();
        data.totalUsers = library.getListUsers().size();

        int avail = 0;
        for (Book b : allBooks) {
            if (b.isStatus()) avail++;
        }
        data.available = avail;
        data.borrowed = data.totalBooks - avail;
        data.revenue = library.getFinancialDAO().getTotalRevenue();
        data.topBooks = library.getBookDAO().getBooksSortedByBorrowCount();
        data.userGrowth = library.getUserDAO().getUserGrowthStats();
        data.returnStats = library.getTransactionDAO().getReturnDurations();

        data.visitSeries = calculateVisitSeries(visitDate, visitViewType);

        if ("Week View".equals(visitViewType)) {
            LocalDate start = visitDate.with(DayOfWeek.MONDAY);
            LocalDate end = visitDate.with(DayOfWeek.SUNDAY);
            data.visitTitle = "Visitors: " + start.format(DateTimeFormatter.ofPattern("dd/MM")) + " to " + end.format(DateTimeFormatter.ofPattern("dd/MM"));
        } else if ("Month View".equals(visitViewType)) {
            data.visitTitle = "Visitors: " + visitDate.getMonth() + " " + visitDate.getYear();
        } else {
            data.visitTitle = "Visitors in " + visitDate.getYear();
        }

        return data;
    }

    private void updateUI(DashboardData data) {
        view.getLblTotalBooks().setText(String.valueOf(data.totalBooks));
        view.getLblTotalUsers().setText(String.valueOf(data.totalUsers));
        view.getLblAvailableBooks().setText(String.valueOf(data.available));
        view.getLblBorrowedBooks().setText(String.valueOf(data.borrowed));

        String currency = library.getSettingsDAO().getSetting("currency_unit");
        if (currency == null || currency.isEmpty()) currency = "VNĐ";
        view.getLblTotalRevenue().setText(String.format("%,.0f %s", data.revenue, currency));

        view.getLblSystemCountDisplay().setText(LanguageManager.getText("label.system_count") + " " + data.totalBooks);

        // Update Pie Chart Status
        view.getDataAvailable().setPieValue(data.available);
        view.getDataBorrowed().setPieValue(data.borrowed);
        view.getDataAvailable().setName(LanguageManager.getText("status.available") + " (" + data.available + ")");
        view.getDataBorrowed().setName(LanguageManager.getText("status.borrowed") + " (" + data.borrowed + ")");

        // Update Top Books Chart
        double maxBorrowCount = data.topBooks.isEmpty() ? 0 : data.topBooks.get(0).getSoLuotMuon();
        if (currentUpperBound == 0) {
            currentUpperBound = (maxBorrowCount > 0 ? maxBorrowCount : 5) * 1.5;
            view.getYAxisTopBooks().setUpperBound(currentUpperBound);
        }
        if (maxBorrowCount >= currentUpperBound * 0.93 || (maxBorrowCount < currentUpperBound * 0.2 && maxBorrowCount > 0)) {
            currentUpperBound = Math.ceil(maxBorrowCount * 1.5);
            view.getYAxisTopBooks().setUpperBound(currentUpperBound);
        }

        XYChart.Series<String, Number> seriesTop;
        if (view.getTopBooksChart().getData().isEmpty()) {
            seriesTop = new XYChart.Series<>();
            view.getTopBooksChart().getData().add(seriesTop);
        } else {
            seriesTop = view.getTopBooksChart().getData().get(0);
        }

        ObservableList<XYChart.Data<String, Number>> newDataTop = FXCollections.observableArrayList();
        int limit = Math.min(5, data.topBooks.size());
        for (int i = 0; i < limit; i++) {
            Book b = data.topBooks.get(i);
            if (b.getSoLuotMuon() > 0) {
                String shortName = b.getName().length() > 15 ? b.getName().substring(0, 12) + "..." : b.getName();
                newDataTop.add(new XYChart.Data<>(shortName, b.getSoLuotMuon()));
            }
        }
        seriesTop.setData(newDataTop);

        // Update User Growth Chart
        XYChart.Series<String, Number> seriesUser;
        if (view.getUserGrowthChart().getData().isEmpty()) {
            seriesUser = new XYChart.Series<>();
            view.getUserGrowthChart().getData().add(seriesUser);
        } else {
            seriesUser = view.getUserGrowthChart().getData().get(0);
        }

        ObservableList<XYChart.Data<String, Number>> newDataUser = FXCollections.observableArrayList();
        int totalSoFar = 0;
        for (Map.Entry<String, Integer> entry : data.userGrowth.entrySet()) {
            totalSoFar += entry.getValue();
            newDataUser.add(new XYChart.Data<>(entry.getKey(), totalSoFar));
        }
        seriesUser.setData(newDataUser);

        // Update Return Rate Chart
        String maxDaysStr = library.getSettingsDAO().getSetting("max_borrow_days");
        int maxDays = (maxDaysStr == null || maxDaysStr.isEmpty()) ? 60 : Integer.parseInt(maxDaysStr);
        int onTime = 0, late = 0;
        for (long[] r : data.returnStats) {
            if (r[0] <= maxDays) onTime++;
            else late++;
        }
        view.getReturnRateChart().getData().clear();
        if (onTime + late > 0) {
            view.getReturnRateChart().getData().add(new PieChart.Data("On time (" + onTime + ")", onTime));
            view.getReturnRateChart().getData().add(new PieChart.Data("Late (" + late + ")", late));
        }

        // Update Visit Chart
        view.getLblVisitTitle().setText(data.visitTitle);
        if (data.visitSeries != null) {
            if (view.getVisitChart().getData().isEmpty()) {
                view.getVisitChart().getData().add(data.visitSeries);
            } else {
                XYChart.Series<String, Number> currentSeries = view.getVisitChart().getData().get(0);
                currentSeries.setData(data.visitSeries.getData());
            }
        } else {
            view.getVisitChart().getData().clear();
        }
    }

    private XYChart.Series<String, Number> calculateVisitSeries(LocalDate selectedDate, String viewType) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if ("Week View".equals(viewType)) {
            LocalDate start = selectedDate.with(DayOfWeek.MONDAY);
            LocalDate end = selectedDate.with(DayOfWeek.SUNDAY);
            Map<String, Integer> map = visitDAO.getVisitsInDateRange(start, end);
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                String label = d.getDayOfWeek().toString().substring(0,3) + "\n" + d.format(DateTimeFormatter.ofPattern("dd/MM"));
                series.getData().add(new XYChart.Data<>(label, map.getOrDefault(d.toString(), 0)));
            }
        } else if ("Month View".equals(viewType)) {
            YearMonth ym = YearMonth.from(selectedDate);
            Map<String, Integer> map = visitDAO.getVisitsInDateRange(ym.atDay(1), ym.atEndOfMonth());
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                series.getData().add(new XYChart.Data<>(String.valueOf(day), map.getOrDefault(ym.atDay(day).toString(), 0)));
            }
        } else {
            int year = selectedDate.getYear();
            Map<Integer, Integer> map = visitDAO.getVisitsByMonthInYear(year);
            for (int m = 1; m <= 12; m++) {
                series.getData().add(new XYChart.Data<>("M" + m, map.getOrDefault(m, 0)));
            }
        }
        return series;
    }

    private void triggerImmediateVisitUpdate() {
        LocalDate d = view.getDatePickerVisit().getValue() != null ? view.getDatePickerVisit().getValue() : LocalDate.now();
        String v = view.getCmbVisitViewType().getValue() != null ? view.getCmbVisitViewType().getValue() : "Week View";

        new Thread(() -> {
            XYChart.Series<String, Number> series = calculateVisitSeries(d, v);
            String title;
            if ("Week View".equals(v)) {
                LocalDate start = d.with(DayOfWeek.MONDAY);
                LocalDate end = d.with(DayOfWeek.SUNDAY);
                title = "Visitors: " + start.format(DateTimeFormatter.ofPattern("dd/MM")) + " to " + end.format(DateTimeFormatter.ofPattern("dd/MM"));
            } else if ("Month View".equals(v)) {
                title = "Visitors: " + d.getMonth() + " " + d.getYear();
            } else {
                title = "Visitors in " + d.getYear();
            }

            Platform.runLater(() -> {
                view.getLblVisitTitle().setText(title);
                view.getVisitChart().getData().clear();
                view.getVisitChart().getData().add(series);
            });
        }).start();
    }

    private void handleAnalyzeInventory() {
        try {
            int systemCount = library.getBooks().size();
            String input = view.getTxtActualCount().getText().trim();
            if (input.isEmpty()) return;
            int actualCount = Integer.parseInt(input);
            int diff = systemCount - actualCount;

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            if (diff > 0) {
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.existing"), actualCount));
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.lost"), diff));
            } else if (diff < 0) {
                pieData.add(new PieChart.Data(LanguageManager.getText("label.system_count"), systemCount));
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.surplus"), Math.abs(diff)));
            } else {
                pieData.add(new PieChart.Data(LanguageManager.getText("slice.existing"), actualCount));
            }

            view.getInventoryChart().setData(pieData);
            String statusText = (diff == 0 ? "OK" : (diff > 0 ? "-" + diff : "+" + Math.abs(diff)));
            view.getInventoryChart().setTitle(LanguageManager.getText("chart.inventory") + " (" + statusText + ")");
        } catch (Exception e) {}
    }

    public void refreshData() {
        Thread oneTimeUpdateThread = new Thread(() -> {
            try {
                final LocalDate[] selectedDate = {LocalDate.now()};
                final String[] selectedView = {"Week View"};

                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                Platform.runLater(() -> {
                    if (view.getDatePickerVisit().getValue() != null)
                        selectedDate[0] = view.getDatePickerVisit().getValue();
                    if (view.getCmbVisitViewType().getValue() != null)
                        selectedView[0] = view.getCmbVisitViewType().getValue();
                    latch.countDown();
                });
                latch.await();

                DashboardData data = loadDashboardData(selectedDate[0], selectedView[0]);
                Platform.runLater(() -> updateUI(data));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        oneTimeUpdateThread.setDaemon(true);
        oneTimeUpdateThread.start();
    }
}