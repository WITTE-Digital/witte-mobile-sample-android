package digital.witte.mobile.sample;

import android.content.Context;
import android.net.Uri;

import com.tapkey.mobile.concurrent.Async;
import com.tapkey.mobile.concurrent.Promise;
import com.tapkey.mobile.concurrent.PromiseSource;

import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenRequest;

import java.util.HashMap;
import java.util.HashSet;

import digital.witte.mobile.sample.backend.IBackendAccessor;

/**
 * The TokenProvider class is responsible for providing access tokens for authentication purposes.
 * It retrieves the Tapkey access token via token exchange using the provided idToken.
 */
public class TokenProvider {
    private final static String AuthConfigScheme = "https";
    private final static String AuthConfigAuthority = "login.tapkey.com";

    private final static String TokenExchangeClientId = "wma-native-mobile-app";
    private final static String TokenExchangeGrantType = "http://tapkey.net/oauth/token_exchange";

    private final static String TokenExchangeScopeRegisterMobiles = "register:mobiles";
    private final static String TokenExchangeScopeReadUser = "read:user";
    private final static String TokenExchangeScopeHandleKeys = "handle:keys";

    private final static String TokenExchangeParamProviderKey = "provider";
    private final static String TokenExchangeParamSubjectTokenTypeKey = "subject_token_type";
    private final static String TokenExchangeParamSubjectTokenKey = "subject_token";
    private final static String TokenExchangeParamAudienceKey = "audience";
    private final static String TokenExchangeParamRequestedTokenTypeKey = "requested_token_type";

    private final static String TokenExchangeParamProviderValue = "wma.oauth";
    private final static String TokenExchangeParamSubjectTokenTypeValue = "jwt";
    private final static String TokenExchangeParamAudienceValue = "tapkey_api";
    private final static String TokenExchangeParamRequestedTokenTypeValue = "access_token";

    private final Context _context;
    private final IBackendAccessor _backendAccessor;

    /**
     * Constructs a new TokenProvider object.
     *
     * @param context The context of the application.
     * @param myBackend The backend accessor used to retrieve tokens.
     */
    public TokenProvider(Context context, IBackendAccessor myBackend) {
        _context = context;
        _backendAccessor = myBackend;
    }

    /**
     * Retrieves the access token for the API.
     *
     * @return A Promise that resolves to a String representing the access token.
     * @throws IllegalArgumentException if the flinkey idToken is null or empty.
     */
    public Promise<String> AccessToken() {
        PromiseSource<String> promiseSource = new PromiseSource<>();

        Async.executeAsync(_backendAccessor::requestIdToken).continueOnUi(idToken -> {
            if (null == idToken || "".equals(idToken)) {
                promiseSource.setException(new IllegalArgumentException("The flinkey idToken must not be null or empty."));
            }
            else {
                // Retrieve the Tapkey access token via token exchange.
                Uri.Builder builder = new Uri.Builder();
                Uri authorizationServer = builder
                        .scheme(AuthConfigScheme)
                        .encodedAuthority(AuthConfigAuthority)
                        .build();

                AuthorizationServiceConfiguration.fetchFromIssuer(authorizationServer, (serviceConfiguration, fetchFromIssuerException) -> {
                    if (null != fetchFromIssuerException) {
                        promiseSource.setException(fetchFromIssuerException);
                    }
                    else {
                        TokenRequest.Builder tokenRequestBuilder =
                                new TokenRequest.Builder(serviceConfiguration, TokenExchangeClientId)
                                        .setCodeVerifier(null)
                                        .setGrantType(TokenExchangeGrantType)
                                        .setScopes(new HashSet<String>() {{
                                            add(TokenExchangeScopeRegisterMobiles);
                                            add(TokenExchangeScopeReadUser);
                                            add(TokenExchangeScopeHandleKeys);
                                        }})
                                        .setAdditionalParameters(new HashMap<String, String>() {{
                                            put(TokenExchangeParamProviderKey, TokenExchangeParamProviderValue);
                                            put(TokenExchangeParamSubjectTokenTypeKey, TokenExchangeParamSubjectTokenTypeValue);
                                            put(TokenExchangeParamSubjectTokenKey, idToken);
                                            put(TokenExchangeParamAudienceKey, TokenExchangeParamAudienceValue);
                                            put(TokenExchangeParamRequestedTokenTypeKey, TokenExchangeParamRequestedTokenTypeValue);
                                        }});

                        TokenRequest tokenRequest = tokenRequestBuilder.build();

                        AuthorizationService authService = new AuthorizationService(_context);
                        authService.performTokenRequest(tokenRequest, (response, performTokenRequestException) -> {
                            if (null != performTokenRequestException) {
                                promiseSource.setException(performTokenRequestException);
                            }
                            else {
                                promiseSource.setResult(response.accessToken);
                            }
                        });
                    }
                });
            }
            return null;
        }).catchOnUi(executeAsyncException -> {
            promiseSource.setException(executeAsyncException);
            return null;
        }).conclude();

        return promiseSource.getPromise();
    }
}
