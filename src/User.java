import java.util.ArrayList;

public class User {
    private String id;
    private String name;
    private ArrayList<Book> danhSachMuon = new ArrayList<>();
    public User(){}
    public User(String id, String name){
        this.id = id;
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setDanhSachMuon(Book book, String ngayMuonSach) {
        book.setNgayMuon(ngayMuonSach);
        this.danhSachMuon.add(book);
    }
    public void traSach(Book book){
        this.danhSachMuon.removeIf(bok -> bok.getId().equals(book.getId()));
    }

    public ArrayList<Book> getDanhSachMuon() {
        return this.danhSachMuon;
    }
    public void inTTin(){
        for(Book bok : this.danhSachMuon){
            System.out.println("ID: "+ bok.getId());
            System.out.println("Name: "+ bok.getName());
            System.out.println("Author: "+ bok.getAuthor());
            System.out.println("Year: "+ bok.getYear());
            System.out.println("Ngày mượn: " + bok.getNgayMuon());
        }
    }
}
