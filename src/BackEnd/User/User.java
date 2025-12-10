package BackEnd.User;

// Loại bỏ các import không cần thiết sau khi loại bỏ các List/ArrayList

public class User {
    private String id;
    private String name;
    private int soSachDaMuon = 0; // Số lượt mượn tổng cộng (được đọc/ghi từ DB)


    public User(){}

    public User(String id, String name){
        this.id = id;
        this.name = name;
    }

    // =======================================================
    // GETTER / SETTER CƠ BẢN
    // =======================================================

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