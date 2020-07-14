package com.vincentkammerer.flutter_service;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class FlutterForegroundServiceBroadcastReceiver extends BroadcastReceiver {
  static String TAG = "FlutterForegroundServiceBroadcastReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    /*
    * Two kinds of intents can be received:
    *   - to start the foreground service with a notification
    *   - or to execute a dart callback within a previously started service
    * */

    // Start the foreground service with a notification
    Notification notification =
        intent.getExtras() == null ? null : (Notification) intent.getExtras().getParcelable("notification");
    if (notification != null) {
      Intent notificationIntent = new Intent(context, FlutterForegroundService.class);
      notificationIntent.putExtra("notification", notification);
      int notificationId = intent.getIntExtra("notificationId", -1);
      notificationIntent.putExtra("notificationId", notificationId);
      ContextCompat.startForegroundService(context, notificationIntent);
      return;
    }

    // Execute a dart callback within a previously started service
    long callbackHandle = intent.getLongExtra("callbackHandle", 0);
    if (callbackHandle != 0) {
      if (!FlutterForegroundService.isInstanceCreated()) {
        Log.w(TAG, "FlutterForegroundService can run dart callbacks only if it has been started previously");
        return;
      }
      Intent callbackIntent = new Intent(context, FlutterForegroundService.class);
      callbackIntent.putExtra("callbackHandle", callbackHandle);
      int id = intent.getIntExtra("id", -1);
      callbackIntent.putExtra("id", id);
      FlutterForegroundService.enqueueBackgroundTaskProcessing(callbackIntent);
    }
  }
}
