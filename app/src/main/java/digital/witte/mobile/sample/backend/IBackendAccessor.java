package digital.witte.mobile.sample.backend;

import org.json.JSONException;

import java.io.IOException;

public interface IBackendAccessor {
    /**
     * Called to retrieve a flinkey idToken for the current user.
     *
     * @return flinkey idToken (JWT)
     */
    String requestIdToken() throws IOException, JSONException;
}
