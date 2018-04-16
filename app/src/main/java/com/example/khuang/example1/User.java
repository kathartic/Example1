package com.example.khuang.example1;

import org.bson.Document;

/**
 * Created by khuang on 4/14/2018.
 */

// object that represents user, corresponds to fields in db
public class User {

    private String name;
    private String uid;
    private String bikeId;
    private boolean bikeInUse;
    private Integer points;
    private Integer fines;
    private static User user;

    private User(String uid) {
        this.uid = uid;
    }

    public static User getUser() { return user; }

    // method to make user from bson doc
    public static void makeUserFromDoc(Document doc) {
        user = new User((String) doc.get("uid"));
        user.name = (String) doc.get("name");
        user.bikeInUse = (boolean) doc.get("bikeInUse");
        user.bikeId = (String) doc.get("bikeId");
        user.points = (Integer) doc.get("points");
        user.fines = (Integer) doc.get("fines");
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getUid() { return uid; }

    public void setUid(String uid) { this.uid = uid; }

    public String getBikeId() { return bikeId; }

    public void setBikeId(String bikeId) { this.bikeId = bikeId; }

    public boolean isBikeInUse() { return bikeInUse; }

    public void setBikeInUse(boolean bikeInUse) { this.bikeInUse = bikeInUse; }

    public Integer getPoints() { return points; }

    public void setPoints(Integer points) { this.points = points; }

    public float getFine() { return fines; }

    public void setFine(Integer fines) { this.fines = fines; }

}
