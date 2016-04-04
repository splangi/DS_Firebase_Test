package plangitech.ee.firebasetest;

import android.location.Location;

/**
 * Created by Siim on 3.04.2016.
 */
public class UserLocation {

    private double lat;
    private double lng;

    public UserLocation(Location location){
        this.lat = location.getLatitude();
        this.lng = location.getLongitude();
    }

    public UserLocation() {
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
