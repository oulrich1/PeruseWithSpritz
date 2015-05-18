package com.oriahulrich.perusalwithspritz.database;

/**
 * Created by oriahulrich on 12/18/14.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.oriahulrich.perusalwithspritz.pojos.Perusal;

import java.util.ArrayList;

/**
 * Maintains the database connection and supports adding new Perusals
 * and fetching all * Perusals. (acts as a DB controller)
 *
 */
public class SQLiteDAO {

    private static final String TAG = "***SQLITEDAO***: ";
    private Context context;
    private SQLiteDatabase sqLiteDatabase;
    private SQLiteDBHelper sqLiteDBHelper;
    private String[] PerusalsColumns = {
            SQLiteDBHelper.COLUMN_ID,
            SQLiteDBHelper.COLUMN_TITLE,
            SQLiteDBHelper.COLUMN_TEXT,
            SQLiteDBHelper.COLUMN_TEXT_PARTITION_IDX,
            SQLiteDBHelper.COLUMN_SPEED,
            SQLiteDBHelper.COLUMN_MODE
    };

    public SQLiteDAO(Context context) {
        Log.d(TAG, "in constructor");
        sqLiteDBHelper = new SQLiteDBHelper(context);
        this.context = context;
    }

    public void open() {
        Log.d(TAG, "open");
        sqLiteDatabase = sqLiteDBHelper.getWritableDatabase();
    }

    public void close() {
        Log.d(TAG, "close");
        sqLiteDBHelper.close();
    }

    // create if not duplciate
    public Perusal createPerusal(Perusal perusal)
    {
        String perusalTitle = perusal.getTitle();

        // Check for duplications
        String duplicateCheckQuery =
                "select * from "
                        + SQLiteDBHelper.TABLE_PERUSALS
                        + " where title like '" + perusalTitle + "';";

        Cursor cursor = sqLiteDatabase.rawQuery(duplicateCheckQuery, null);

        if (cursor != null && cursor.moveToFirst()) {
//            Toast.makeText(context, "Perusal is already in the list", Toast.LENGTH_LONG).show();
            return null;
        } else if (perusalTitle.isEmpty()) {
//            Toast.makeText(context, "You must start Perusing!!", Toast.LENGTH_LONG).show();
            return null;
        }

        // if the title isn't blank or a duplicate, insert it
        ContentValues contentValues = new ContentValues(); // toLowerCase()
        contentValues.put( SQLiteDBHelper.COLUMN_TITLE, perusalTitle.trim() );
        contentValues.put( SQLiteDBHelper.COLUMN_TEXT,  perusal.getText().toLowerCase().trim() );
        contentValues.put( SQLiteDBHelper.COLUMN_TEXT_PARTITION_IDX,  perusal.getTextPartitionIndex());
        contentValues.put( SQLiteDBHelper.COLUMN_SPEED, perusal.getSpeed() );
        contentValues.put( SQLiteDBHelper.COLUMN_MODE, perusal.getMode().ordinal() );

        long insertId = sqLiteDatabase.insert(
                SQLiteDBHelper.TABLE_PERUSALS,
                null,
                contentValues
        );

        if ( insertId < 0 ) {
            Toast.makeText( context, "Failed to add perusal to DB: "
                            + perusalTitle, Toast.LENGTH_SHORT).show();
            return perusal;
        }

        Log.d(TAG, "Insert ID: " + insertId);

        cursor = sqLiteDatabase.query(
                SQLiteDBHelper.TABLE_PERUSALS,
                PerusalsColumns,
                SQLiteDBHelper.COLUMN_ID + " = " + insertId,
                null, null, null, null
        );

        /* Retrieve from database and return this value.
           Not strictly necessary but nice for testing */
        Perusal newPerusal;
        if ( cursor.moveToFirst() && cursor.getCount() != 0 ) {
             newPerusal = cursorToPerusal(cursor);
        } else {
            newPerusal = perusal;
        }
        cursor.close();

        return newPerusal;
    }

    public void deletePerusal(Perusal perusal) {
        long id = perusal.getId();
        Log.d(TAG, "Perusal deleted with id: " + id);

        sqLiteDatabase.delete(
                SQLiteDBHelper.TABLE_PERUSALS,
                SQLiteDBHelper.COLUMN_ID + " = " + id,
                null
        );
    }

    public void deleteAllPerusals() {
        Log.d(TAG, "All perusals deleted from database. (no table drop)");
        sqLiteDatabase.delete( SQLiteDBHelper.TABLE_PERUSALS, null, null );
    }

    public int updatePerusalTitle(String oldPerusalTitle, String newPerusalTitle) {

        if (newPerusalTitle.isEmpty()) {
            Toast.makeText(context, "You must specify a new Perusal title", Toast.LENGTH_LONG).show();
            return 0;
        }

        ContentValues newContentValues = new ContentValues();
        newContentValues.put(SQLiteDBHelper.COLUMN_TITLE, newPerusalTitle.trim());

        return sqLiteDatabase.update(
                SQLiteDBHelper.TABLE_PERUSALS,
                newContentValues,
                SQLiteDBHelper.COLUMN_TITLE + " = \"" + oldPerusalTitle + "\"", // ie: "title = 'theOldTitle'"
                null
        );
    }

    // the perusal object stores the un-mutable id. this method
    // takes perusal, and updates the DB's entry for this perusal
    // with the perusal's current Text Partition index
    public int updatePerusalCurSelectionIdx(Perusal perusal) {
        if (perusal.getTextPartitionIndex() < 0) {
            Log.d(TAG, "updatePerusalCurSelectionIdx got a negative perusal text partition index");
            return 0;
        }

        ContentValues newContentValues = new ContentValues();
        newContentValues.put(SQLiteDBHelper.COLUMN_TEXT_PARTITION_IDX,
                perusal.getTextPartitionIndex());

        return sqLiteDatabase.update(
                SQLiteDBHelper.TABLE_PERUSALS,
                newContentValues,
                SQLiteDBHelper.COLUMN_ID + " = \"" + perusal.getId() + "\"",
                null
        );
    }

    public ArrayList<Perusal> getAllPerusals() {
        ArrayList<Perusal> PerusalList = new ArrayList<>();

        Cursor cursor;
        cursor = sqLiteDatabase.query(
                SQLiteDBHelper.TABLE_PERUSALS,
                PerusalsColumns,
                null, null, null, null, null
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Perusal Perusal = cursorToPerusal(cursor);
            PerusalList.add(Perusal);
            cursor.moveToNext();
        }

        cursor.close();
        return PerusalList;
    }

    private Perusal cursorToPerusal(Cursor cursor) {
        Perusal perusal = new Perusal();
        perusal.setId(cursor.getLong(0));
        perusal.setTitle(cursor.getString(1));
        perusal.setText(cursor.getString(2));
        perusal.setTextPartitionIndex(cursor.getInt(3));
        perusal.setSpeed(cursor.getInt(4));
        perusal.setModeInt(cursor.getInt(5));
        return perusal;
    }

}
