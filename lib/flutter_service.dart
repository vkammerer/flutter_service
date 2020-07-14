import 'dart:async';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const String _pluginChannelName =
    "com.vincentkammerer.flutter_service/plugin_channel";
const String _backgroundExecutorChannelName =
    "com.vincentkammerer.flutter_service/background_executor_channel";
const String _initializeMethod = "INITIALIZE_METHOD";
const String _stopForegroundServiceMethod = "STOP_FOREGROUND_SERVICE_METHOD";

const String _callbackInitializedMethod = "initialized";

void _flutterServiceCallbackDispatcher() {
  WidgetsFlutterBinding.ensureInitialized();

  const MethodChannel _channel =
      MethodChannel(_backgroundExecutorChannelName, JSONMethodCodec());
  _channel.setMethodCallHandler((MethodCall call) async {
    final dynamic args = call.arguments;
    final CallbackHandle handle = CallbackHandle.fromRawHandle(args[0]);

    final Function closure = PluginUtilities.getCallbackFromHandle(handle);

    if (closure == null) {
      print('Fatal: could not find callback');
      exit(-1);
    }

    if (closure is Function()) {
      closure();
    } else if (closure is Function(int)) {
      final int id = args[1];
      closure(id);
    }
  });

  _channel.invokeMethod<void>(_callbackInitializedMethod);
}

typedef CallbackHandle _GetCallbackHandle(Function callback);

class FlutterService {
  static MethodChannel _channel =
      const MethodChannel(_pluginChannelName, JSONMethodCodec());

  static _GetCallbackHandle _getCallbackHandle =
      (Function callback) => PluginUtilities.getCallbackHandle(callback);

  static final String foregroundServiceBroadcastReceiverClassName =
      'com.vincentkammerer.flutter_service.FlutterForegroundServiceBroadcastReceiver';
  static final String jobIntentServiceBroadcastReceiverClassName =
      'com.vincentkammerer.flutter_service.FlutterForegroundServiceBroadcastReceiver';

  static Future<bool> initialize({
    bool jobIntentService = true,
    bool foregroundService = false,
  }) async {
    List<String> serviceTypes = [];
    if (jobIntentService) serviceTypes.add('JobIntentService');
    if (foregroundService) serviceTypes.add('ForegroundService');
    final CallbackHandle handle =
        _getCallbackHandle(_flutterServiceCallbackDispatcher);
    if (handle == null) {
      return false;
    }
    final bool r = await _channel.invokeMethod<bool>(
        _initializeMethod, <dynamic>[handle.toRawHandle(), serviceTypes]);
    return r ?? false;
  }

  static Future<bool> stopForegroundService() async {
    final bool r =
        await _channel.invokeMethod<bool>(_stopForegroundServiceMethod);
    return r ?? false;
  }
}
