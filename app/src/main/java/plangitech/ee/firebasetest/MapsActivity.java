package plangitech.ee.firebasetest;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.core.AuthProviderType;
import com.firebase.ui.auth.core.FirebaseLoginBaseActivity;
import com.firebase.ui.auth.core.FirebaseLoginError;
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

public class MapsActivity extends FirebaseLoginBaseActivity implements Firebase.AuthResultHandler, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, ChildEventListener {

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Firebase firebaseRef
            = new Firebase("https://dsfirebasetest.firebaseio.com/users");
    private Firebase firebaseUserRef;
    private Map<String, Marker> markerMap = new HashMap<>();
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        initViews();

    }

    private void initViews(){
        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setText(firebaseRef.getAuth() != null ? "Logout" : "Login");
        locationRequest = LocationRequest.create().setInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseRef.getAuth() == null) {
                    showLoginDialog();
                } else {
                    logout();
                }
            }
        });
    }

    private void showLoginDialog(){
        String[] strings = new String[]{"Facebook login", "Anonymous login"};
        new AlertDialog.Builder(MapsActivity.this).setItems(strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showFirebaseLoginPrompt();
                        break;
                    case 1:
                        firebaseRef.authAnonymously(MapsActivity.this);
                        break;

                }
            }
        }).show();
    }


    @Override
    protected Firebase getFirebaseRef() {
        return firebaseRef;
    }

    @Override
    protected void onFirebaseLoginProviderError(FirebaseLoginError firebaseLoginError) {
        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onFirebaseLoginUserError(FirebaseLoginError firebaseLoginError) {
        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onFirebaseLoggedIn(AuthData authData) {
        super.onFirebaseLoggedIn(authData);
        firebaseUserRef = firebaseRef.child(authData.getUid());
        firebaseUserRef.onDisconnect().removeValue();
        firebaseRef.child(authData.getUid()).child("name").setValue(authData.getProviderData().get("displayName"));
        loginButton.setText("Logout");
        startBrodcastingLocation();
    }

    @Override
    protected void onFirebaseLoggedOut() {
        super.onFirebaseLoggedOut();
        if (firebaseUserRef != null){
            firebaseUserRef.removeValue();
            stopBroadcastingLocation();
        }
        loginButton.setText("Login");
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
        setEnabledAuthProvider(AuthProviderType.FACEBOOK);
    }

    @Override
    protected void onStop() {
        stopListeningForLocationUpdates();
        googleApiClient.disconnect();
        logout();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        googleMap.clear();
        startListeneingForLocationUpdates();
        googleMap.setMyLocationEnabled(true);
    }

    private void stopBroadcastingLocation(){
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        if (firebaseUserRef != null){
            firebaseUserRef.removeValue();
            firebaseUserRef = null;
        }
        logout();
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        Log.d("onChildAdded", dataSnapshot.toString());
        if (!markerMap.containsKey(dataSnapshot.getKey())){
            String uid = dataSnapshot.getKey();
            User user = dataSnapshot.getValue(User.class);
            if (user.getUserLocation() != null){
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.title(user.getName());
                markerOptions.position(new LatLng(user.getUserLocation().getLat(), user.getUserLocation().getLng()));
                Marker marker = map.addMarker(markerOptions);
                markerMap.put(uid, marker);
            }

        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        Log.d("onChildChanged", dataSnapshot.toString());
        Marker marker = markerMap.get(dataSnapshot.getKey());
        if (marker != null){
            User user = dataSnapshot.getValue(User.class);
            marker.setPosition(new LatLng(user.getUserLocation().getLat(), user.getUserLocation().getLng()));
            if (!marker.getTitle().equals(user.getName())){
                marker.setTitle(user.getName());
            }
        } else{
            onChildAdded(dataSnapshot, s);
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
        startBrodcastingLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (firebaseUserRef != null && location != null){
            firebaseUserRef.child("userLocation").setValue(new User.UserLocation(location));
        }
    }

    private void startBrodcastingLocation(){
        if (googleApiClient.isConnected()){
            onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onAuthenticated(AuthData authData) {
        onFirebaseLoggedIn(authData);
    }

    @Override
    public void onAuthenticationError(FirebaseError firebaseError) {
        Log.d("onAuthenticationError", firebaseError.getMessage());
        Toast.makeText(MapsActivity.this, "Anonymous authentication failed", Toast.LENGTH_SHORT).show();
    }
}
