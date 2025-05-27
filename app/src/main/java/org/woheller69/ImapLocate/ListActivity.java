package org.woheller69.ImapLocate;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;

import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.woheller69.ImapLocate.Data.ImapNotesAccount;
import org.woheller69.ImapLocate.Data.NotesDb;
import org.woheller69.ImapLocate.Data.OneNote;
import org.woheller69.ImapLocate.Miscs.Imaper;
import org.woheller69.ImapLocate.Miscs.UpdateThread;
import org.woheller69.ImapLocate.Miscs.Utilities;
import org.woheller69.ImapLocate.Sync.SyncService;
import org.woheller69.ImapLocate.Sync.SyncUtils;

import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import static org.woheller69.ImapLocate.AccountConfigurationActivity.ACTION;
import static org.woheller69.ImapLocate.GpsSvc.ACTION_STOP_SERVICE;


public class ListActivity extends AppCompatActivity {
    private static final int ADD_ACCOUNT = 7;

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeError = -1;

    public static final String ACCOUNTNAME = "ACCOUNTNAME";
    public static final String SYNCINTERVAL = "SYNCINTERVAL";
    public static final String CHANGED = "CHANGED";
    public static final String SYNCED = "SYNCED";
    public static final String SYNCED_ERR_MSG = "SYNCED_ERR_MSG";
    private static final String ACCOUNTSPINNER_POS = "ACCOUNTSPINNER_POS";

    private ArrayList<OneNote> noteList;
    private NotesListAdapter listToView;
    private ArrayAdapter<String> spinnerList;
    private static final String AUTHORITY = Utilities.PackageName + ".provider";
    private Spinner accountSpinner;
    public static ImapNotesAccount ImapNotesAccount;
    private static AccountManager accountManager;
    @Nullable
    private static NotesDb storedNotes = null;
    private static List<String> currentList;

