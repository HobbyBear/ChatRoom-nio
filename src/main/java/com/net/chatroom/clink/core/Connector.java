package com.net.chatroom.clink.core;

import com.net.chatroom.clink.impl.SocketChannelAdapter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @author: xch
 * @create: 2019-07-09 09:46
 **/
@AllArgsConstructor
@NoArgsConstructor
public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    private UUID key = UUID.randomUUID();

    private Receiver receiver;

    private Sender sender;

    private SocketChannel channel;

    public void setup(SocketChannel socketChannel) throws IOException {

        this.channel = socketChannel;
        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);
        this.receiver = adapter;
        this.sender = adapter;
        readNextMessage();
    }

    private void readNextMessage() {
        if (receiver != null) {
            try {
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                System.out.println("开始接收数据异常：" + e.getMessage());
            }
        }
    }



    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    @Override
    public void close() throws IOException {

    }
    private IoArgs.IoArgsEventListener echoReceiveListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            // 打印
            onReceiveNewMessage(args.bufferString());
            // 读取下一条数据
            readNextMessage();
        }
    };

    protected void onReceiveNewMessage(String str) {
        System.out.println(key.toString() + ":" + str);
    }
}
