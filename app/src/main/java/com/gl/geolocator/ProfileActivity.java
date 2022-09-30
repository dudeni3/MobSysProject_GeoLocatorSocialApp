package com.gl.geolocator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.bumptech.glide.Glide;
import com.gl.geolocator.entities.peeps;
import com.gl.geolocator.entities.post;
import com.gl.geolocator.utils.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.geofire.GeoFire;
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
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
public class ProfileActivity extends AppCompatActivity {
    DatabaseReference mDatabase;
    ProgressDialog pd;
    FirebaseUser firebaseUser;
    DatabaseReference mPostReference;
    ArrayList<HashMap<String, String>> personList1;
    MyPostsAdapter myPostsAdapter;
    RecyclerView recyclerView;
    int TAKE_PHOTO_CODE = 999;
    String filePath;
    String userName, profilePic;
    ImageView profilePicImageView;
    TextView userNameTextView;
    SwipeRefreshLayout mswipe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        recyclerView = (RecyclerView) findViewById(R.id.myPosts_recycler_view);
        myPostsAdapter = new MyPostsAdapter(ProfileActivity.this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(ProfileActivity.this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        mswipe = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh1);
        mswipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getMyPosts();
                mswipe.setRefreshing(false);
            }
        });
        if (!Utils.isnet(ProfileActivity.this)) {
            Toast.makeText(ProfileActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        } else {
            firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                mDatabase = FirebaseDatabase.getInstance().getReference();
                profilePicImageView = (ImageView) findViewById(R.id.profile_pic);
                userNameTextView = (TextView) findViewById(R.id.nameuser);
                mDatabase.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        peeps pep = dataSnapshot.getValue(peeps.class);
                        if (pep != null) {
                            userName = pep.getn();
                            userNameTextView.setText(userName);
                            profilePic = pep.getprof();
                            if(profilePic !=null || profilePic !="") {

                                Glide.with(ProfileActivity.this)
                                        .load(profilePic)
                                        .error(R.drawable.avatar)
                                        .placeholder(R.drawable.avatar)
                                        .into(profilePicImageView);
                            }
                            else{
                                profilePicImageView.setImageResource(R.drawable.avatar);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        }
                });
                pd = new ProgressDialog(ProfileActivity.this);
                pd.setMessage("Loading...");
                pd.show();
                getMyPosts();
            }
        }
        FloatingActionButton fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 113);
            }
        });
        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File image = null;
                try {
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    image = File.createTempFile(
                            "Campfire_" + System.currentTimeMillis(),  /* prefix */
                            ".jpg",         /* suffix */
                            storageDir      /* directory */
                    );
                    filePath = image.getAbsolutePath();
                } catch (IOException e) {
                }
                if (image != null) {
                    Uri photoURI = FileProvider.getUriForFile(ProfileActivity.this,
                            "com.gl.geolocator.fileprovider",
                            image);
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
                }
            }
        });
    }
    public void getMyPosts() {
        if (firebaseUser != null) {
        personList1 = new ArrayList<HashMap<String, String>>();
        mPostReference = FirebaseDatabase.getInstance().getReference().child(firebaseUser.getUid() + "posts");
        mPostReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    // TODO: handle the post
                    post post = postSnapshot.getValue(post.class);
                    HashMap<String, String> persons = new HashMap<String, String>();
                    if (post != null) {
                        persons.put("title", post.getcap());
                        persons.put("hearts", String.valueOf(post.getecount()));
                        persons.put("purl", post.getpurl());
                        persons.put("time", String.valueOf(post.getdattime()));
                        persons.put("pic", post.getimg());
                        persons.put("postid", postSnapshot.getKey());
                        personList1.add(persons);
                    }
                }
                Log.e("Main5.adapter", "new adapter");
                if(personList1.size()>=1) {
                    myPostsAdapter.setData(personList1);
                    recyclerView.setAdapter(myPostsAdapter);
                }
                else{
                    Toast.makeText(ProfileActivity.this,"Nothing posted",Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("Main5.mPostReference", "loadPost:onCancelled", databaseError.toException());
            }
        });
        pd.dismiss();
    }
}
    String delimg,postid;
    public void rem(View v){
        mDatabase = FirebaseDatabase.getInstance().getReference();
        RelativeLayout parentrow=(RelativeLayout)v.getParent();
        ImageView im=(ImageView)parentrow.getChildAt(0);
        delimg=String.valueOf(im.getTag());
        TextView t=(TextView)parentrow.getChildAt(1);
        postid=t.getTag().toString();
        t=(TextView)parentrow.getChildAt(3);
        myPostsAdapter.personList.remove(Integer.parseInt(t.getTag().toString()));
        myPostsAdapter.notifyDataSetChanged();
        GeoFire geoFire = new GeoFire(mDatabase.child("gf"));
        geoFire.removeLocation(postid, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    } else {
            mDatabase.child(firebaseUser.getUid()+"posts").child(postid).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                   } else {
                mDatabase.child("posts").child(postid).removeValue(new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                        } else {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReference();
// Create a reference to the file to delete
                        StorageReference desertRef = storageRef.child("images/" + delimg);
// Delete the file
                        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // File deleted successfully

                                Toast.makeText(ProfileActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(Exception exception) {
                                // Uh-oh, an error occurred!
                                exception.printStackTrace();
                            }
                        });
                    }
                    }
                });
            }
            }
        });
                }
            }
        });
    }
    public void changeProfilePic(View v){
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Intent i = new Intent(ProfileActivity.this, ChangeProfilePicActivity.class);
            i.putExtra("prof", picturePath);
            startActivity(i);
        }
        if (requestCode == 113 && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Intent i = new Intent(ProfileActivity.this, UploadPostActivity.class);
            i.putExtra("data1", picturePath);
            startActivity(i);
        }
        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            Log.d("CameraDemo", "Pic saved");
            Intent in=new Intent(ProfileActivity.this, UploadPostActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            in.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            in.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            in.putExtra("data1", filePath);
            startActivity(in);
        }
    }
}