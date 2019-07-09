package com.net.chatroom.clink.core;

import java.io.IOException;

/**
 * @author: xch
 * @create: 2019-07-09 09:53
 **/
public class IoContext {
    //单例
    private static IoContext INSTANCE;

    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }
    public static StartedBoot setup() {
        return new StartedBoot();
    }

    public static IoContext get() {
        return INSTANCE;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
    }

    public static class StartedBoot {
        private IoProvider ioProvider;

        private StartedBoot() {

        }

        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }
}
