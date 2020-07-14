package com.vincentkammerer.flutter_service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import androidx.core.app.JobIntentService;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FlutterJobIntentService extends JobIntentService {

  private static final String TAG = "FlutterJobIntentService";
  private static final int JOB_ID = 1985; // Random job ID.

  private static List<Intent> taskQueue = Collections.synchronizedList(new LinkedList<Intent>());

  /**
   * Background Dart execution context.
   */
  private static FlutterBackgroundExecutor flutterBackgroundExecutor;

  @Override
  public void onCreate() {
    super.onCreate();
    if (flutterBackgroundExecutor == null) {
      flutterBackgroundExecutor = new FlutterBackgroundExecutor(FlutterJobIntentService.class);
    }
    Context context = getApplicationContext();
    flutterBackgroundExecutor.startBackgroundIsolate(context);
  }

  public static void startBackgroundIsolate(Context context, long callbackHandle) {
    if (flutterBackgroundExecutor != null) {
      Log.w(TAG, "Attempted to start a duplicate background isolate. Returning...");
      return;
    }
    flutterBackgroundExecutor = new FlutterBackgroundExecutor(FlutterJobIntentService.class);
    flutterBackgroundExecutor.startBackgroundIsolate(context, callbackHandle);
  }

  /**
   * Called once the Dart isolate ({@code flutterBackgroundExecutor}) has finished initializing.
   *
   * <p>Invoked by {@link FlutterServicePlugin} when it receives the {@code
   * FlutterJobIntentService.initialized} message. Processes all alarm events that came in while the
   * isolate was starting.
   */
  static void onInitialized() {
    Log.i(TAG, "FlutterJobIntentService onInitialized!");
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

  /**
   * Schedule the alarm to be handled by the {@link FlutterJobIntentService}.
   */
  public static void enqueueBackgroundTaskProcessing(Context context, Intent intent) {
    Log.i(TAG, "FlutterJobIntentService enqueueBackgroundTaskProcessing.");
    enqueueWork(context, FlutterJobIntentService.class, JOB_ID, intent);
  }

  /**
   * Executes a Dart callback, as specified within the incoming {@code intent}.
   *
   * <p>Invoked by our {@link JobIntentService} superclass after a call to {@link
   * JobIntentService#enqueueWork(Context, Class, int, Intent);}.
   *
   * <p>If there are no pre-existing callback execution requests, other than the incoming {@code
   * intent}, then the desired Dart callback is invoked immediately.
   *
   * <p>If there are any pre-existing callback requests that have yet to be executed, the incoming
   * {@code intent} is added to the {@link #taskQueue} to invoked later, after all pre-existing
   * callbacks have been executed.
   */
  @Override
  protected void onHandleWork(final Intent intent) {
    Log.i(TAG, "FlutterJobIntentService onHandleWork. ThreadName:" + Thread.currentThread().getName());
    // If we're in the middle of processing queued alarms, add the incoming
    // intent to the queue and return.
    synchronized (taskQueue) {
      if (!flutterBackgroundExecutor.isRunning()) {
        Log.i(TAG, "FlutterJobIntentService has not yet started.");
        taskQueue.add(intent);
        return;
      }
    }

    // There were no pre-existing callback requests. Execute the callback
    // specified by the incoming intent.
    final CountDownLatch latch = new CountDownLatch(1);
    new Handler(getMainLooper())
        .post(
            new Runnable() {
              @Override
              public void run() {
                flutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, latch);
              }
            });

    try {
      latch.await();
    } catch (InterruptedException ex) {
      Log.i(TAG, "Exception waiting to execute Dart callback", ex);
    }
  }
}
