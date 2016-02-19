package me.ele.idgen.common;

import java.util.Properties;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.alibaba.fastjson.JSON;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.state.ConnectionState;
import com.netflix.curator.framework.state.ConnectionStateListener;
import com.netflix.curator.retry.RetryNTimes;

public class CuratorClient {

	private static final Log logger = LogFactory.getLog(CuratorClient.class);
	private static String ZKURL;
	private static String RETRYN;
	private static String RETRYTIME;
	private static String AUTH;
	private static CuratorFramework client;

	public static CuratorFramework getClient() {
		return client;
	}

	static {
		try {
			Resource resource = new ClassPathResource("/config.properties");
			Properties props = PropertiesLoaderUtils.loadProperties(resource);
			ZKURL = props.getProperty("idgen.zk.url");
			RETRYN = props.getProperty("idgen.zk.retry.n");
			RETRYTIME = props.getProperty("idgen.zk.retry.time");
			AUTH = props.getProperty("idgen.zk.auth");
			logger.debug("zk.url:" + ZKURL);
			RetryPolicy retryPolicy = new RetryNTimes(Integer.valueOf(RETRYN).intValue(), Integer.valueOf(RETRYTIME).intValue());
			client = CuratorFrameworkFactory.builder().connectString(ZKURL).retryPolicy(retryPolicy).authorization("digest", AUTH.getBytes()).build();
			client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
				public void stateChanged(CuratorFramework client, ConnectionState state) {
					if (state == ConnectionState.LOST) {
						CuratorClient.logger.warn("----------" + Thread.currentThread().getId() + "ConnectionState.LOST----------");
					} else if (state == ConnectionState.CONNECTED) {
						CuratorClient.logger.warn("----------" + Thread.currentThread().getId() + "ConnectionState.CONNECTED----------");
					} else if (state == ConnectionState.RECONNECTED) {
						CuratorClient.logger.warn("----------" + Thread.currentThread().getId() + "ConnectionState.RECONNECTED----------");
						try {
							IDConfig.initial();
						} catch (Exception e) {
							CuratorClient.logger.error("Error in initial when reconnected", e);
							IDUtil.monitorNotifyError("Error in initial when reconnected", e);
						}
					} else {
						CuratorClient.logger.warn("----------" + Thread.currentThread().getId() + "unknown----------" + state);
					}
				}
			});
			client.start();
			logger.info("Idgen zk client start...");
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					CuratorClient.logger.info("Run idgen shutdown hook now...");
					if (CuratorClient.client != null)
						CuratorClient.client.close();
				}
			}, "IdgenShutdownHook"));
		} catch (Exception e) {
			logger.error("initial client error:", e);
			IDUtil.monitorNotifyError("Initial client error", e);
		}
	}

	public static <T> T getNodeData(CuratorFramework client, String path, Class<T> c) throws Exception {
		T result = c.newInstance();
		byte[] b = (byte[]) client.getData().forPath(path);
		result = (T) JSON.parseObject(new String(b), c);
		return result;
	}

}
