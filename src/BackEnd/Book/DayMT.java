package BackEnd.Book;

import java.time.LocalDate;

public class DayMT {
    // Thêm trường dữ liệu để liên kết bản ghi này với người dùng/sách
    private String relatedUserId; // Ai đã thực hiện giao dịch này

    private LocalDate ngayMuon;
    private LocalDate ngayTra;

    public DayMT() {}

    public DayMT(String relatedUserId, LocalDate ngayMuon, LocalDate ngayTra) {
        this.relatedUserId = relatedUserId;
        this.ngayMuon = ngayMuon;
        this.ngayTra = ngayTra;
    }

    public void setRelatedUserId(String relatedUserId) {
        this.relatedUserId = relatedUserId;
    }

    public String getRelatedUserId() {
        return this.relatedUserId;
    }

    public void setNgayMuon(LocalDate ngayMuon) {
        this.ngayMuon = ngayMuon;
    }

    public void setNgayTra(LocalDate ngayTra) {
        this.ngayTra = ngayTra;
    }

    public LocalDate getNgayMuon() {
        return ngayMuon;
    }

    public LocalDate getNgayTra() {
        return ngayTra;
    }
}
