package me.ele.idgen;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;
import me.ele.idgen.client.IDGenService;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import com.alibaba.fastjson.JSON;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.api.ACLBackgroundPathAndBytesable;
import com.netflix.curator.framework.recipes.locks.InterProcessMutex;
import com.netflix.curator.framework.state.ConnectionState;
import com.netflix.curator.framework.state.ConnectionStateListener;
import com.netflix.curator.retry.RetryNTimes;

public class IDGenServiceCuratorImpl implements IDGenService {
	private static final Log logger = LogFactory.getLog(IDGenServiceCuratorImpl.class);
	private static String root = "/seq";
	private static String lockroot = "/locks";
	private int JISHU;
	private static CuratorFramework client;
	private static String ZKURL = System.getProperty("zk.url", "10.0.18.141:2181");
	private static String RETRYN = System.getProperty("retry.n", "0");
	private static String RETRYTIME = System.getProperty("retry.time", "0");

	public IDGenServiceCuratorImpl() {
		this.JISHU = 0;
	}

	public String getOneSimpleNextId(String id) {
		long nextValue = Long.valueOf(id).longValue() + 1L;
		if (id.endsWith("909"))
			nextValue += 9090L;
		else if (id.endsWith("9")) {
			nextValue += 90L;
		}
		return String.valueOf(nextValue);
	}

	public String getNextIdImpl(String namespace, String table, int quantity) throws Exception {
		String ret = "Error message:unknown exception";

		Stat stat1 = (Stat) client.checkExists().forPath(root + "/" + namespace);
		if (stat1 == null) {
			((ACLBackgroundPathAndBytesable) client.create().withMode(CreateMode.PERSISTENT)).forPath(root + "/" + namespace,
					new String("1").getBytes());
		}
		Stat stat = (Stat) client.checkExists().forPath(root + "/" + namespace + "/" + table);
		String seq = "1";
		if (stat == null) {
			((ACLBackgroundPathAndBytesable) client.create().withMode(CreateMode.PERSISTENT)).forPath(root + "/" + namespace + "/" + table,
					new String("1").getBytes());
			ret = seq;
			for (int i = 0; i < quantity - 1; ++i) {
				seq = getOneSimpleNextId(seq);
				ret = ret + "," + seq;
			}
		} else {
			byte[] b = (byte[]) client.getData().forPath(root + "/" + namespace + "/" + table);
			seq = new String(b);
			if (((seq.length() >= 2) && (!(seq.substring(seq.length() - 2, seq.length() - 1).equals("0"))))
					|| ((seq.length() >= 4) && (!(seq.substring(seq.length() - 4, seq.length() - 3).equals("0"))))) {
				ret = "Error Message:invalid seq:" + seq;
				throw new RuntimeException(ret);
			}
			ret = "";
			for (int i = 0; i < quantity; ++i) {
				seq = getOneSimpleNextId(seq);
				ret = ret + seq + ",";
			}
			ret = ret.substring(0, ret.length() - 1);
		}

		client.setData().forPath(root + "/" + namespace + "/" + table, seq.getBytes());
		logger.info("seq:" + seq + "|ret:" + ret);
		return ret;
	}

