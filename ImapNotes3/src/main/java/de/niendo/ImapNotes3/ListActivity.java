package de.niendo.ImapNotes3;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import de.niendo.ImapNotes3.Data.Db;
import de.niendo.ImapNotes3.Data.ImapNotesAccount;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.Miscs.Imaper;
import de.niendo.ImapNotes3.Miscs.Notifier;
import de.niendo.ImapNotes3.Miscs.SyncThread;
import de.niendo.ImapNotes3.Miscs.UpdateThread;
import de.niendo.ImapNotes3.Miscs.Utilities;
import de.niendo.ImapNotes3.Sync.SyncService;
import de.niendo.ImapNotes3.Sync.SyncUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import static de.niendo.ImapNotes3.AccountConfigurationActivity.ACTION;


public class ListActivity extends AppCompatActivity implements OnItemSelectedListener, Filterable {
    private static final int SEE_DETAIL = 2;
    public static final int DELETE_BUTTON = 3;
    private static final int NEW_BUTTON = 4;
    private static final int SAVE_BUTTON = 5;
    private static final int EDIT_BUTTON = 6;
    private static final int ADD_ACCOUNT = 7;

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeError = -1;


    //region Intent item names
    public static final String EDIT_ITEM_NUM_IMAP = "EDIT_ITEM_NUM_IMAP";
    public static final String EDIT_ITEM_TXT = "EDIT_ITEM_TXT";
    public static final String EDIT_ITEM_COLOR = "EDIT_ITEM_COLOR";
    public static final String ACCOUNTNAME = "ACCOUNTNAME";
    public static final String SYNCINTERVAL = "SYNCINTERVAL";
    public static final String CHANGED = "CHANGED";
    public static final String SYNCED = "SYNCED";
    public static final String SYNCED_ERR_MSG = "SYNCED_ERR_MSG";
    private static final String SAVE_ITEM_COLOR = "SAVE_ITEM_COLOR";
    private static final String SAVE_ITEM = "SAVE_ITEM";
    private static final String DELETE_ITEM_NUM_IMAP = "DELETE_ITEM_NUM_IMAP";
    private static final String ACCOUNTSPINNER_POS = "ACCOUNTSPINNER_POS";
    private static final String SORT_BY_DATE = "SORT_BY_DATE";
    private static final String SORT_BY_TITLE = "SORT_BY_TITLE";
    private static final String SORT_BY_COLOR = "SORT_BY_COLOR";
    //endregion

    private ArrayList<OneNote> noteList;
    private NotesListAdapter listToView;
    private ArrayAdapter<String> spinnerList;
    private static final String AUTHORITY = Utilities.PackageName + ".provider";
    private Spinner accountSpinner;
    public static ImapNotesAccount ImapNotesAccount;
    private static AccountManager accountManager;
    @Nullable
    private static Db storedNotes = null;
    private static List<String> currentList;
    private static Menu actionMenu;
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
    @NonNull
    private final BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, @NonNull Intent intent) {
            Log.d(TAG, "BroadcastReceiver.onReceive");
            String accountName = intent.getStringExtra(ACCOUNTNAME);
            boolean isChanged = intent.getBooleanExtra(CHANGED, false);
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
                ;

                status.setText(statusText);

                if (isChanged) {
                    if (storedNotes == null) {
                        storedNotes = new Db(getApplicationContext());
                    }
                    storedNotes.OpenDb();
                    storedNotes.notes.GetStoredNotes(noteList, accountName, getSortOrder());
                    listToView.notifyDataSetChanged();
                    storedNotes.CloseDb();
                }
            }
        }
    };

    public void onDestroy() {
        super.onDestroy();
        // in case of debug, uncomment next instruction
        // logcat will be sent by mail
        // send mail action is done by the user, so he can refuse
        // SendLogcatMail();
//        this.imapFolder.SetPrefs();
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
        //Log.d(TAG,"Request a sync for:"+mAccount);
        ContentResolver.cancelSync(mAccount, AUTHORITY);
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setElevation(0); // or other
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.ActionBgColor)));

        this.accountSpinner = findViewById(R.id.accountSpinner);
        ListActivity.currentList = new ArrayList<>();

        this.accountSpinner.setOnItemSelectedListener(this);

        //ImapNotesAccount = new ImapNotesAccount();
        ListActivity.accountManager = AccountManager.get(getApplicationContext());
        ListActivity.accountManager.addOnAccountsUpdatedListener(
                new AccountsUpdateListener(), null, true);

        status = findViewById(R.id.status);

        spinnerList = new ArrayAdapter<>
                (this, R.layout.account_spinner_item, ListActivity.currentList);
        accountSpinner.setAdapter(spinnerList);

        this.noteList = new ArrayList<>();
        //((de.niendo.ImapNotes3) this.getApplicationContext()).SetNotesList(this.noteList);
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

        if (ListActivity.storedNotes == null)
            storedNotes = new Db(getApplicationContext());

        // When item is clicked, we go to NoteDetailActivity
        listview.setOnItemClickListener((parent, widget, selectedNote, rowId) -> {
            Log.d(TAG, "onItemClick");
            Intent toDetail = new Intent(widget.getContext(), NoteDetailActivity.class);
            toDetail.putExtra(NoteDetailActivity.selectedNote, (OneNote) parent.getItemAtPosition(selectedNote));
            toDetail.putExtra(NoteDetailActivity.useSticky, ListActivity.ImapNotesAccount.usesticky);
            toDetail.putExtra(NoteDetailActivity.ActivityType, NoteDetailActivity.ActivityTypeEdit);
            startActivityForResult(toDetail, SEE_DETAIL);
            Log.d(TAG, "onItemClick, back from detail.");

            //TriggerSync(status);
        });

        Button editAccountButton = findViewById(R.id.editAccountButton);
        editAccountButton.setOnClickListener(clickListenerEditAccount);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            //if (type.startsWith("text/")) {
            Intent toNew = new Intent(this, NoteDetailActivity.class);
            toNew.putExtra("SHARE", true);
            toNew.putExtra(NoteDetailActivity.useSticky, ListActivity.ImapNotesAccount.usesticky);
            toNew.putExtra(NoteDetailActivity.ActivityType, NoteDetailActivity.ActivityTypeAdd);
            toNew.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT));
            toNew.putExtra(Intent.EXTRA_SUBJECT, intent.getStringExtra(Intent.EXTRA_SUBJECT));

            toNew.putExtra("TYPE", type);
            startActivityForResult(toNew, ListActivity.NEW_BUTTON);

            //} else if (type.startsWith("image/")) {

            // handleSendImage(intent); // Handle single image being sent
            // }
