package com.adhkar.salawat;

import android.os.Bundle;

import com.adhkar.salawat.reminder.ReminderPlugin;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ReminderPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
