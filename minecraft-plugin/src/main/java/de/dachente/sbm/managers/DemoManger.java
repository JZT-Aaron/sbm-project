package de.dachente.sbm.managers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import de.dachente.sbm.main.Main;

public class DemoManger {
    private static String stackId = System.getenv("STACK_ID");
    public static Instant timestamp;

    public static void setCloseTimestamp(Instant newTimestamp) {
        timestamp = newTimestamp;
    }

    public static void closeServer() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://mc-manager-api:1320/delete-server/" + stackId))
            .header("Accept", "application/json")
            .DELETE()
            .build();
        
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if(response.statusCode() >= 200 && response.statusCode() < 300) Main.getPlugin().getLogger().info("Command for Deletion was transmitted successfully.");
                    else Main.getPlugin().getLogger().warning("Error at Deleting. Statuscode: " + response.statusCode());
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }
}
