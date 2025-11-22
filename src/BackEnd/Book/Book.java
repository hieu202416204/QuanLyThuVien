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
    public Book(){}
    public Book(String id, String name, String author,
                String year){
        this.id = id;
        this.author = author;
        this.name = name;
        this.year =  year;
        //this.imagePath = ""; // mặc định chưa có ảnh
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

    public boolean isStatus() {
        return this.status;
    }
//    public String getImagePath() { return imagePath; }
//    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void inTTin(){
        System.out.println("ID: "+ this.getId());
        System.out.println("Name: "+ this.getName());
        System.out.println("Author: "+ this.getAuthor());
        System.out.println("Year: "+ this.getYear());
        System.out.println("Status: "+ this.isStatus());
        System.out.println("");
    }
}
