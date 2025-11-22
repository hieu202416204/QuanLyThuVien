package BackEnd.Histories;

import java.time.LocalDateTime;

public class UserInUserHistory {
    private String name;
    private String id;
    private String trangThai;
    private LocalDateTime localDateTime;
    private String bookName;
    public UserInUserHistory(){
    }
    public UserInUserHistory(String name, String id, String trangThai, LocalDateTime localDateTime){
        this.name = name;
        this.id = id;
        this.trangThai = trangThai;
        this.localDateTime = localDateTime;
    }
    public UserInUserHistory(String name, String id, String trangThai, LocalDateTime localDateTime, String bookName){
        this.name = name;
        this.id = id;
        this.trangThai = trangThai;
        this.localDateTime = localDateTime;
        this.bookName = bookName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTrangThai() {
        return trangThai;
    }
    public String getBookName(){
        return this.bookName;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public void setLocalDateTime() {
        this.localDateTime = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }
}
