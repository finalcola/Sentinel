package com.alibaba.csp.sentinel.cluster.server.connection;

import java.util.List;

import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.log.RecordLog;

/**
 * @author xuyue
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ScanIdleConnectionTask implements Runnable {

    private final ConnectionPool connectionPool;

    public ScanIdleConnectionTask(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public void run() {
        try {
            // 最大空闲时间
            int idleSeconds = ClusterServerConfigManager.getIdleSeconds();
            long idleTimeMillis = idleSeconds * 1000;
            if (idleTimeMillis < 0) {
                idleTimeMillis = ServerTransportConfig.DEFAULT_IDLE_SECONDS * 1000;
            }
            long now = System.currentTimeMillis();
            // 遍历连接池
            List<Connection> connections = connectionPool.listAllConnection();
            for (Connection conn : connections) {
                // 关闭空闲连接
                if ((now - conn.getLastReadTime()) > idleTimeMillis) {
                    RecordLog.info(
                        String.format("[ScanIdleConnectionTask] The connection <%s:%d> has been idle for <%d>s. "
                            + "It will be closed now.", conn.getRemoteIP(), conn.getRemotePort(), idleSeconds)
                    );
                    conn.close();
                }
            }
        } catch (Throwable t) {
            RecordLog.warn("[ScanIdleConnectionTask] Failed to clean-up idle tasks", t);
        }
    }
}
