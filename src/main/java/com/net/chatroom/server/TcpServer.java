package com.net.chatroom.server;

import com.net.chatroom.server.handler.ClientHandler;
import com.net.chatroom.utils.CloseUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: xch
 * @create: 2019-07-09 11:10
 **/
public class TcpServer implements ClientHandler.ClientHandlerCallback {

    private final int port;

    private ClientLisener lisener;

    private Selector selector;

    private ServerSocketChannel server;

    private List<ClientHandler> clientHandlerList = new ArrayList<>();

    private final ExecutorService forwardingThreadPoolExecutor;

    public TcpServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            this.server = server;
            System.out.println("服务器信息：" + server.getLocalAddress());
            //启动客户端监听
            this.lisener = new ClientLisener();
            this.lisener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (lisener != null) {
            lisener.exit();
        }
        CloseUtil.closeAll(server, selector);
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler handler, String msg) {
        // 异步提交转发任务
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TcpServer.this) {
                for (ClientHandler clientHandler : clientHandlerList) {
                    if (clientHandler.equals(handler)) {
                        // 跳过自己
                        continue;
                    }
                    // 对其他客户端发送消息
                    clientHandler.send(msg);
                }
            }
        });
    }

    private class ClientLisener extends Thread {
        private boolean done = false;

        void exit() {
            done = true;
            // 唤醒当前的阻塞
            selector.wakeup();
        }

        @Override
        public void run() {
            Selector selector = TcpServer.this.selector;
            System.out.println("服务器准备就绪~");
            while (!done) {
                try {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    for (SelectionKey key : selectionKeys) {
                        if (done) {
                            break;
                        }
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            // 非阻塞状态拿到客户端连接
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            // 客户端构建异步线程
                            ClientHandler clientHandler = new ClientHandler(socketChannel, TcpServer.this);
                            // 添加同步处理
                            synchronized (TcpServer.this) {
                                clientHandlerList.add(clientHandler);
                            }
                        }
                    }
                    selectionKeys.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("服务器已关闭");
        }
    }
}
