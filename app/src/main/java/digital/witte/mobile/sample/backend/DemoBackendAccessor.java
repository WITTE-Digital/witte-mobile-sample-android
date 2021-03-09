package digital.witte.mobile.sample.backend;

import org.json.JSONException;

import java.io.IOException;

import digital.witte.mobile.sample.BuildConfig;

/**
 * This class is for demonstration purpose only and should not be part of your production app.
 * The corresponding functionality should be part of your backend implementation instead.
 */
public class DemoBackendAccessor implements IBackendAccessor {
    /**
     * Your flinkey Customer-ID.
     * TODO: Add your flinkey Customer Id here.
     */
    public final static int FlinkeyCustomerId = Integer.parseInt(BuildConfig.FLINKEY_CUSTOMER_ID);

    /**
     * Your SDK Key.
     * TODO: Add your SDK Key here.
     */
    public final static String FlinkeySdkKey = BuildConfig.FLINKEY_SDK_KEY;

    /**
     * Your flinkey-API-Key.
     * TODO: Add your flinkey-API-Key here.
     */
    public final static String FlinkeyApiKey = BuildConfig.FLINKEY_API_KEY;

    /**
     * User Id of one specific flinkey user (this needs to be retrieved at runtime in production apps)
     * TODO: Add your flinkey user Id here.
     */
    public final static int FlinkeyUserId = Integer.parseInt(BuildConfig.FLINKEY_USER_ID);

    /**
     * Your flinkey API Manager Username
     * TODO: Add your flinkey API Manager Username here.
     */
    private final static String FlinkeyApiManagerUsername = BuildConfig.FLINKEY_API_MANAGER_USERNAME;

    /**
     * Your flinkey API Manager Password
     * TODO: Add your flinkey API Manager Password here.
     */
    private final static String FlinkeyApiManagerPassword = BuildConfig.FLINKEY_API_MANAGER_PASSWORD;

    /**
     * {@inheritDoc}
     */
    @Override
    public String requestIdToken() throws IOException, JSONException {
        String flinkeyIdToken = null;

        DemoOAuth2TokenRequest demoOAuth2TokenRequest = new DemoOAuth2TokenRequest();
        String flinkeyAccessToken = demoOAuth2TokenRequest.execute(FlinkeyApiKey, FlinkeyApiManagerUsername, FlinkeyApiManagerPassword);
        if (null != flinkeyAccessToken) {
            DemoSdkTokenRequest demoSdkTokenRequest = new DemoSdkTokenRequest();
            flinkeyIdToken = demoSdkTokenRequest.execute(FlinkeyCustomerId, FlinkeyApiKey, FlinkeySdkKey, FlinkeyUserId, flinkeyAccessToken);
        }

        return flinkeyIdToken;
    }
}
