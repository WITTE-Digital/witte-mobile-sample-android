package digital.witte.mobile.sample.backend;

import org.json.JSONException;

import java.io.IOException;

/**
 * This interface represents an accessor for the backend functionality.
 */
public interface IBackendAccessor {
    /**
     * Called to retrieve a flinkey idToken for the current user.
     *
     * @return flinkey idToken (JWT)
     * @throws IOException   if an I/O error occurs
     * @throws JSONException if there is an error with JSON parsing
     */
    String requestIdToken() throws IOException, JSONException;
}
