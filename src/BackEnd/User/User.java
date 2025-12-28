package BackEnd.User;

// Loại bỏ các import không cần thiết sau khi loại bỏ các List/ArrayList

import java.time.LocalDate;

public class User {
    private String id;
    private String name;
    private int soSachDaMuon = 0; // Số lượt mượn tổng cộng (được đọc/ghi từ DB)
    private String email;
    private String avatarPath;
    private LocalDate creatAt;
    private String personalIdNumber;

    public User(){
        this.creatAt = LocalDate.now();
    }
    public User(String id, String name){
        this.id = id;
        this.name = name;
        this.creatAt = LocalDate.now();
    }

    public User(String id, String name, String email){
        this.id = id;
        this.name = name;
        this.email = email;
        this.creatAt = LocalDate.now();
    }
    public User(String id, String name, String email, String avatarPath){
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarPath = avatarPath;
        this.creatAt = LocalDate.now();
    }
    public User(String id, String name, String email, String avatarPath, String personalIdNumber){
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarPath = avatarPath;
        this.creatAt = LocalDate.now();
        this.personalIdNumber = personalIdNumber;
    }
    // =======================================================
    // GETTER / SETTER CƠ BẢN
    // =======================================================


    public void setPersonalIdNumber(String personalIdNumber) {
        this.personalIdNumber = personalIdNumber;
    }

    public String getPersonalIdNumber() {
        return this.personalIdNumber;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getAvatarPath() {
        return this.avatarPath;
    }
    public LocalDate getCreatedAt() { return creatAt;}
    public void setCreatedAt(LocalDate createdAt) { this.creatAt = createdAt; }
    // =======================================================
    // THỐNG KÊ / HỖ TRỢ DB
    // =======================================================

    /**
     * Phương thức này được dùng bởi UserDAO để tải giá trị soSachDaMuon từ DB.
     * Tránh gọi trực tiếp phương thức này trong logic nghiệp vụ.
     */
    public void setSoSachDaMuonFromDB(int count) {
        this.soSachDaMuon = count;
    }

    /**
     * Dùng trong logic nghiệp vụ để tăng thống kê trong bộ nhớ trước khi ghi vào DB.
     * Lưu ý: Giá trị này phải được ghi xuống DB thông qua UserDAO.updateUserBorrowedCount.
     */
    public void updateSoSachDaMuon() {
        this.soSachDaMuon++;
    }

    public int getSoSachDaMuon() {
        return this.soSachDaMuon;
    }


    // =======================================================
}