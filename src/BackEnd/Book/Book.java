package BackEnd.Book;

import BackEnd.Histories.BookHistory;

public class Book {
    private BookHistory bookHistory = new BookHistory();
    private String id;
    private  String name;
    private String author;
    private String year;
    private boolean status = true;
    private String imagePath; // Đường dẫn ảnh bìa
    private int soLuotMuon = 0;
    private String category;
    public Book(){}
    public Book(String id, String name, String author,
                String year){
        this.id = id;
        this.author = author;
        this.name = name;
        this.year =  year;
    }    public Book(String id, String name, String author,
                     String year, String category){
        this.id = id;
        this.author = author;
        this.name = name;
        this.year =  year;
        this.category = "General";
    }

    public Book(String id, String name, String author,
                String year, String imagePath, String category){
        this.id = id;
        this.author = author;
        this.name = name;
        this.year =  year;
        this.imagePath = imagePath;
        this.category = "General";
    }
    // getter vaf setter cho imagePath

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImagePath() {
        return imagePath;
    }

    // trả về danh sách lịch sử mượn sach
    public void setBookHistories(DayMT day){
        this.bookHistory.setLichSuMuonSach(day);
    }
    public BookHistory getBookHistory(){
        return this.bookHistory;
    }
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getYear() {
        return this.year;
    }

    public void setSoLuotMuon() {
        this.soLuotMuon++;
    }

    public int getSoLuotMuon() {
        return this.soLuotMuon;
    }
    public void setSoLuotMuonFromDB(int count) {
        this.soLuotMuon = count;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isStatus() {
        return this.status;
    }
    // Thêm 2 thuộc tính mới
    private int damagePercent = 0; // Mặc định 0% (Bình thường)
    private String damageDetails = ""; // Chi tiết hư hỏng

    // Thêm Getters và Setters cho chúng
    public int getDamagePercent() {
        return damagePercent;
    }

    public void setDamagePercent(int damagePercent) {
        this.damagePercent = damagePercent;
    }

    public String getDamageDetails() {
        return damageDetails;
    }

    public void setDamageDetails(String damageDetails) {
        this.damageDetails = damageDetails;
    }
}
