package com.example.christina.researchsuitedemo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.christina.researchsuitedemo.geofenceManagement.RSuiteGeofenceManager;
import com.example.christina.researchsuitedemo.studyManagement.RSActivity;
import com.example.christina.researchsuitedemo.studyManagement.RSActivityManager;
import com.example.christina.researchsuitedemo.studyManagement.RSApplication;
import com.example.christina.researchsuitedemo.studyManagement.RSTaskBuilderManager;

import edu.cornell.tech.foundry.researchsuitetaskbuilder.RSTBStateHelper;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MainActivity extends RSActivity {

    public static final int SHOW_AS_ACTION_ALWAYS = 2;
    private static final int REQUEST_SETTINGS = 0xff31;
    RSTBStateHelper stateHelper;
    private static final String TAG = "OnboardingActivity";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private RSuiteGeofenceManager.PendingGeofenceTask mPendingGeofenceTask = RSuiteGeofenceManager.PendingGeofenceTask.NONE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stateHelper = RSTaskBuilderManager.getBuilder().getStepBuilderHelper().getStateHelper();
        RSApplication app = (RSApplication) getApplication();
        app.initializeGeofenceManager();

        startMonitoringGeofences();
    }

    private void startMonitoringGeofences() {
        if (!checkPermissions()) {
            mPendingGeofenceTask = RSuiteGeofenceManager.PendingGeofenceTask.ADD;
            requestPermissions();
        } else {
            RSuiteGeofenceManager.getInstance().startMonitoringGeofences(this);
        }
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     */
    private void performPendingGeofenceTask() {
        if (mPendingGeofenceTask == RSuiteGeofenceManager.PendingGeofenceTask.ADD) {
            RSuiteGeofenceManager.getInstance().startMonitoringGeofences(this);
        } else if (mPendingGeofenceTask == RSuiteGeofenceManager.PendingGeofenceTask.REMOVE) {
            RSuiteGeofenceManager.getInstance().stopMonitoringGeofences(this);
        }
    }



    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        Log.i(TAG, "Requesting permission");
        // Request permission. It's possible this can be auto answered if device policy
        // sets the permission in a given state or the user denied the permission
        // previously and checked "Never ask again".
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.");
                performPendingGeofenceTask();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
                mPendingGeofenceTask = RSuiteGeofenceManager.PendingGeofenceTask.NONE;
            }
        }
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        MenuItem menuItem = menu.add("settings");
//        menuItem.setIcon(android.R.drawable.ic_menu_preferences);
//        menuItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS);
//        menuItem.setIntent(new Intent(this, SettingsActivity.class));
//        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//
//                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//                startActivityForResult(intent, MainActivity.REQUEST_SETTINGS);
//
//                return true;
//            }
//        });
//
//
//
//        return super.onCreateOptionsMenu(menu);
//
//    }
}
