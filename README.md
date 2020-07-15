### Warning: This is an experiment   
This project is an attempt at addressing some of the issues discussed in the general thread at https://github.com/flutter/flutter/issues/32164.

I have created it because:
- it solves a real world issue I have in one of my applications, similar to the one described in [this comment](https://github.com/flutter/flutter/issues/32164#issuecomment-490231243).
- it may help the discussion about what a good long term design could be for background execution in Flutter?

Please open an issue in this repository if you have any comment or suggestion.

### What is it?   
`flutter_service` is a [Flutter plugin](https://flutter.dev/docs/development/packages-and-plugins/developing-packages) that other plugins can depend on in order to:   
- implement a long running / background execution context for Flutter Dart code on Android.
- share this execution context with other plugins.

### Why?   
Flutter exposes low level utilities on Android to execute Dart code, like:
```java
/*
* Simplified example
*/
FlutterEngine flutterEngine = new FlutterEngine(context);
DartExecutor executor = flutterEngine.getDartExecutor();
executor.executeDartCallback(dartCallback);
```

However, two problems arise when developers implement this pattern themselves:
- the full implementation is tedious and error prone.
- it isn't possible for the application plugins to share that execution context.

### Current contexts   
`flutter_services` currently exposes two Android execution contexts:   
- a [JobIntentService](https://developer.android.com/reference/androidx/core/app/JobIntentService)
- a [Foreground Service](https://developer.android.com/guide/components/services)

But other contexts could be added in the future, such as
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- or other general purpose low level implementations

### Usage   
##### Plugin developer   
A plugin developer can decide to depend on `flutter_service` by:
- adding it as a dependency in `pubspec.yaml`
- broadcasting an [Intent](https://developer.android.com/reference/android/content/Intent) to
  - `com.vincentkammerer.flutter_service.JobIntentServiceBroadcastReceiver`
  - or `com.vincentkammerer.flutter_service.ForegroundServiceBroadcastReceiver`

The signatures of the Intents are currently not documented as they are likely to change but can be seen in the [Android implementation code](https://github.com/vkammerer/flutter_service/tree/master/android/src/main/java/com/vincentkammerer/flutter_service).

##### User   
A user can then implement it in their application by:
- initializing `flutter_service` with:
```dart
// at least one of the two should be set to true
FlutterService.initialize(
  jobIntentService: true,
  foregroundService: true,
);
```
- and then calling the plugins via their own APIs.   

### Example   
The [example Flutter project](https://github.com/vkammerer/flutter_service/blob/master/example/lib/main.dart) shows how the following plugins could take advantage of `flutter_service`:
- `android_alarm_manager`
- `flutter_local_notifications`

In order to make them work with `flutter_service`, they have been adapted, and the corresponding repositories can be found at:
- [android_alarm_manager](https://github.com/vkammerer/android_alarm_manager)
- [flutter_local_notifications_broadcast](https://github.com/vkammerer/flutter_local_notifications_broadcast)

As the example shows, the following workflow can then occur:
- `flutter_local_notifications_broadcast` starts the Foreground Service by broadcasting a notification
- `android_alarm_manager` schedules a Dart callback to execute within that Foreground Service at a later time   

![example diagram for flutter_service](https://vkammerer.github.io/flutter_service/images/flutter_service_example.svg?)

### Potential other use cases   
While the current example is very basic, it shows how other plugins could benefit from a common background execution context.   
For example, [firebase_messaging](https://github.com/FirebaseExtended/flutterfire/tree/master/packages/firebase_messaging) could also execute its dart callbacks within a `flutter_service` context, and an application could be designed in the following manner:   

![potential use diagram for flutter_service](https://vkammerer.github.io/flutter_service/images/flutter_service_potential_use.svg)

