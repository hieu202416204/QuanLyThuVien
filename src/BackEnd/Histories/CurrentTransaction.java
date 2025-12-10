package BackEnd.Histories;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class CurrentTransaction {
    private String userId;
    private String userName;
    private String bookId;
    private String bookName;
    private LocalDate borrowDate;

    public CurrentTransaction(String userId, String userName, String bookId, String bookName, LocalDate borrowDate) {
        this.userId = userId;
        this.userName = userName;
        this.bookId = bookId;
        this.bookName = bookName;
        this.borrowDate = borrowDate;
    }

    // Tính số ngày đã mượn
    public long getDaysElapsed() {
        if (borrowDate == null) return 0;
        return ChronoUnit.DAYS.between(borrowDate, LocalDate.now());
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getBookId() { return bookId; }
    public String getBookName() { return bookName; }
    public LocalDate getBorrowDate() { return borrowDate; }
}