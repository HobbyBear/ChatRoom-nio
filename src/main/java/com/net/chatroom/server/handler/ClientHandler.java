package com.net.chatroom.server.handler;

import com.net.chatroom.clink.core.Connector;
import com.net.chatroom.utils.CloseUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: xch
 * @create: 2019-07-09 11:26
 **/
public class ClientHandler {
    private final Connector connector;
    private final SocketChannel socketChannel;
    private final String clientInfo;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;

    public ClientHandler(SocketChannel socketChannel,ClientHandlerCallback clientHandlerCallback) throws IOException {
        socketChannel.configureBlocking(false);
        this.socketChannel = socketChannel;
        this.clientHandlerCallback = clientHandlerCallback;
        connector = new Connector(){
            @Override
            public void onChannelClosed(SocketChannel channel) {
                super.onChannelClosed(channel);

            }

            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);
                clientHandlerCallback.onNewMessageArrived(ClientHandler.this,str);
            }
        };
        connector.setup(socketChannel);
        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void exit(){
        CloseUtil.closeAll(connector,socketChannel);
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }
    private class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtil.closeAll(selector);
            executorService.shutdownNow();
        }

        void send(String str) {
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        class WriteRunnable implements Runnable {
            private final String msg;

            WriteRunnable(String msg) {
                this.msg = msg + '\n';
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());
                // 反转操作, 重点
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        int len = socketChannel.write(byteBuffer);
                        // len = 0 合法
                        if (len < 0) {
                            System.out.println("客户端已无法发送数据！");
                            ClientHandler.this.exit();
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exit();
                    }
                }
            }
        }
    }

}
