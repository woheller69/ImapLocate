package com.Pau.ImapNotes2;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.Pau.ImapNotes2.Data.ConfigurationFieldNames;
import com.Pau.ImapNotes2.Data.ImapNotes2Account;
import com.Pau.ImapNotes2.Data.Security;
import com.Pau.ImapNotes2.Miscs.ImapNotes2Result;
import com.Pau.ImapNotes2.Miscs.Imaper;
import com.Pau.ImapNotes2.Miscs.Result;
import com.Pau.ImapNotes2.Miscs.Notifier;
import com.Pau.ImapNotes2.Miscs.Utilities;

import java.util.List;

import javax.mail.MessagingException;


public class AccountConfigurationActivity extends AccountAuthenticatorActivity implements OnItemSelectedListener {
    /**
     * Cannot be final or NonNull because it needs the application context which is not available
     * until onCreate.
     */
    //private ConfigurationFile settings;

    //region Intent item names and values.
    public static final String ACTION = "ACTION";
    public static final String ACCOUNTNAME = "ACCOUNTNAME";
    private static final int TO_REFRESH = 999;
    private static final String AUTHORITY = Utilities.PackageName + ".provider";
    private static final String TAG = "IN_AccountConfActivity";
    @Nullable
    private static Account myAccount = null;
    private static AccountManager accountManager;
    private final OnClickListener clickListenerRemove = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Click on Remove Button
            accountManager.removeAccount(myAccount, null, null, null);
            Notifier.Show(R.string.account_removed, getApplicationContext(), 3);
            finish();//finishing activity
        }
    };
    private AppCompatDelegate mDelegate;
    private Imaper imapFolder;
    private TextView accountnameTextView;
    private TextView usernameTextView;
    private TextView passwordTextView;
    private TextView serverTextView;
    private TextView portnumTextView;
    private NumberPicker syncIntervalNumberPicker;
    private TextView folderTextView;
    private CheckBox stickyCheckBox;
    private Spinner securitySpinner;
    @NonNull
    private Security security = Security.None;
    //private int security_i;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    /* is this important?
    @Override
    @NonNull
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }
  */

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    @Nullable
    private Actions action;
    //    public static final String EDIT_ACCOUNT = "EDIT_ACCOUNT";
