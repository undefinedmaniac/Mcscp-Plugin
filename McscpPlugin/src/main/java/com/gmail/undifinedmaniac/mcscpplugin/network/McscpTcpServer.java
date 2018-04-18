package com.gmail.undifinedmaniac.mcscpplugin.network;

import com.gmail.undifinedmaniac.mcscpplugin.McscpPlugin;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * This class contains a TCP listen server which accepts
 * incoming connections from MCSCP clients
 */
public class McscpTcpServer extends BukkitRunnable {

    private InetSocketAddress mAddress;
    private Selector mSelector;
    private SelectionKey mServerKey;
    private HashMap<SelectionKey, McscpClient> mClients;
    private McscpPlugin mPlugin;

    public McscpTcpServer(McscpPlugin plugin, String address, int port) {
        mAddress = new InetSocketAddress(address, port);
        mClients = new HashMap<>();
        mPlugin = plugin;
    }

    /**
     * Processes events (AKA accept connections, notify clients
     * about read and write events)
     */
    @Override
    public void run() {

        int numberOfKeys = 0;

        try {
            numberOfKeys = mSelector.selectNow();
        } catch (IOException error) {
            mPlugin.printMsg("ERROR: IOException while processing events");
        }

        if (numberOfKeys > 0) {
            Iterator keys = mSelector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();

                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable())
                    accept(key);
                else if (key.isReadable())
                    readEvent(key);
                else if (key.isWritable())
                    writeEvent(key);
            }
        }
    }

    /**
     * Get the selector from the instance
     * @return
     */
    public Selector getSelector() {
        return mSelector;
    }

    /**
     * Get the McscpPlugin
     * @return the plugin
     */
    public McscpPlugin getPlugin() {
        return mPlugin;
    }

    /**
     * Start the listen server and wait for incoming connections
     */
    public void start() {
        try {
            //Create a selector
            mSelector = Selector.open();

            //Create a server channel and begin listening for connections
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(mAddress);
            mServerKey = serverChannel.register(mSelector, SelectionKey.OP_ACCEPT);

            mPlugin.printMsg("TCP server online at address: " + mAddress.getHostName() +
                              " and port: " + mAddress.getPort());
        } catch (IOException error) {
            mPlugin.printMsg("ERROR: IOException while starting TCP server on address: " +
                    mAddress.getHostName() + " and port: " + mAddress.getPort());
        }
    }

    /**
     * Stop the listen server and disconnect all existing clients
     */
    public void stop() {
        closeServer();

        //Make a copy of the clients so that we can remove them as we loop
        List<McscpClient> clients = new ArrayList<>(mClients.values());

        for (McscpClient client : clients)
            dropClient(client);

        try {
            mSelector.close();
        } catch (IOException error) {
            mPlugin.printMsg("ERROR: IOException while closing server");
        }

        mPlugin.printMsg("TCP server offline. Goodbye!");

        this.cancel();
    }

    /**
     * Disconenct a client from the server
     * @param client the client to disconnect
     */
    public void dropClient(McscpClient client) {
        client.close();
        mClients.remove(client.key());
    }

    public void logEvent(String newData) {
        for (McscpClient client : mClients.values())
            client.logEvent(newData);
    }

    /**
     * Accept an incoming connection
     * @param key the key which received the event
     */
    private void accept(SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();

        try {
            SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);
            Socket socket = channel.socket();
            SocketAddress remoteAddress = socket.getRemoteSocketAddress();
            mPlugin.printMsg("Client connected: " + remoteAddress);
            SelectionKey clientKey = channel.register(mSelector, SelectionKey.OP_READ);
            McscpClient client = new McscpClient(this, channel, clientKey);
            mClients.put(clientKey, client);
            client.startHandshake();
        } catch (IOException error) {
            mPlugin.printMsg("ERROR: IOException while accepting client connection");
        }
    }

    /**
     * Notifies a client that a read event has occurred
     * @param key
     */
    private void readEvent(SelectionKey key) {
        McscpClient client = mClients.get(key);
        client.readEvent();
    }

    /**
     * Notifies a client that write event has occurred
     * @param key
     */
    private void writeEvent(SelectionKey key) {
        McscpClient client = mClients.get(key);
        client.writeEvent();
    }

    /**
     * Closes the server
     */
    private void closeServer() {
        ServerSocketChannel serverChannel = (ServerSocketChannel) mServerKey.channel();

        try {
            serverChannel.close();
        } catch (IOException error) {
            mPlugin.printMsg("ERROR: IOException while closing server");
        }

        mServerKey.cancel();
    }
}
