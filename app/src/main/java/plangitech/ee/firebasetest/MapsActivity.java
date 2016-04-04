package plangitech.ee.firebasetest;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, ChildEventListener {

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Firebase firebaseRef  = new Firebase("https://dsfirebasetest.firebaseio.com/positions");
    private Firebase firebaseLocationRef;
    private Map<String, Marker> markerMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        locationRequest = LocationRequest.create().setInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        stopLocationService();
        googleApiClient.disconnect();
        stopListeningForLocationUpdates();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        googleMap.clear();
        stopBroadcastingLocation();
        startListeneingForLocationUpdates();
        googleMap.setMyLocationEnabled(true);
        startListeneingForLocationUpdates();
    }

    private void startLocationService(){
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void stopLocationService(){
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    private void startBroadcastingLocation(){
        firebaseRef.authAnonymously(new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                firebaseLocationRef = firebaseRef.child(authData.getUid());
                firebaseLocationRef.onDisconnect().removeValue();
                onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.d("onAuthenticationError", firebaseError.getMessage());
                Toast.makeText(MapsActivity.this, "Anonymous authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopBroadcastingLocation(){
        if (firebaseLocationRef != null){
            firebaseLocationRef.removeValue();
            firebaseLocationRef = null;
        }
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        Log.d("onChildAdded", dataSnapshot.toString());
        if (!markerMap.containsKey(dataSnapshot.getKey())){
            String uid = dataSnapshot.getKey();
            UserLocation userLocation = dataSnapshot.getValue(UserLocation.class);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(userLocation.getLat(), userLocation.getLng()));
            Marker marker = map.addMarker(markerOptions);
            markerMap.put(uid, marker);
        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        Log.d("onChildChanged", dataSnapshot.toString());
        Marker marker = markerMap.get(dataSnapshot.getKey());
        if (marker != null){
            UserLocation userLocation = dataSnapshot.getValue(UserLocation.class);
            marker.setPosition(new LatLng(userLocation.getLat(), userLocation.getLng()));
        }
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        Log.d("onChildRemoved", dataSnapshot.toString());
        Marker marker = markerMap.remove(dataSnapshot.getKey());
        if (marker != null){
            marker.remove();
        }
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {

    }

    private void startListeneingForLocationUpdates(){
        firebaseRef.addChildEventListener(this);
    }

    private void stopListeningForLocationUpdates(){
        firebaseRef.removeEventListener(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationService();
        startBroadcastingLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (firebaseLocationRef != null && location != null){
            firebaseLocationRef.setValue(new UserLocation(location));
        }
    }

}
