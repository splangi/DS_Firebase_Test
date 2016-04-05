package plangitech.ee.firebasetest;

import android.location.Location;

/**
 * Created by Siim on 3.04.2016.
 */
public class User {

    private UserLocation userLocation;
    private String name;


    public User(Location location){
        userLocation = new UserLocation(location);
    }

    public User() {
    }

    public UserLocation getUserLocation() {
        return userLocation;
    }

    public String getName() {
        return name == null ? "Anonymous" : name;
    }


    public static class UserLocation{

        private double lat;
        private double lng;

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        public UserLocation(Location location) {
            this.lat = location.getLatitude();
            this.lng = location.getLongitude();
        }

        public UserLocation() {
        }
    }
}
