package de.niendo.ImapLocate.Data;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ImapNotesAccount {

    private static final String TAG = "IN_ImapNotesAccount";
    private static final String DEFAULT_FOLDER_NAME = "Notes";

    @NonNull
    public final String accountName;
    @NonNull
    public final String username;
    @NonNull
    public final String password;
    @NonNull
    public final String server;
    @NonNull
    public final String portnum;
    @NonNull
    public final Security security;
    public final boolean usesticky;
    public final int syncInterval;
    @NonNull
    private final String imapfolder;
    @Nullable
    private final Account account;
    private File dirForNewFiles;
    private File dirForDeletedFiles;
    private File rootDir;
    private File rootDirAccount;

    public ImapNotesAccount(@NonNull String accountName,
                             @NonNull String username,
                             @NonNull String password,
                             @NonNull String server,
                             @NonNull String portNumber,
                             @NonNull Security security,
                             boolean useSticky,
                             int syncInterval,
                             @NonNull String folderName) {
        account = null;
        this.accountName = accountName;
        this.username = username;
        this.password = password;
        this.server = server;
        this.security = security;
        this.portnum = portNumber;
        this.usesticky = useSticky;
        this.imapfolder = folderName;
        this.syncInterval = syncInterval;
    }

    public ImapNotesAccount(@NonNull Account account,
                             @NonNull Context applicationContext) {
        this.accountName = account.name;
        rootDir = applicationContext.getFilesDir();
        rootDirAccount = new File(rootDir, accountName);
        dirForNewFiles = new File(rootDirAccount, "new");
        dirForDeletedFiles = new File(rootDirAccount, "deleted");

        this.account = account;
        AccountManager am = AccountManager.get(applicationContext);
        syncInterval = Integer.parseInt(am.getUserData(account, ConfigurationFieldNames.SyncInterval));
        username = am.getUserData(account, ConfigurationFieldNames.UserName);
        password = am.getPassword(account);
        server = am.getUserData(account, ConfigurationFieldNames.Server);
        portnum = am.getUserData(account, ConfigurationFieldNames.PortNumber);
        security = Security.from(am.getUserData(account, ConfigurationFieldNames.Security));
        usesticky = "true".equals(am.getUserData(account, ConfigurationFieldNames.UseSticky));
        imapfolder = am.getUserData(account, ConfigurationFieldNames.ImapFolder);

    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void CreateLocalDirectories() {
        Log.d(TAG, "CreateLocalDirs(String: " + accountName);
        dirForNewFiles.mkdirs();
        dirForDeletedFiles.mkdirs();
    }


    public void ClearHomeDir() {
        try {
            FileUtils.deleteDirectory(rootDirAccount);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // for anbox - otherwise it will crash
        catch (Error e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }


    /*
    @NonNull
    public String toString() {
        return this.accountName + ":" + this.username + ":" + this.password + ":"
                + this.server + ":" + this.portnum + ":" + this.security + ":"
                + this.usesticky + ":" + this.imapfolder + ":" + Boolean.toString(this.accountHasChanged);
    }*/
/*

    public String GetAccountName() {
        return accountName;
    }

*/
    @Nullable
    public Account GetAccount() {
        return this.account;
    }

    //public void SetAccountname(String accountName) {
    //    this.accountName = accountName;
    //}
/*

    @NonNull
    public String GetUsername() {
        return this.username;
    }

    public void SetUsername(@NonNull String Username) {
        this.username = Username;
    }
*/

  /*  @NonNull
    public String GetPassword() {
        return this.password;
    }

    public void SetPassword(@NonNull String Password) {

        this.password = Password;
    }

    @NonNull
    public String GetServer() {
        return this.server;
    }

    public void SetServer(@NonNull String Server) {
        this.server = Server;
    }
*/
  /*  @NonNull
    public String GetPortnum() {
        return this.portnum;
    }

    public void SetPortnum(@NonNull String Portnum) {

        this.portnum = Portnum;
    }

    @NonNull
    public Security GetSecurity() {
        return security;
    }

    public void SetSecurity(@NonNull Security security) {

        this.security = security;
    }

    public void SetSecurity(String security) {
        Log.d(TAG, "Set: " + security);
        SetSecurity(Security.from(security));
    }

    public boolean GetUsesticky() {
        return this.usesticky;
    }
*/
    //public void SetUsesticky(boolean Usesticky) {
    //    this.usesticky = Usesticky;
    //}

   /* public String GetSyncinterval() {
        return this.syncInterval;
    }
*/
    //public void SetSyncinterval(String Syncinterval) {
    //    this.syncInterval = Syncinterval;
    //}

    /*
    public void SetaccountHasNotChanged() {
        this.accountHasChanged = false;
    }
    */
/*

    @NonNull
    public Boolean GetaccountHasChanged() {
        return this.accountHasChanged;
    }
*/


    @Nullable
    public String GetImapFolder() {
        if (this.imapfolder.isEmpty())
            return DEFAULT_FOLDER_NAME;
        return this.imapfolder;
    }

    @Nullable
    public File GetRootDirAccount() {
        return rootDirAccount;
    }

/*

    private void SetFolderName(@NonNull String folder) {
        this.imapfolder = folder;
    }

*/

/*
    public void Clear() {
        this.username = null;
        this.password = null;
        this.server = null;
        this.portnum = null;
        this.security = Security.None;
        this.usesticky = false;
        this.imapfolder = null;
        this.accountHasChanged = false;
    }*/
}
