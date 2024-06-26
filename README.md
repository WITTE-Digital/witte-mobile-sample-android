# witte-mobile-sample-android

The WITTE mobile sample showcases the integration of the Tapkey Mobile SDK in an Android app. Its focus is on simplicity, so it is easy to understand with the least dependencies possible. Therefore, it is not a reference implementation that adheres to architectural best practices.

Please visit the flinkey for Developers pages for a Getting Started guide and further information: 
* https://developers.flinkey.de/mobile-sdk/getting-started/android/

## What it does
1. Authenticates with the Tapkey Mobile SDK
2. Triggers flinkey boxes
3. Checks digital keys

## Prerequisites
* Access to the flinkey UAT Portal with activated developer feature
* A flinkey box in the flinkey UAT environment
* An actual Android phone (the emulator will not work)
* A GitHub Account to create a personal access token (PAT)

## Build & Run
### Configure access to GitHub packages
This app has a dependency to the [witte-mobile-mobile-library-for-android](https://github.com/WITTE-Digital/witte-mobile-library-for-android). The library is available through GitHub packages which requires authentication. [Create a personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) with the scope 'read:packages' and enter it as value of the variable 'password'. Your GitHub user name needs to be entered as value of the variable 'username'.

```gradle
// File: app/build.gradle
maven {
    url "https://maven.pkg.github.com/WITTE-Digital/witte-mobile-library-for-android"
    credentials {
        username = githubProperties['gpr.usr'] ?: (System.getenv("GPR_USER") ?: '<GitHub user name>')
        password = githubProperties['gpr.key'] ?: (System.getenv("GPR_API_KEY") ?: '<GitHub personal access token>')
    }
}
```

### Add your Customer Id, SDK Key, flinkey-API-Key and API Manager Credentials
This sample includes requests to the flinkey API which require ids, keys and credentials. This allows to use the app in a self-contained way without having an app backend in place. 

**None of these ids, keys and credentials must be included in production apps!** 

Production apps must not call the flinkey API themselfes. Instead they need their own backend which handles the communication to the flinkey API via server to server communication.

```java
// File: DemoBackendAccessor.java

/**
 * Your flinkey Customer Id.
 * TODO: Add your flinkey Customer Id here.
 */
public final static int FlinkeyCustomerId = ...;

/**
 * Your SDK Key.
 * TODO: Add your flinkey SDK Key here.
 */
public final static String FlinkeySdkKey = ...;

/**
 * Your flinkey-API_Key.
 * TODO: Add your flinkey Subscription Key here.
 */
public final static String FlinkeyApiKey = ...;

/**
 * Your flinkey API Manager Username
 * TODO: Add your flinkey API Manager Username here.
 */
private final static String FlinkeyApiManagerUsername = ...;

/**
 * Your flinkey API Manager Password
 * TODO: Add your flinkey API Manager Password here.
 */
private final static String FlinkeyApiManagerPassword = ...;
```

### Add a flinkey user Id
For the sake of simplicity this sample app uses a single user Id. Production apps will retrieve the user Ids dynamically through their own back backend.

```java
// File: DemoBackendAccessor.java

/**
 * User Id of one specific flinkey user.
 * TODO: Add your flinkey user Id here.
 */
public final static int FlinkeyUserId = ...;
```

## More Information
* flinkey for Developers: https://developers.flinkey.com/
* Tapkey for Developers: https://developers.tapkey.io/
* Tapkey Mobile Library Reference Documentation: https://developers.tapkey.io/mobile/android/reference/Tapkey.MobileLib/latest/