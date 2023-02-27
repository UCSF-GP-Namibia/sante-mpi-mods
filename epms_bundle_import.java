package bmt;
 
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path; 

public class epms_bundle_import {

    // one instance, reuse
    private final static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();


    public static HttpResponse<String> authenticateClient() throws Exception {

        // form parameters
        Map<Object, Object> data;
        data = new HashMap<>();
        data.put("grant_type", "client_credentials");
        data.put("client_id", "Quantum");
        data.put("client_secret", "***");

        HttpRequest request = HttpRequest.newBuilder()
                .POST(buildFormDataFromMap(data))
                .uri(URI.create("http://localhost:8080/auth/oauth2_token"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;

    }

    HttpResponse<String> refreshToken(String refreshToken) throws Exception {

        // form parameters
        Map<Object, Object> data;
        data = new HashMap<>();
        data.put("grant_type", "refresh_token");
        data.put("refresh_token", refreshToken);
        data.put("client_id", "Quantum");
        data.put("client_secret", "***");

        HttpRequest request = HttpRequest.newBuilder()
                .POST(buildFormDataFromMap(data))
                .uri(URI.create("http://localhost:8080/auth/oauth2_token"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response;

    }

    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
       
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
    public static void main( String[] args ) throws Exception 
    {

        for (int i=1; i<=63; i++){ 
            String fn = "epms/bundle_"+i+".json";

            Path fileName = Path.of(fn);
            String content = Files.readString(fileName);
            
            HttpResponse<String> auth_response = authenticateClient();
            JSONObject token = new JSONObject(auth_response.body());
            
            String accessToken = (String) token.get("access_token"); 
            
            String auth = "Bearer " + accessToken;

            URL url = new URL("http://localhost:8080/fhir/Bundle");
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Authorization", auth);
            http.setRequestProperty("Accept", "text/html,application/fhir+xml,application/xml;q=0.9,*/*;q=0.8");
        
            http.setRequestProperty("Accept", "application/fhir+json");
            http.setRequestProperty("Content-Type", "application/fhir+json");

            String data = content;

            byte[] out = data.getBytes(StandardCharsets.UTF_8);

            OutputStream stream = http.getOutputStream();
            stream.write(out);

            System.out.println(fn + ": "+http.getResponseCode() + " " + http.getResponseMessage());
            http.disconnect();
        }
    }
}
