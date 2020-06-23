import 'dart:async';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const String _pluginChannelName =
    "com.vincentkammerer.flutter_service/plugin_channel";
const String _backgroundExecutorChannelName =
    "com.vincentkammerer.flutter_service/background_executor_channel";
const String _initializeMethod = "initialize";
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

typedef DateTime _Now();
typedef CallbackHandle _GetCallbackHandle(Function callback);

class FlutterService {
  static MethodChannel _channel =
      const MethodChannel(_pluginChannelName, JSONMethodCodec());

  static _Now _now = () => DateTime.now();
  static _GetCallbackHandle _getCallbackHandle =
      (Function callback) => PluginUtilities.getCallbackHandle(callback);

  /// This is exposed for the unit tests. It should not be accessed by users of
  /// the plugin.
  @visibleForTesting
  static void setTestOverides(
      {_Now now, _GetCallbackHandle getCallbackHandle}) {
    _now = (now ?? _now);
    _getCallbackHandle = (getCallbackHandle ?? _getCallbackHandle);
  }

  static Future<bool> initialize(
      {bool jobIntentService = true, bool foregroundService = false}) async {
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
}
