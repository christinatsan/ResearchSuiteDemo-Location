package com.example.christina.researchsuitedemo.geofenceManagement;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

/**
 * Created by jameskizer on 6/13/17.
 */

public class RSuiteGeofenceManager implements OnCompleteListener<Void> {



    private static final String TAG = RSuiteGeofenceManager.class.getSimpleName();

    private static RSuiteGeofenceManager manager = null;
    private static Object managerLock = new Object();

    private double home_lat = 0;
    private double home_lng = 0;
    private double work_lat = 0;
    private double work_lng = 0;

    static final int LOITERING_DELAY = 60*1000;


    private ArrayList<Geofence> mGeofenceList;

//    public void setHomeCoords(double lat, double lng){
//        this.home_lat = lat;
//        this.home_lng = lng;
//    }
//
//    public void setWorkCoords(double lat, double lng){
//        this.work_lat = lat;
//        this.work_lng = lng;
//    }

    public double getHomeLat(){
        return this.home_lat;
    }

    public double getHomeLng(){
        return this.home_lng;
    }

    public double getWorkLat(){
        return this.work_lat;
    }

    public double getWorkLng(){
        return this.work_lng;
    }



    public enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent = null;
    private Context mContext;

    @Nullable
    public static RSuiteGeofenceManager getInstance() {
        synchronized (managerLock) {
            return manager;
        }
    }

    @Nullable
    public static void init(Context context,double homeLat,double homeLng, double workLat, double workLng) {
        synchronized (managerLock) {
            manager = new RSuiteGeofenceManager(context,homeLat,homeLng,workLat,workLng);
//            if (manager == null) {
//                manager = new RSuiteGeofenceManager(context,homeLat,homeLng,workLat,workLng);
//            }
        }
    }

    private RSuiteGeofenceManager(Context context,double homeLat,double homeLng, double workLat, double workLng) {
        this.home_lat = homeLat;
        this.home_lng = homeLng;
        this.work_lat = workLat;
        this.work_lng = workLng;
        this.mGeofenceList = this.populateGeofenceList();
        this.mContext = context;
        mGeofencingClient = LocationServices.getGeofencingClient(context);
    }


    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    public boolean startMonitoringGeofences(Context context) {
        if (!checkPermissions(context)) {
            return false;
        }

        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent(context))
                .addOnCompleteListener(this);

        return true;
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    public boolean stopMonitoringGeofences(Context context) {
        if (!checkPermissions(context)) {
            return false;
        }

        mGeofencingClient.removeGeofences(getGeofencePendingIntent(context)).addOnCompleteListener(this);
        return true;
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            updateGeofencesAdded(mContext, !getGeofencesAdded(mContext));
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = RSuiteGeofenceErrorMessages.getErrorString(mContext, task.getException());
            Log.w(TAG, errorMessage);
        }
    }

    private ArrayList<Geofence> populateGeofenceList() {

        ArrayList<Geofence> geofenceList = new ArrayList<>();

        Log.d("thought coords: ",String.valueOf(getHomeLat()));

        Geofence.Builder geofenceBuilder_home = new Geofence.Builder()
                .setRequestId("HOME_150_1")
                .setCircularRegion(this.home_lat,this.home_lng,150.0f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE);

        geofenceBuilder_home.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        geofenceBuilder_home.setLoiteringDelay(LOITERING_DELAY);
        geofenceList.add(geofenceBuilder_home.build());

        Geofence.Builder geofenceBuilder_work = new Geofence.Builder()
                .setRequestId("WORK_150_1")
                .setCircularRegion(this.work_lat,this.work_lng,150.0f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE);

        geofenceBuilder_work.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        geofenceBuilder_work.setLoiteringDelay(LOITERING_DELAY);
        geofenceList.add(geofenceBuilder_work.build());

// USE THIS IF YOU DONT WANT A DWELL
// geofenceBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);

        return geofenceList;
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent(Context context) {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent == null) {
            Intent intent = new Intent(context, RSuiteGeofenceTransitionsIntentService.class);
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // addGeofences() and removeGeofences().
            mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return mGeofencePendingIntent;
    }



    /**
     * Returns true if geofences were added, otherwise false.
     */
    private boolean getGeofencesAdded(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                RSuiteConstants.GEOFENCES_ADDED_KEY, false);
    }

    /**
     * Stores whether geofences were added ore removed in {@link SharedPreferences};
     *
     * @param added Whether geofences were added or removed.
     */
    private void updateGeofencesAdded(Context context, boolean added) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(RSuiteConstants.GEOFENCES_ADDED_KEY, added)
                .apply();
    }

    private boolean checkPermissions(Context context) {
        int permissionState = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