    @NonNull
    private final BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, @NonNull Intent intent) {
            Log.d(TAG, "BroadcastReceiver.onReceive");
            String accountName = intent.getStringExtra(ACCOUNTNAME);
            boolean isSynced = intent.getBooleanExtra(SYNCED, false);
            String syncInterval = String.valueOf(intent.getIntExtra(SYNCINTERVAL, 14));
            String errorMessage = intent.getStringExtra(SYNCED_ERR_MSG);
            Log.d(TAG, "if " + accountName + " " + ImapNotesAccount.accountName);
            if (accountName.equals(ImapNotesAccount.accountName)) {
                String statusText = OldStatus;
                if (isSynced) {
                    // Display last sync date
                    //DateFormat dateFormat =
                    //        android.text.format.DateFormat.getDateFormat(getApplicationContext());
                    Date date = new Date();
                    String sdate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date);
                    statusText = getText(R.string.Last_sync) + sdate;
                    if (!syncInterval.equals("0"))
                        statusText += " (" + syncInterval + " " + getText(R.string.minutes_short) + ")";
                }
                status.setBackgroundColor(getColor(R.color.StatusBgColor));
                if (!errorMessage.isEmpty()) {
                    statusText = errorMessage;
                    status.setBackgroundColor(getColor(R.color.StatusBgErrColor));
                }

                status.setText(statusText);

                storedNotes.GetStoredNotes(noteList, accountName, OneNote.DATE + " DESC");
                listToView.notifyDataSetChanged();

            }
        }
    };
    // FIXME
    // Hack! accountManager.addOnAccountsUpdatedListener
    // OnAccountsUpdatedListener is called to early - so not all
    // Date in AccountManager is saved - it gives crashes on the very first start
    public Boolean EnableAccountsUpdate = true;
    // Ensure that we never have to check for null by initializing reference.
    @NonNull
    private static Account[] accounts = new Account[0];
    private static String OldStatus;
    private final OnClickListener clickListenerEditAccount = v -> {
        Intent res = new Intent();
        String mPackage = Utilities.PackageName;
        String mClass = ".AccountConfigurationActivity";
        res.setComponent(new ComponentName(mPackage, mPackage + mClass));
        res.putExtra(ACTION, AccountConfigurationActivity.Actions.EDIT_ACCOUNT);
        res.putExtra(AccountConfigurationActivity.ACCOUNTNAME, ListActivity.ImapNotesAccount.accountName);
        startActivity(res);
    };
    private static final String TAG = "IN_Listactivity";
    //@Nullable
    private TextView status;

    public void onDestroy() {
        super.onDestroy();
    }

    private static void TriggerSync(@NonNull TextView statusField) {
        OldStatus = statusField.getText().toString();
        statusField.setText(R.string.syncing);
        Account mAccount = ListActivity.ImapNotesAccount.GetAccount();
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        Log.d(TAG,"Request a sync for:"+mAccount);
        ContentResolver.cancelSync(mAccount, AUTHORITY);
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setElevation(0); // or other
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.ActionBgColor)));

        checkAndRequestPerms();

        this.accountSpinner = findViewById(R.id.accountSpinner);
        ListActivity.currentList = new ArrayList<>();

        ImapNotes3.setContent(findViewById(android.R.id.content));

        //ImapNotesAccount = new ImapNotesAccount();
        ListActivity.accountManager = AccountManager.get(getApplicationContext());
        ListActivity.accountManager.addOnAccountsUpdatedListener(
                new AccountsUpdateListener(), null, true);

        status = findViewById(R.id.status);

        spinnerList = new ArrayAdapter<>
                (this, R.layout.account_spinner_item, ListActivity.currentList);
        accountSpinner.setAdapter(spinnerList);

        this.noteList = new ArrayList<>();

        this.listToView = new NotesListAdapter(
                this,
                this.noteList,
                new String[]{OneNote.TITLE, OneNote.DATE},
                new int[]{R.id.noteTitle, R.id.noteLastChange},
                OneNote.BGCOLOR);

        ListView listview = findViewById(R.id.notesList);
        listview.setAdapter(this.listToView);

        listview.setTextFilterEnabled(true);

        Imaper imapFolder = new Imaper();
        ((ImapNotes3) this.getApplicationContext()).SetImaper(imapFolder);

        storedNotes = NotesDb.getInstance(getApplicationContext());

        Button editAccountButton = findViewById(R.id.editAccountButton);
        editAccountButton.setOnClickListener(clickListenerEditAccount);
    }

    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        int len = accountManager.getAccounts().length;
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE);
        accountSpinner.setSelection((int) preferences.getLong(ACCOUNTSPINNER_POS, 0));
        if (len > 0) updateAccountSpinner();

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncFinishedReceiver, new IntentFilter(SyncService.SYNC_FINISHED), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(syncFinishedReceiver, new IntentFilter(SyncService.SYNC_FINISHED));
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        SharedPreferences.Editor preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE).edit();
        preferences.putLong(ACCOUNTSPINNER_POS, accountSpinner.getSelectedItemId());
        preferences.apply();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }


    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {

        getMenuInflater().inflate(R.menu.list, menu);

        MenuBuilder m = (MenuBuilder) menu;
        m.setOptionalIconsVisible(true);


        return true;
    }


    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stop_gps:
                startService(new Intent(ImapNotes3.getAppContext(), GpsSvc.class).setAction(ACTION_STOP_SERVICE));
                return true;
            case R.id.refresh:
                TriggerSync(status);
                return true;
            case R.id.start_gps:
                TriggerSync(status);
                checkAndRequestPerms();
                Intent intentSvc = new Intent(this, GpsSvc.class);
                // If startForeground() in Service is called on UI thread, it won't show notification
                // unless Service is started with startForegroundService().
                if (!GpsSvc.mIsRunning) startForegroundService(intentSvc);
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName())));

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode);
        switch (requestCode) {
            case ListActivity.ADD_ACCOUNT:
                Log.d(TAG, "onActivityResult AccountsUpdateListener");
                // Hack! accountManager.addOnAccountsUpdatedListener
                if (resultCode == ResultCodeSuccess) {
                    EnableAccountsUpdate = true;
                    ListActivity.accountManager.addOnAccountsUpdatedListener(
                            new AccountsUpdateListener(), null, true);
                }
                break;
            default:
                Log.d(TAG, "Received wrong request to save message");
        }
    }


    private void updateAccountSpinner() {

        this.spinnerList.notifyDataSetChanged();
        if (this.accountSpinner.getSelectedItemId() == android.widget.AdapterView.INVALID_ROW_ID) {
            this.accountSpinner.setSelection(0);
        }

        if (ListActivity.currentList.size() == 1) {
            Account account = ListActivity.accounts[0];
            ListActivity.ImapNotesAccount = new ImapNotesAccount(account, getApplicationContext());
            UpdateThread.setImapNotesAccount(ListActivity.ImapNotesAccount);
        }
    }

    private class AccountsUpdateListener implements OnAccountsUpdateListener {

        @Override
        public void onAccountsUpdated(@NonNull Account[] accounts) {
            Log.d(TAG, "onAccountsUpdated");
            List<String> newList;

            //invoked when the AccountManager starts up and whenever the account set changes
            ArrayList<Account> newAccounts = new ArrayList<>();
            for (final Account account : accounts) {
                if (account.type.equals(Utilities.PackageName)) {
                    newAccounts.add(account);
                }
            }
            // Hack! accountManager.addOnAccountsUpdatedListener
            if ((newAccounts.size() > 0) & (EnableAccountsUpdate)) {
                Account[] ImapNotesAccounts = new Account[newAccounts.size()];
                int i = 0;
                for (final Account account : newAccounts) {
                    ImapNotesAccounts[i] = account;
                    i++;
                }
                ListActivity.accounts = ImapNotesAccounts;
                newList = new ArrayList<>();
                for (Account account : ListActivity.accounts) {
                    newList.add(account.name);
                }
                if (newList.size() == 0) return;

                boolean equalLists = true;
                ListIterator<String> iter = ListActivity.currentList.listIterator();
                while (iter.hasNext()) {
                    String s = iter.next();
                    if (!(newList.contains(s))) {
                        iter.remove();
                        // Why try here?
                        try {
                            FileUtils.deleteDirectory(new File(ImapNotes3.GetRootDir(), s));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        equalLists = false;
                    }
                }
                for (String accountName : newList) {
                    if (!(ListActivity.currentList.contains(accountName))) {
                        ListActivity.currentList.add(accountName);
                        SyncUtils.CreateLocalDirectories(new File(ImapNotes3.GetRootDir(), accountName));

                        equalLists = false;
                    }
                }
                if (equalLists) return;
                updateAccountSpinner();
            } else {
                // Hack! accountManager.addOnAccountsUpdatedListener
                if (EnableAccountsUpdate) {
                    File filesDir = ImapNotes3.GetRootDir();
                    EnableAccountsUpdate = false;
                    ListActivity.accountManager.removeOnAccountsUpdatedListener(new AccountsUpdateListener());
                    try {
                        FileUtils.cleanDirectory(filesDir);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (Error err) {
                        // TODO Auto-generated catch block
                        err.printStackTrace();
                    }
                    Intent res = new Intent();
                    String mPackage = Utilities.PackageName;
                    String mClass = ".AccountConfigurationActivity";
                    res.setComponent(new ComponentName(mPackage, mPackage + mClass));
                    // Hack! accountManager.addOnAccountsUpdatedListener
                    startActivityForResult(res, ListActivity.ADD_ACCOUNT);
                }
            }
        }
    }

    private void checkAndRequestPerms() {
        List<String> perms = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION);

        } else  if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                String message = this.getString(R.string.gps_background) + " \n\n >> " + getPackageManager().getBackgroundPermissionOptionLabel().toString() + " <<";

                alertDialogBuilder.setMessage(message);
                alertDialogBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 2));
                alertDialogBuilder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
        }

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)){
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[] {}), 1);
        }
    }

}

