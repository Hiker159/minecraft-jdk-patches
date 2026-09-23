import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.*;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

/** Run with each game's exact Netty jars and -Dio.netty.transport.noNative=true. */
public class NettyFallbackSmoke {
    public static void main(String[] args) throws Exception {
        if (KQueue.isAvailable() || Epoll.isAvailable()
                || !KQueue.unavailabilityCause().getMessage().contains("noNative"))
            throw new AssertionError("Native transports were not disabled");
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
        Channel server = null, client = null;
        CompletableFuture<Integer> received = new CompletableFuture<>();
        try {
            server = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel c) {
                        c.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                ctx.writeAndFlush(msg);
                            }
                        });
                    }
                }).bind("127.0.0.1", 0).sync().channel();
            client = new Bootstrap().group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel c) {
                        c.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf b) {
                                received.complete((int)b.readUnsignedByte());
                            }
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                                received.completeExceptionally(t);
                            }
                        });
                    }
                }).connect((InetSocketAddress)server.localAddress()).sync().channel();
            client.writeAndFlush(Unpooled.buffer(1).writeByte(42)).sync();
            if (received.get(10, TimeUnit.SECONDS) != 42) throw new AssertionError("Echo failed");
            System.out.println("PASS: native transport disabled; NIO server bind, client connect and bidirectional data");
        } finally {
            if (client != null) client.close().sync();
            if (server != null) server.close().sync();
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }
}
