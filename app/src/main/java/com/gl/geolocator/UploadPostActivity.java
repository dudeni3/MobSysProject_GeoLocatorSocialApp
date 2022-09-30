package com.gl.geolocator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gl.geolocator.entities.peeps;
import com.gl.geolocator.entities.post;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.ui.auth.AuthUI;
import com.gl.geolocator.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Arrays;
import java.util.List;
public class UploadPostActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    String path;
    ImageView iv;
    DatabaseReference mDatabase;
    FirebaseUser firebaseUser;
    ProgressDialog progressDialog;
    StorageReference storageRef;
    private Location mLastLocation;
    boolean flag=false;
    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;
    double latitude=0.0, longitude=0.0;
    private LocationRequest mLocationRequest;
    String userName, profile, pic;
    Bitmap bitmap;
    String downloadUrl;
    String postId;
    TextView caption;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploadpost);
        // First we need to check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
            createLocationRequest();
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
                            .build(),
                    1
            );
        } else {
            flag=displayLocation();
            // mDatabase = FirebaseDatabase.getInstance().getReference();
            iv = (ImageView) findViewById(R.id.uiv);
            Intent i1 = getIntent();
            path = i1.getStringExtra("data1");
            bitmap = BitmapFactory.decodeFile(path);
            iv.setImageBitmap(bitmap);
            firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mDatabase.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    peeps pep = dataSnapshot.getValue(peeps.class);
                    if (pep != null) {
                        userName = pep.getn();
                        profile = pep.getprof();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // If there's an upload in progress, saveImage the reference so you cancel query it later
        if (storageRef != null) {
            outState.putString("reference", storageRef.toString());
        }
    }
    public void uploadImage(View v) {
        progressDialog = new ProgressDialog(UploadPostActivity.this);
        progressDialog.setMessage("Uploading file....");
        // pd.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        if (latitude!=0.0 && longitude!=0.0) {
            caption = (TextView) findViewById(R.id.title);
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference imagesRef = storage.getReference();
            storageRef = imagesRef.child("images");
            Uri file = Uri.fromFile(new File(path));
// Create the file metadata
            String imagemetadata = "image/jpeg";
                pic=file.getLastPathSegment();
            if(pic!=null && pic.contains("png")){
                imagemetadata = "image/png";
            }
            else if(pic!=null && pic.contains("gif")){
                imagemetadata = "image/gif";
            }
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType(imagemetadata)
                    .build();
// Upload file and metadata to the path 'images/mountains.jpg'
            UploadTask uploadTask = storageRef.child(pic).putFile(file, metadata);
//            UploadTask uploadTask = storageRef.child(file.getLastPathSegment()).putFile(file, metadata);
            if(progressDialog !=null && !progressDialog.isShowing())
                progressDialog.show();
// Listen for state changes, errors, and completion of the upload.
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    progressDialog.setProgress(((int) progress));
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception exception) {
                    // Handle unsuccessful uploads
                    Toast.makeText(UploadPostActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    finish();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Handle successful uploads on complete
                    progressDialog.dismiss();
                    storageRef.child(pic).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            downloadUrl = uri.toString();
                            Toast.makeText(UploadPostActivity.this, "Upload complete", Toast.LENGTH_SHORT).show();
                            updateDB(caption.getText().toString());
                            finish();
                        }
                    });
                }
            });
        } else {
            Toast.makeText(UploadPostActivity.this,"Couldn't get the location. Make sure location is enabled on the device",Toast.LENGTH_SHORT).show();
        }
    }
    void updateDB(String cap) {
        String locality = Utils.getAddress(UploadPostActivity.this,latitude,longitude);
        System.out.println("locality "+locality);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        post pe1 = new post(downloadUrl, cap,1,pic);
        postId = mDatabase.child("posts").push().getKey();
        post pe = new post(downloadUrl, cap, userName, profile,1, firebaseUser.getUid(), postId,locality);
        mDatabase.child("posts").child(postId).setValue(pe);
        mDatabase.child(firebaseUser.getUid()+"posts").child(postId).setValue(pe1);
        GeoFire geoFire = new GeoFire(mDatabase.child("gf"));
        geoFire.setLocation(postId, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
            }
        });
    }
    public void cancel(View v) {
        File sourceFile = new File(path);
        if (sourceFile.exists() || sourceFile.isFile()) {
            sourceFile.delete();
        }
        Intent in = new Intent(UploadPostActivity.this, ProfileActivity.class);
        startActivity(in);
    }
    public void saveImage(View v) {
        finish();
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
        checkPlayServices();
        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    /**
     * Method to display the location on UI
     * */
    private boolean displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(45000);
        mLocationRequest.setFastestInterval(18000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    /**
     * Method to verify google play services on the device
     * */
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        }
    @Override
    public void onConnected(Bundle arg0) {
        flag=displayLocation();
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
        // Assign the new location
        mLastLocation = location;
        flag= displayLocation();
    } }