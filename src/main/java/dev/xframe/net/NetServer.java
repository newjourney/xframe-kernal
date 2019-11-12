package dev.xframe.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.xframe.inject.Inject;
import dev.xframe.inject.Injection;
import dev.xframe.net.cmd.CommandContext;
import dev.xframe.net.codec.MessageCrypt;
import dev.xframe.net.codec.MessageCrypts;
import dev.xframe.net.server.ServerChannelInitializer;
import dev.xframe.net.server.ServerLifecycleListener;
import dev.xframe.net.server.ServerMessageHandler;
import dev.xframe.net.server.ServerMessageInterceptor;
import dev.xframe.utils.XThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 网络服务启动
 * @author luzj
 */
public class NetServer {
	
	private static Logger logger = LoggerFactory.getLogger(NetServer.class);
	
	public static int defaultThreads() {
        return Runtime.getRuntime().availableProcessors() * 2;
    }
	
	@Inject
	private CommandContext cmdCtx;
	@Inject
	private ServerMessageInterceptor interceptor;
	@Inject
	private ServerLifecycleListener listener;
	
	private Channel bossChannel;
	private NioEventLoopGroup bossGroup;
	private NioEventLoopGroup workerGroup;
	
	private MessageCrypt crypt = MessageCrypts.fromSysOps();
	private int threads = defaultThreads();

	private int port;
	
	public NetServer listening(int port) {
	    this.port = port;
	    return this;
	}
	
	public NetServer working(int threads)  {
	    this.threads = threads;
	    return this;
	}
	
	public NetServer crypting(MessageCrypt crypt) {
	    this.crypt = crypt;
	    return this;
	}
	
	public NetServer startup() {
	    Injection.inject(this);
	    
	    bossGroup = new NioEventLoopGroup(1, new XThreadFactory("netty.boss"));
	    workerGroup = new NioEventLoopGroup(threads, new XThreadFactory("netty.worker"));
        NetMessageHandler dispatcher = new ServerMessageHandler(listener, cmdCtx, interceptor);
        
        ServerBootstrap bootstrap =
	            new ServerBootstrap()
    	            .group(bossGroup, workerGroup)
    	            .channel(NioServerSocketChannel.class)
    	            .childHandler(new ServerChannelInitializer(dispatcher, crypt))
    	            .childOption(ChannelOption.SO_KEEPALIVE, true)//开启时系统会在连接空闲一定时间后像客户端发送请求确认连接是否有效
    	            .childOption(ChannelOption.TCP_NODELAY, true)//关闭Nagle算法
//    	            .childOption(ChannelOption.SO_LINGER, 0)//连接关闭时,偿试把未发送完成的数据继续发送(等待时间, 如果为0则直接设置连接为CLOSE状态 不进行TIME_WAIT...)
    	            .childOption(ChannelOption.SO_SNDBUF, 4096)//系统sockets发送数据buff的大小(k)
    	            .childOption(ChannelOption.SO_RCVBUF, 2048)//---接收(k)
    	            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)//使用bytebuf池, 默认不使用
    	            .childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())//使用bytebuf池, 默认不使用
    	            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(2048*1024, 4096*1024))//消息缓冲区
    	            .option(ChannelOption.SO_REUSEADDR, true)//端口重用,如果开启则在上一个进程未关闭情况下也能正常启动
    	            .option(ChannelOption.SO_BACKLOG, 64);//最大等待连接的connection数量
        
        workerGroup.setIoRatio(100);//优先处理网络任务(IOTask)再处理UserTask
        
	    try {
	        bossChannel = bootstrap.bind(port).sync().channel();
            logger.info("NetServer listening to port : " + port);
        } catch (InterruptedException e) {
            logger.error("NetServer start failed ...", e);
        }
	    return this;
	}
	
	public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
	    bossChannel.close().awaitUninterruptibly();
	}
	
}