//        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            //          if (type.startsWith("image/")) {
            // handleSendMultipleImages(intent); // Handle multiple images being sent
            //        }
//        } else {
            // Handle other intents, such as being started from the home screen
        }


    }

    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        //int len = accounts.length;
        int len = accountManager.getAccounts().length;
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE);
        accountSpinner.setSelection((int) preferences.getLong(ACCOUNTSPINNER_POS, 0));
        if (len > 0) updateAccountSpinner();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        registerReceiver(syncFinishedReceiver, new IntentFilter(SyncService.SYNC_FINISHED));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        unregisterReceiver(syncFinishedReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        SharedPreferences.Editor preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE).edit();
        preferences.putLong(ACCOUNTSPINNER_POS, accountSpinner.getSelectedItemId());

        preferences.putBoolean(SORT_BY_DATE, actionMenu.findItem(R.id.sort_date).isChecked());
        preferences.putBoolean(SORT_BY_TITLE, actionMenu.findItem(R.id.sort_title).isChecked());
        preferences.putBoolean(SORT_BY_COLOR, actionMenu.findItem(R.id.sort_color).isChecked());

        preferences.apply();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void RefreshList() {
        new SyncThread(
                ImapNotesAccount.accountName,
                noteList,
                listToView,
                R.string.refreshing_notes_list,
                storedNotes,
                getSortOrder(),
                getApplicationContext()).execute();

        status.setBackgroundColor(getColor(R.color.StatusBgColor));
        status.setText(R.string.welcome);
    }

    private void UpdateList(String suid,
                            String noteBody,
                            String bgColor,
                            UpdateThread.Action action) {
        new UpdateThread(ImapNotesAccount,
                noteList,
                listToView,
                R.string.updating_notes_list,
                suid,
                noteBody,
                bgColor,
                getApplicationContext(),
                action,
                storedNotes).execute();
    }


    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        actionMenu = menu;
        getMenuInflater().inflate(R.menu.list, menu);

        MenuBuilder m = (MenuBuilder) menu;
        m.setOptionalIconsVisible(true);

        // Associate searchable configuration with the SearchView
        // disable SearchManager and setSearchableInfo .. it seems confusing and useless
        //SearchManager searchManager =
        //        (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.search);
        SearchView searchView =
                (SearchView) menuItem.getActionView();
        // searchView.setSearchableInfo(
        //         searchManager.getSearchableInfo(getComponentName()));
        SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered
                listToView.getFilter().filter(newText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // this is your adapter that will be filtered
                listToView.getFilter().filter(query);
                return true;
            }
        };
        // restore List and Filter after closing search
        searchView.setOnCloseListener(() -> {
            this.listToView.ResetFilterData(noteList);
            return true;
        });

        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                accountSpinner.setEnabled(false);
                listToView.getFilter().filter("");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                accountSpinner.setEnabled(true);
                listToView.getFilter().filter("");
                return true;
            }
        });

        searchView.setOnQueryTextListener(textChangeListener);

        // load values from disk
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE);

        if (preferences.getBoolean(SORT_BY_TITLE, false))
            actionMenu.findItem(R.id.sort_title).setChecked(true);
        else if (preferences.getBoolean(SORT_BY_COLOR, false))
            actionMenu.findItem(R.id.sort_color).setChecked(true);
        else
            actionMenu.findItem(R.id.sort_date).setChecked(true);

        return true;
    }

    private String getSortOrder() {
        if (actionMenu.findItem(R.id.sort_title).isChecked())
            return "UPPER(" + OneNote.TITLE + ") ASC";
        if (actionMenu.findItem(R.id.sort_color).isChecked()) return OneNote.BGCOLOR + " ASC";

        return OneNote.DATE + " DESC";
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newaccount:
                Intent res = new Intent();
                String mPackage = Utilities.PackageName;
                String mClass = ".AccountConfigurationActivity";
                res.setComponent(new ComponentName(mPackage, mPackage + mClass));
                res.putExtra(ACTION, AccountConfigurationActivity.Actions.CREATE_ACCOUNT);
                res.putExtra(ACCOUNTNAME, ImapNotesAccount.accountName);
                startActivity(res);
                return true;
            case R.id.refresh:
                //TextView status = (TextView) findViewById(R.id.status);
                TriggerSync(status);
                return true;
            case R.id.newnote:
                Intent toNew = new Intent(this, NoteDetailActivity.class);
                toNew.putExtra(NoteDetailActivity.useSticky, ListActivity.ImapNotesAccount.usesticky);
                toNew.putExtra(NoteDetailActivity.ActivityType, NoteDetailActivity.ActivityTypeAdd);
                startActivityForResult(toNew, ListActivity.NEW_BUTTON);
                return true;
            case R.id.sort_date:
            case R.id.sort_title:
            case R.id.sort_color: {
                item.setChecked(true);
                RefreshList();
                return true;
            }

            case R.id.about:
                String about = getString(R.string.license) + "\n";
                about += "Name: " + BuildConfig.APPLICATION_ID + "\n";
                about += "Version: " + BuildConfig.VERSION_NAME + "\n";
                about += "Code: " + BuildConfig.VERSION_CODE + "\n";
                about += "Build typ: " + BuildConfig.BUILD_TYPE + "\n";
                about += getString(R.string.internet) + "\n";
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.about) + " " + BuildConfig.APPLICATION_NAME)
                        .setIcon(R.drawable.ic_launcher)
                        .setMessage(about)
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Do nothing
                        })
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode);
        switch (requestCode) {
            case ListActivity.SEE_DETAIL:
                // Returning from NoteDetailActivity
                if (resultCode == ListActivity.DELETE_BUTTON) {
                    // Delete Message asked for
                    // String suid will contain the Message Imap UID to delete
                    String suid = data.getStringExtra(DELETE_ITEM_NUM_IMAP);
                    this.UpdateList(suid, null, null, UpdateThread.Action.Delete);
                }
                if (resultCode == ListActivity.EDIT_BUTTON) {
                    String txt = data.getStringExtra(EDIT_ITEM_TXT);
                    String suid = data.getStringExtra(EDIT_ITEM_NUM_IMAP);
                    String bgcolor = data.getStringExtra(EDIT_ITEM_COLOR);
                    //Log.d(TAG,"Received request to edit message:"+suid);
                    //Log.d(TAG,"Received request to replace message with:"+txt);
                    this.UpdateList(suid, txt, bgcolor, UpdateThread.Action.Update);
                    //TextView status = (TextView) findViewById(R.id.status);
                    TriggerSync(status);
                }
                break;
            case ListActivity.NEW_BUTTON:
                // Returning from NewNoteActivity
                if (resultCode == ListActivity.EDIT_BUTTON) {
                    //String res = data.getStringExtra(SAVE_ITEM);
                    String txt = data.getStringExtra(EDIT_ITEM_TXT);
                    //Log.d(TAG,"Received request to save message:"+res);
                    String bgcolor = data.getStringExtra(EDIT_ITEM_COLOR);
                    this.UpdateList("", txt, bgcolor, UpdateThread.Action.Insert);
                    TriggerSync(status);
                }
                break;
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

    // Spinner item selected listener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Account account = ListActivity.accounts[pos];
        // Check periodic sync. If set to 86400 (once a day), set it to 900 (15 minutes)
        // this is due to bad upgrade to v4 which handles offline mode and syncing
        // Remove this code after V4.0 if version no more used
        List<PeriodicSync> currentSyncs = ContentResolver.getPeriodicSyncs(account, AUTHORITY);
        for (PeriodicSync onesync : currentSyncs) {
            if (onesync.period == 86400) {
                ContentResolver.setIsSyncable(account, AUTHORITY, 1);
                ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
                ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), 60);
                Notifier.Show("Recreating this account is recommended to manage sync interval. Set to 15 minutes in the meantime",
                        getApplicationContext(),
                        2);
            }
        }
        listToView.ResetFilterData(noteList);
        ListActivity.ImapNotesAccount = new ImapNotesAccount(account, getApplicationContext());
        this.RefreshList();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub

    }

    private void updateAccountSpinner() {

        this.spinnerList.notifyDataSetChanged();
        //this.accountSpinner.setSelection(spinnerList.getPosition(currentAccountname));
        if (this.accountSpinner.getSelectedItemId() == android.widget.AdapterView.INVALID_ROW_ID) {
            this.accountSpinner.setSelection(0);
        }

        if (ListActivity.currentList.size() == 1) {
            Account account = ListActivity.accounts[0];
            ListActivity.ImapNotesAccount = new ImapNotesAccount(account, getApplicationContext());
/*            ImapNotesAccount.SetUsername(ListActivity.accountManager.getUserData(account, ConfigurationFieldNames.UserName));
            String pwd = ListActivity.accountManager.getPassword(account);
            ImapNotesAccount.SetPassword(pwd);
            ImapNotesAccount.SetServer(ListActivity.accountManager.getUserData(account, ConfigurationFieldNames.Server));
            ImapNotesAccount.SetPortnum(ListActivity.accountManager.getUserData(account, ConfigurationFieldNames.PortNumber));
            ImapNotesAccount.SetSecurity(ListActivity.accountManager.getUserData(account, ConfigurationFieldNames.Security));
            ImapNotesAccount.SetUsesticky("true".equals(accountManager.getUserData(account, ConfigurationFieldNames.UseSticky)));
            ImapNotesAccount.SetSyncinterval(ListActivity.accountManager.getUserData(account, ConfigurationFieldNames.SyncInterval));
            //ImapNotesAccount.SetaccountHasChanged();
 */
        }
    }

