package de.niendo.ImapLocate.Miscs;

import androidx.annotation.NonNull;

import android.net.TrafficStats;
import android.util.Log;

import de.niendo.ImapLocate.Data.Security;

import com.sun.mail.util.MailSSLSocketFactory;

import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

public class Imaper {

    private Store store;
    private static final String TAG = "IN_Imaper";

    // --Commented out by Inspection (11/26/16 11:43 PM):public static final String PREFS_NAME = "PrefsFile";

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeException = -2;
    public static final int ResultCodeCantConnect = -1;


    @NonNull
    public ImapNotesResult ConnectToProvider(String username,
                                             String password,
                                             String server,
                                             String portnum,
                                             @NonNull Security security,
                                             int threadID) throws MessagingException {

        TrafficStats.setThreadStatsTag(threadID);

        MailSSLSocketFactory sf;
        try {
            sf = new MailSSLSocketFactory();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return new ImapNotesResult(-1,
                    "Can't connect to server",
                    -1);
        }

        String proto = security.proto;

        Properties props = new Properties();

        props.setProperty(String.format("mail.%s.host", proto), server);
        props.setProperty(String.format("mail.%s.port", proto), portnum);
        props.setProperty("mail.store.protocol", proto);

        if (security.acceptcrt) {
            sf.setTrustedHosts(server);
            if (proto.equals("imap")) {
                props.put("mail.imap.ssl.socketFactory", sf);
                props.put("mail.imap.starttls.enable", "true");
            }
        } else if (security != Security.None) {
            props.put(String.format("mail.%s.ssl.checkserveridentity", proto), "true");
            if (proto.equals("imap")) {
                props.put("mail.imap.starttls.enable", "true");
            }
        }

        if (proto.equals("imaps")) {
            props.put("mail.imaps.socketFactory", sf);
        }

        props.setProperty("mail.imap.connectiontimeout", "1000");

        /* TODO: implement proxy handling properly.
        boolean useProxy = false;
        //noinspection ConstantConditions,ConstantConditions
        if (useProxy) {
            props.put("mail.imap.socks.host", "10.0.2.2");
            props.put("mail.imap.socks.port", "1080");
        }

         */
        Session session = Session.getInstance(props, null);
//session.setDebug(true);
        store = session.getStore(proto);
        try {
            store.connect(server, username, password);

            return new ImapNotesResult(ResultCodeSuccess,
                    "",
                    -1);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
            return new ImapNotesResult(ResultCodeException,
                    e.getMessage(),
                    -1);
        }
    }

    private boolean IsConnected() {
        return store != null && store.isConnected();
    }

    @Override
    protected void finalize() throws MessagingException {
        if (IsConnected()) {
            store.close();
        }
    }
}
