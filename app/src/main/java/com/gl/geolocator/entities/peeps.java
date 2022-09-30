package com.gl.geolocator.entities;
public class peeps {
    public String name;
    public String email;
    public int f;
    public String cf;
    public String prof;
    public peeps() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }
    public peeps(String username, String p,int fc) {
        this.name = username;
        this.email = p;
        this.f=fc;
        this.cf="cf";
        prof="";
    }
    public peeps(String username, String p) {
        this.name = username;
        this.email = p;
        prof="";
    }
    public String getn() {
        return name;
    }
    public void setn(String name) {
        this.name = name;
    }
    public String getemail() {
        return email;
    }
    public void setemail(String email) {
        this.email= email;
    }
    public String getprof() {
        return prof;
    }
    public void setprof(String prof) {
        this.prof = prof;
    }
}