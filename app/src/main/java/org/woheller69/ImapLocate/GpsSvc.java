package org.woheller69.ImapLocate;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static org.woheller69.ImapLocate.BuildConfig.APPLICATION_ID;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.woheller69.ImapLocate.Miscs.UpdateThread;

public class GpsSvc extends Service implements LocationListener {

  public static final String ACTION_STOP_SERVICE = APPLICATION_ID + ".action.STOP_SERVICE";
  public static boolean mIsRunning = false;
  private GnssStatus.Callback mGnssStatusCallback;
  private final LocationManager mLocManager = (LocationManager) ImapNotes3.getAppContext().getSystemService(Context.LOCATION_SERVICE);
  private final PowerManager mPowerManager = (PowerManager) ImapNotes3.getAppContext().getSystemService(Context.POWER_SERVICE);
  private final NotificationManagerCompat mNotifManager = NotificationManagerCompat.from(ImapNotes3.getAppContext());
  private Location mGpsLoc;
  private long mGpsLocTime;
  public static final long MIN_DELAY = 10000;
  private WakeLock mWakeLock;
  private Builder mNotifBuilder;
  private final int NOTIF_ID = 100;
  private static final String CHANNEL_ID = "channel_gps_lock";
  private static final String CHANNEL_NAME = "GPS";
  private static long lastSyncTime;
  private static Location lastSyncLocation;
  private int mTotalSats, mUsedSats;
  private Future<?> mFuture;
  private final Object NOTIF_UPDATE_LOCK = new Object();
  private long mLastUpdate;

