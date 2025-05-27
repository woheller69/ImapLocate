package org.woheller69.ImapLocate.Miscs;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import org.woheller69.ImapLocate.Data.ImapNotesAccount;
import org.woheller69.ImapLocate.Data.NotesDb;
import org.woheller69.ImapLocate.Data.OneNote;
import org.woheller69.ImapLocate.ImapNotes3;
import org.woheller69.ImapLocate.ListActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.MessagingException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;

// TODO: move arguments from execute to constructor.
public class UpdateThread extends AsyncTask<Object, Void, Boolean> {
    private static final String TAG = "IN_UpdateThread";
    private static ImapNotesAccount imapNotesAccount;
    private final String noteBody;
    private final String bgColor;
    private final Action action;
    private String suid;
    private boolean bool_to_return;
    private final NotesDb storedNotes;
    private OneNote currentNote;
    private ArrayList<OneNote> noteList;
    private static final String AUTHORITY = Utilities.PackageName + ".provider";

    public static void setImapNotesAccount(ImapNotesAccount account){
        imapNotesAccount = account;
    }
    /*
    Assign all fields in the constructor because we never reuse this object.  This makes the code
    typesafe.  Make them final to prevent accidental reuse.
    */
    public UpdateThread(String noteBody) {
        Log.d(TAG, "UpdateThread: " + noteBody);
        this.noteBody = noteBody;
        this.bgColor = "white";
        this.storedNotes = NotesDb.getInstance(ImapNotes3.getAppContext());
        currentNote = null;
        //Notifier.Show(resId, applicationContext, 1);
        this.noteList = new ArrayList<>();
        storedNotes.GetStoredNotes(noteList, imapNotesAccount.accountName, OneNote.DATE + " DESC");
        if (noteList.isEmpty()){
            this.suid = "";
            this.action = Action.Insert;
        }   else {
            HashMap hm = noteList.get(0);
            this.suid = hm.get(OneNote.UID).toString();
            this.action = Action.Update;
        }
        Log.d("IN_UpdateThread","SUID "+suid);
        TriggerSync();
    }

    private static void TriggerSync() {
        Account mAccount = imapNotesAccount.GetAccount();
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        Log.d(TAG,"Request a sync for:"+mAccount);
        ContentResolver.cancelSync(mAccount, AUTHORITY);
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);
    }
    @Override
    protected Boolean doInBackground(Object... stuffs) {

        try {
            // Do we have a note to remove?
            if (action == Action.Delete) {
                //Log.d(TAG,"Received request to delete message #"+suid);
                // Here we delete the note from the local notes list
                //Log.d(TAG,"Delete note in Listview");
                MoveMailToDeleted(suid);
                storedNotes.DeleteANote(suid, imapNotesAccount.accountName);
                bool_to_return = true;
            }

            // Do we have a note to add?
            if ((action == Action.Insert) || (action == Action.Update)) {
//Log.d(TAG,"StickyNote ? "+((ImapNotesAccount)stuffs[1]).GetUsesticky());
                Log.d(TAG, "Action Insert/Update:" + suid);
                String oldSuid = suid;
                Log.d(TAG, "Received request to add new message: " + noteBody + "===");
                // Use the first line as the tile
                String[] tok = Html.fromHtml(noteBody, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE).toString().split("\n", 2);
                String title = tok[0];
                //String position = "0 0 0 0";
                String body = (imapNotesAccount.usesticky) ?
                        noteBody.replaceAll("\n", "\\\\n") : noteBody;

                //"<html><head></head><body>" + noteBody + "</body></html>";

                String DATE_FORMAT = Utilities.internalDateFormatString;
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);
                String stringDate = sdf.format(date);
                currentNote = new OneNote(title, stringDate, "", imapNotesAccount.accountName, bgColor);
                // Add note to database
                if (!suid.startsWith("-")) {
                    // no temp. suid in use
                    suid = storedNotes.GetTempNumber(currentNote.GetAccount());
                }
                currentNote.SetUid(suid);
                // Here we ask to add the new note to the new note folder
                // Must be done AFTER uid has been set in currentNote
                Log.d(TAG, "doInBackground body: " + body);
                WriteMailToNew(currentNote, imapNotesAccount.usesticky, body);
                if ((action == Action.Update) && (!oldSuid.startsWith("-"))) {
                    MoveMailToDeleted(oldSuid);
                }
                storedNotes.DeleteANote(oldSuid, currentNote.GetAccount());
                storedNotes.InsertANoteInDb(currentNote);

                // Add note to noteList but change date format before
                //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext);
                String sdate = DateFormat.getDateTimeInstance().format(date);
                currentNote.SetDate(sdate);
                bool_to_return = true;
            }

        } catch (Exception e) {
            Log.d(TAG, "Action: " + action);
            e.printStackTrace();
            bool_to_return = false;
        }
        return bool_to_return;
    }

    protected void onPostExecute(Boolean result) {

    }


    /**
     * @param suid IMAP ID of the note.
     */
    private void MoveMailToDeleted(@NonNull String suid) {
        File directory = ListActivity.ImapNotesAccount.GetRootDirAccount();
        // TODO: Explain why we need to omit the first character of the UID
        File from = new File(directory, suid);
        if (!from.exists()) {
            String positiveUid = suid.substring(1);
            from = new File(directory + "/new", positiveUid);
            // TODO: Explain why it is safe to ignore the result of delete.
            //noinspection ResultOfMethodCallIgnored
            from.delete();
        } else {
            File to = new File(directory + "/deleted/" + suid);
            // TODO: Explain why it is safe to ignore the result of rename.
            //noinspection ResultOfMethodCallIgnored
            from.renameTo(to);
        }
    }

    private void WriteMailToNew(@NonNull OneNote note,
                                boolean useSticky,
                                String noteBody) throws MessagingException, IOException {
        Log.d(TAG, "WriteMailToNew: " + noteBody);
        //String body = null;

        // Here we add the new note to the new note folder
        //Log.d(TAG,"Add new note");
        Message message;
        if (useSticky) {
            message = StickyNote.GetMessageFromNote(note, noteBody);
        } else {
            message = HtmlNote.GetMessageFromNote(note, noteBody);
        }
        message.setSubject(note.GetTitle());
        MailDateFormat mailDateFormat = new MailDateFormat();
        // Remove (CET) or (GMT+1) part as asked in github issue #13
        String headerDate = (mailDateFormat.format(new Date())).replaceAll("\\(.*$", "");
        message.addHeader("Date", headerDate);
        // Get temporary UID
        String uid = Integer.toString(Math.abs(Integer.parseInt(note.GetUid())));
        File accountDirectory = imapNotesAccount.GetRootDirAccount();
        File directory = new File(accountDirectory, "new");
        try {
            message.setFrom(new InternetAddress(note.GetAccount(), false));
        } catch (AddressException e) {
            Log.d(TAG, "setFrom: " + e.toString());
            //message.setFrom(new InternetAddress(""));
        }
        File outfile = new File(directory, uid);
        OutputStream str = new FileOutputStream(outfile);
        message.writeTo(str);
        str.close();
    }

    public enum Action {
        Update,
        Insert,
        Delete
    }

}
