package de.niendo.ImapNotes3.Data;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Represents metadata about a note in a way that can be used by a ListAdapter.  The list adapter
 * needs objects that have a map interface because it must fetch the items by string name.
 */
public class OneNote extends HashMap<String, String> {
    public static final String TITLE = "title";
    public static final String DATE = "date";
    public static final String BGCOLOR = "bgColor";
    public static final String UID = "uid";
    public static final String ACCOUNT = "account";


    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public OneNote(String title, String date, String uid, String account, String bgColor) {
        super();
        put(TITLE, title);
        put(DATE, date);
        put(UID, uid);
        put(ACCOUNT, account);
        put(BGCOLOR, bgColor);
    }

    @NonNull
    public String GetTitle() {
        return this.get(TITLE);
    }

    @NonNull
    String GetDate() {
        return this.get(DATE);
    }

    @NonNull
    public String GetUid() {
        return this.get(UID);
    }

    @NonNull
    public String GetAccount() {
        return this.get(ACCOUNT);
    }

    @NonNull
    public String GetBgColor() {
        return this.get(BGCOLOR);
    }


    public void SetDate(String date) {
        this.put(DATE, date);
    }

    public void SetUid(String uid) {
        this.put(UID, uid);
    }

    public void SetBGColor(String bgColor) {
        this.put(BGCOLOR, bgColor);
    }

    @NonNull
    @Override
    public String toString() {
        return ("Title:" + this.GetTitle() +
                " Date: " + this.GetDate() +
                " BgColor: " + this.GetBgColor() +
                " Account: " + this.GetAccount() +
                " Uid: " + this.GetUid());
    }
}
