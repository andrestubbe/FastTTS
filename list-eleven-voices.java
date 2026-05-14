import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.FileInputStream;
import java.util.Properties;

public class ListVoices {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream("fasttts.properties")) {
            props.load(is);
        }
        String apiKey = props.getProperty("elevenlabs.api.key");
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.elevenlabs.io/v1/voices"))
            .header("xi-api-key", apiKey)
            .GET()
            .build();
            
        System.out.println("Fetching voices for your account...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response:\n" + response.body());
    }
}
