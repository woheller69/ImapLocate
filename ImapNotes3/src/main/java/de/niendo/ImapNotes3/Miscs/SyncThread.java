package de.niendo.ImapNotes3.Miscs;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import de.niendo.ImapNotes3.Data.Db;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.ListActivity;
import de.niendo.ImapNotes3.NotesListAdapter;

import java.util.ArrayList;

//import de.niendo.ImapNotes3.Data.NotesDb;

public class SyncThread extends AsyncTask<Object, Void, Boolean> {
    // --Commented out by Inspection (11/26/16 11:48 PM):boolean bool_to_return;
// --Commented out by Inspection START (11/26/16 11:48 PM):
//    @NonNull
//    ImapNotesResult res = new ImapNotesResult();
// --Commented out by Inspection STOP (11/26/16 11:48 PM)
    private static final String TAG = "SyncThread";
    private final @StringRes
    int resId;
    private final NotesListAdapter adapter;
    private final ArrayList<OneNote> notesList;
    private final String sortOrder;
    private final String imapNotesAccountName;
    /**
     * SQLite database that holds status information about the notes.
     */
    // TODO: NoteDb should probably never be null.
    @NonNull
    private final Db storedNotes;

    // TODO: remove unused arguments.
    public SyncThread(String imapNotesAccountName,
                      ArrayList<OneNote> noteList,
                      NotesListAdapter listToView,
                      @StringRes int resId,
                      @Nullable Db storedNotes,
                      String sortOrder,
                      Context applicationContext) {
        //this.imapFolder = imapFolder;
        this.imapNotesAccountName = imapNotesAccountName;
        this.notesList = noteList;
        this.adapter = listToView;
        this.resId = resId;
        this.sortOrder = sortOrder;
        //Notifier.Show(resId, applicationContext, 1);
        this.storedNotes = (storedNotes == null) ? new Db(applicationContext) : storedNotes;

    }

    // Do not pass arguments via execute; the object is never reused so it is quite safe to pass
    // the arguments in the constructor.
    @NonNull
    @Override
    protected Boolean doInBackground(Object... stuffs) {
        /*String username = null;
        String password = null;
        String server = null;
        String portnum = null;
        String security = null;
        String usesticky = null;
*/
         /*       this.adapter = ((NotesListAdapter) stuffs[3]);
        this.notesList = ((ArrayList<OneNote>) stuffs[2]);
        this.storedNotes = ((NotesDb) stuffs[5]);
        this.ctx = (Context) stuffs[6];
 */
        //username = ((ImapNotesAccount) stuffs[1]).GetUsername();
        //password = ((ImapNotesAccount) stuffs[1]).GetPassword();
        //server = ((ImapNotesAccount) stuffs[1]).GetServer();
        //portnum = ((ImapNotesAccount) stuffs[1]).GetPortnum();
        //security = ((ImapNotesAccount) stuffs[1]).GetSecurity();
        //usesticky = ((ImapNotesAccount) stuffs[1]).GetUsesticky();


        storedNotes.OpenDb();
        storedNotes.notes.GetStoredNotes(this.notesList, imapNotesAccountName, sortOrder);
        storedNotes.CloseDb();
        return true;
    }

    protected void onPostExecute(Boolean result) {
        if (result) {
            this.adapter.notifyDataSetChanged();
        }
    }
}
