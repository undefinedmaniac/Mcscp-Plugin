package com.gmail.undifinedmaniac.mcscpplugin.network;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the initial handshake between the server and an MCSCP client
 */
public class McscpHandshake {

    private static final String PROTOCOL = "MCSCPV1.0.0";

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("\\[HANDSHAKE]:\\[PROTOCOL:(.*)]");

    private State mState = State.Idle;

    private enum State {
        Idle, SentProtocol,
        VerifiedClientProtocol, Complete,
        Failed;
    }

    McscpHandshake() {

    }

    /**
     * Gets the first message in the handshake
     * @return The first message
     */
    public String start() {
        if (mState == State.Idle) {
            mState = State.SentProtocol;
            return String.format("[HANDSHAKE]:[PROTOCOL:%s]", PROTOCOL);
        }
        return "";
    }

    public boolean processNewData(String data) {
        switch(mState) {
            case SentProtocol:
                Matcher matcher = PROTOCOL_PATTERN.matcher(data);
                if (matcher.find()) {
                    String clientProtocol = matcher.group(1);
                    if (clientProtocol.equals(PROTOCOL)) {
                        mState = State.VerifiedClientProtocol;
                        return true;
                    } else {
                        mState = State.Failed;
                        return false;
                    }
                }
                break;
            case VerifiedClientProtocol:
                mState = State.Complete;
                return data.toUpperCase().equals("[HANDSHAKE]:[READY]");
        }
        return false;
    }

    public String getNextMessage() {
        switch(mState) {
            case VerifiedClientProtocol:
                return "[HANDSHAKE]:[READY]";
            case Complete:
                return "[HANDSHAKE]:[COMPLETE]";
        }
        return "";
    }

    /**
     * Checks if the handshake is finished
     * @return true if finished, otherwise false
     */
    public boolean complete() {
        return mState == State.Complete;
    }
}
