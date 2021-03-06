package com.example.user.tofiee;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.util.DirectionConverter;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;


import com.example.user.tofiee.interfaces.ConstantInterface;
import com.example.user.tofiee.util.AppUtility;
import com.example.user.tofiee.util.RunTimePermission;


public class MapsActivity extends AppCompatActivity implements ConstantInterface, OnMapReadyCallback, DirectionCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private TextView textAddress;
    private LatLng currentLatLng;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private AppUtility appUtility;
    private RunTimePermission runTimePermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);



        appUtility = new AppUtility(this);
        runTimePermission = new RunTimePermission(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        textAddress = (TextView) findViewById(R.id.textAddress);
        textAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This is to prevent user to click on the map under the distance text.
            }
        });
        if (appUtility.checkPlayServices()) {
            googleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            googleApiClient.connect();
        }


    }

    @Override
    public void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }



    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
        }

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(latLng));

                //Google Direction Library
                GoogleDirection.withServerKey(getString(R.string.map_direction_key))
                        .from(currentLatLng)
                        .to(new LatLng(latLng.latitude, latLng.longitude))
                        .transportMode(TransportMode.DRIVING)
                        .execute(MapsActivity.this);

                showDistance(latLng);

            }
        });
    }

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        if (direction.isOK()) {
            ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
            googleMap.addPolyline(DirectionConverter.createPolyline(this, directionPositionList, 6, ContextCompat.getColor(this, R.color.Red)));
        }
    }

    @Override
    public void onDirectionFailure(Throwable t) {

    }

    private void showDistance(LatLng latLng) {
        Location locationA = new Location("Location A");
        locationA.setLatitude(latLng.latitude);
        locationA.setLongitude(latLng.longitude);
        Location locationB = new Location("Location B");
        locationB.setLatitude(currentLatLng.latitude);
        locationB.setLongitude(currentLatLng.longitude);

        textAddress.setText("Distance : " + new DecimalFormat("##.##").format(locationA.distanceTo(locationB)) + "m");



        Float  dist=locationA.distanceTo(locationB);
        showFares(dist);
    }
    private void showFares(Float dist) {

        final AlertDialog.Builder alertdialog=new AlertDialog.Builder(this);
        alertdialog.setTitle("Alert Info....!!!");
        alertdialog.setMessage("Distance - "+dist+" m");
        alertdialog.setIcon(R.drawable.ic_launcher_foreground);

        alertdialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Toast.makeText(getApplicationContext(),"Ok",Toast.LENGTH_SHORT).show();
            }
        });
        alertdialog.show();
    }

    // checking Runtime permission
    private void getPermissions(String[] strings) {

        runTimePermission.requestPermission(strings, new RunTimePermission.RunTimePermissionListener() {

            @Override
            public void permissionGranted() {
                locationChecker(googleApiClient, MapsActivity.this);
            }

            @Override
            public void permissionDenied() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    // Checking whether location service is enable or not.
    public void locationChecker(GoogleApiClient mGoogleApiClient, final Activity activity) {

        // Creating location request object
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(activity, 1000);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                        break;
                }
            }
        });
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (googleMap == null) {
            mapFragment.getMapAsync(this);
        }
    }
    public void searchLocation(View view) //btnSearchDestination Click
    {

        googleMap.clear();
        EditText locationSearch = (EditText) findViewById(R.id.searchDest);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            googleMap .addMarker(new MarkerOptions().position(latLng).title(location));
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            //Toast.makeText(getApplicationContext(),address.getLatitude()+" "+address.getLongitude(),Toast.LENGTH_LONG).show();



            //-------------------------+++++++++++++++++++++++++++++++++++++++++++++++++
            GoogleDirection.withServerKey(getString(R.string.map_direction_key))
                    .from(currentLatLng)
                    .to(new LatLng(latLng.latitude, latLng.longitude))
                    .transportMode(TransportMode.DRIVING)
                    .execute(MapsActivity.this);

            showDistance(latLng);

            //-------------------------+++++++++++++++++++++++++++++++++++++++++++++++++

        }
    }


}