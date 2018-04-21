package com.gmail.undifinedmaniac.mcscpplugin.network;

import com.gmail.undifinedmaniac.mcscpplugin.command.McscpCommand;
import com.gmail.undifinedmaniac.mcscpplugin.table.McscpPlayerTable;
import com.gmail.undifinedmaniac.mcscpplugin.table.McscpServerTable;

import java.util.HashMap;
import java.util.Queue;
import java.util.Iterator;
import java.util.LinkedList;

import java.io.IOException;

import java.net.SocketAddress;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;

/**
 * Represents a single client connected to the server
 * Handles read / write events, session flags, and command requests
 */
public class McscpClient {

    public enum Flag {
        ReportPlayerJoin, ReportPlayerLeave, CmdResponse,
        ReportChat, ReportPlayerDeath, SendServerLog
    }

    private McscpTcpServer mServer;
    private SocketChannel mChannel;
    private SelectionKey mKey;

    private Queue<ByteBuffer> mOutgoingBuffer;
    private McscpHandshake mHandshake;
    private McscpCommand mCommand;
    private HashMap<Flag, Boolean> mFlags;

    McscpClient(McscpTcpServer server, SocketChannel channel, SelectionKey key) {
        mServer = server;
        mChannel = channel;
        mKey = key;

        mOutgoingBuffer = new LinkedList<>();
        mHandshake = new McscpHandshake();
        mCommand = null;
        mFlags = new HashMap<>();
    }

    public static Flag getFlagType(String flagName) {
        switch (flagName.toUpperCase()) {
            case "REPORTPLAYERJOIN": {
                return Flag.ReportPlayerJoin;
            }
            case "REPORTPLAYERLEAVE": {
                return Flag.ReportPlayerLeave;
            }
            case "CMDRESPONSE": {
                return Flag.CmdResponse;
            }
            case "REPORTCHAT": {
                return Flag.ReportChat;
            }
            case "REPORTPLAYERDEATH": {
                return Flag.ReportPlayerDeath;
            }
            case "SENDSERVERLOG": {
                return Flag.SendServerLog;
            }
            default: {
                return null;
            }
        }
    }

    public static Boolean convertStringToBool(String value) {
        switch (value.toUpperCase()) {
            case "TRUE": {
                return true;
            }
            case "FALSE": {
                return false;
            }
            default: {
                return null;
            }
        }
    }

    /**
     * Get the associated server for this client
     * @return the server
     */
    public McscpTcpServer server() {
        return mServer;
    }

    /**
     * Get the associated SelectionKey for this client
     * @return the SelectionKey
     */
    public SelectionKey key() {
        return mKey;
    }

    /**
     * Get this clients remote address
     * @return the remote address
     */
    public SocketAddress address() {
        return mChannel.socket().getRemoteSocketAddress();
    }

    /**
     * Get the value of a session flag
     * @param flag the flag to check
     * @return true if enabled, otherwise false
     */
    public boolean getFlag(Flag flag) {
        Boolean value = mFlags.get(flag);
        if (value != null)
            return value;

        return false;
    }

    /**
     * Set the state of a session flag
     * @param flag the flag to set
     * @param value the value to set
     */
    public void setFlag(Flag flag, boolean value) {
        mFlags.put(flag, value);
    }

    /**
     * Start the handshake by sending the first message to the client
     */
    public void startHandshake() {
        sendToClient(mHandshake.start());
    }

    /**
     * Disconnect from the client
     */
    public void close() {
        SocketAddress remoteAddress = address();
        mServer.getDataFetcher().logMessage(Level.INFO,"Client disconnected: " + remoteAddress);

        try {
            mChannel.close();
        } catch (IOException error) {
            mServer.getDataFetcher().logMessage(Level.SEVERE,"ERROR: IOException while closing socket from disconnected client: " +
                    remoteAddress);
        }

        mKey.cancel();
    }

    /**
     * Handles a read event from the client by continuing the handshake /
     * processing commands
     */
    public void readEvent() {
        //Allocate a buffer for reading
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numberOfBytesRead = -1;

        try {
            //Read from the client
            numberOfBytesRead = mChannel.read(buffer);
        } catch (IOException error) {
            mServer.getDataFetcher().logMessage(Level.SEVERE, "ERROR: IOException while reading data from client: " +
                    address());
        }

        //Drop the client if no bytes were read
        if (numberOfBytesRead == -1) {
            mServer.dropClient(this);
            return;
        }

        buffer.flip();

        //Decode the UTF-8 input and trim the message
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
        String message = charBuffer.toString();
        message = message.trim();

        //If the handshake is not finished, continue with it
        if (!mHandshake.complete()) {
            if (mHandshake.processNewData(message)) {
                String reply = mHandshake.getNextMessage();

                if (!reply.isEmpty())
                    sendToClient(reply);

                //Request all the table data once we are finished with the handshake
                if (mHandshake.complete())
                    mServer.requestAllTableData(this);
            } else {
                mServer.dropClient(this);
            }
        } else {
            //Work on command processing - if there is no current command
            // a new one will be created. Otherwise, new data will be added
            // to the existing command
            mCommand = new McscpCommand(this, message);

            mServer.getCommandProcessor().processCommand(mCommand);

            if (mCommand.hasReply()) {
                boolean replyEnabled = true;

                //Do not send data from a console command if the CmdResponse flag is false
                if (mCommand.getType() == McscpCommand.CommandType.Console &&
                        !getFlag(Flag.CmdResponse))
                    replyEnabled = false;

                if (replyEnabled)
                    sendToClient(mCommand.getReply());
            }
        }
    }

