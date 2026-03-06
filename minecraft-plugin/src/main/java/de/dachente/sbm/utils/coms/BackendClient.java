package de.dachente.sbm.utils.coms;

import com.google.gson.Gson;

import de.dachente.sbm.main.Main;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackendClient {
    private final OkHttpClient httpClient;
    private final String backendUrl;
    private final String apiKey;
    private final Gson gson;

    public BackendClient(String backendUrl, String apiKey) {
        this.backendUrl = backendUrl;
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    public boolean postEvent(String endpoint, Object eventData) {
        try  {
            String jsonBody = gson.toJson(eventData);

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                .url(backendUrl + endpoint)
                .addHeader("X-API-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            Response response = httpClient.newCall(request).execute();

            if(response.isSuccessful()) {
                Main.getPlugin().getLogger().info("Event send: " + endpoint);
                return true;
            } else {
                Main.getPlugin().getLogger().warning("Backend Post Error: " + response.code());
                return false;
            }
        } catch (Exception e) {
            Main.getPlugin().getLogger().warning("Error while sending Event: " + e.getMessage());
            return false;
        }
    }
}
