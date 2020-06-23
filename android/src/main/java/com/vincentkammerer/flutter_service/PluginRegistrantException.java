package com.vincentkammerer.flutter_service;

public class PluginRegistrantException extends RuntimeException {
  public PluginRegistrantException() {
    super(
        "PluginRegistrantCallback is not set. Did you forget to call "
            + "FlutterServicePlugin.setPluginRegistrant? See the README for instructions.");
  }
}
