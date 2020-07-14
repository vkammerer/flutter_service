# flutter_service

**Warning: Highly experimental**   

flutter_service is a [Flutter plugin](https://flutter.dev/docs/development/packages-and-plugins/developing-packages) that other plugins can depend on in order to:   
- implement a long running / background execution context for Dart code.
- share this execution context with other plugins.

### Why?   
Flutter is great, and Flutter plugins are great, and most of the time they "just work".   

However, when developers need to run Dart code in the background, things get more complicated because:
- they need to implement their own abstraction of long running processes that can execute Dart code.
- the plugins they use in the foreground don't work necessarily well together in these contexts.

A general discussion of this topic can be found at https://github.com/flutter/flutter/issues/32164, and an example of the particular problem this plugin is trying to address can be read in [this comment](https://github.com/flutter/flutter/issues/32164#issuecomment-490231243).

### Current implementation   
`flutter_services` currently exposes two Android execution contexts:   
- a [JobIntentService](https://developer.android.com/reference/androidx/core/app/JobIntentService)
- a [Foreground Service](https://developer.android.com/guide/components/services)

### Usage
A plugin developer can decide to depend on `flutter_service` by:
- adding it as a dependency in `pubspec.yaml`
- broadcasting an [Intent](https://developer.android.com/reference/android/content/Intent) to
  - `com.vincentkammerer.flutter_service.JobIntentServiceBroadcastReceiver`
  - or `com.vincentkammerer.flutter_service.ForegroundServiceBroadcastReceiver`

The signature of the `Intent` is currently not documented as this is likely to change but can be seen in the [Android implementation](https://github.com/vkammerer/flutter_service/tree/master/android/src/main/java/com/vincentkammerer/flutter_service).

A user can then implement it by:
- initializing `flutter_service` with:
```dart
// at least one of the two should be set to true
FlutterService.initialize(
  jobIntentService: true,
  foregroundService: true,
);
```
- and then calling the plugin via its own API.   

### Example   
The [example](https://github.com/vkammerer/flutter_service/blob/master/example/lib/main.dart) shows how the following plugins could take advantage of `flutter_service`:
- [android_alarm_manager](https://github.com/flutter/plugins/tree/master/packages/android_alarm_manager)
- [flutter_local_notifications](https://github.com/MaikuB/flutter_local_notifications/tree/master/flutter_local_notifications)

In order to make them work with `flutter_service`, they have been adapted, and the corresponding repositories can be found at:
- [android_alarm_manager](https://github.com/vkammerer/android_alarm_manager)
- [flutter_local_notifications_broadcast](https://github.com/vkammerer/flutter_local_notifications_broadcast)

As the example shows, the following workflow can then occur:
- a Foreground Service is started with a notification generated by `flutter_local_notifications_broadcast`
- later on, even if the main application is closed, the Android Alarm manager can initiate a dart callback to execute within that Foreground Service via `android_alarm_manager`

While this example is very basic, it shows how other plugins which require background execution contexts could benefit from this common abstraction. For example, [firebase_messaging](https://github.com/FirebaseExtended/flutterfire/tree/master/packages/firebase_messaging) could also execute its dart callbacks within a `flutter_service` context.

