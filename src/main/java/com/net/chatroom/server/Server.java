package com.net.chatroom.server;

import com.net.chatroom.clink.core.IoContext;
import com.net.chatroom.clink.impl.IoSelectorProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author: xch
 * @create: 2019-07-09 11:40
 **/
public class Server {
    public static void main(String[] args) throws IOException {
        //IoSelectorProvider new 完以后就能监听读和写了
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();
        TcpServer tcpServer = new TcpServer(8082);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
            return;
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            tcpServer.broadcast(str);
        } while (!"00bye00".equalsIgnoreCase(str));

        tcpServer.stop();
        IoContext.close();
    }
}
