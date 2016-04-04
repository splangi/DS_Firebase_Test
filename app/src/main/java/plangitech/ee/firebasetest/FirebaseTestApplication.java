
package plangitech.ee.firebasetest;

import android.app.Application;

import com.firebase.client.Firebase;

/**
 * Created by Siim on 4.04.2016.
 */
public class FirebaseTestApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
