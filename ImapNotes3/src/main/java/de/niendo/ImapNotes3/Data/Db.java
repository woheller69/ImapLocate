package de.niendo.ImapNotes3.Data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

/**
 * Created by kj on 2017-01-15 14:29.
 */

public class Db {

    private static final String TAG = "IN_Db";
    @NonNull
    public final NotesDb notes;
    @NonNull
    private final NotesDbHelper defaultHelper;
    SQLiteDatabase notesDb;

    public Db(@NonNull Context applicationContext) {
        this.defaultHelper = new NotesDbHelper(applicationContext);
        notes = new NotesDb(this);
    }


    /**
     * TODO: can we make this implement closeable?
     */
    public void OpenDb() {
        this.notesDb = this.defaultHelper.getWritableDatabase();

    }

    public void CloseDb() {
        this.notesDb.close();

    }

    public void insert(@NonNull String table,
                       String nullColumnHack,
                       @NonNull ContentValues values) {
        notesDb.insert(table, nullColumnHack, values);
    }


    /**
     * Database helper that creates and maintains the SQLite database.
     */
    private static class NotesDbHelper extends SQLiteOpenHelper {

        private static final int NOTES_VERSION = 3;

        private static final String DATABASE_NAME = "NotesDb";
        private final Patch[] PATCHES = new Patch[]{
                new Patch() {
                    public void apply(@NonNull SQLiteDatabase _db) {
                        //Log.d(TAG,"upgrade: v2 to v3");
                        _db.execSQL("Drop table notesTable;");
                        CreateNotesDb(_db);
                    }
                }
/*
           ,new Patch() {
              public void apply(SQLiteDatabase _db) {
                Log.d(TAG,"upgrade: v3 to v4");
                _db.execSQL("Drop table notesTable;");
                _db.execSQL(NotesDb.CREATE_NOTES_DB);
              }
           }
*/
        };

        NotesDbHelper(@NonNull Context currentApplicationContext) {
            super(currentApplicationContext, DATABASE_NAME, null, NOTES_VERSION);
        }

        @Override
        public void onCreate(@NonNull SQLiteDatabase _db) {
            CreateNotesDb(_db);
        }

        private void CreateNotesDb(@NonNull SQLiteDatabase _db) {
            _db.execSQL(NotesDb.CREATE_NOTES_DB);
        }

        @Override
        public void onUpgrade(@NonNull SQLiteDatabase _db,
                              int oldVersion,
                              int newVersion) {
            //Log.d(TAG,"onUpgrade from:"+oldVersion+" to:"+newVersion);
            for (int i = oldVersion; i < newVersion; i++) {
                PATCHES[i - 2].apply(_db);
            }
        }

        private static class Patch {
            public void apply(SQLiteDatabase _db) {
            }
        }
    }

}
