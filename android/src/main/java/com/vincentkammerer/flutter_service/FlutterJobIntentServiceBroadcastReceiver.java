package com.vincentkammerer.flutter_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FlutterJobIntentServiceBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    FlutterJobIntentService.enqueueBackgroundTaskProcessing(context, intent);
  }
}
