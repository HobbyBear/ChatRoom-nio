package com.net.chatroom.clink.core;

/**
 * @author: xch
 * @create: 2019-07-09 09:47
 **/
public interface Sender {

    boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener);
}