    /**
     * Handles a write event for the client by trying to write out messages from the
     * outgoing buffer
     */
    public void writeEvent() {
        boolean finished = false;

        try {
            //Process as much of the outgoing buffer as we can
            finished = processOutgoingBuffer();
        } catch (IOException error) {
            mServer.getDataFetcher().logMessage(Level.SEVERE, "ERROR: IOException while processing buffered writes for the client: " +
                    address());
        }

        //If the outgoing buffer is empty, deregister the socket for write events
        if (finished) {
            try {
                mChannel.register(mServer.getSelector(), SelectionKey.OP_READ);
            } catch (ClosedChannelException error) {
                error.printStackTrace();
            }
        }
    }

    public void serverTableUpdate(McscpServerTable.Key key, String valueString) {
        if (mHandshake.complete()) {
            sendToClient(String.format("[UPDATE]:[KEY:%s]:[VALUE:%s]", key.toString().toUpperCase(), valueString));
        }
    }
    public void playerTableUpdate(String uuid, McscpPlayerTable.Key key, String valueString) {
        if (mHandshake.complete()) {
            sendToClient(String.format("[UPDATE]:[KEY:PLAYER:%s]:[UUID:%s]:[VALUE:%s]", key.toString().toUpperCase(), uuid, valueString));
        }
    }

    public void playerJoinEvent(String uuid) {
        if (getFlag(Flag.ReportPlayerJoin) && mHandshake.complete())
            sendToClient(String.format("[EVENT]:[TYPE:PLAYERJOIN]:[UUID:%s]", uuid));
    }

    public void playerLeaveEvent(String uuid) {
        if (getFlag(Flag.ReportPlayerLeave) && mHandshake.complete())
            sendToClient(String.format("[EVENT]:[TYPE:PLAYERLEAVE]:[UUID:%s]", uuid));
    }

    public void chatEvent(String uuid, String message) {
        if (getFlag(Flag.ReportChat) && mHandshake.complete())
            sendToClient(String.format("[EVENT]:[TYPE:CHAT]:[UUID:%s]:[MESSAGE:%s]", uuid, message));
    }

    public void deathEvent(String uuid, String message) {
        if (getFlag(Flag.ReportPlayerDeath) && mHandshake.complete())
            sendToClient(String.format("[EVENT]:[TYPE:DEATH]:[UUID:%s]:[MESSAGE:%s]", uuid, message));
    }

    public void logEvent(String newData) {
        if (getFlag(Flag.SendServerLog) && mHandshake.complete())
            sendToClient(String.format("[LOG]:[DATA:%s]", newData));
    }

    /**
     * Sends a string to the client (and ensure it eventually gets there)
     * @param message the string
     */
    private void sendToClient(String message) {
        boolean success = false;

        try {
            //Attempt to send the message to the client
            success = write(message);
        } catch (IOException error) {
            mServer.getDataFetcher().logMessage(Level.SEVERE, "ERROR: IOException while sending data to client: " +
                    address());
        }

        //If we were not successful, register the socket for read/write and wait
        //To try and resend the data when the outgoing buffer is processed
        if (!success) {
            try {
                mChannel.register(mServer.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            } catch (ClosedChannelException error) {
                error.printStackTrace();
            }
        }
    }

    /**
     * Try to resend data that has built up in the outgoing buffer
     * @return true if the buffer is empty, false if it still has data after the
     * socket's buffer is full again
     * @throws IOException
     */
    private boolean processOutgoingBuffer() throws IOException {
        Iterator<ByteBuffer> bufferList = mOutgoingBuffer.iterator();

        //Try to resend data to the client
        while (bufferList.hasNext()) {
            ByteBuffer buffer = bufferList.next();

            if (!attemptWrite(buffer))
                return false;
            else
                bufferList.remove();
        }

        return true;
    }

    /**
     * Sends a string to the client on failure adds bytes to the outgoing
     * buffer
     * @param message the string to send
     * @return true if the operation was successful, false otherwise
     * @throws IOException
     */
    private boolean write(String message) throws IOException {
        byte[] bytes = message.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return write(buffer);
    }

    /**
     * Sends bytes to the client and on failure adds bytes to the outgoing
     * buffer
     * @param bytes the bytes to send
     * @return true if the operation was successful, false otherwise
     * @throws IOException
     */
    private boolean write(ByteBuffer bytes) throws IOException {
        if (!attemptWrite(bytes)) {
            mOutgoingBuffer.add(bytes);
            return false;
        }

        return true;
    }

    /**
     * Attempts a write to the client
     * @param bytes
     * @return true if successful, false otherwise
     * @throws IOException
     */
    private boolean attemptWrite(ByteBuffer bytes) throws IOException {
        mChannel.write(bytes);

        return !bytes.hasRemaining();
    }
}
