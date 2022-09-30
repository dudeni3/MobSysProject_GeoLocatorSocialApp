package com.gl.geolocator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.gl.geolocator.utils.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
public class ChangeProfilePicActivity extends AppCompatActivity {
    String path;
    Button uploadButton;
    DatabaseReference mDatabase;
    ProgressDialog progressDialog;
    FirebaseUser firebaseUser;
    StorageReference storageRef;
    String downloadURL;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_profile_pic);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (!Utils.isnet(ChangeProfilePicActivity.this)) {
            Toast.makeText(ChangeProfilePicActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        } else {
            Intent i = getIntent();
            path = i.getStringExtra("prof");
            if(path!=null||path!="") {
                ImageView img = (ImageView) findViewById(R.id.profile_pic_imageview);
                img.setImageBitmap(BitmapFactory.decodeFile(path));
                uploadButton = (Button) findViewById(R.id.set_profile_button);
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (firebaseUser != null) {
                            mDatabase = FirebaseDatabase.getInstance().getReference();
                            upld();
                        }
                    }
                });

            }
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
    public void upld() {
        progressDialog = new ProgressDialog(ChangeProfilePicActivity.this);
        progressDialog.setMessage("Uploading file....");
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference imagesRef = storage.getReference();
        StorageReference imagesRef1 = imagesRef.child("images");
            storageRef = imagesRef1.child("prof");
            Uri file = Uri.fromFile(new File(path));
// Create the file metadata
        String imagemetadata = "image/jpeg";
        String pic=file.getLastPathSegment();
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
            UploadTask uploadTask = storageRef.child(file.getLastPathSegment()).putFile(file, metadata);
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
                    progressDialog.dismiss();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Handle successful uploads on complete
                    progressDialog.dismiss();
                    storageRef.child(pic).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            downloadURL = uri.toString();
                            up(downloadURL);
                        }
                    });
                }
            });
    }
    void up(String cap) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child(firebaseUser.getUid()).child("prof").setValue(downloadURL).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mDatabase.child("users").child(firebaseUser.getUid()).child("prof").setValue(downloadURL).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ChangeProfilePicActivity.this, "Profile Picture updated", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        finish();
                    }
                });
            }
        });
    }
}