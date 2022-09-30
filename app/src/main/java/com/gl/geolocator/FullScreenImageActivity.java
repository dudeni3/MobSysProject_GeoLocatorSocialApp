package com.gl.geolocator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gl.geolocator.utils.OnSwipeTouchListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class FullScreenImageActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferenceEditor;
    String posturl, title;
    ImageView imageView;
    TextView textView;
    JSONObject jsonObject, jsonObject1;
    JSONArray jsonArray;
    ImageButton heartsImageButton;
    DatabaseReference mDatabase;
    FirebaseUser firebaseUser;
    int count;
    ProgressDialog progressDialog;
    boolean heartClicked = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_fullscreenimage);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferenceEditor = sharedPreferences.edit();
        RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.fullscreen_relative_layout);
        relativeLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeBottom() {
                finish();
            }
        });
        imageView =(ImageView)findViewById(R.id.myimg);
        textView =(TextView)findViewById(R.id.textView2);
        heartsImageButton =(ImageButton)findViewById(R.id.hearts);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Intent in=getIntent();
        posturl =in.getStringExtra("posturl");
        title =in.getStringExtra("title");
        postid=in.getStringExtra("pid");
        uid=in.getStringExtra("uid");
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
           sharedPreferenceEditor.putString(getString(R.string.hr), "{\"hearts\":[{\"pid\":\"vabc@xyz.com1492183468305.mp4\",\"purl\":\"vabc@xyz.com1492183468305.mp4\"}]}");
            sharedPreferenceEditor.apply();
        }
        progressDialog = new ProgressDialog(FullScreenImageActivity.this);
        progressDialog.setMessage("Loading...");
        textView.setText(title);
        new getimg().execute(posturl);
        try {
            if (jsonObject != null) {
                jsonArray = jsonObject.getJSONArray("hearts");
                for (int kkk = 0; kkk < jsonArray.length(); kkk++) {
                    jsonObject1 = jsonArray.getJSONObject(kkk);
                    if (jsonObject1.getString("purl").equals(posturl)) {
                        heartClicked = true;
                        heartsImageButton.setBackgroundResource(R.mipmap.hearts);
                        break;
                    } else {
                        heartsImageButton.setBackgroundResource(R.mipmap.dhearts);
                    }
                }
            } else {
                heartsImageButton.setBackgroundResource(R.mipmap.dhearts);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public void hrtclick(View v){
        if(heartClicked){
        }
        else{
            heartClicked = true;
            String purl= posturl;
            try{
                JSONObject njo = new JSONObject();
                //  JSONArray jar=jo.getJSONArray("hr");
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
                    incrementCounter(postid,uid,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    String uid,postid;
    public void incrementCounter(String p,String usrid,int co) {
        if (firebaseUser != null) {
            postid=p;
            uid=usrid;
            count=co;
            mDatabase.child("posts").child(postid).child("ecount").runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(final MutableData currentData) {
                    if (currentData.getValue() == null) {
                        currentData.setValue(1);
                    } else {
                        currentData.setValue((Long) currentData.getValue() + count);
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
                }
            });
        }
    }
    class getimg extends AsyncTask<String,String,Bitmap>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                // Log exception
                return null;
            }
           // return null;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            progressDialog.dismiss();
            if(bitmap!=null){
                imageView.setImageBitmap(bitmap);
            }
            else{
                imageView.setImageResource(R.drawable.troll);
            }
        }
    }
}