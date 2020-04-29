import 'dart:async';

import 'package:flutter/services.dart';

class FlutterService {
  static const String _channelName =
      "com.vincentkammerer.flutter_service/channel";
  static const MethodChannel _channel = const MethodChannel(_channelName);

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
