package de.niendo.ImapNotes3.Miscs;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import android.text.Html;
import android.util.Log;

import de.niendo.ImapNotes3.Data.ImapNotesAccount;
import de.niendo.ImapNotes3.Data.NotesDb;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.ListActivity;
import de.niendo.ImapNotes3.NotesListAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

// TODO: move arguments from execute to constructor.
public class UpdateThread extends AsyncTask<Object, Void, Boolean> {
    private static final String TAG = "IN_UpdateThread";
    private final ImapNotesAccount ImapNotesAccount;
    private final @StringRes
    int resId;
    private final NotesListAdapter adapter;
    private final ArrayList<OneNote> notesList;
    private final String noteBody;
    private final String bgColor;
    private final WeakReference<Context> applicationContextRef;
    private final Action action;
    private String suid;
    private boolean bool_to_return;
    private final NotesDb storedNotes;
    private OneNote currentNote;
    private int indexToDelete;

    /*
    Assign all fields in the constructor because we never reuse this object.  This makes the code
    typesafe.  Make them final to prevent accidental reuse.
    */
    public UpdateThread(ImapNotesAccount ImapNotesAccount,
                        ArrayList<OneNote> noteList,
                        NotesListAdapter listToView,
                        @StringRes int resId,
                        String suid,
                        String noteBody,
                        String bgColor,
                        Context context,
                        Action action) {
        Log.d(TAG, "UpdateThread: " + noteBody);
        this.ImapNotesAccount = ImapNotesAccount;
        this.notesList = noteList;
        this.adapter = listToView;
        this.resId = resId;
        this.suid = suid;
        this.noteBody = noteBody;
        this.bgColor = bgColor;
        this.applicationContextRef = new WeakReference<>(context);
        this.action = action;
        this.storedNotes = NotesDb.getInstance(context);
        currentNote = null;
        indexToDelete = -1;
        //Notifier.Show(resId, applicationContext, 1);
    }

    @Override
    protected Boolean doInBackground(Object... stuffs) {

        try {
            // Do we have a note to remove?
            if (action == Action.Delete) {
                //Log.d(TAG,"Received request to delete message #"+suid);
                // Here we delete the note from the local notes list
                //Log.d(TAG,"Delete note in Listview");
                indexToDelete = getIndexByNumber(suid);
                MoveMailToDeleted(suid);
                storedNotes.DeleteANote(suid, ImapNotesAccount.accountName);
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
                String body = (ImapNotesAccount.usesticky) ?
                        noteBody.replaceAll("\n", "\\\\n") : noteBody;

                //"<html><head></head><body>" + noteBody + "</body></html>";

                String DATE_FORMAT = Utilities.internalDateFormatString;
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);
                String stringDate = sdf.format(date);
                currentNote = new OneNote(title, stringDate, "", ImapNotesAccount.accountName, bgColor);
                // Add note to database
                if (!suid.startsWith("-")) {
                    // no temp. suid in use
                    suid = storedNotes.GetTempNumber(currentNote.GetAccount());
                }
                currentNote.SetUid(suid);
                // Here we ask to add the new note to the new note folder
                // Must be done AFTER uid has been set in currentNote
                Log.d(TAG, "doInBackground body: " + body);
                WriteMailToNew(currentNote, ImapNotesAccount.usesticky, body);
                if ((action == Action.Update) && (!oldSuid.startsWith("-"))) {
                    MoveMailToDeleted(oldSuid);
                }
                storedNotes.DeleteANote(oldSuid, currentNote.GetAccount());
                storedNotes.InsertANoteInDb(currentNote);

                // Add note to noteList but change date format before
                //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext);
                String sdate = DateFormat.getDateTimeInstance().format(date);
                currentNote.SetDate(sdate);
                indexToDelete = getIndexByNumber(oldSuid);
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
        if (result) {
            if (indexToDelete >= 0) notesList.remove(indexToDelete);
            if (!(currentNote == null)) notesList.add(0, currentNote);
            adapter.notifyDataSetChanged();
        }
    }

    private int getIndexByNumber(String pNumber) {
        for (OneNote _item : notesList) {
            if (_item.GetUid().equals(pNumber))
                return notesList.indexOf(_item);
        }
        return -1;
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

    @NonNull
    private Message MakeMessageWithAttachment(String subject,
                                              String message,
                                              String filePath,
                                              Session session)
            throws IOException, MessagingException {

        Message msg = new MimeMessage(session);


        msg.setSubject(subject);
        msg.setSentDate(new Date());

        // creates message part
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(message, "text/html");

        // creates multi-part
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        // add attachment

        MimeBodyPart attachPart = new MimeBodyPart();

        attachPart.attachFile(filePath);


        multipart.addBodyPart(attachPart);

        // sets the multi-part as e-mail's content
        msg.setContent(multipart);
        return msg;
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
        File accountDirectory = ImapNotesAccount.GetRootDirAccount();
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
