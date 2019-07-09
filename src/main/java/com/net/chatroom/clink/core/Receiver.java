package com.net.chatroom.clink.core;

import java.io.IOException;

/**
 * @author: xch
 * @create: 2019-07-09 09:47
 **/
public interface Receiver {
    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;
}
