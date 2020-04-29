package com.vincentkammerer.flutter_service;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterServicePlugin
 */
public class FlutterServicePlugin implements FlutterPlugin, MethodCallHandler {

  private static final String CHANNEL_NAME = "com.vincentkammerer.flutter_service/channel";
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    // TODO: Move channel instantiation to initialize method using flutterBinding.getBinaryMessenger ?
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(),
        CHANNEL_NAME);
    initizalize();
  }

  public static void registerWith(Registrar registrar) {
    FlutterServicePlugin instance = new FlutterServicePlugin();
    final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
    instance.initizalize();
  }

  private void initizalize() {
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
