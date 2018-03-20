package com.andrewtakao.alight;

import java.util.ArrayList;

/**
 * Created by andrew.takao on 1/23/2018.
 */

public class User {
    public String userId;
    public String userName;
//    public String fullName;
    public ArrayList<String> friends;

    public User(String tempUserId, String tempUserName) {
        userId = tempUserId;
        userName = tempUserName;
        friends = new ArrayList<>();
    }

    //Adding a currentUsername would be difficult- not sure how to check that the currentUsername is unique, requires scouring entire database.
    //Perhaps another time
//    public User(String tempUserId, String tempUserName, String tempFullName) {
//        userId = tempUserId;
////        userName = tempUserName;
//        fullName = tempFullName;
//        friends = new ArrayList<>();
//    }

    public User(String tempUserId, String tempFullName, String tempUserName, ArrayList<String> tempFriends) {
        userId = tempUserId;
//        fullName = tempFullName;
        userName = tempUserName;
        friends = tempFriends;
    }

}
