-injars C:/Users/hieuv/IdeaProjects/QuanLyThuVien/out/artifacts/QuanLyThuVien_jar/QuanLyThuVien.jar
-outjars C:/Users/hieuv/IdeaProjects/QuanLyThuVien/out/artifacts/QuanLyThuVien_jar/QuanLyThuVien-obf.jar

-dontshrink
-dontoptimize
-dontwarn
-ignorewarnings

-overloadaggressively
-useuniqueclassmembernames
-repackageclasses ''

# Entry point
-keep public class FrontEnd.Launcher {
    public static void main(java.lang.String[]);
}

# JavaFX
-keep class javafx.** { *; }
-keepclassmembers class * {
    @javafx.fxml.FXML *;
}

# GIỮ UI
-keep class FrontEnd.** { *; }

# KHÔNG keep:
# BackEnd.*
# Database.*
# XuLiAnh.*