// --Commented out by Inspection START (12/2/16 8:49 PM):
//    // In case of neccessary debug  with user approval
//    // This function will be called from onDestroy
//    public void SendLogcatMail() {
//        String emailData = "";
//        try {
//            Process process = Runtime.getRuntime().exec("logcat -d");
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream()));
//
//            StringBuilder sb = new StringBuilder();
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                sb.append(line).append("\n");
//            }
//            emailData = sb.toString();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        //send file using email
//        Intent emailIntent = new Intent(Intent.ACTION_SEND);
//        String to[] = {"nb@dagami.org"};
//        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
//        // the attachment
//        emailIntent.putExtra(Intent.EXTRA_TEXT, emailData);
//        // the mail subject
//        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Logcat content for de.niendo.ImapNotes3 debugging");
//        emailIntent.setType("message/rfc822");
//        startActivity(Intent.createChooser(emailIntent, "Send email..."));
//    }
// --Commented out by Inspection STOP (12/2/16 8:49 PM)

    @Nullable
    @Override
    public Filter getFilter() {
        return null;
    }

    private class AccountsUpdateListener implements OnAccountsUpdateListener {

        @Override
        public void onAccountsUpdated(@NonNull Account[] accounts) {
            Log.d(TAG, "onAccountsUpdated");
            List<String> newList;
            //Integer newListSize = 0;
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
                            String stringDir = ImapNotes3.ConfigurationDirPath(getApplicationContext()) + "/" + s;
                            FileUtils.deleteDirectory(new File(stringDir));
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
                        SyncUtils.CreateLocalDirectories(accountName, getApplicationContext());

                        equalLists = false;
                    }
                }
                if (equalLists) return;
                updateAccountSpinner();
            } else {
                // Hack! accountManager.addOnAccountsUpdatedListener
                if (EnableAccountsUpdate) {
                    File filesDir = ImapNotes3.ConfigurationDir(getApplicationContext());
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
}

