import 'dart:async';
import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/material.dart';
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
