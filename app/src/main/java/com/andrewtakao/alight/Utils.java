package com.andrewtakao.alight;

import com.google.firebase.database.FirebaseDatabase;

import java.io.File;

/**
 * Created by andrewtakao on 3/12/18.
 */

public class Utils {
    private static FirebaseDatabase mDatabase;

    public static FirebaseDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
        }
        return mDatabase;
    }

    public static String readableKey(String key) {
        return key.replace("*", ".");
    }
    public static boolean fileExist(String fname){
        File file = new File(fname);
        return file.exists();
    }



}
