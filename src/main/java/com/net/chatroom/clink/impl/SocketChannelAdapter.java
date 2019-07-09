package com.net.chatroom.clink.impl;

import com.net.chatroom.clink.core.IoArgs;
import com.net.chatroom.clink.core.IoProvider;
import com.net.chatroom.clink.core.Receiver;
import com.net.chatroom.clink.core.Sender;
import com.net.chatroom.utils.CloseUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: xch
 * @create: 2019-07-09 10:05
 **/
public class SocketChannelAdapter implements Sender, Receiver, Closeable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final SocketChannel channel;

    private final IoProvider ioProvider;

    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventListener receiveIOEventListener;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;
        channel.configureBlocking(false);
    }

    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed");
        }
        receiveIOEventListener = listener;
        return ioProvider.registerInput(channel,inputCallback );
    }

    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) {
        return false;
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            //接触注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
        }
        //关闭
        CloseUtil.closeAll(channel);
        listener.onChannelClosed(channel);
    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }

    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            IoArgs args = new IoArgs();
            IoArgs.IoArgsEventListener listener = SocketChannelAdapter.this.receiveIOEventListener;
            if (listener != null) {
                listener.onStarted(args);
            }
            try {
                if (args.read(channel) > 0 && listener != null) {
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot read any data");
                }
            } catch (IOException e) {
                CloseUtil.closeAll(SocketChannelAdapter.this);
            }
        }
    };
}
