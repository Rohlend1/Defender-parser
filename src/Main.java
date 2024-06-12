import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        //TIP <b>Указать свою директорию где лог файл.
        // В формате '/Users/JohnDoe/root_directory/'.
        // В конце '/' обязателен</b>
        new FileListener("<root_directory>");
    }
}