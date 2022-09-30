package com.gl.geolocator;
import android.app.Activity;
import android.content.Context;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;
public class MyPostsAdapter extends RecyclerView.Adapter<MyPostsAdapter.MyViewHolder> {
 //   private List<Image> images;
    private Activity activity;
    ArrayList<HashMap<String, String>> personList;
    HashMap<String, String> post;
    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView postImageView;
        TextView caption;
        TextView heartsTextView;
        ImageView hearts;
        public MyViewHolder(View view) {
            super(view);
            postImageView = (ImageView) view.findViewById(R.id.image1);
            caption =(TextView)view.findViewById(R.id.name1);
            heartsTextView =(TextView)view.findViewById(R.id.heartst2);
            hearts =(ImageView) view.findViewById(R.id.hearts2);
        }
    }
    public MyPostsAdapter(Activity a) {
        activity=a;
        post = new HashMap<String,String>();
    }
    void setData(ArrayList<HashMap<String, String>> personList1){
        personList=personList1;
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item1, parent, false);
        return new MyViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        post = personList.get(position);
        holder.caption.setText(post.get("title"));
        holder.caption.setTag(post.get("postid"));
        holder.heartsTextView.setTag(position);
        holder.heartsTextView.setText(post.get("hearts"));
        holder.postImageView.setTag(post.get("pic"));
        Glide.with(activity)
                .load(post.get("purl"))
                .error(R.drawable.troll)
                .placeholder(R.drawable.troll)
                .into(holder.postImageView);
    }
    @Override
    public int getItemCount() {
        return personList.size();
    }
    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
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

