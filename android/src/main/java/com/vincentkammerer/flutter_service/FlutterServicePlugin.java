package com.vincentkammerer.flutter_service;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * FlutterServicePlugin
 */
public class FlutterServicePlugin implements FlutterPlugin, MethodCallHandler {

  private static final String CHANNEL_NAME = "com.vincentkammerer.flutter_service/plugin_channel";
  protected static final String SHARED_PREFERENCES_KEY = "com.vincentkammerer.flutter_service.shared_preference_key";
  private static String INITIALIZE_METHOD = "initialize";
  private static String TAG = "FlutterServicePlugin";
  private Context context;
  private Object initializationLock = new Object();
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    initialize(flutterPluginBinding.getApplicationContext(),
        flutterPluginBinding.getBinaryMessenger());
  }

  public static void registerWith(Registrar registrar) {
    FlutterServicePlugin instance = new FlutterServicePlugin();
    instance.initialize(registrar.context(), registrar.messenger());
  }

  private void initialize(Context context, BinaryMessenger messenger) {
    synchronized (initializationLock) {
      this.context = context;
      channel = new MethodChannel(messenger, CHANNEL_NAME, JSONMethodCodec.INSTANCE);
      channel.setMethodCallHandler(this);
    }
  }

  /**
   * Sets the Dart callback handle for the Dart method that is responsible for initializing the
   * background Dart isolate, preparing it to receive Dart callback tasks requests.
   */
  public static void setCallbackDispatcher(Context context, long callbackHandle) {
    FlutterBackgroundExecutor.setCallbackDispatcher(context, callbackHandle);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    String method = call.method;
    Object arguments = call.arguments;
    try {
      if (method.equals(INITIALIZE_METHOD)) {
        long callbackHandle = ((JSONArray) arguments).getLong(0);
        JSONArray serviceTypes = (JSONArray) ((JSONArray) arguments).get(1);
        setCallbackDispatcher(context, callbackHandle);
        for (int i = 0; i < serviceTypes.length(); i++) {
          String serviceType = serviceTypes.getString(i);
          if (serviceType.equals("JobIntentService")) {
            Log.i(TAG, "serviceType:" + serviceType);
            FlutterJobIntentService.startBackgroundIsolate(context, callbackHandle);
          } else if (serviceType.equals("ForegroundService")) {
            Log.i(TAG, "serviceType:" + serviceType);
            FlutterForegroundService.startBackgroundIsolate(context, callbackHandle);
          }
        }
        result.success(true);
      } else {
        result.notImplemented();
      }
    } catch (JSONException e) {
      result.error("error", "JSON error: " + e.getMessage(), null);
    } catch (PluginRegistrantException e) {
      result.error("error", "PluginRegistrantException error: " + e.getMessage(), null);
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    this.context = null;
    channel.setMethodCallHandler(null);
  }
}
