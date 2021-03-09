package digital.witte.mobile.sample.backend;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class is for demonstration purpose only and should not be part of your production app.
 * The corresponding functionality should be part of your backend implementation instead.
 */
class DemoOAuth2TokenRequest {
    /**
     * Called to retrive a flinkey API access token.
     *
     * @param flinkeyApiKey      The flinkey-API-Key.
     * @param apiManagerUserName The username of the API Manager.
     * @param apiManagerPassword The password of the API Manager.
     * @return flinkey API access token (JWT).
     */
    public String execute(String flinkeyApiKey, String apiManagerUserName, String apiManagerPassword) throws IOException, JSONException {
        String flinkeyAccessToken;

        // prepare content
        String postBody = String.format("username=%s&password=%s&grant_type=password",
                URLEncoder.encode(apiManagerUserName, "UTF-8"),
                URLEncoder.encode(apiManagerPassword, "UTF-8"));

        byte[] postBodyBytes = postBody.getBytes(StandardCharsets.UTF_8);
        int postBodyLength = postBodyBytes.length;

        // create connection
        URL url = new URL("https://api.flinkey.de/v3/oauth2/token");
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
        httpsURLConnection.setRequestMethod("POST");
        httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpsURLConnection.setRequestProperty("charset", "utf-8");
        httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(postBodyLength));
        httpsURLConnection.setRequestProperty("flinkey-API-Key", flinkeyApiKey);

        OutputStream outputStream = httpsURLConnection.getOutputStream();
        outputStream.write(postBodyBytes);
        outputStream.flush();
        outputStream.close();

        // execute request
        InputStream inputStream = httpsURLConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        StringBuilder sb = new StringBuilder();
        while (null != (line = br.readLine())) {
            sb.append(line);
        }
        inputStream.close();
        httpsURLConnection.disconnect();

        // evaluate result
        JSONObject response = new JSONObject(sb.toString());
        flinkeyAccessToken = response.optString("access_token", "");

        return flinkeyAccessToken;
    }
}
