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

        Perusal newPerusal;
        if ( cursor.moveToFirst() && cursor.getCount() != 0 ) {
             newPerusal = cursorToPerusal(cursor);
        }
        else
        {
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

    public int updatePerusalTitle(String oldPerusalTitle, String newPerusalTitle) {

        if (newPerusalTitle.isEmpty()) {
            Toast.makeText(context, "You must specify a new Perusal title", Toast.LENGTH_LONG).show();
            return 0;
        }

        ContentValues newContentValues = new ContentValues();
        newContentValues.put("title", newPerusalTitle.trim());

        return sqLiteDatabase.update(
                SQLiteDBHelper.TABLE_PERUSALS,
                newContentValues,
                "title = \"" + oldPerusalTitle + "\"",
                null
        );
    }

    public ArrayList<Perusal> getAllPerusals() {
        ArrayList<Perusal> PerusalList = new ArrayList<Perusal>();

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
        perusal.setSpeed(cursor.getInt(3));
        perusal.setModeInt(cursor.getInt(4));
        return perusal;
    }


    /** Takes a new recipe and updates the recipe with the same ID, which exists in the database
     *  TODO: check to see if the id of the recipe we wish to update already exists in the database
     *        before updating */
//    public int updateRecipeFavorite(Recipe newRecipe) {
//        if (newRecipe == null) {
//            Toast.makeText(context, "Update Recipe Favorite newRecipe is null..", Toast.LENGTH_LONG).show();
//            return 0;
//        }
//
//        long id                         = newRecipe.getRecipeId();
//        String newRecipeTitle           = newRecipe.getRecipeTitle();
//        String newRecipePerusalList  = newRecipe.getRecipeTitle();
//        String newRecipeUrl             = newRecipe.getRecipeTitle();
//        String newRecipePicUrl          = newRecipe.getRecipePicUrl();
//
//        ContentValues newContentValues = new ContentValues();
//        newContentValues.put(SQLiteDBHelper.RECIPE_FAV_COLUMN_TITLE, newRecipeTitle.toLowerCase().trim());
//        newContentValues.put(SQLiteDBHelper.RECIPE_FAV_COLUMN_Perusal_LIST, newRecipePerusalList.toLowerCase().trim());
//        newContentValues.put(SQLiteDBHelper.RECIPE_FAV_COLUMN_URL, newRecipeUrl.toLowerCase().trim());
//        newContentValues.put(SQLiteDBHelper.RECIPE_FAV_COLUMN_PIC_URL, newRecipePicUrl.toLowerCase().trim());
//
//        return sqLiteDatabase.update(
//                SQLiteDBHelper.TABLE_RECIPE_FAVS,
//                newContentValues,
//                SQLiteDBHelper.RECIPE_FAV_COLUMN_ID + " = \"" + id + "\"",
//                null
//        );
//    }

}
