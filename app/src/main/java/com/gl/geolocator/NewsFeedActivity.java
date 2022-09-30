package com.gl.geolocator;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gl.geolocator.entities.peeps;
import com.gl.geolocator.entities.post;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.gl.geolocator.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
public class NewsFeedActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    int SIGN_IN_REQUEST_CODE = 123;
    int REQUEST_CHECK_SETTINGS = 456;
    String myLocality;

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                Toast.makeText(this,
                        "Successfully signed in. Welcome!",
                        Toast.LENGTH_LONG)
                        .show();
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                peeps pe = new peeps(firebaseUser.getDisplayName(), firebaseUser.getEmail(), 0);
                peeps pe1 = new peeps(firebaseUser.getDisplayName(), firebaseUser.getEmail());
                mDatabase = FirebaseDatabase.getInstance().getReference();
                mDatabase.child(firebaseUser.getUid()).setValue(pe);
                mDatabase.child("users").child(firebaseUser.getUid()).setValue(pe1);
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    if (response != null) {
                        Log.e(TAG, "Sign-in cancelled: "+response.getError().getMessage());
                    }
                }
                Toast.makeText(this,
                        "We couldn't sign you in. Please try again later.",
                        Toast.LENGTH_LONG)
                        .show();
                if (response != null) {
                    Log.e(TAG, "Sign-in error: "+response.getError().getMessage());
                }
                // Close the app
                finish();
            }
        }
        if (requestCode == 112 && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Intent i = new Intent(NewsFeedActivity.this, ChangeProfilePicActivity.class);
            i.putExtra("prof", picturePath);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(i);
        }
        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            Log.d("CameraDemo", "Pic saved");
            Intent in=new Intent(NewsFeedActivity.this, UploadPostActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            in.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            in.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            in.putExtra("data1", filePath);
            startActivity(in);
        }
        if (requestCode == REQUEST_CHECK_SETTINGS){
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        if (checkPlayServices()) {
                            // Resuming the periodic location updates
                            if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
                                startLocationUpdates();
                            }
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        showLocationEnableDialog();
                        break;
                    default:
                        break;
                }
        }
    }
    DatabaseReference mDatabase;
    ProgressDialog progressDialog;
    FirebaseUser firebaseUser;
    DatabaseReference mPostReference;
    ValueEventListener postListener;
    ArrayList<HashMap<String, String>> personList1,keylist;
    PostsAdapter postsAdapter;
    RecyclerView recyclerView;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    LocationRequest mLocationRequest;
    boolean mRequestingLocationUpdates = true;
    double latitude=0.0, longitude=0.0;
    private static final String TAG = NewsFeedActivity.class.getSimpleName();
    boolean locationFlag = false;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    int TAKE_PHOTO_CODE = 999;
    String filePath;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferenceEditor;
    JSONObject jsonObject;
    JSONArray jsonArray;
    SwipeRefreshLayout swipeRefreshLayout;
    Boolean keyEnteredFlag =false;
    HashMap<String, String> keyHashMap;
    Query queryRef;
    String postid,purl;
    String uid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newsfeed);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferenceEditor = sharedPreferences.edit();
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        postsAdapter = new PostsAdapter(NewsFeedActivity.this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(NewsFeedActivity.this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        progressDialog = new ProgressDialog(NewsFeedActivity.this);
        progressDialog.setMessage("Loading...");
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getFeed();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            showLocationPermissionRequiredDialog();
        }
        else{
            createLocationRequest();
        }
        if (!Utils.isnet(NewsFeedActivity.this)) {
            Toast.makeText(NewsFeedActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());
            firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null) {
                // Start sign in/sign updateDB activity
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .setLogo(R.mipmap.ic_launcher)      // Set logo drawable
                                .setTheme(R.style.FirebaseUITheme)
                                .build(),
                        SIGN_IN_REQUEST_CODE
                );
            }
                mDatabase = FirebaseDatabase.getInstance().getReference();
                updateValuesFromBundle(savedInstanceState);
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    File image = null;
                        try {
                            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                             image = File.createTempFile(
                                    "Campfire_"+System.currentTimeMillis(),  /* prefix */
                                    ".jpg",         /* suffix */
                                    storageDir      /* directory */
                            );
                            filePath =image.getAbsolutePath();
                        }
                        catch (IOException e)
                        {
                        }
                        if (image != null) {
                            Uri photoURI = FileProvider.getUriForFile(NewsFeedActivity.this,
                                    "com.gl.geolocator.fileprovider",
                                    image);
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
                        }
                    }
                });

    }
    void showLocationEnableDialog(){
        // notify user
        AlertDialog.Builder dialog = new AlertDialog.Builder(NewsFeedActivity.this);
        dialog.setMessage("Location not enabled");
        dialog.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
                //get gps
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
            }
        });
        dialog.show();
    }
    void showLocationPermissionRequiredDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(NewsFeedActivity.this);
        dialog.setTitle("Need for Permissions");
        dialog.setMessage("Location,Camera and Storage Permissions are Required to view posts of this location");
        dialog.setPositiveButton("Give Permissions", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                ActivityCompat.requestPermissions(NewsFeedActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_LOCATION);
                //get gps
            }
        });
        dialog.show();
    }
    void getFeed(){
        try{
            if (sharedPreferences.getString(getString(R.string.hr), null) != null) {
                jsonObject = new JSONObject(sharedPreferences.getString(getString(R.string.hr), null));
            } else {
                sharedPreferenceEditor.putString(getString(R.string.hr), "{\"hearts\":[{\"pid\":\"vabc@xyz.com1492183468305.mp4\",\"purl\":\"vabc@xyz.com1492183468305.mp4\"}]}");
                sharedPreferenceEditor.apply();
            }
            jsonObject = new JSONObject(sharedPreferences.getString(getString(R.string.hr), null));
            if(jsonObject !=null) {
                jsonArray = jsonObject.getJSONArray("hearts");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //edit.putString(getString(R.string.hr), "{\"hearts\":[{\"pid\":\"vabc@xyz.com1492183468305.mp4\"}]}");
            sharedPreferenceEditor.putString(getString(R.string.hr), "{\"hearts\":[{\"pid\":\"vabc@xyz.com1492183468305.mp4\",\"purl\":\"vabc@xyz.com1492183468305.mp4\"}]}");
            sharedPreferenceEditor.apply();
        }
            locationFlag = displayLocation();
            mRequestingLocationUpdates = true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                         createLocationRequest();
                    }

                } else {
                    // permission denied, Disable the
                    // functionality that depends on this permission.
                    if (ActivityCompat.shouldShowRequestPermissionRationale(NewsFeedActivity.this, Manifest.permission.CAMERA)) {
                        //Show permission explanation dialog...
                        new AlertDialog.Builder(NewsFeedActivity.this)
                                .setTitle("Need for Permissions")
                                .setMessage("1. " + getString(R.string.app_name) + " needs access to your camera for clicking pics.\n" +
                                        "2. Also provide access to device storage for saving those pics.\n" +
                                        "3. Also location access to share in your geo-location.\n" +
                                        "3. And Internet access to have fun on " + getString(R.string.app_name) + " ;)")
                                // Specifying a listener allows you to take an action before dismissing the dialog.
                                // The dialog is automatically dismissed when a dialog button is clicked.
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (ActivityCompat.checkSelfPermission(NewsFeedActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                                                ActivityCompat.checkSelfPermission(NewsFeedActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                                                || ActivityCompat.checkSelfPermission(NewsFeedActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                                                ActivityCompat.requestPermissions(NewsFeedActivity.this,
                                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA},
                                                        MY_PERMISSIONS_REQUEST_LOCATION);
                                            }
                                    }
                                })
                                .setIcon(android.R.drawable.ic_menu_info_details)
                                .show();
                    } else {
                        //Never ask again selected, or device policy prohibits the app from having that permission.
                        //So, disable that feature, or fall back to another situation...
                        new AlertDialog.Builder(NewsFeedActivity.this)
                                .setTitle("Need for Permissions")
                                .setMessage("Location, Camera and Storage permissions are required for smooth functioning of " + getString(R.string.app_name) + ".\n" +
                                        "Please provide necessary permissions from Settings.\n")
                                // Specifying a listener allows you to take an action before dismissing the dialog.
                                // The dialog is automatically dismissed when a dialog button is clicked.
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                })
                                // A null listener allows the button to dismiss the dialog and take no further action.
                                .setNegativeButton(android.R.string.no, null)
                                .setIcon(android.R.drawable.ic_menu_info_details)
                                .show();
                    }
                }
                return;
            }

        }
    }
    void geo(){
        progressDialog.show();
        personList1 = new ArrayList<HashMap<String, String>>();
        keylist = new ArrayList<HashMap<String, String>>();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        keyHashMap = new HashMap<String, String>();
        GeoFire geoFire = new GeoFire(mDatabase.child("gf"));
        if (latitude!=0.0 && longitude!=0.0) {
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latitude, longitude), 1.0);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                   keyEnteredFlag =true;
                    keyHashMap.put("postid",key);
                    queryRef =   mDatabase.child("posts").child(key).orderByValue();//   mDatabase.child("posts").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            post post = dataSnapshot.getValue(post.class);
                            if (post != null) {
                                HashMap<String, String> persons = new HashMap<String, String>();
                                persons.put("title", post.getcap());
                                persons.put("un", post.getun());
                                persons.put("hearts", String.valueOf(post.getecount()));
                                persons.put("prof", post.getprof());
                                persons.put("purl", post.getpurl());
                                persons.put("uid", post.getuid());
                                persons.put("postid", post.getpostid());
                                if(post.getLocality() == null){
                                    persons.put("locality", myLocality);
                                }
                                else if(post.getLocality().isEmpty()){
                                    persons.put("locality", myLocality);
                                }
                                else {
                                    persons.put("locality", post.getLocality());
                                }
                                persons.put("time",String.valueOf(post.getdattime()));
                                personList1.add(persons);
                                Log.e("Main2.adapter", "new adapter");
                                postsAdapter.setData(personList1);
                                recyclerView.setAdapter(postsAdapter);
                                if(progressDialog!=null && progressDialog.isShowing())
                                    progressDialog.dismiss();
                            }
                            keylist.add(keyHashMap);
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                            if(progressDialog!=null && progressDialog.isShowing())
                                progressDialog.dismiss();
                        }
                    });
                }
                @Override
                public void onKeyExited(String key) {
                    if(progressDialog!=null && progressDialog.isShowing())
                        progressDialog.dismiss();
                    }
                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    if(progressDialog!=null && progressDialog.isShowing())
                        progressDialog.dismiss();
                }
                @Override
                public void onGeoQueryReady() {
                    if(!keyEnteredFlag){
                        Toast.makeText(NewsFeedActivity.this,"No Posts to show. Make your own post",Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onGeoQueryError(DatabaseError error) {
                    if(progressDialog!=null && progressDialog.isShowing())
                        progressDialog.dismiss();
                }
            });
        }
        if(progressDialog!=null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
    protected void createLocationRequest() {
        progressDialog.show();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(45000);
        mLocationRequest.setFastestInterval(18000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    if(progressDialog!=null && progressDialog.isShowing())
                            progressDialog.dismiss();
                        getFeed();
                } catch (ApiException exception) {
                    if(progressDialog!=null && progressDialog.isShowing())
                        progressDialog.dismiss();
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        NewsFeedActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            showLocationEnableDialog();
                            break;
                    }
                }
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (firebaseUser != null) {
            if (checkPlayServices()) {
                // Resuming the periodic location updates
                if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
                    startLocationUpdates();
                }
            }
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        // Remove post value event listener
        if (postListener != null) {
            mPostReference.removeEventListener(postListener);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Stopping the periodic location updates
        if (firebaseUser != null) {
            if (checkPlayServices()) {
                // Resuming the periodic location updates
                if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
                    stopLocationUpdates();
                }
            }
        }
    }
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        super.onSaveInstanceState(outState);
    }
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        // Update the value of mRequestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }
    }
    public void imgclick(View v) {
        RelativeLayout parentrow = (RelativeLayout) v.getParent();
        ImageView im = (ImageView) parentrow.getChildAt(0);
        String uid=String.valueOf(im.getTag());
        im = (ImageView) parentrow.getChildAt(2);
        String purl=String.valueOf(im.getTag());
        ImageButton img = (ImageButton) parentrow.getChildAt(4);
        String postid=keylist.get(Integer.parseInt(img.getTag().toString())).get("postid");
        TextView tt = (TextView) parentrow.getChildAt(3);
        Intent in =new Intent(NewsFeedActivity.this, FullScreenImageActivity.class);
        in.putExtra("posturl",purl);
        in.putExtra("title",tt.getText());
        in.putExtra("pid",postid);
        in.putExtra("uid",uid);
        startActivity(in);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // adds item to action bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_my_prof) {
            Intent in=new Intent(NewsFeedActivity.this, ProfileActivity.class);
            startActivity(in);
        }
        if (id == R.id.action_signOut) {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(NewsFeedActivity.this, "Successfully Signed Out", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (id == R.id.action_chng_prof) {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 112);
        }
        return super.onOptionsItemSelected(item);
    }
    public void hclick(View v) {
        RelativeLayout parentrow = (RelativeLayout) v.getParent();
        ImageView im = (ImageView) parentrow.getChildAt(0);
        uid=String.valueOf(im.getTag());
        im = (ImageView) parentrow.getChildAt(2);
        purl=String.valueOf(im.getTag());
        ImageButton img = (ImageButton) parentrow.getChildAt(4);
        img.setEnabled(false);
        img.setBackgroundResource(R.mipmap.hearts);
        TextView tv=(TextView)findViewById(R.id.un);
        postid=tv.getTag().toString();
        try{
            JSONObject njo = new JSONObject();
            njo.put("pid", postid);
            njo.put("purl", purl);
            jsonArray.put(njo);
            for (int k = 0; k < jsonArray.length(); k++) {
                njo = jsonArray.getJSONObject(k);
            }
            String narr = "{\"hearts\":" + jsonArray.toString() + "}";
            sharedPreferenceEditor.putString(getString(R.string.hr), narr);
            sharedPreferenceEditor.apply();
                if(postid!=null)
                    incrementCounter(postid,uid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void incrementCounter(String p,String usrid) {
        if (firebaseUser != null) {
            postid=p;
            uid=usrid;
        mDatabase.child("posts").child(postid).child("ecount").runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(final MutableData currentData) {
                    if (currentData.getValue() == null) {
                        currentData.setValue(1);
                    } else {
                        currentData.setValue((Long) currentData.getValue() + 1);
                    }
                    return Transaction.success(currentData);
                }
                @Override
                public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                }
            });
            mDatabase.child(uid+"posts").child(postid).child("ecount").runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(final MutableData currentData) {
                    if (currentData.getValue() == null) {
                        currentData.setValue(1);
                    } else {
                        currentData.setValue((Long) currentData.getValue() + 1);
                    }
                    return Transaction.success(currentData);
                }
                @Override
                public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                    if (firebaseError == null) {
                        postsAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }
    private boolean displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false;
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
             latitude = mLastLocation.getLatitude();
             longitude = mLastLocation.getLongitude();
            if (firebaseUser != null) {
                geo();

            }
            return true;
        } else {
             return false;
        }
    }
    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }
    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }
    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }
    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }
    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }
    @Override
    public void onConnected(Bundle arg0) {
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }
    @Override
    public void onLocationChanged(Location location) {
        if(mLastLocation!=null){
        // Assign the new location
        float dist = location.distanceTo(mLastLocation);
        Log.e("Main2.loc changed", "distance= " + dist);
        /*----------to get City-Name from coordinates ------------- */
            String cityName=null;
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(NewsFeedActivity.this, Locale.getDefault());
            try {
                System.out.println("latitude " +latitude);
                myLocality = Utils.getAddress(NewsFeedActivity.this,latitude,longitude);
            }
            catch (Exception e){
                myLocality = null;
            }
        if (dist >= 900) {
            // Displaying the new location on UI
            locationFlag = displayLocation();
        }
    }
        else{
            if(location!=null){
                mLastLocation = location;
                latitude = location.getLatitude();
                longitude = location.getLongitude();

                myLocality = Utils.getAddress(NewsFeedActivity.this,latitude,longitude);
                if (firebaseUser != null) {
                    geo();

                }
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(progressDialog!=null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
}