	public String getNextId(String namespace, String table, int quantity) {
		long tStart = System.currentTimeMillis();
		long tWaitLockStart = 0L;
		long tWaitLockEnd = 0L;
		long lockClients = 0L;
		String ret = "Error message: unknown error;" + namespace + "|" + table + "|" + quantity;
		if ((quantity <= 0) || (quantity > 500)) {
			ret = "Error message:invalid parameter quantity[1-500]:" + quantity;
			monitor(namespace, table, quantity, tStart, tWaitLockStart, tWaitLockEnd, System.currentTimeMillis(), lockClients, ret);
			throw new RuntimeException(ret);
		}
		this.JISHU += 1;
		String key = Thread.currentThread().getId() + "|" + namespace + "|" + table + "|" + quantity;
		logger.debug(this.JISHU + "|" + " come in:" + Thread.currentThread().getId());

		InterProcessMutex lock = new InterProcessMutex(client, lockroot + "/" + namespace + "|" + table);
		try {
			logger.info("----------" + key + "acquire lock----------");
			tWaitLockStart = System.currentTimeMillis();
			lockClients = getLockClients(lock);
			if (lock.acquire(30L, TimeUnit.SECONDS)) {
				tWaitLockEnd = System.currentTimeMillis();
				try {
					logger.info("---------->>>>" + key + "get lock----------");
					ret = getNextIdImpl(namespace, table, quantity);
					logger.info(key + "|return:" + ret);
				} finally {
					lock.release();
					logger.info("----------" + key + "release----------");
				}
			} else {
				throw new IllegalStateException(" could not acquire the lock");
			}
		} catch (Exception e) {
			ret = "Error message:[" + key + "]" + e.getMessage();
			logger.error(ret, e);
			monitor(namespace, table, quantity, tStart, tWaitLockStart, tWaitLockEnd, System.currentTimeMillis(), lockClients, ret);
			throw new RuntimeException(ret, e);
		}
		logger.debug(this.JISHU + "|" + key + " come over:" + Thread.currentThread().getId());
		monitor(namespace, table, quantity, tStart, tWaitLockStart, tWaitLockEnd, System.currentTimeMillis(), lockClients, ret);
		return ret;
	}

	private int getLockClients(InterProcessMutex lock) {
		try {
			return lock.getParticipantNodes().size();
		} catch (Exception e) {
			logger.error("getLockClients error:", e);
		}
		return 0;
	}

	private void monitor(String namespace, String object, int quantity, long tStart, long tWaitLockStart, long tWaitLockEnd, long tEnd,
			long lockClients, String result) {
		Map mmap = new HashMap();
		mmap.put("me.ele.idgen.KV_ITEM_KEY", "idgen-monitor");
		mmap.put("timestamp", Long.valueOf(tStart));
		mmap.put("spent time", Long.valueOf(tEnd - tStart));
		mmap.put("lock wait time", Long.valueOf(tWaitLockEnd - tWaitLockStart));
		mmap.put("wait lock clients", Long.valueOf(lockClients));
		mmap.put("result", result);
//		mmap.put("request address", RpcContext.getContext().getRemoteAddressString());
//		mmap.put("local address", RpcContext.getContext().getLocalAddressString());
		mmap.put("namespace", namespace);
		mmap.put("object", object);
		mmap.put("quantity", Integer.valueOf(quantity));
		logger.info(JSON.toJSONString(mmap));
	}

	static {
		logger.debug("zk.url:" + ZKURL);
		RetryPolicy retryPolicy = new RetryNTimes(Integer.valueOf(RETRYN).intValue(), Integer.valueOf(RETRYTIME).intValue());
		client = CuratorFrameworkFactory.newClient(ZKURL, retryPolicy);

		client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			public void stateChanged(CuratorFramework client, ConnectionState state) {
				if (state == ConnectionState.LOST) {
					IDGenServiceCuratorImpl.logger.warn("----------" + Thread.currentThread().getId() + "ConnectionState.LOST----------");
				} else if (state == ConnectionState.CONNECTED) {
					IDGenServiceCuratorImpl.logger.warn("----------" + Thread.currentThread().getId() + "ConnectionState.CONNECTED----------");
				} else if (state == ConnectionState.RECONNECTED) {
					IDGenServiceCuratorImpl.logger.warn("----------" + Thread.currentThread().getId() + "ConnectionState.RECONNECTED----------");
				} else
					IDGenServiceCuratorImpl.logger.warn("----------" + Thread.currentThread().getId() + "unknown----------" + state);
			}
		});
		client.start();
		Stat stat1 = null;
		try {
			stat1 = (Stat) client.checkExists().forPath(root);
			if (stat1 == null)
				((ACLBackgroundPathAndBytesable) client.create().withMode(CreateMode.PERSISTENT)).forPath(root, new byte[0]);
		} catch (Exception e) {
			logger.error("Check & Create Root error:", e);
		}
		logger.info("Idgen zk client start...");
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				IDGenServiceCuratorImpl.logger.info("Run idgen shutdown hook now...");
				if (IDGenServiceCuratorImpl.client != null)
					IDGenServiceCuratorImpl.client.close();
			}
		}, "IdgenShutdownHook"));
	}
}