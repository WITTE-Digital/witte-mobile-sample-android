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
    public Promise<String> refreshAuthenticationAsync(String s, CancellationToken cancellationToken) {
        return _witteTokenProvider.AccessToken();
    }

    @Override
    public void onRefreshFailed(String s) {
    }
}
