package com.net.chatroom.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author: xch
 * @create: 2019-07-02 17:05
 **/
public class CloseUtil {

    public static void closeAll(Closeable... closeables) {
        Stream.of(closeables).forEach(t -> {
            Optional.ofNullable(t).ifPresent(c -> {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
