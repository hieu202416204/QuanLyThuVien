import java.util.ArrayList;
import java.util.List;

public class Library {
    private List<User> listUser = new ArrayList<>();
    private ArrayList<Book> danhSachSach = new ArrayList<>();
    public Library(){}
    public void addBook(Book book){
        this.danhSachSach.add(book);
    }
    public List<Book> listSach(){
        return this.danhSachSach;
    }
    public boolean deleteBook(String id) {
        return this.danhSachSach.removeIf(book -> book.getId().equals(id));
    }
    public boolean choMuonSach(User user, String BookId, String ngayMuonSach){
        for(Book bok : this.danhSachSach){
            if(bok.getId().equals(BookId)&&bok.isStatus()){
                user.setDanhSachMuon(bok, ngayMuonSach);
                bok.setStatus(false);
                return true;
            }
        }
        return false;
    }
    public boolean traSach(User user, String BookId){
        for(Book bok : this.danhSachSach) {
            if(bok.getId().equals(BookId)) {
                user.traSach(bok);
                bok.setStatus(true);
                return true;
            }
        }
        return false;
    }
    public Book searchBookId(String id){
        for(Book bok : this.danhSachSach){
            if(bok.getId().equals(id)){
                return bok;
            }
        }
        return null;
    }
    public List<Book> searchBookAuthor(String author){
        List<Book> listBook = new ArrayList<>();
        for(Book bok : this.danhSachSach){
            if(bok.getAuthor().equals(author)){
                listBook.add(bok);
            }
        }
        return listBook;
    }
    public List<Book> searchBookName(String name){
        List<Book> listBook = new ArrayList<>();
        for(Book bok : this.danhSachSach){
            if(bok.getName().equals(name)){
                listBook.add(bok);
            }
        }
        return listBook;
    }

    public void addListUser(User user) {
        this.listUser.add(user);
    }

    public List<User> getListUser() {
        return listUser;
    }
}
