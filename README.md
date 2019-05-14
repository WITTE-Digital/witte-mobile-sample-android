# witte-mobile-sample-android

The WITTE mobile sample for Android is a sample Android project illustrating the basic steps to integrate the Tapkey SDK in conjunction with the WITTE backend in order to be able to trigger (open/close) a box.

## Features
* Authenticate with the Tapkey Mobile Library
* Query local keys
* Trigger (open/close) a box

## Getting Started
### Authentication
A user needs to be authenticated with the Tapkey backend via the Tapkey Mobile Library. This is achived using a Java Web Token - the idToken - that is retrieved from the Tapkey backend and passed on to the Tapkey Mobile Library.

#### Retrieve idToken
The idToken is retrieved from the WITTE backend for a specific user. The WITTE Mobile Library provides a class (IdTokenRequest) that can be used to query the idToken.

```java
import digital.witte.wittemobilelibrary.net.IdTokenRequest;

String idToken = null;

try {

    int witteUserId = <your WITTE user ID>;
    IdTokenRequest request = new IdTokenRequest();
    idToken = request.execute(_witteConfiguration, witteUserId);
}
catch (IOException e) {

    e.printStackTrace();
}
catch (Exception e) {

    e.printStackTrace();
}
```

#### Authenticate with the Tapkey backend
The authentication with the Tapkey backend is done using the UserManager object which is part of the Tapkey Mobile Library. A Identity object containing the idToken is passed to UserManager's authenticateAsync method.
```java
Identity identity = new Identity(Configuration.IpId, idToken);
_userManager.authenticateAsync(identity, CancellationTokens.None)
        .continueOnUi(user -> {
            // success
            return null;
        })
        .catchOnUi(e -> {
            // authentication failed
            return null;
        })
        .finallyOnUi(() -> {
            // call completed
        })
        .conclude();
```

#### Install and identity provider for re-authentication
In order to enable the Tapkey Mobile Library ot re-authenticate a user a custom identity provider needs to be implemented and registered. The class WitteIdentityProvider is part of WITTE mobile library.

```java
import digital.witte.wittemobilelibrary.Configuration;
import digital.witte.wittemobilelibrary.net.IdTokenRequest;

public class WitteIdentityProvider implements IdentityProvider {

    private final String _sdkKey;
    private final String _subscriptionKey;
    private final int _customerId;
    private final int _userId;

    public WitteIdentityProvider(Configuration configuration, int userId) {

        _sdkKey = configuration.getWitteSdkKey();
        _subscriptionKey = configuration.getWitteSubscriptionKey();
        _customerId = configuration.getWitteCustomerId();
        _userId = userId;
    }

    @Override
    public Promise<Void> logOutAsync(
        User user, 
        CancellationToken cancellationToken) {

        // unsed
        return null;
    }

    @Override
    public Promise<Identity> refreshToken(
        User user, 
        CancellationToken cancellationToken) {

        return Async.executeAsync(() -> {
            Identity result = null;

            try {

                IdTokenRequest request = new IdTokenRequest();
                String idToken = request.execute(
                    _customerId, 
                    _sdkKey, 
                    _subscriptionKey, 
                    _userId);
                
                result = new Identity(Configuration.IpId, idToken);
            }
            catch (IOException e) {

                e.printStackTrace();
            }
            catch (Exception e){

                e.printStackTrace();
            }

            return result;
        });
    }
}
```

### Query local keys
```java
_keyManager.queryLocalKeysAsync(
    firstUser, 
    forceUpdate, 
    CancellationTokens.None)
        .continueOnUi((Func1<List<CachedKeyInformation>, Void, Exception>) cachedKeyInformations -> {
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
_bleLockManager
        .executeCommandAsync(
                new String[0],
                lockId,
                tlcpConnection -> _commandExecutionFacade.triggerLockAsync(tlcpConnection, CancellationTokens.None),
                CancellationTokens.None)
        .continueOnUi(commandResult -> {
            if (commandResult.getCommandResultCode() == CommandResult.CommandResultCode.Ok) {

                // success
                Toast.makeText(getContext(), "triggerLock successful", Toast.LENGTH_SHORT).show();

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
                // error
                return false;
            }
        })
        .catchOnUi(exception -> {
            // exception
            return false;
        });
    }
```
## Get Help