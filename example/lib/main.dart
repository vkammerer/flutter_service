import 'dart:async';
import 'dart:isolate';
import 'dart:math';
import 'dart:ui';

import 'package:android_alarm_manager/android_alarm_manager.dart';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_local_notifications_broadcast/flutter_local_notifications_broadcast.dart';
import 'package:flutter_service/flutter_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Global [SharedPreferences] object.
SharedPreferences prefs;

/// The [SharedPreferences] key to access the alarm fire count.
const String countKey = 'count';

/// A port used to communicate from a background isolate to the UI isolate.
final ReceivePort port = ReceivePort();

/// The name associated with the UI isolate's [SendPort].
const String isolateName = 'isolate';

final FlutterLocalNotificationsBroadcast flutterLocalNotificationsBroadcast =
    FlutterLocalNotificationsBroadcast();
final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

NotificationAppLaunchDetails notificationAppLaunchDetails;

var androidPlatformChannelSpecifics = AndroidNotificationDetails(
    'your channel id', 'your channel name', 'your channel description',
    importance: Importance.max, priority: Priority.high, ticker: 'ticker');

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  IsolateNameServer.registerPortWithName(
    port.sendPort,
    isolateName,
  );
  prefs = await SharedPreferences.getInstance();
  if (!prefs.containsKey(countKey)) {
    await prefs.setInt(countKey, 0);
  }

  notificationAppLaunchDetails =
      await flutterLocalNotificationsPlugin.getNotificationAppLaunchDetails();

  var initializationSettings = InitializationSettings(
      android: AndroidInitializationSettings('app_icon'));
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

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _counter = 0;

  @override
  void initState() {
    super.initState();
    initialize();
  }

  void initialize() async {
    FlutterService.initialize(jobIntentService: true, foregroundService: true);
    port.listen((_) async {
      await prefs.reload();
      setState(() {
        _counter = prefs.getInt(countKey);
      });
    });
    setState(() {
      _counter = prefs.getInt(countKey);
    });
  }

  static Future<void> updateCounter() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.reload();
    int currentCount = prefs.getInt(countKey);
    await prefs.setInt(countKey, currentCount + 1);

    // This will be null if we're running in the background.
    SendPort uiSendPort = IsolateNameServer.lookupPortByName(isolateName);
    uiSendPort?.send(null);
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
                Text('Shared preferences counter: $_counter'),
                RaisedButton(
                  child: Text(
                    'Update counter',
                  ),
                  onPressed: () async {
                    updateCounter();
                  },
                ),
                RaisedButton(
                  child: Text('Show Notification in ForegroundService'),
                  onPressed: () async {
                    await flutterLocalNotificationsBroadcast.broadcast(
                      1,
                      'Hello',
                      'I am the Foreground Service notification',
                      FlutterService
                          .foregroundServiceBroadcastReceiverClassName,
                      notificationDetails: androidPlatformChannelSpecifics,
                    );
                  },
                ),
                RaisedButton(
                  child: Text(
                    'Update counter from JobIntentService via Alarm Manager',
                  ),
                  onPressed: () async {
                    await AndroidAlarmManager.oneShot(
                      const Duration(seconds: 5),
                      // Ensure we have a unique alarm ID.
                      Random().nextInt(pow(2, 31)),
                      updateCounter,
                      exact: true,
                      wakeup: true,
                      allowWhileIdle: true,
                    );
                  },
                ),
                RaisedButton(
                  child: Text(
                      'Update counter from ForegroundService via Alarm Manager'),
                  onPressed: () async {
                    await AndroidAlarmManager.oneShot(
                      const Duration(seconds: 10),
                      // Ensure we have a unique alarm ID.
                      Random().nextInt(pow(2, 31)),
                      updateCounter,
                      exact: true,
                      allowWhileIdle: true,
                      wakeup: true,
                      rescheduleOnReboot: true,
                      serviceType: 'ForegroundService',
                    );
                  },
                ),
                RaisedButton(
                  child: Text('Stop ForegroundService'),
                  onPressed: () async {
                    await FlutterService.stopForegroundService();
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
