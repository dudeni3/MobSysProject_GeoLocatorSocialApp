package com.gl.geolocator.entities;
import java.util.Date;
public class post {
     String picurl;
     String caption;
    long dattime;
    int ecount;
    String prof;
    String uname;
    String uid;
    String img;
    String postid;
    String locality;
    public String getLocality() {
        return locality;
    }
    public void setLocality(String locality) {
        this.locality = locality;
    }
    public post(String picurl, String caption, int ecount, String img) {
        this.picurl = picurl;
        this.caption = caption;
        this.ecount=ecount;
        this.img=img;
        // Initialize to current time
        dattime = -1*(new Date().getTime());
    }
    public post(String picurl, String caption,String name,String prof,int ecount,String uid,String postid, String locality) {
        this.picurl = picurl;
        this.caption = caption;
        this.ecount=ecount;
        uname=name;
        this.prof=prof;
        // Initialize to current time
        dattime = -1*(new Date().getTime());
        this.uid=uid;
        this.postid=postid;
        this.locality = locality;
    }
    public post(){
    }
    public String getpurl() {
        return picurl;
    }
    public void setpurl(String picurl) {
        this.picurl = picurl;
    }
    public String getcap() {
        return caption;
    }
    public void setcap(String caption) {
        this.caption = caption;
    }
    public long getdattime() {
        return dattime;
    }
    public void setdattime(long messageTime) {
        this.dattime = messageTime;
    }
    public int getecount() {
        return ecount;
    }
    public void setecount(int ecount) {
        this.ecount = ecount;
    }
    public String getun() {
        return uname;
    }
    public void setun(String uname) {
        this.uname = uname;
    }
    public String getuid() {
        return uid;
    }
    public void setuid(String uid) {
        this.uid = uid;
    }
    public String getprof() {
        return prof;
    }
    public void setprof(String prof) {
        this.prof = prof;
    }
    public String getimg() {
        return img;
    }
    public void setimg(String img) {
        this.img = img;
    }
    public String getpostid() {
        return postid;
    }
    public void setpostid(String postid) {
        this.postid = postid;
    }
}