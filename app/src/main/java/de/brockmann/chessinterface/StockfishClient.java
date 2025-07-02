package de.brockmann.chessinterface;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

/**
 * Simple client for the stockfish.online API.
 */
public class StockfishClient {
    private static final String API_ENDPOINT = "https://stockfish.online/api/s/v2.php";
    private final SimpleChessEngine fallback = new SimpleChessEngine();

    public boolean start() {
        // No initialization required for HTTP engine
        return true;
    }

    public void stop() {
        // Nothing to stop
    }

    public void setElo(int elo) {
        // This API does not support ELO based strength
    }

    public String getBestMove(String fen, int depth) {
        try {
            String query = String.format("fen=%s&depth=%d",
                    URLEncoder.encode(fen, StandardCharsets.UTF_8.name()),
                    depth);
            URL url = new URL(API_ENDPOINT + "?" + query);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject obj = new JSONObject(sb.toString());
                String best = obj.optString("bestmove");
                if (!best.isEmpty()) {
                    String[] parts = best.split(" ");
                    if (parts.length > 1) {
                        return parts[1];
                    }
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            // ignore and fall back to local engine
        }

        // if API call failed or returned nothing, use the lightweight engine
        return fallback.bestMove(fen, 2);
    }
}
