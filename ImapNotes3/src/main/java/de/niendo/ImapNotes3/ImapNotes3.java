package de.niendo.ImapNotes3;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.view.View;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import de.niendo.ImapNotes3.Miscs.Imaper;

import java.io.File;

public class ImapNotes3 extends Application {
    private static Context mContext;
    private Imaper thisSessionImapFolder;

    public static Context getAppContext() {
        return mContext;
    }

    public static File GetRootDir() {
        return mContext.getFilesDir();
    }

    public static File GetSharedPrefsDir() {
        return new File(mContext.getFilesDir().getParent(), "shared_prefs");
    }

    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

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

    public static Snackbar showAction(
            View view,
            @StringRes int actionTextId,
            @StringRes int actionButtonId,
            Runnable actionCallback) {

        Snackbar snackbar =
                Snackbar.make(view, actionTextId, BaseTransientBottomBar.LENGTH_INDEFINITE)
                        .setAction(actionButtonId, v -> actionCallback.run());
        snackbar
                .getView()
                .setBackgroundColor(mContext.getColor(R.color.ShareActionBgColor));
        snackbar.setTextColor(mContext.getColor(R.color.ShareActionTxtColor));
        snackbar.setActionTextColor(mContext.getColor(R.color.ShareActionTxtColor));
        snackbar.show();
        return snackbar;
    }


    public static void ShowMessage(@StringRes int resId, View view, int durationSeconds) {
        ShowMessage(mContext.getResources().getString(resId), view, durationSeconds);
    }

    public static void ShowMessage(String message, View view, int durationSeconds) {
        Snackbar snackbar =
                Snackbar.make(view, message, durationSeconds * 1000);
        snackbar
                .getView()
                .setBackgroundColor(mContext.getColor(R.color.ShareActionBgColor));
        snackbar.setTextColor(mContext.getColor(R.color.ShareActionTxtColor));
        snackbar.setActionTextColor(mContext.getColor(R.color.ShareActionTxtColor));

        snackbar.show();
    }

}
