/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.netty.channel;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.unix.ServerDomainSocketChannel;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Factory for EpollEventLoopGroup.
 *
 * @author croudet
 */
@Singleton
@Requires(classes = Epoll.class, condition = EpollAvailabilityCondition.class)
@Internal
@Named(EventLoopGroupFactory.NATIVE)
@BootstrapContextCompatible
public class EpollEventLoopGroupFactory implements EventLoopGroupFactory {

    /**
     * Creates an EpollEventLoopGroup.
     *
     * @param threads       The number of threads to use.
     * @param threadFactory The thread factory.
     * @param ioRatio       The io ratio.
     * @return An EpollEventLoopGroup.
     */
    @Override
    public EventLoopGroup createEventLoopGroup(int threads, ThreadFactory threadFactory, @Nullable Integer ioRatio) {
        return new EpollEventLoopGroup(threads, threadFactory);
    }

    /**
     * Creates an EpollEventLoopGroup.
     *
     * @param threads  The number of threads to use.
     * @param executor An Executor.
     * @param ioRatio  The io ratio.
     * @return An EpollEventLoopGroup.
     */
    @Override
    public EventLoopGroup createEventLoopGroup(int threads, Executor executor, @Nullable Integer ioRatio) {
        return new EpollEventLoopGroup(threads, executor);
    }

    /**
     * Returns the server channel class.
     *
     * @return EpollServerSocketChannel.
     */
    @Override
    public Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return EpollServerSocketChannel.class;
    }

    @Override
    public Class<? extends ServerDomainSocketChannel> domainServerSocketChannelClass() throws UnsupportedOperationException {
        try {
            return EpollServerDomainSocketChannel.class;
        } catch (NoClassDefFoundError e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @NonNull
    @Override
    public EpollServerSocketChannel serverSocketChannelInstance(@Nullable EventLoopGroupConfiguration configuration) {
        return new EpollServerSocketChannel();
    }

    @Override
    public ServerChannel domainServerSocketChannelInstance(@Nullable EventLoopGroupConfiguration configuration) {
        try {
            return new EpollServerDomainSocketChannel();
        } catch (NoClassDefFoundError e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @NonNull
    @Override
    public Class<? extends SocketChannel> clientSocketChannelClass(@Nullable EventLoopGroupConfiguration configuration) {
        return EpollSocketChannel.class;
    }

    @Override
    public SocketChannel clientSocketChannelInstance(EventLoopGroupConfiguration configuration) {
        return new EpollSocketChannel();
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public Class<? extends Channel> channelClass(NettyChannelType type) throws UnsupportedOperationException {
        return switch (type) {
            case SERVER_SOCKET -> EpollServerSocketChannel.class;
            case CLIENT_SOCKET -> EpollSocketChannel.class;
            case DOMAIN_SERVER_SOCKET -> EpollServerDomainSocketChannel.class;
            case DATAGRAM_SOCKET -> EpollDatagramChannel.class;
        };
    }

    @Override
    public Class<? extends Channel> channelClass(NettyChannelType type, @Nullable EventLoopGroupConfiguration configuration) {
        return channelClass(type);
    }

    @Override
    public Channel channelInstance(NettyChannelType type, @Nullable EventLoopGroupConfiguration configuration) {
        return switch (type) {
            case SERVER_SOCKET -> new EpollServerSocketChannel();
            case CLIENT_SOCKET -> new EpollSocketChannel();
            case DOMAIN_SERVER_SOCKET -> new EpollServerDomainSocketChannel();
            case DATAGRAM_SOCKET -> new EpollDatagramChannel();
        };
    }
}
