import 'dart:async';
import 'dart:math';

import 'package:android_alarm_manager/android_alarm_manager.dart';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_local_notifications_broadcast/flutter_local_notifications_broadcast.dart';
import 'package:flutter_service/flutter_service.dart';

final FlutterLocalNotificationsBroadcast flutterLocalNotificationsBroadcast =
    FlutterLocalNotificationsBroadcast();
final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

NotificationAppLaunchDetails notificationAppLaunchDetails;

var androidPlatformChannelSpecifics = AndroidNotificationDetails(
    'your channel id', 'your channel name', 'your channel description',
    importance: Importance.Max, priority: Priority.High, ticker: 'ticker');

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  notificationAppLaunchDetails =
      await flutterLocalNotificationsPlugin.getNotificationAppLaunchDetails();

  var initializationSettingsAndroid = AndroidInitializationSettings('app_icon');
  var initializationSettingsIOS = IOSInitializationSettings();
  var initializationSettings = InitializationSettings(
      initializationSettingsAndroid, initializationSettingsIOS);
  await flutterLocalNotificationsPlugin.initialize(
    initializationSettings,
    onSelectNotification: (String payload) async {
      if (payload != null) {
        debugPrint('notification payload: ' + payload);
      }
    },
  );
  runApp(MyApp());
}

class PaddedRaisedButton extends StatelessWidget {
  final String buttonText;
  final VoidCallback onPressed;

  const PaddedRaisedButton({
    @required this.buttonText,
    @required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(0.0, 0.0, 0.0, 20.0),
      child: RaisedButton(child: Text(buttonText), onPressed: onPressed),
    );
  }
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    FlutterService.initialize(jobIntentService: true, foregroundService: true);
  }

  Future<void> _broadcastNotification() async {
    await flutterLocalNotificationsBroadcast.broadcast(
        1,
        'plain title',
        'plain body',
        'com.vincentkammerer.flutter_service.FlutterForegroundServiceBroadcastReceiver',
        notificationDetails: androidPlatformChannelSpecifics,
        payload: 'item x');
  }

  Future<void> _stopForegroundService() async {
    await FlutterService.stopForegroundService();
  }

  static Future<void> foregroundServiceCallback() async {
    print('Alarm fired!');

    // Get the previous cached count and increment it.
    await Future.delayed(Duration(seconds: 2));
    DateTime _now = DateTime.now();
    await flutterLocalNotificationsBroadcast.broadcast(
        1,
        'Updated in ForegroundService',
        'notification updated in ForegroundService at ${_now.hour}:${_now.minute}:${_now.second}',
        'com.vincentkammerer.flutter_service.FlutterForegroundServiceBroadcastReceiver',
        notificationDetails: androidPlatformChannelSpecifics,
        payload: 'item x');
  }

  static Future<void> jobIntentServiceCallback() async {
    print('Alarm fired 2!');

    // Get the previous cached count and increment it.
    await Future.delayed(Duration(seconds: 2));
    DateTime _now = DateTime.now();
    await flutterLocalNotificationsBroadcast.broadcast(
        1,
        'Updated in JobIntentService',
        'notification updated in JobIntentService at ${_now.hour}:${_now.minute}:${_now.second}',
        'com.vincentkammerer.flutter_service.FlutterForegroundServiceBroadcastReceiver',
        notificationDetails: androidPlatformChannelSpecifics,
        payload: 'item x');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Padding(
          padding: EdgeInsets.all(8.0),
          child: Center(
            child: Column(
              children: <Widget>[
                PaddedRaisedButton(
                  buttonText: 'Show Notification in Foreground Service',
                  onPressed: () async {
                    await _broadcastNotification();
                  },
                ),
                PaddedRaisedButton(
                  buttonText: 'Stop Foreground Service',
                  onPressed: () async {
                    await _stopForegroundService();
                  },
                ),
                PaddedRaisedButton(
                  buttonText:
                      'Schedule Notification update from ForegroundService',
                  onPressed: () async {
                    await AndroidAlarmManager.oneShot(
                        const Duration(seconds: 10),
                        // Ensure we have a unique alarm ID.
                        Random().nextInt(pow(2, 31)),
                        foregroundServiceCallback,
                        exact: true,
                        allowWhileIdle: true,
                        wakeup: true,
                        rescheduleOnReboot: true,
                        serviceType: 'ForegroundService');
                  },
                ),
                PaddedRaisedButton(
                  buttonText:
                      'Schedule Notification update from JobIntentService',
                  onPressed: () async {
                    await AndroidAlarmManager.oneShot(
                      const Duration(seconds: 10),
                      // Ensure we have a unique alarm ID.
                      Random().nextInt(pow(2, 31)),
                      jobIntentServiceCallback,
                      exact: true,
                      allowWhileIdle: true,
                      wakeup: true,
                      rescheduleOnReboot: true,
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
