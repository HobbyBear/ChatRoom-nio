package com.net.chatroom.clink.impl;

import com.net.chatroom.clink.core.IoProvider;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: xch
 * @create: 2019-07-09 10:04
 **/
public class IoSelectorProvider implements IoProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    //是否处于某个过程中
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlePool;

    private final ExecutorService outputHandlePool;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();
        inputHandlePool = Executors.newFixedThreadPool(4);
        outputHandlePool = Executors.newFixedThreadPool(4);
        //开始监听输入输出
        startRead();
    }

    private void startRead() {
        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(inRegInput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    private void handleSelection(SelectionKey key, int ops, HashMap<SelectionKey, Runnable> map, ExecutorService pool) {
        key.cancel();
        Runnable runnable = null;
        runnable = map.get(key);

        if (runnable != null && !pool.isShutdown()) {
            pool.execute(runnable);
        }
    }

    private void waitSelection(AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callback) != null;
    }

    private SelectionKey registerSelection(SocketChannel channel, Selector selector, int registerOps, AtomicBoolean locker, HashMap<SelectionKey, Runnable> map, HandleInputCallback callback) {

        synchronized (locker) {
            locker.set(true);
            try {
                selector.wakeup();
                SelectionKey key = channel.register(selector, registerOps);
                map.put(key, callback);
                return key;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                locker.set(false);
                locker.notify();
            }
        }
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return false;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel,readSelector,inputCallbackMap);
    }



    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel,writeSelector,outputCallbackMap);
    }

    private void unRegisterSelection(SocketChannel channel, Selector selector, HashMap<SelectionKey, Runnable> map) {
        if (channel.isRegistered()){
            SelectionKey key = channel.keyFor(selector);
            if (key != null){
                key.cancel();
                map.remove(key);
                selector.wakeup();
            }
        }
    }

    @Override
    public void close() throws IOException {

    }
}
