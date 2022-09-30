package com.gl.geolocator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.MyViewHolder> {
 //   private List<Image> images;
    private Activity activity;
    ArrayList<HashMap<String, String>> personList;
    HashMap<String, String> post;
    SharedPreferences sharedPreferences;
    JSONObject jsonObject, jsonObject1;
    JSONArray jsonArray;
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView profileThumbnail;
        public ImageView post;
        TextView userName;
        TextView locationName;
        TextView caption;
        ImageButton hearts;
        public MyViewHolder(View view) {
            super(view);
            profileThumbnail = (ImageView) view.findViewById(R.id.imageView);
            post = (ImageView) view.findViewById(R.id.image);
            userName =(TextView)view.findViewById(R.id.un);
            locationName =(TextView)view.findViewById(R.id.locationName);
            caption =(TextView)view.findViewById(R.id.captionTextView);
            hearts =(ImageButton) view.findViewById(R.id.hearts1);
        }
    }
    public PostsAdapter(Activity a) {
        activity=a;
        post = new HashMap<String,String>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        try{
            jsonObject = new JSONObject(sharedPreferences.getString(activity.getString(R.string.hr), null));
            if(jsonObject !=null) {
                System.out.println("lazy hjo " + jsonObject.toString());
                jsonArray = jsonObject.getJSONArray("hearts");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    void setData(ArrayList<HashMap<String, String>> personList1){
        personList=personList1;
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new MyViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
       post =personList.get(position);
        holder.profileThumbnail.setTag(post.get("uid"));
        holder.post.setTag(post.get("purl"));
        holder.hearts.setTag(position);
        holder.userName.setTag(post.get("postid"));
       if(post.get("title")!=null && !post.get("title").isEmpty())
           holder.caption.setText(post.get("title"));
       holder.userName.setText(post.get("un"));
       holder.locationName.setText(post.get("locality"));
        Glide.with(activity)
                .asDrawable()
                .load(post.get("purl"))
                .placeholder(R.drawable.troll)
                .error(R.drawable.troll)
                .into(holder.post);
        Glide.with(activity)
                .asDrawable()
                .load(post.get("prof"))
                .placeholder(R.drawable.avatar)
                .error(R.drawable.avatar)
                .into(holder.profileThumbnail);
        if(checkHearts(post.get("purl"))){
            holder.hearts.setEnabled(false);
            holder.hearts.setBackgroundResource(R.mipmap.hearts);
        }
        else {
            holder.hearts.setEnabled(true);
            holder.hearts.setBackgroundResource(R.mipmap.dhearts);
        }
    }
    @Override
    public int getItemCount() {
        return personList.size();
    }
    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }
    private boolean checkHearts(String postUrl){
        try {
            if (jsonObject != null) {
                jsonArray = jsonObject.getJSONArray("hearts");
                for (int kkk = 0; kkk < jsonArray.length(); kkk++) {
                    jsonObject1 = jsonArray.getJSONObject(kkk);
                    if (jsonObject1.getString("purl").equals(postUrl)) {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return false;
    }
    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {
        private GestureDetector gestureDetector;
        private ClickListener clickListener;
        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}