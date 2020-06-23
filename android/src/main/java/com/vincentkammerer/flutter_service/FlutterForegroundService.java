package com.vincentkammerer.flutter_service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FlutterForegroundService extends Service {

  private static final String TAG = "FlutterForegroundServ";

  /**
   * Background Dart execution context.
   */
  private static FlutterBackgroundExecutor flutterBackgroundExecutor;
  private static FlutterForegroundService instance = null;

  public static boolean isInstanceCreated() {
    return instance != null;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;

    if (flutterBackgroundExecutor == null) {
      flutterBackgroundExecutor = new FlutterBackgroundExecutor(FlutterForegroundService.class);
    }
    Context context = getApplicationContext();
    flutterBackgroundExecutor.startBackgroundIsolate(context);
  }

  @Override
  public void onDestroy() {
    instance = null;

    super.onDestroy();
  }


  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Notification notif = intent.getExtras().getParcelable("notification");
    startForeground(1, notif);
    return START_STICKY;
  }

  // TODO(mattcarroll): make taskQueue per-instance, not static.
  private static List<Intent> taskQueue = Collections.synchronizedList(new LinkedList<Intent>());

  public static void startBackgroundIsolate(Context context, long callbackHandle) {
    if (flutterBackgroundExecutor != null) {
      Log.w(TAG, "Attempted to start a duplicate background isolate. Returning...");
      return;
    }
    flutterBackgroundExecutor = new FlutterBackgroundExecutor(FlutterForegroundService.class);
    flutterBackgroundExecutor.startBackgroundIsolate(context, callbackHandle);
  }

  static void onInitialized() {
    Log.i(TAG, "FlutterForegroundService onInitialized!");
    synchronized (taskQueue) {
      // Handle all the alarm events received before the Dart isolate was
      // initialized, then clear the queue.
      Iterator<Intent> i = taskQueue.iterator();
      while (i.hasNext()) {
        flutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(i.next(), null);
      }
      taskQueue.clear();
    }
  }

  public static void enqueueBackgroundTaskProcessing(Intent intent) {
    // If we're in the middle of processing queued alarms, add the incoming
    // intent to the queue and return.
    synchronized (taskQueue) {
      if (!flutterBackgroundExecutor.isRunning()) {
        Log.i(TAG, "FlutterForegroundService has not yet started.");
        taskQueue.add(intent);
        return;
      }
    }
    flutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, null);
  }

}
