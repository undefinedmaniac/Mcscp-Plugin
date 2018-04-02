package com.gmail.undifinedmaniac.mcscpplugin.network;

import com.gmail.undifinedmaniac.mcscpplugin.command.McscpCommand;
import com.gmail.undifinedmaniac.mcscpplugin.command.McscpCommandProcessor;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

/**
 * Represents a single client connected to the server
 * Handles read / write events, session flags, and command requests
 */
public class McscpClient implements Listener {

    public enum Flag {
        ReportPlayerJoin, ReportPlayerLeave, CmdResponse,
        ReportChatUpdate, ReportPlayerDeath
    }

    private McscpTcpServer mServer;
    private SocketChannel mChannel;
    private SelectionKey mKey;

    private Queue<ByteBuffer> mOutgoingBuffer;
    private McscpHandshake mHandshake;
    private McscpCommand mCommand;
    private HashMap<Flag, Boolean> mFlags;

    public McscpClient(McscpTcpServer server, SocketChannel channel, SelectionKey key) {
        mServer = server;
        mChannel = channel;
        mKey = key;

        mOutgoingBuffer = new LinkedList<>();
        mHandshake = new McscpHandshake();
        mCommand = null;
        mFlags = new HashMap<>();

        Bukkit.getServer().getPluginManager().registerEvents(this, mServer.getPlugin());
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
     * Start the handshake by sending the first message to the client
     */
    public void startSession() {
        sendToClient(mHandshake.connected());
    }

    /**
     * Disconnect from the client
     */
    public void close() {
        SocketAddress remoteAddress = address();
        mServer.getPlugin().printMsg("Client disconnected: " + remoteAddress);

        try {
            mChannel.close();
        } catch (IOException error) {
            mServer.getPlugin().printMsg("ERROR: IOException while closing socket from disconnected client: " +
                    remoteAddress);
        }

        mKey.cancel();
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
            mServer.getPlugin().printMsg("ERROR: IOException while reading data from client: " +
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
        if (!mHandshake.finished()) {
            String reply = mHandshake.messageReceived(message);

            if (reply == null)
                mServer.dropClient(this);
            else
                sendToClient(reply);
        } else {
            //Work on command processing - if there is no current command
            // a new one will be created. Otherwise, new data will be added
            // to the existing command
            if (mCommand == null) {
                mCommand = new McscpCommand(this, message);
            } else {
                mCommand.addData(message);
            }

            //Execute the command once it is ready
            if (mCommand.isReady()) {
                McscpCommandProcessor.getInstance().processCommand(mCommand);
                //After the command is finished, check if it has data for our client
                if (mCommand.hasReply()) {
                    boolean replyEnabled = true;

                    //Do not send data from a console command if the CmdResponse flag is false
                    if (mCommand.getType() == McscpCommand.CommandType.Console &&
                            !getFlag(Flag.CmdResponse))
                        replyEnabled = false;

                    if (replyEnabled)
                        sendToClient(mCommand.getReply());
                }
                mCommand = null;
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
            mServer.getPlugin().printMsg("ERROR: IOException while processing buffered writes for the client: " +
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

    /**
     * Event handler for player join events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (getFlag(Flag.ReportPlayerJoin) && mHandshake.finished()) {
            sendToClient("EVENT:PLAYERJOIN:" + event.getPlayer().getDisplayName());
        }
    }

    /**
     * Event handler for player leave events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (getFlag(Flag.ReportPlayerLeave) && mHandshake.finished()) {
            sendToClient("EVENT:PLAYERLEAVE:" + event.getPlayer().getDisplayName());
        }
    }

    /**
     * Event handler for chat update events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatEvent(AsyncPlayerChatEvent event) {
        if (getFlag(Flag.ReportChatUpdate) && mHandshake.finished()) {
            sendToClient("EVENT:CHATUPDATE:" + event.getPlayer().getDisplayName() +
                    " " + event.getMessage());
        }
    }

    /**
     * Event handler for player death events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (getFlag(Flag.ReportPlayerDeath) && mHandshake.finished()) {
            sendToClient("EVENT:PLAYERDEATH:" + event.getDeathMessage());
        }
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
            mServer.getPlugin().printMsg("ERROR: IOException while sending data to client: " +
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
