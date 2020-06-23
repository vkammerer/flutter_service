package com.vincentkammerer.flutter_service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.dart.DartExecutor.DartCallback;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.view.FlutterMain;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlutterBackgroundExecutor implements MethodCallHandler {

  public static final String BACKGROUND_EXECUTOR_CHANNEL_NAME = "com.vincentkammerer.flutter_service/background_executor_channel";
  public static final String INTIALIZED_METHOD = "initialized";

  private static final String TAG = "FlutterBackgroundExec";
  private static final String CALLBACK_HANDLE_KEY = "callback_handle";

  /**
   * The {@link MethodChannel} that connects the Android side of this plugin with the background
   * Dart isolate that was created by this plugin.
   */
  private MethodChannel backgroundChannel;

  private FlutterEngine backgroundFlutterEngine;

  private AtomicBoolean isCallbackDispatcherReady = new AtomicBoolean(false);

  private Class executionContextClass;

  public FlutterBackgroundExecutor(Class executionContextClass) {
    this.executionContextClass = executionContextClass;
  }

  public void startBackgroundIsolate(Context context) {
    if (!isRunning()) {
      SharedPreferences p =
          context.getSharedPreferences(FlutterServicePlugin.SHARED_PREFERENCES_KEY, 0);
      long callbackHandle = p.getLong(CALLBACK_HANDLE_KEY, 0);
      startBackgroundIsolate(context, callbackHandle);
    }
  }

  public void startBackgroundIsolate(Context context, long callbackHandle) {
    if (backgroundFlutterEngine != null) {
      Log.e(TAG, "Background isolate already started");
      return;
    }

    String appBundlePath = FlutterMain.findAppBundlePath();
    AssetManager assets = context.getAssets();
    if (appBundlePath != null && !isRunning()) {
      backgroundFlutterEngine = new FlutterEngine(context);

      // We need to create an instance of `FlutterEngine` before looking up the
      // callback. If we don't, the callback cache won't be initialized and the
      // lookup will fail.
      FlutterCallbackInformation flutterCallback =
          FlutterCallbackInformation.lookupCallbackInformation(callbackHandle);
      if (flutterCallback == null) {
        Log.e(TAG, "Fatal: failed to find callback");
        return;
      }

      DartExecutor executor = backgroundFlutterEngine.getDartExecutor();
      initializeMethodChannel(executor);
      DartCallback dartCallback = new DartCallback(assets, appBundlePath, flutterCallback);

      executor.executeDartCallback(dartCallback);
    }
  }

  private void initializeMethodChannel(BinaryMessenger isolate) {
    backgroundChannel =
        new MethodChannel(
            isolate, BACKGROUND_EXECUTOR_CHANNEL_NAME, JSONMethodCodec.INSTANCE);
    backgroundChannel.setMethodCallHandler(this);
  }

  public static void setCallbackDispatcher(Context context, long callbackHandle) {
    SharedPreferences prefs =
        context.getSharedPreferences(FlutterServicePlugin.SHARED_PREFERENCES_KEY, 0);
    prefs.edit().putLong(CALLBACK_HANDLE_KEY, callbackHandle).apply();
  }

  public boolean isRunning() {
    return isCallbackDispatcherReady.get();
  }

  private void onInitialized() {
    isCallbackDispatcherReady.set(true);
    if (this.executionContextClass == FlutterForegroundService.class) {
      FlutterForegroundService.onInitialized();
    } else if (this.executionContextClass == FlutterJobIntentService.class) {
      FlutterJobIntentService.onInitialized();
    }
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    String method = call.method;
    Object arguments = call.arguments;
    try {
      if (method.equals(INTIALIZED_METHOD)) {
        // This message is sent by the background method channel as soon as the background isolate
        // is running. From this point forward, the Android side of this plugin can send
        // callback handles through the background method channel, and the Dart side will execute
        // the Dart methods corresponding to those callback handles.
        onInitialized();
        result.success(true);
      } else {
        result.notImplemented();
      }
    } catch (PluginRegistrantException e) {
      result.error("error", "BackgroundTask error: " + e.getMessage(), null);
    }
  }

  /**
   * Executes the desired Dart callback in a background Dart isolate.
   *
   * <p>The given {@code intent} should contain a {@code long} extra called "callbackHandle", which
   * corresponds to a callback registered with the Dart VM.
   */
  public void executeDartCallbackInBackgroundIsolate(Intent intent, final CountDownLatch latch) {
    Log.i(TAG,
        "executeDartCallbackInBackgroundIsolate. ThreadName:" + Thread.currentThread().getName());
    // Grab the handle for the callback associated with this alarm. Pay close
    // attention to the type of the callback handle as storing this value in a
    // variable of the wrong size will cause the callback lookup to fail.
    long callbackHandle = intent.getLongExtra("callbackHandle", 0);

    // If another thread is waiting, then wake that thread when the callback returns a result.
    MethodChannel.Result result = null;
    if (latch != null) {
      result =
          new MethodChannel.Result() {
            @Override
            public void success(Object result) {
              Log.w(TAG, "executeDartCallbackInBackgroundIsolate success");
              latch.countDown();
            }

            @Override
            public void error(String errorCode, String errorMessage, Object errorDetails) {
              latch.countDown();
            }

            @Override
            public void notImplemented() {
              latch.countDown();
            }
          };
    }

    Map<String, Object> messageData = new HashMap<>();
    messageData.put("id", intent.getIntExtra("id", -1));

    // Handle the alarm event in Dart. Note that for this plugin, we don't
    // care about the method name as we simply lookup and invoke the callback
    // provided.
    backgroundChannel.invokeMethod(
        "invokeBackgroundTaskCallback", new Object[]{callbackHandle, messageData}, result);
  }

}
