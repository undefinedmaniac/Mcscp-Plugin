package com.gmail.undifinedmaniac.mcscpplugin.network;

import com.gmail.undifinedmaniac.mcscpplugin.command.McscpCommandProcessor;
import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpDataFetcher;
import com.gmail.undifinedmaniac.mcscpplugin.table.McscpPlayerTable;
import com.gmail.undifinedmaniac.mcscpplugin.table.McscpServerTable;

import java.util.*;

import java.io.IOException;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 * This class contains a TCP listen server which accepts
 * incoming connections from MCSCP clients
 */
public class McscpTcpServer {

    private InetSocketAddress mAddress;
    private Selector mSelector;
    private SelectionKey mServerKey;
    private HashMap<SelectionKey, McscpClient> mClients;
    private IMcscpDataFetcher mFetcher;
    private McscpCommandProcessor mCommandProcessor;

    private McscpServerTable mServerTable;
    private Map<String, McscpPlayerTable> mPlayerTables;
    private int mTickCount = 0;

    public McscpTcpServer(IMcscpDataFetcher fetcher, String address, int port) {
        mAddress = new InetSocketAddress(address, port);
        mClients = new HashMap<>();
        mFetcher = fetcher;
        mCommandProcessor = new McscpCommandProcessor(mFetcher);
        mServerTable = new McscpServerTable(mFetcher, this);
        mServerTable.updateKeys();
        mPlayerTables = new HashMap<>();
    }

    /**
     * Processes events (AKA accept connections, notify clients
     * about read and write events)
     */
    public void processEvents() {

        int numberOfKeys = 0;

        try {
            numberOfKeys = mSelector.selectNow();
        } catch (IOException error) {
            mFetcher.logMessage(Level.SEVERE, "ERROR: IOException while processing events");
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

        if (mTickCount >= 20) {
            mTickCount = 0;
            if (mClients.size() != 0) {
                mServerTable.updateKeys();
                for (McscpPlayerTable table : mPlayerTables.values())
                    table.updateKeys();
            }
        }

        mTickCount++;
    }

    /**
     * Get the selector from the instance
     * @return the selector
     */
    public Selector getSelector() {
        return mSelector;
    }

    public IMcscpDataFetcher getDataFetcher() {
        return mFetcher;
    }

    public McscpCommandProcessor getCommandProcessor() {
        return mCommandProcessor;
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

            mFetcher.logMessage(Level.INFO,"TCP server online at address: " + mAddress.getHostName() +
                              " and port: " + mAddress.getPort());
        } catch (IOException error) {
            mFetcher.logMessage(Level.SEVERE,"ERROR: IOException while starting TCP server on address: " +
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
            mFetcher.logMessage(Level.SEVERE,"ERROR: IOException while closing server");
        }

        mFetcher.logMessage(Level.INFO,"TCP server offline. Goodbye!");
    }

    public void requestAllTableData(McscpClient client) {
        Map<McscpServerTable.Key, String> data = mServerTable.getAllData();
        for (McscpServerTable.Key key : data.keySet())
            client.serverTableUpdate(key, data.get(key));
    }

    public void serverTableUpdate(McscpServerTable.Key key, String valueString) {
        for (McscpClient client : mClients.values())
            client.serverTableUpdate(key, valueString);
    }

    public void playerTableUpdate(String uuid, McscpPlayerTable.Key key, String valueString) {
        for (McscpClient client : mClients.values())
            client.playerTableUpdate(uuid, key, valueString);
    }

    /**
     * Disconenct a client from the server
     * @param client the client to disconnect
     */
    public void dropClient(McscpClient client) {
        client.close();
        mClients.remove(client.key());
    }

    public void playerJoinEvent(String uuid) {
        for (McscpClient client : mClients.values())
            client.playerJoinEvent(uuid);

        McscpPlayerTable table = new McscpPlayerTable(uuid, mFetcher, this);
        mPlayerTables.put(uuid, table);
        table.updateKeys();
    }

    public void playerLeaveEvent(String uuid) {

        mPlayerTables.remove(uuid);

        for (McscpClient client : mClients.values())
            client.playerLeaveEvent(uuid);
    }

    public void chatEvent(String uuid, String message) {
        for (McscpClient client : mClients.values())
            client.chatEvent(uuid, message);
    }

    public void deathEvent(String uuid, String message) {
        for (McscpClient client : mClients.values())
            client.deathEvent(uuid, message);
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
            mFetcher.logMessage(Level.INFO, "Client connected: " + remoteAddress);
            SelectionKey clientKey = channel.register(mSelector, SelectionKey.OP_READ);
            McscpClient client = new McscpClient(this, channel, clientKey);
            mClients.put(clientKey, client);
            client.startHandshake();
        } catch (IOException error) {
            mFetcher.logMessage(Level.SEVERE,"ERROR: IOException while accepting client connection");
        }
    }

    /**
     * Notifies a client that a read event has occurred
     * @param key the key of the event
     */
    private void readEvent(SelectionKey key) {
        McscpClient client = mClients.get(key);
        client.readEvent();
    }

    /**
     * Notifies a client that write event has occurred
     * @param key key the key of the event
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
            mFetcher.logMessage(Level.SEVERE,"ERROR: IOException while closing server");
        }

        mServerKey.cancel();
    }
}
