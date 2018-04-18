package com.gmail.undifinedmaniac.mcscpplugin;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

public class LogAppender extends AbstractAppender {

    private McscpPlugin mPlugin;
    private String mLogData;

    public LogAppender(McscpPlugin plugin) {
        super("McscpServer", null, null);
        mPlugin = plugin;
        mLogData = "";
        start();
    }

    public String getEntireLog() {
        return mLogData;
    }

    @Override
    public void append(LogEvent event) {
        String newData = (new SimpleDateFormat("hh:mm a").format(
                new Date()) + " [" + event.getLevel().toString() + "] " +
                event.getMessage().getFormattedMessage());
        mLogData += String.format("%s\r\n", newData);
        mPlugin.logEvent(newData);
    }
}