//    public static final String CREATE_ACCOUNT = "CREATE_ACCOUNT";
    //endregion
    private final OnClickListener clickListenerLogin = v -> {
        // Click on Login Button
        Log.d(TAG, "clickListenerLogin  onClick");
        CheckNameAndLogIn();
    };
    private final OnClickListener clickListenerEdit = v -> {
        // Click on Edit Button
        Log.d(TAG, "clickListenerEdit onClick");
        CheckNameAndLogIn();
    };

    private final View.OnFocusChangeListener FinishEmailEdit = (v, r) -> {
        if (!v.hasFocus()) {
            TextView tv = (TextView) v;
            String[] mail = tv.getText().toString().split("@");
            if ((mail.length == 2) && serverTextView.getText().toString().isEmpty()) {
                serverTextView.setText("imap." + mail[1]);
            }
        }
    };


    /*
        private final TextWatcher textWatcher = new TextWatcher(){

            public void beforeTextChanged(CharSequence chars, int start, int count, int after){}
            public void afterTextChanged(Editable editable){}
            public void onTextChanged(CharSequence chars, int start, int before, int count) {

            }

        };*/
    @Nullable
    private String accountname;

    private void CheckNameAndLogIn() {
        if (accountnameTextView.getText().toString().contains("'")) {
            Notifier.Show(R.string.quotation_marks_not_allowed, getApplicationContext(), 3);
        } else {
            DoLogin();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        //settings = new ConfigurationFile(getApplicationContext());
        setContentView(R.layout.account_selection);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.ActionBgColor)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView headingTextView = findTextViewById(R.id.heading);
        accountnameTextView = findTextViewById(R.id.accountnameEdit);
        usernameTextView = findTextViewById(R.id.usernameEdit);
        usernameTextView.setOnFocusChangeListener(FinishEmailEdit);
        passwordTextView = findTextViewById(R.id.passwordEdit);
        serverTextView = findTextViewById(R.id.serverEdit);
        portnumTextView = findTextViewById(R.id.portnumEdit);
        //syncintervalTextView = findTextViewById(R.id.syncintervalEdit);
        //syncintervalTextView.addTextChangedListener(textWatcher);
        syncIntervalNumberPicker = findViewById(R.id.syncintervalMinutes);
        syncIntervalNumberPicker.setMaxValue(24 * 60);
        syncIntervalNumberPicker.setMinValue(1);
        syncIntervalNumberPicker.setValue(15);

        folderTextView = findTextViewById(R.id.folderEdit);
        stickyCheckBox = findViewById(R.id.stickyCheckBox);

        securitySpinner = findViewById(R.id.securitySpinner);
        /*List<String> list = new ArrayList<String>();
        list.add("None");
        list.add("SSL/TLS");
        list.add("SSL/TLS (accept all certificates)");
        list.add("STARTTLS");
        list.add("STARTTLS (accept all certificates)");
        */
        List<String> list = Security.Printables();
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
        securitySpinner.setAdapter(dataAdapter);
        // Spinner item selection Listener
        securitySpinner.setOnItemSelectedListener(this);
        securitySpinner.setSelection(Security.SSL_TLS.ordinal());
        //imapNotes2Account = new ImapNotes2Account();
        imapFolder = ((ImapNotes2) getApplicationContext()).GetImaper();
        //settings = new ConfigurationFile();

        Bundle extras = getIntent().getExtras();
        // TODO: find out if extras can be null.
        if (extras != null) {
            if (extras.containsKey(ACTION)) {
                action = (Actions) (extras.getSerializable(ACTION));
            }
            if (extras.containsKey(ACCOUNTNAME)) {
                accountname = extras.getString(ACCOUNTNAME);
            }
        }


        // Settings can never be null so there is no need to guard it
        //if (settings != null) {
/*
        accountnameTextView.setText(settings.GetAccountname());
        usernameTextView.setText(settings.GetUsername());
        passwordTextView.setText(settings.GetPassword());
        serverTextView.setText(settings.GetServer());
        portnumTextView.setText(settings.GetPortnum());
        security = settings.GetSecurity();
        // Can never be null. if (security == null) security = "0";
        //int security_i = security.ordinal();
        securitySpinner.setSelection(security.ordinal());
        stickyCheckBox.setChecked(settings.GetUsesticky());
        folderTextView.setText(settings.GetFoldername());
*/
        //syncintervalTextView.setText(R.string.default_sync_interval);
        //}

        LinearLayout layout = findViewById(R.id.buttonsLayout);
        accountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType(Utilities.PackageName);
        for (Account account : accounts) {
            if (account.name.equals(accountname)) {
                myAccount = account;
                break;
            }
        }

        // action can never be null
        if (myAccount == null) {
            action = Actions.CREATE_ACCOUNT;
        }

        if (action == Actions.EDIT_ACCOUNT) {
            // Here we have to edit an existing account
            headingTextView.setText(R.string.editAccount);
            accountnameTextView.setText(accountname);
            accountnameTextView.setEnabled(false);
            usernameTextView.setText(GetConfigValue(ConfigurationFieldNames.UserName));
            passwordTextView.setText(accountManager.getPassword(myAccount));
            serverTextView.setText(GetConfigValue(ConfigurationFieldNames.Server));
            portnumTextView.setText(GetConfigValue(ConfigurationFieldNames.PortNumber));
            Log.d(TAG, "Security: " + GetConfigValue(ConfigurationFieldNames.Security));
            security = Security.from(GetConfigValue(ConfigurationFieldNames.Security));
            stickyCheckBox.setChecked(Boolean.parseBoolean(GetConfigValue(ConfigurationFieldNames.UseSticky)));
            //syncintervalTextView.setText(GetConfigValue(ConfigurationFieldNames.SyncInterval));
            syncIntervalNumberPicker.setValue(Integer.parseInt(GetConfigValue(ConfigurationFieldNames.SyncInterval)));
            folderTextView.setText(GetConfigValue(ConfigurationFieldNames.ImapFolder));
            //if (security == null) security = "0";
            //security_i = security.ordinal();
            securitySpinner.setSelection(security.ordinal());
            Button buttonEdit = new Button(this);
            buttonEdit.setText(R.string.save);
            Log.d(TAG, "Set onclick listener edit");
            buttonEdit.setOnClickListener(clickListenerEdit);
            layout.addView(buttonEdit);
            Button buttonRemove = new Button(this);
            buttonRemove.setText(R.string.remove);
            buttonRemove.setOnClickListener(clickListenerRemove);
            layout.addView(buttonRemove);
        } else {
            // Here we have to create a new account
            Button buttonView = new Button(this);
            buttonView.setText(R.string.check_and_create_account);
            Log.d(TAG, "Set onclick listener login");
            buttonView.setOnClickListener(clickListenerLogin);
            layout.addView(buttonView);
        }

        // Don't display keyboard when on note detail, only if user touches the screen
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
    }

    private TextView findTextViewById(int id) {
        return findViewById(id);
    }

    private String GetConfigValue(@NonNull String name) {
        return accountManager.getUserData(myAccount, name);
    }

    private String GetTextViewText(@NonNull TextView textView) {
        return textView.getText().toString().trim();
    }

    // DoLogin method is defined in account_selection.xml (account_selection layout)
    private void DoLogin() {
        Log.d(TAG, "DoLogin");
        final ImapNotes2Account imapNotes2Account = new ImapNotes2Account(
                GetTextViewText(accountnameTextView),
                GetTextViewText(usernameTextView),
                GetTextViewText(passwordTextView),
                GetTextViewText(serverTextView),
                GetTextViewText(portnumTextView),
                security,
                stickyCheckBox.isChecked(),
                syncIntervalNumberPicker.getValue(),
                GetTextViewText(folderTextView));
        // No need to check for valid numbers because the field only allows digits.  But it is
        // possible to remove all characters which causes the program to crash.  The easiest fix is
        // to add a zero at the beginning so that we are guaranteed to be able to parse it but that
        // leaves us with a zero sync. interval.
      /*  Result<Integer> synchronizationInterval = GetSynchronizationInterval();
        if (synchronizationInterval.succeeded) {
*/
        new LoginThread(
                imapNotes2Account,
                this,
                action).execute();
        //  }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        security = Security.from(position);
        portnumTextView.setText(security.defaultPort);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }

    void Clear() {

        accountnameTextView.setText("");
        usernameTextView.setText("");
        passwordTextView.setText("");
        serverTextView.setText("");
        portnumTextView.setText("");
        syncIntervalNumberPicker.setValue(15);
        securitySpinner.setSelection(0);
        folderTextView.setText("");
        stickyCheckBox.setChecked(false);
    }

    /**
     *
     */
    enum Actions {
        CREATE_ACCOUNT,
        EDIT_ACCOUNT
    }

    class LoginThread extends AsyncTask<Void, Void, Result<String>> {

        private final ImapNotes2Account imapNotes2Account;

        private final AccountConfigurationActivity accountConfigurationActivity;

        private final Actions action;

        LoginThread(ImapNotes2Account imapNotes2Account,
                    AccountConfigurationActivity accountConfigurationActivity,
                    Actions action) {
            this.imapNotes2Account = imapNotes2Account;
            this.accountConfigurationActivity = accountConfigurationActivity;
            this.action = action;
            Notifier.Show(R.string.logging_in, accountConfigurationActivity, 1);
            //this.synchronizationInterval = synchronizationInterval;
        }

        /*

                class Result{
                    final String message;
                    final boolean succeeded;

                    Result(String message,
                           boolean succeeded) {
                        this.message = message;
                        this.succeeded = succeeded;
                    }
                }
        */
        @NonNull
        protected Result<String> doInBackground(Void... none) {
            Log.d(TAG, "doInBackground");
            try {
                ImapNotes2Result res = imapFolder.ConnectToProvider(
                        imapNotes2Account.username,
                        imapNotes2Account.password,
                        imapNotes2Account.server,
                        imapNotes2Account.portnum,
                        imapNotes2Account.security
                );
                //accountConfigurationActivity = accountConfigurationActivity;
                if (res.returnCode != Imaper.ResultCodeSuccess) {
                    Log.d(TAG, "doInBackground IMAP Failed");
                    return new Result<>("IMAP operation failed: " + res.errorMessage, false);
                }
                // TODO: Find out if "com.Pau.ImapNotes2" is the same as getApplicationContext().getPackageName(). -YES
                final Account account = new Account(imapNotes2Account.accountName, Utilities.PackageName);
                final AccountManager am = AccountManager.get(accountConfigurationActivity);
                accountConfigurationActivity.setResult(AccountConfigurationActivity.TO_REFRESH);
                if (action == Actions.EDIT_ACCOUNT) {
                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                    setAccountAuthenticatorResult(result);
                    setUserData(am, account);
                    // Run the Sync Adapter Periodically
                    ContentResolver.setIsSyncable(account, AUTHORITY, 1);
                    ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
                    ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), imapNotes2Account.syncInterval);
                    return new Result<>("Account has been modified", true);
                } else {
                    if (!am.addAccountExplicitly(account, imapNotes2Account.password, null)) {
                        return new Result<>(getString(R.string.account_already_exists_or_is_null), false);
                    }
                    // TODO: make function for these repeated lines.
                    Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                    setAccountAuthenticatorResult(result);
                    setUserData(am, account);
                    // Run the Sync Adapter Periodically
                    ContentResolver.setIsSyncable(account, AUTHORITY, 1);
                    ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
                    ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), imapNotes2Account.syncInterval);
                    Log.d(TAG, "doInBackground End");
                    return new Result<>(getString(R.string.account_added), true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return new Result<>("Unexpected exception: " + e.getMessage(), false);
            } finally {
                Log.d(TAG, "doInBackground Finally");
            }
        }


        private void setUserData(@NonNull AccountManager am,
                                 @NonNull Account account) {
            am.setUserData(account, ConfigurationFieldNames.UserName, imapNotes2Account.username);
            am.setUserData(account, ConfigurationFieldNames.Server, imapNotes2Account.server);
            am.setUserData(account, ConfigurationFieldNames.PortNumber, imapNotes2Account.portnum);
            am.setUserData(account, ConfigurationFieldNames.SyncInterval, Integer.toString(imapNotes2Account.syncInterval));
            am.setUserData(account, ConfigurationFieldNames.Security, imapNotes2Account.security.name());
            am.setUserData(account, ConfigurationFieldNames.UseSticky, String.valueOf(imapNotes2Account.usesticky));
            am.setUserData(account, ConfigurationFieldNames.ImapFolder, imapNotes2Account.imapfolder);
        }

        protected void onPostExecute(@NonNull Result<String> result) {
            try {
                imapFolder.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            if (result.succeeded) {
                accountConfigurationActivity.Clear();
                // Hack! accountManager.addOnAccountsUpdatedListener
                setResult(Listactivity.ResultCodeSuccess);
            } else {
                // Hack! accountManager.addOnAccountsUpdatedListener
                setResult(Listactivity.ResultCodeError);
            }
            Notifier.Show(result.result, getApplicationContext(), 3);
            if (action == Actions.EDIT_ACCOUNT) {
                finish();
            }
        }

    }
}
