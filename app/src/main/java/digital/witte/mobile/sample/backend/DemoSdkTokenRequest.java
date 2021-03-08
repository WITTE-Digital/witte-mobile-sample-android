package digital.witte.mobile.sample.backend;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class is for demonstration purpose only and should not be part of your production app.
 * The corresponding functionality should be part of your backend implementation instead.
 */
class DemoSdkTokenRequest {
    /**
     * Called to retrieve a flinkey idToken.
     *
     * @param flinkeyCustomerId  The flinkey Customer-ID.
     * @param flinkeyApiKey      The flinkey-API-Key.
     * @param flinkeySdkKey      The flinkey SDK Key.
     * @param flinkeyUserId      A flinkey user id.
     * @param flinkeyAccessToken A flinkey API access token.
     * @return flinkey idToken (JWT)
     */
    public String execute(int flinkeyCustomerId, String flinkeyApiKey, String flinkeySdkKey, int flinkeyUserId, String flinkeyAccessToken) throws IOException, JSONException {
        String idToken;

        // prepare content
        JSONObject postParams = new JSONObject();
        postParams.put("customerId", flinkeyCustomerId);
        postParams.put("userId", flinkeyUserId);
        postParams.put("sdkKey", flinkeySdkKey);
        String postBody = postParams.toString();

        byte[] postBodyBytes = postBody.getBytes(StandardCharsets.UTF_8);

        // create connection
        URL url = new URL("https://api.flinkey.de/v3/sdk/token");
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
        httpsURLConnection.setRequestMethod("POST");
        httpsURLConnection.setRequestProperty("Content-Type", "application/json");
        httpsURLConnection.setRequestProperty("flinkey-API-Key", flinkeyApiKey);
        httpsURLConnection.setRequestProperty("Authorization", String.format("Bearer %s", flinkeyAccessToken));

        OutputStream outputStream = httpsURLConnection.getOutputStream();
        outputStream.write(postBodyBytes);
        outputStream.flush();
        outputStream.close();

        // execute request
        InputStream inputStream = httpsURLConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder sb = new StringBuilder();
        while (null != (line = br.readLine())) {
            sb.append(line);
        }
        inputStream.close();
        httpsURLConnection.disconnect();

        // evaluate result
        JSONObject response = new JSONObject(sb.toString());
        idToken = response.optString("id_token", "");

        return idToken;
    }
}
