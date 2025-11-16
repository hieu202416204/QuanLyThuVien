public class Book {
    private String id;
    private  String name;
    private String author;
    private String year;
    private String ngayMuon, ngayTra;
    private boolean status = true;
    public static final int MAX_DAY = 60;
    public Book(){}
    public Book(String id, String name, String author,
                String year){
        this.id = id;
        this.author = author;
        this.name = name;
        this.year =  year;
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

    public boolean isStatus() {
        return this.status;
    }

    public void setNgayMuon(String ngayMuon) {
        this.ngayMuon = ngayMuon;
    }

    public String getNgayMuon() {
        return this.ngayMuon;
    }

    public void setNgayTra(String ngayTra) {
        this.ngayTra = ngayTra;
    }

    public String getNgayTra() {
        return this.ngayTra;
    }
    public void inTTin(){
        System.out.println("ID: "+ this.getId());
        System.out.println("Name: "+ this.getName());
        System.out.println("Author: "+ this.getAuthor());
        System.out.println("Year: "+ this.getYear());
        System.out.println("Status: "+ this.isStatus());
        System.out.println("");
    }
}
