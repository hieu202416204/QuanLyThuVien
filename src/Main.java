public class Main {
    public static void main(String[] args) {
        Library library =  new Library();
        Book book1 = new Book("111111", "kakakaka", "Longtoc", "02/02/2006");
        Book book2 = new Book("111211", "kakakaka", "Longtoc", "02/02/2006");

        library.addBook(book1);
        library.addBook(book2);
        for(Book bok : library.listSach()){
            bok.inTTin();
        }
        User user1 = new User("202416204", "Hieu");
        library.choMuonSach(user1, "111111", "16/11/2025");
        for(Book bok : library.listSach()){
            bok.inTTin();
        }
        user1.inTTin();

    }
}