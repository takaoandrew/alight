package com.andrewtakao.alight;

import com.google.firebase.database.FirebaseDatabase;

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



}
