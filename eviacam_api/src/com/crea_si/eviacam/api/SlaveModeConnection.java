package com.crea_si.eviacam.api;

public interface SlaveModeConnection {
    public void onConnected(SlaveMode connection);
    public void onDisconnected();
}