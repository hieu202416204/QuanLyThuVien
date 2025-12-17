package BackEnd.Utils;

import BackEnd.Book.Book;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookInfoHelper {

    // ================== API URLs ==================
    private static final String GOOGLE_API_URL =
            "https://www.googleapis.com/books/v1/volumes?q=isbn:";
    private static final String GOOGLE_API_KEY =
            "AIzaSyDWqop_nKuvL2ol1A1XYJ0CAwXQyZDWmog";

    private static final String OPEN_LIBRARY_URL =
            "https://openlibrary.org/api/books?format=json&jscmd=data&bibkeys=ISBN:";

    private static final String WORLDCAT_URL =
            "https://xisbn.worldcat.org/webservices/xid/isbn/";

    private static final String ISBNSEARCH_URL =
            "https://isbnsearch.org/isbn/";

    // ================== ENTRY POINT ==================
    public static Book fetchBookDetails(String isbn) {
        String cleanIsbn = isbn.replaceAll("[^0-9Xx]", "");
        if (cleanIsbn.isEmpty()) return null;

        Book book;

        book = fetchFromGoogle(cleanIsbn);
        if (book != null) return book;

        book = fetchFromOpenLibrary(cleanIsbn);
        if (book != null) return book;

        book = fetchFromWorldCat(cleanIsbn);
        if (book != null) return book;

        book = fetchFromISBNSearch(cleanIsbn);
        if (book != null) return book;

        return null;
    }

    // ================== GOOGLE BOOKS ==================
    private static Book fetchFromGoogle(String isbn) {
        try {
            String url = GOOGLE_API_URL + isbn + "&key=" + GOOGLE_API_KEY;
            String json = sendRequest(url);

            if (json == null || json.contains("\"totalItems\": 0")) return null;

            Book book = new Book();
            book.setId(isbn);

            book.setName(extractJsonValue(json, "\"title\"\\s*:\\s*\"([^\"]+)\""));
            book.setAuthor(extractJsonValue(json, "\"authors\"\\s*:\\s*\\[\\s*\"([^\"]+)\""));

            book.setYear(extractYear(json));

            String img = extractJsonValue(json, "\"thumbnail\"\\s*:\\s*\"([^\"]+)\"");
            if (img != null)
                book.setImagePath(img.replace("http://", "https://").replace("\\u0026", "&"));

            book.setStatus(true);
            return isValid(book);

        } catch (Exception e) {
            return null;
        }
    }

    // ================== OPEN LIBRARY ==================
    private static Book fetchFromOpenLibrary(String isbn) {
        try {
            String json = sendRequest(OPEN_LIBRARY_URL + isbn);
            if (json == null || json.equals("{}")) return null;

            Book book = new Book();
            book.setId(isbn);

            book.setName(extractJsonValue(json, "\"title\"\\s*:\\s*\"([^\"]+)\""));
            book.setAuthor(extractJsonValue(json, "\"name\"\\s*:\\s*\"([^\"]+)\""));
            book.setYear(extractYear(json));

            String img = extractJsonValue(json, "\"medium\"\\s*:\\s*\"([^\"]+)\"");
            if (img != null) book.setImagePath(img.replace("http://", "https://"));

            book.setStatus(true);
            return isValid(book);

        } catch (Exception e) {
            return null;
        }
    }

    // ================== WORLDCAT ==================
    private static Book fetchFromWorldCat(String isbn) {
        try {
            String url = WORLDCAT_URL + isbn + "?method=getMetadata&format=json";
            String json = sendRequest(url);
            if (json == null || !json.contains("\"list\"")) return null;

            Book book = new Book();
            book.setId(isbn);

            book.setName(extractJsonValue(json, "\"title\"\\s*:\\s*\"([^\"]+)\""));
            book.setAuthor(extractJsonValue(json, "\"author\"\\s*:\\s*\"([^\"]+)\""));
            book.setYear(extractYear(json));

            book.setStatus(true);
            return isValid(book);

        } catch (Exception e) {
            return null;
        }
    }

    // ================== ISBNSEARCH ==================
    private static Book fetchFromISBNSearch(String isbn) {
        try {
            String html = sendRequest(ISBNSEARCH_URL + isbn);
            if (html == null || !html.contains("<h1>")) return null;

            Book book = new Book();
            book.setId(isbn);

            book.setName(extractJsonValue(html, "<h1>(.*?)</h1>"));
            book.setAuthor(extractJsonValue(html, "Author[s]*:</strong>\\s*(.*?)<"));
            book.setYear(extractJsonValue(html, "Published:</strong>\\s*(\\d{4})"));

            book.setStatus(true);
            return isValid(book);

        } catch (Exception e) {
            return null;
        }
    }

    // ================== HTTP ==================
    private static String sendRequest(String urlStr) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ================== REGEX UTIL ==================
    private static String extractJsonValue(String text, String regex) {
        Matcher m = Pattern.compile(
                regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String extractYear(String text) {
        Matcher m = Pattern.compile("\\b(18|19|20)\\d{2}\\b").matcher(text);
        return m.find() ? m.group() : "N/A";
    }

    private static Book isValid(Book book) {
        return (book.getName() != null && !book.getName().isEmpty()) ? book : null;
    }

    // ================== IMAGE DOWNLOAD ==================
    public static java.io.File downloadCoverImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) return null;

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setReadTimeout(5000);

            java.io.InputStream in = conn.getInputStream();
            java.io.File temp =
                    java.io.File.createTempFile("downloaded_cover_", ".jpg");

            java.nio.file.Files.copy(
                    in, temp.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            in.close();
            return temp;

        } catch (Exception e) {
            return null;
        }
    }
}
