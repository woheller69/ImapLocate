package de.niendo.ImapNotes3;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import androidx.annotation.NonNull;

import de.niendo.ImapNotes3.Miscs.Imaper;

import java.io.File;


/*
Changed name by appending a k so that I can have this and the original installed side by side,
perhaps.
 */
public class ImapNotes3 extends Application {

    private static final String configurationFileName = BuildConfig.APPLICATION_NAME + ".conf";
    private Imaper thisSessionImapFolder;
/*
    // Called when starting the application.
    public void onCreate() {
        super.onCreate();
        // Save the context in a static so that it is easy to access everywhere.
        //de.niendo.ImapNotes3.context = getApplicationContext();
    }*/

    @NonNull
    public static String ConfigurationFilePath(@NonNull Context applicationContext) {
        return ConfigurationDirPath(applicationContext) + "/" + configurationFileName;
    }


    public static String ConfigurationDirPath(@NonNull Context applicationContext) {

        return ConfigurationDir(applicationContext).getPath();
    }


    public static File ConfigurationDir(@NonNull Context applicationContext) {

        return applicationContext.getFilesDir();
    }


// --Commented out by Inspection START (11/26/16 11:44 PM):
//    // ?
//    public void SetConfigurationFile(ConfigurationFile currentSettings) {
//        this.thisSessionConfigurationFile = currentSettings;
//    }
// --Commented out by Inspection STOP (11/26/16 11:44 PM)

// --Commented out by Inspection START (11/26/16 11:44 PM):
//    // ?
//    public ConfigurationFile GetConfigurationFile() {
//        return this.thisSessionConfigurationFile;
//    }
// --Commented out by Inspection STOP (11/26/16 11:44 PM)

    // ?
    public void SetImaper(Imaper currentImaper) {
        this.thisSessionImapFolder = currentImaper;
    }

    // ?
    public Imaper GetImaper() {
        return this.thisSessionImapFolder;
    }

    public ImapNotes3() {
        if (BuildConfig.DEBUG)
//            StrictMode.enableDefaults();
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
//                    .penaltyDeath()
                    .build());
    }
    /*// ?
    public void SetNotesList(ArrayList<OneNote> currentNotesList) {
    }
*/
// --Commented out by Inspection START (11/26/16 11:44 PM):
//    // ?
//    public ArrayList<OneNote> GetNotesList() {
//        return this.noteList;
//    }
// --Commented out by Inspection STOP (11/26/16 11:44 PM)

}
