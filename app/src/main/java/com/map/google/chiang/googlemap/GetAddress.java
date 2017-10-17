package com.map.google.chiang.googlemap;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

public class GetAddress extends AsyncTask<String, String, String> {

    private MapsActivity context;
    private String url;
    private String googleAddressData;
    private GoogleMap mMap;
    private float zoomLevel;
    private boolean setAddress;

    public GetAddress(MapsActivity context, GoogleMap mMap, float zoomLevel, boolean setAddress) {
        this.context = context;
        this.mMap = mMap;
        this.zoomLevel = zoomLevel;
        this.setAddress = setAddress;
    }

    @Override
    protected String doInBackground(String... params) {
        url = (String) params[0];

        DownloadUrl downloadUrl = new DownloadUrl();

        try {
            googleAddressData = downloadUrl.readUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return googleAddressData;
    }

    @Override
    protected void onPostExecute(String s) {
        LocationInfo locationInfo = null;
        DataParser dataParser = new DataParser();
        locationInfo = dataParser.parseAddress(s);

        if(locationInfo != null) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(locationInfo.getLocation())
                    .title(locationInfo.getAddress())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.favourite))
                    .draggable(true));
            Log.i("android","Lat: " + Double.toString(locationInfo.getLocation().latitude) + " Lng: " + Double.toString(locationInfo.getLocation().longitude));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationInfo.getLocation(),zoomLevel));
            // Zoom in, animating the camera.
            mMap.animateCamera(CameraUpdateFactory.zoomIn());

            // Zoom out to zoom level 10, animating with a duration of 2 seconds.
            mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);
            context.setMarker(marker);
            if(setAddress) {
                context.setAddressET(locationInfo.getAddress());
            }
            marker.showInfoWindow();
        }
    }
}
