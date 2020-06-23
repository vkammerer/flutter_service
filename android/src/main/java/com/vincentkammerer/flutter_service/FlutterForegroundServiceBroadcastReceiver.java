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
    int id = intent.getIntExtra("id", -1);

    Intent serviceIntent = new Intent(context, FlutterForegroundService.class);
    serviceIntent.putExtra("callbackHandle", callbackHandle);
    serviceIntent.putExtra("id", id);
    Notification notif = intent.getExtras().getParcelable("notification");
    if (notif != null) {
      serviceIntent.putExtra("notification", notif);
    }

    if (FlutterForegroundService.isInstanceCreated() && callbackHandle != 0) {
      FlutterForegroundService.enqueueBackgroundTaskProcessing(serviceIntent);
    } else {
      ContextCompat.startForegroundService(context, serviceIntent);
    }
  }
}
