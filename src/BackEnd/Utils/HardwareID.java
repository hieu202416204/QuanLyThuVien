package BackEnd.Utils;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class HardwareID {

    /**
     * Lấy MAC Address của máy tính (dùng làm ID duy nhất).
     * Sẽ lấy MAC Address của Network Interface đầu tiên không phải là Loopback.
     * @return Chuỗi MAC Address hoặc "UNKNOWN" nếu không tìm thấy.
     */
    public static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();

                // Bỏ qua các giao diện ảo hoặc loopback (127.0.0.1)
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                    continue;
                }

                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    // Trả về chuỗi MAC đầu tiên tìm thấy
                    return sb.toString().toUpperCase();
                }
            }
        } catch (SocketException e) {
            System.err.println("Lỗi khi lấy MAC Address: " + e.getMessage());
        }
        return "UNKNOWN";
    }
}