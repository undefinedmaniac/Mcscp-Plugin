package com.gmail.undifinedmaniac.mcscpplugin.network;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the initial handshake between the server and an MCSCP client
 */
public class McscpHandshake {

    private static final String PROTOCOL = "MCSCPV1.0";

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^P:(.+?)(?:\\s|$)");

    private Stage mStage = Stage.Idle;

    private enum Stage {
        Idle, SentProtocol,
        VerifyClientProtocol, StartSession;
    }

    public McscpHandshake() {

    }

    /**
     * Gets the initial message to send to the client
     * once it is connected
     * @return the first message to send
     */
    public String connected() {
        mStage = Stage.SentProtocol;
        StringBuilder builder = new StringBuilder();
        builder.append("P:");
        builder.append(PROTOCOL);
        builder.append(" HI");
        return builder.toString();
    }

    /**
     * Gets the next message to send to the client during the
     * handshake
     * @param message the last message received from the client
     * @return the next message to send to the client
     */
    public String messageReceived(String message) {
        if (mStage == Stage.SentProtocol) {
            mStage = Stage.VerifyClientProtocol;
            Matcher matcher = PROTOCOL_PATTERN.matcher(message);
            if (matcher.find()) {
                String clientProtocol = matcher.group(1);
                if (clientProtocol != null && verifyClientProtocol(clientProtocol)) {
                    return "GOOD TO GO";
                }
            }
        } else if (mStage == Stage.VerifyClientProtocol) {
            if (message.equals("GOOD TO GO")) {
                mStage = Stage.StartSession;
                return "SESSION STARTED";
            }
        }

        return null;
    }

    /**
     * Checks if the handshake is finished
     * @return true if finished, otherwise false
     */
    public boolean finished() {
        return mStage == Stage.StartSession;
    }

    /**
     * Verifies that a client's MCSCP version is
     * compatible with this server
     * @param protocol the client's protocol
     * @return true if compatible, otherwise false
     */
    private boolean verifyClientProtocol(String protocol) {
        return (protocol.equals(PROTOCOL));
    }
}
