package com.map.google.chiang.googlemap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.google.android.gms.common.api.CommonStatusCodes.API_NOT_CONNECTED;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ResultCallback<LocationSettingsResult>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 100;
    private static final int REQUEST_CHECK_SETTINGS = 101;
    private boolean permissionGranted = false, settingText = false, login = false;
    private GoogleMap mMap = null;
    private GoogleApiClient mGoogleApiClient;
    private Marker marker = null;
    private LocationRequest locationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LatLng latLng = null;
    private EditText addressET;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private PlacesAutoCompleteAdapter mAutoCompleteAdapter;
    private AppCompatImageView delete;
    private ViewGroup infoWindow;
    private TextView infoTitle;
    private TextView infoCords;
    private Button infoLoginButton;
    private Button infoFavButton;
    private OnInfoWindowElemTouchListener infologinButtonListener, infofavButtonListener;
    private MapWrapperLayout mapWrapperLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        buildGoogleApiClient();

        setupLocationRequest();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_REQUEST_CODE);
            }
            return;
        } else {
            buildLocationSettingsRequest();
            checkLocationSettings();
        }
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    private void setupLocationRequest() {
        // 18sec per request for 12hours usage
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if(permissionGranted) {
            if(mMap != null) {
                mMap.getUiSettings().setMapToolbarEnabled(false);
                mapWrapperLayout.init(mMap, getPixelsFromDp(this, 39 + 20));
                this.infoWindow = (ViewGroup)getLayoutInflater().inflate(R.layout.marker_info_layout, null);
                this.infoTitle = (TextView)infoWindow.findViewById(R.id.addressTV);
                this.infoCords = (TextView)infoWindow.findViewById(R.id.cordsTV);
                this.infoLoginButton = (Button)infoWindow.findViewById(R.id.loginBtn);
                this.infoFavButton = (Button)infoWindow.findViewById(R.id.favBtn);

                this.infologinButtonListener = new OnInfoWindowElemTouchListener(infoLoginButton,
                        ContextCompat.getDrawable(this, R.drawable.login_norm),
                        ContextCompat.getDrawable(this, R.drawable.login_pressed)) {

                    @Override
                    protected void onClickConfirmed(View v, Marker markerz) {
                        if(marker != null) {
                            marker.remove();
                        }
                        login = true;
                        float zoom = mMap.getCameraPosition().zoom;
                        getAddressFromLatLng(markerz.getPosition().latitude, markerz.getPosition().longitude, mMap, zoom, false);
                        Toast.makeText(MapsActivity.this, "CONGRATS, you have logged in!", Toast.LENGTH_SHORT).show();
                    }
                };

                this.infofavButtonListener = new OnInfoWindowElemTouchListener(infoFavButton,
                        ContextCompat.getDrawable(this, R.drawable.fav_norm),
                        ContextCompat.getDrawable(this, R.drawable.fav_pressed)) {

                    @Override
                    protected void onClickConfirmed(View v, Marker markerz) {
                        if(marker != null) {
                            marker.remove();
                        }
                        login = false;
                        float zoom = mMap.getCameraPosition().zoom;
                        getAddressFromLatLng(markerz.getPosition().latitude, markerz.getPosition().longitude, mMap, zoom, false);
                        Toast.makeText(MapsActivity.this, marker.getTitle() + " Added to Favourite! And you are logged out!", Toast.LENGTH_SHORT).show();
                    }
                };

                this.infoLoginButton.setOnTouchListener(infologinButtonListener);
                this.infoFavButton.setOnTouchListener(infofavButtonListener);

                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                    @Override
                    public void onMarkerDragStart(Marker marker) { }

                    @Override
                    public void onMarkerDrag(Marker marker) { }

                    @Override
                    public void onMarkerDragEnd(Marker markerDragged) {
                        if(marker != null) {
                            marker.remove();
                        }
                        float zoom = mMap.getCameraPosition().zoom;
                        getAddressFromLatLng(markerDragged.getPosition().latitude, markerDragged.getPosition().longitude, mMap, zoom, false);
                    }
                });

                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) { return null; }

                    @Override
                    public View getInfoContents(Marker marker) {
                        LatLng latLng = marker.getPosition();
                        infoTitle.setText(marker.getTitle());
                        infoCords.setText("Latitude: " + latLng.latitude + " , Longitude: " + latLng.longitude);
                        if(login) {
                            // do stuffs for user not login
                            infofavButtonListener.setMarker(marker);
                            infoFavButton.setEnabled(true);
                            infoFavButton.setVisibility(View.VISIBLE);
                            infoLoginButton.setEnabled(false);
                            infoLoginButton.setVisibility(View.GONE);
                        } else {
                            // do stuffs for user logged in
                            infoFavButton.setEnabled(false);
                            infoFavButton.setVisibility(View.GONE);
                            infoLoginButton.setEnabled(true);
                            infoLoginButton.setVisibility(View.VISIBLE);
                            infologinButtonListener.setMarker(marker);
                        }
                        mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);
                        return infoWindow;
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    buildLocationSettingsRequest();
                    checkLocationSettings();
                } else {
                    permissionGranted = false;
                    Toast.makeText(getApplicationContext(), "This application requires LocationInfo Permission to be Allowed.", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        mLocationSettingsRequest = builder.build();
    }

    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                /* Load stuffs */
                permissionGranted = true;
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                if (mGoogleApiClient.isConnected()) {
                    requestLocationUpdates();
                }
                // Set up autocompletePlaces here
                setupAutoCompletePlaces();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Toast.makeText(getBaseContext(), "Your device does not support LocationInfo Services.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) { }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_CANCELED && data !=null){
            switch (requestCode) {
                // Check for the integer request code originally supplied to startResolutionForResult().
                case REQUEST_CHECK_SETTINGS:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            permissionGranted = true;
                            if (mGoogleApiClient.isConnected()) {
                                requestLocationUpdates();
                            }
                            // Set up autoCompletePlaces here
                            setupAutoCompletePlaces();
                            break;
                    }
                    break;
            }
        } else {
            permissionGranted = false;
            Log.i("android", "onActivityResult Req chk settings cencelled " + Boolean.toString(permissionGranted));
            Toast.makeText(getBaseContext(), "This application requires LocationInfo Service to be turned on!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(permissionGranted) {
            if(mGoogleApiClient.isConnected()) {
                requestLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(permissionGranted) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(permissionGranted)
            mGoogleApiClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            // stop something
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if(latLng != null) {
            if(mMap != null) {
                if(marker != null) {
                    marker.remove();
                }
                getAddressFromLatLng(latLng.latitude, latLng.longitude, mMap, 15, false);
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
                addressET.setEnabled(true);
            }
        }
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_REQUEST_CODE);
            }
            return;
        }
        // Request LocationInfo with Loop
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    private void setupAutoCompletePlaces() {
        addressET = (EditText) findViewById(R.id.addressET);
        delete=(AppCompatImageView)findViewById(R.id.crossImageView);
        mapWrapperLayout = (MapWrapperLayout)findViewById(R.id.map_relative_layout);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAutoCompleteAdapter =  new PlacesAutoCompleteAdapter(this, R.layout.searchview_adapter,
                mGoogleApiClient, new LatLngBounds(new LatLng(-0, 0), new LatLng(0, 0)), new AutocompleteFilter.Builder().
                setTypeFilter(Place.TYPE_COUNTRY).setCountry("SG").build());

        mRecyclerView=(RecyclerView)findViewById(R.id.locationResultRecyclerView);
        mLinearLayoutManager=new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mAutoCompleteAdapter);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addressET.setText("");
            }
        });

        addressET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(settingText) {
                    settingText = false;
                } else {
                    if (mGoogleApiClient.isConnected()) {
                        mAutoCompleteAdapter.getFilter().filter(charSequence.toString());
                    }else if(!mGoogleApiClient.isConnected()){
                        Toast.makeText(getApplicationContext(), API_NOT_CONNECTED,Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
        mRecyclerView.addOnItemTouchListener(
                new PlacesItemClickListener(this, new PlacesItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        try {
                            final PlacesAutoCompleteAdapter.PlaceAutocomplete item = mAutoCompleteAdapter.getItem(position);
                            final String placeId = String.valueOf(item.placeId);
                        //Issue a request to the Places Geo Data API to retrieve a Place object with additional details about the place.
                            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                                    .getPlaceById(mGoogleApiClient, placeId);
                            placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                                @Override
                                public void onResult(PlaceBuffer places) {
                                    if (places.getCount() == 1) {
                                        //RecyclerView Item Clicked
                                        LatLng latLng = new LatLng(places.get(0).getLatLng().latitude, places.get(0).getLatLng().longitude);
                                        if(marker != null) {
                                            marker.remove();
                                        }
                                        float zoom = mMap.getCameraPosition().zoom;
                                        getAddressFromLatLng(latLng.latitude, latLng.longitude, mMap, zoom, true);
                                        //marker = addMarker(latLng, String.valueOf(places.get(0).getAddress()));
                                        settingText = true;
                                        //addressET.setText(String.valueOf(places.get(0).getAddress()));
                                        mAutoCompleteAdapter.clearList();
                                        hideKeyboard();
                                        Toast.makeText(getApplicationContext(), String.valueOf(places.get(0).getAddress()), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "SOMETHING_WENT_WRONG", Toast.LENGTH_SHORT).show();
                                    }
                                    places.release();
                                }
                            });
                        }
                        catch (Exception ex)
                        {
                            Toast.makeText(getApplicationContext(), "SOMETHING_WENT_WRONG", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
        );
        hideKeyboard();
    }

    private Marker addMarker(LatLng latLng, String title) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.favourite))
                .draggable(true));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        // Zoom out to zoom level 10, animating with a duration of 2 seconds.
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
        return marker;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void getAddressFromLatLng(double latitude, double longitude, GoogleMap mMap, float zoom, boolean setAddress)
    {
        String geocodeRequestUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + String.valueOf(latitude) + "," + String.valueOf(longitude) + "&key=" + getResources().getString(R.string.google_maps_key);

        GetAddress getAddress = new GetAddress(this, mMap, zoom, setAddress);
        getAddress.execute(geocodeRequestUrl);
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    private static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    public void setAddressET(String address) {
        addressET.setText(address);
    }
}