  @Deprecated
  @Override
  public void onStatusChanged(String provider, int status, Bundle extras){
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null || !ACTION_STOP_SERVICE.equals(intent.getAction())) {
      showNotif();
      startGpsLocListener();
      lastSyncLocation = null;
      lastSyncTime = 0;
      mIsRunning = true;
      return START_STICKY;
    } else {
      stop();
      return START_NOT_STICKY;
    }
  }

  @Override
  public void onDestroy() {

    super.onDestroy();
  }

  @Override
  public void onLocationChanged(Location location) {
    mGpsLoc = location;
    mGpsLocTime = System.currentTimeMillis();  // because location.getTime() gives wrong time
    updateNotification();
  }

  @Override
  public void onProviderEnabled(String provider) {
    mLastUpdate = 0;
    updateNotification();
  }

  @Override
  public void onProviderDisabled(String provider) {
    mLastUpdate = 0;
    updateNotification();
  }

  private void stop() {
    mIsRunning = false;
    stopGpsLocListener();
    if (mFuture != null) {
      mFuture.cancel(true);
    }
    stopSelf();
  }

  private void showNotif() {
    mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
    mWakeLock.acquire();

    NotificationManagerCompat nm = NotificationManagerCompat.from(ImapNotes3.getAppContext());
    NotificationChannelCompat ch = nm.getNotificationChannelCompat(CHANNEL_ID);
    if (ch == null) {
      ch = new NotificationChannelCompat.Builder(CHANNEL_ID, IMPORTANCE_DEFAULT).setName(CHANNEL_NAME).build();
      nm.createNotificationChannel(ch);
    }

    Intent intent = new Intent(ImapNotes3.getAppContext(), ListActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    PendingIntent pi = PendingIntent.getActivity(ImapNotes3.getAppContext(), NOTIF_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

    mNotifBuilder =
        new Builder(ImapNotes3.getAppContext(), CHANNEL_ID)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For N and below
            .setContentIntent(pi)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle("GPS");

    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
      startForeground(NOTIF_ID, mNotifBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
    } else {
      startForeground(NOTIF_ID, mNotifBuilder.build());
    }

    mLastUpdate = 0;
    updateNotification();
  }

  @SuppressLint("MissingPermission")
  private void startGpsLocListener() {
    mGnssStatusCallback = new GnssStatus.Callback() {
      @Override
      public void onSatelliteStatusChanged(@NonNull GnssStatus status) {

        mTotalSats = mUsedSats = 0;
        for (int i=0;i<status.getSatelliteCount();i++) {
          mTotalSats++;

          if (status.usedInFix(i)) {
            mUsedSats++;
          }
        }
        if ((mUsedSats<4) || (mGpsLoc!=null && System.currentTimeMillis()-mGpsLocTime > 2*MIN_DELAY)) {
          mGpsLoc=null;  //delete last location if less then 4 sats are in use or last update time longer than 2*MIN_DELAY-> fix lost
        }
        updateNotification();
        super.onSatelliteStatusChanged(status);
      }
    };
    mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_DELAY, 0, this);
    mLocManager.registerGnssStatusCallback(mGnssStatusCallback,new Handler(Looper.getMainLooper()));

  }

  private void stopGpsLocListener() {
    if (mWakeLock != null) {
      if (mWakeLock.isHeld()) {
        mWakeLock.release();
      }
      mWakeLock = null;
    }
    mLocManager.removeUpdates(this);
    mLocManager.unregisterGnssStatusCallback(mGnssStatusCallback);
  }

  private synchronized void updateNotification() {
    final ExecutorService BG_EXECUTOR = Executors.newCachedThreadPool();
    if (mFuture != null) {
      mFuture.cancel(true);
    }
    mFuture = BG_EXECUTOR.submit(this::updateNotifBg);
  }

  private void updateNotifBg() {
    synchronized (NOTIF_UPDATE_LOCK) {
      long sleep = MIN_DELAY + mLastUpdate - System.currentTimeMillis();
      if (sleep > 0) {
        try {
          NOTIF_UPDATE_LOCK.wait(sleep);
        } catch (InterruptedException e) {
          return;
        }
      }
      mLastUpdate = System.currentTimeMillis();

      String sText = "", bText="";

      mNotifBuilder.setContentTitle("GPS " +mUsedSats+" / " + mTotalSats);
      if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        sText = bText = "GPS off";
      } else {
        if (mGpsLoc != null
                && (mGpsLoc.getLatitude()) != 0
                && (mGpsLoc.getLongitude()) != 0) {
          sText = String.format(Locale.ENGLISH,"Lat: %.5f\nLon: %.5f", mGpsLoc.getLatitude(), mGpsLoc.getLongitude());

          String updateMessage;
          String speedMessage;
          String bearingMessage;
          String distanceMessage;

          float distance;
          if (lastSyncLocation==null) distance = Float.MAX_VALUE;
          else distance = mGpsLoc.distanceTo(lastSyncLocation);
          long currentTime = System.currentTimeMillis();

          if (((currentTime-lastSyncTime) > 30 * 60000) ||
              (((currentTime-lastSyncTime) > 15 * 60000) && distance > 30) ||
              (((currentTime-lastSyncTime) > 5 * 60000) && distance > 100)){

            updateMessage = String.format(Locale.ENGLISH,"Lat,Lon: %.5f,%.5f<br>Time: %d<br>%s",
                    mGpsLoc.getLatitude(),
                    mGpsLoc.getLongitude(),
                    System.currentTimeMillis(),
                    "https://www.openstreetmap.org/?mlat="+mGpsLoc.getLatitude()+"&mlon="+mGpsLoc.getLongitude()+"#map=16/"+mGpsLoc.getLatitude()+"/"+mGpsLoc.getLongitude());

            if (distance != Float.MAX_VALUE) distanceMessage = String.format(Locale.ENGLISH,"<br>Distance: %.0f m",lastSyncLocation.distanceTo(mGpsLoc));
            else distanceMessage = "";

            if (mGpsLoc.hasSpeed()) speedMessage = String.format(Locale.ENGLISH,"<br>Speed: %.0f m/s",mGpsLoc.getSpeed());
            else speedMessage = "";

            if (mGpsLoc.hasBearing()) bearingMessage = String.format(Locale.ENGLISH,"<br>Bearing: %.1f °",mGpsLoc.getBearing());
            else bearingMessage = "";

            new UpdateThread(updateMessage + speedMessage + bearingMessage + distanceMessage).execute();
            lastSyncTime = System.currentTimeMillis();
            lastSyncLocation = mGpsLoc;
          }
        }
      }
      mNotifBuilder.setContentText(sText);
      mNotifBuilder.setStyle(new BigTextStyle().bigText(bText));
      mNotifBuilder.setShowWhen(false);
      mNotifManager.notify(NOTIF_ID, mNotifBuilder.build());
    }
  }
}
