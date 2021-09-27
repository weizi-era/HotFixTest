package com.example.plugin;

public class PluginExt {

    private boolean debugOn;
    private String applicationName;
    private String output;

    public PluginExt() {
    }

    public PluginExt(boolean debugOn, String applicationName, String output) {
        this.debugOn = debugOn;
        this.applicationName = applicationName;
        this.output = output;
    }

    public boolean isDebugOn() {
        return debugOn;
    }

    public void setDebugOn(boolean debugOn) {
        this.debugOn = debugOn;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
