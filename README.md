# witte-mobile-sample-android

The WITTE mobile sample for Android is a sample Android project illustrating the basic steps to integrate the Tapkey SDK in conjunction with the WITTE backend in order to be able to trigger (open/close) a box.

## Features
* Authenticate with the Tapkey Mobile Library
* Query local keys
* Trigger (open/close) a box

## Getting Started

### Add your WITTE Customer ID, SDK Key and Subscription Key 

```java
// App.java

/**
 * Your WITTE Customer Id.
 * TODO: Add your WITTE Customer Id here.
 */
private final static int CustomerId = -1;

/**
 * Your WITTE SDK Key.
 * TODO: Add your WITTE SDK Key here.
 */
private final static String SdkKey = "Todo: Add your WITTE sdk key here";

/**
 * Your WITTE Subscription Key.
 * TODO: Add your WITTE Subscription Key here.
 */
private final static String SubscriptionKey = "Todo: Add your WITTE subscription key here";
```

### Add a WITTE User ID
For the sake of simplicity this sample app uses a single user ID which is hard coded in the App class. Before you can actually use this sample app to open and close flinkey boxes you need to create a user in the WITTE backend and assign the constant WitteUserId with your users ID.  
```java
// App.java

/**
 * User Id of one specific WITTE user (this id needs to be retrieved at runtime in production apps)
 * TODO: Add your WITTE User Id here.
 */
public final static int WitteUserId = -1;
```

### Authentication
A user needs to be authenticated with the Tapkey backend via the Tapkey Mobile Library. This is achieved using a Java Web Token - the idToken - which is retrieved from the Witte backend. The idToken needs to be exchanged for an access token which is passed on to the Tapkey Mobile Library.

#### Retrieve access token
The access token is retrieved from the Tapkey backend by exchanging an idToken which needs to be retrieved from the WITTE backend for a specific user. 
1. Retrieve idToken from the Witte backen
2. Retrieve access token from the Tapkey backend by exchanging the idToken

The WITTE Mobile Library provides a class (IdTokenRequest) which can be used to query the idToken. For the token exchange we are using the [AppAuth for Android](https://github.com/openid/AppAuth-Android) library by OpenID. The example contains a class WitteTokenProvider that shows the whole process.

```java
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

import digital.witte.wittemobilelibrary.Configuration;
import digital.witte.wittemobilelibrary.net.IdTokenRequest;

public class WitteTokenProvider {

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
    private final Configuration _witteConfiguration;
    private final int _witteUserId;

    public WitteTokenProvider(Context context, Configuration witteConfiguration, int witteUserId) {
        _context = context;
        _witteConfiguration = witteConfiguration;
        _witteUserId = witteUserId;
    }

    public Promise<String> AccessToken() {
        PromiseSource<String> promiseSource = new PromiseSource<>();

        Async.executeAsync(() -> {
            // retrieve WITTE idToken
            IdTokenRequest request = new IdTokenRequest();
            String idToken = request.execute(_witteConfiguration, _witteUserId);
            return idToken;

        }).continueOnUi(idToken -> {
            // retrieve Tapkey access token
            Uri.Builder builder = new Uri.Builder();
            Uri authorizationServer = builder
                    .scheme(AuthConfigScheme)
                    .encodedAuthority(AuthConfigAuthority)
                    .build();

            AuthorizationServiceConfiguration.fetchFromIssuer(authorizationServer, (serviceConfiguration, ex) -> {
                if(null != ex) {
                    promiseSource.setException(ex);
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
                    authService.performTokenRequest(tokenRequest, (response, ex1) -> {
                        if(null != ex1) {
                            promiseSource.setException(ex1);
                        }
                        else {
                            promiseSource.setResult(response.accessToken);
                        }
                    });
                }
            });

            return null;
        }).conclude();

        return promiseSource.getPromise();
    }
}
```

#### Login to the Tapkey backend
The authentication with the Tapkey backend is done using the UserManager object which is part of the Tapkey Mobile Library. An access token is passed to UserManager's logInAsync method.

```java
WitteTokenProvider witteTokenProvider = new WitteTokenProvider(
        this.getContext(),
        App.WitteConfiguration,
        App.WitteUserId);

witteTokenProvider.AccessToken()
        .continueOnUi(accessToken -> {
            if(null != accessToken && accessToken != "") {
                // login with access token
                _userManager.logInAsync(accessToken, CancellationTokens.None)
                        .continueOnUi(userId -> {
                            // success
                            return null;
                        })
                        .catchOnUi(e -> {
                            // authentication failed
                            return null;
                        })
                        .finallyOnUi(() -> {
                            // call completed
                        });
            }
            return null;
        })
        .catchOnUi(e -> {
            e.printStackTrace();
            return null;
        })
        .finallyOnUi(() -> {
            if(null != _progressDialog){
                _progressDialog.dismiss();
            }
            _progressDialog = null;
        });
```

#### Install an token refresh handler for re-authentication
In order to enable the Tapkey Mobile Library ot re-authenticate a user a custom token refresh handler needs to be implemented and registered. The class WitteTokenRefreshHandler is part of this sample.

```java
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

```

### Query local keys
```java
String userId = _userManager.getUsers().get(0);
_keyManager.queryLocalKeysAsync(userId, CancellationTokens.None)        
    .continueOnUi((Func1<List<KeyDetails>, Void, Exception>) cachedKeyInformations -> {
        // success
        return null;
    })
    .catchOnUi(e -> {
        // error
        return null;
    })
    .conclude();
```

### Trigger a box
```java
final int timeoutInMs = 60 * 1000;
        CancellationToken timeout = CancellationTokens.fromTimeout(timeoutInMs);

        String bluetoothAddress = _bleLockScanner.getLock(physicalLockId).getBluetoothAddress();
        _bleBleLockCommunicator.executeCommandAsync(bluetoothAddress, physicalLockId, tlcpConnection -> _commandExecutionFacade.triggerLockAsync(tlcpConnection, timeout), timeout)
                .continueOnUi(commandResult -> {
                    if (commandResult.getCommandResultCode() == CommandResult.CommandResultCode.Ok) {
                        // get the 10 byte box feedback from the command result
                        Object object = commandResult.getResponseData();
                        if (object instanceof byte[]) {

                            byte[] responseData = (byte[]) object;
                            try {
                                BoxFeedback boxFeedback = BoxFeedback.create(responseData);
                                int boxState = boxFeedback.getBoxState();
                                if (BoxState.UNLOCKED == boxState) {

                                    Log.d(TAG, "Box has been opened");
                                }
                                else if (BoxState.LOCKED == boxState) {

                                    Log.d(TAG, "Box has been closed");
                                }
                                else if (BoxState.DRAWER_OPEN == boxState) {

                                    Log.d(TAG, "The drawer of the Box is open.");
                                }
                            }
                            catch (IllegalArgumentException iaEx) {

                                Log.e(TAG, iaEx.getMessage());
                            }
                        }
                        return true;
                    }
                    else {
                        // triggerLock error
                        return false;
                    }
                })
                .catchOnUi(e -> {
                    // triggerLock exception
                    return false;
                });
```
