package digital.witte.mobile.sample;

import com.tapkey.mobile.auth.TokenRefreshHandler;
import com.tapkey.mobile.concurrent.CancellationToken;
import com.tapkey.mobile.concurrent.Promise;

public class WitteTokenRefreshHandler implements TokenRefreshHandler {

    private final WitteTokenProvider _witteTokenProvider;

    public WitteTokenRefreshHandler(WitteTokenProvider witteTokenProvider) {
        _witteTokenProvider = witteTokenProvider;
    }

    @Override
    public Promise<String> refreshAuthenticationAsync(String tapkeyUserId, CancellationToken cancellationToken) {
        return _witteTokenProvider.AccessToken();
    }

    @Override
    public void onRefreshFailed(String tapkeyUserId) {
        // At this point you should logout the user from the app as the token refresh is permanently
        // broken and the TapkeyMobileLib is no longer able to communicate with the Tapkey backend.
        // https://developers.tapkey.io/mobile/android/reference/Tapkey.MobileLib/latest/com/tapkey/mobile/auth/TokenRefreshHandler.html#onRefreshFailed-java.lang.String-
    }
}
