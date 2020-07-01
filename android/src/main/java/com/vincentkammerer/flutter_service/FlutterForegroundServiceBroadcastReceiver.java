package com.vincentkammerer.flutter_service;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class FlutterForegroundServiceBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    long callbackHandle = intent.getLongExtra("callbackHandle", 0);
    Notification notification = intent.getExtras().getParcelable("notification");

    if (FlutterForegroundService.isInstanceCreated() && callbackHandle != 0) {
      Intent callbackIntent = new Intent(context, FlutterForegroundService.class);
      callbackIntent.putExtra("callbackHandle", callbackHandle);
      int id = intent.getIntExtra("id", -1);
      callbackIntent.putExtra("id", id);
      FlutterForegroundService.enqueueBackgroundTaskProcessing(callbackIntent);
    } else if (notification != null) {
      Intent notificationIntent = new Intent(context, FlutterForegroundService.class);
      notificationIntent.putExtra("notification", notification);
      int notificationId = intent.getIntExtra("notificationId", -1);
      notificationIntent.putExtra("notificationId", notificationId);
      ContextCompat.startForegroundService(context, notificationIntent);
    }
  }
}
