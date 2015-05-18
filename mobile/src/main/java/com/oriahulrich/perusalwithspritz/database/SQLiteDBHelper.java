package com.oriahulrich.perusalwithspritz.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Responsible for creating the database, tables
 */
public class SQLiteDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "***SQLITEDBHELPER***: ";

    final static String DB_NAME = "peruse_with_spritz.db";
    final static int DB_VERSION = 3;

    /** Perusal DB TABLE and SQL definitions */
    public static final String TABLE_PERUSALS = "perusals2";

    public static final String COLUMN_ID    = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TEXT  = "textValue";
    public static final String COLUMN_TEXT_PARTITION_IDX  = "textPartitionIndex";
    public static final String COLUMN_SPEED = "speed";
    public static final String COLUMN_MODE  = "spritzMode";

    /** Database sql statements */
    private static final String CREATE_TABLE_PERUSALS= "create table "
            + TABLE_PERUSALS + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_TITLE + " text not null, "
            + COLUMN_TEXT + " text not null, "
            + COLUMN_TEXT_PARTITION_IDX + " integer, "
            + COLUMN_SPEED + " integer not null, "
            + COLUMN_MODE + " integer not null"
            + ");";

    private static final String DB_PERUSALS_TABLE_DROP = "drop table if exists "
            + TABLE_PERUSALS;

    /** Recipes DB TABLE and SQL definitions */
    public SQLiteDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        Log.d(TAG, "in constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(CREATE_TABLE_PERUSALS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DB_PERUSALS_TABLE_DROP);
        onCreate(db);
    }
}
