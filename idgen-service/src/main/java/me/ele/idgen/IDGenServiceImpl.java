package me.ele.idgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;
import me.ele.idgen.client.IDGenService;
import me.ele.idgen.lock.DistributedLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class IDGenServiceImpl implements IDGenService, Watcher {
	private static final Log logger = LogFactory.getLog(IDGenServiceImpl.class);
	private String root;
	private String seqName;
	private int sessionTimeout;
	private ZooKeeper zk;
	private static String ZKURL = System.getProperty("zk.url", "10.128.2.57:2181");

	private static HashMap<String, String> rules = new HashMap() {
	};
	private List<Exception> exception;

	public IDGenServiceImpl() {
		this.root = "/seq";

		this.sessionTimeout = 30000;

		this.exception = new ArrayList();
	}

	public ZooKeeper getZk() {
		if (this.zk == null) {
			try {
				this.zk = new ZooKeeper(ZKURL, this.sessionTimeout, this);
				Stat stat = this.zk.exists(this.root, false);
				if (stat == null) {
					this.zk.create(this.root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
			} catch (Exception e) {
				logger.info("getZk failure:", e);
			}
		}
		return this.zk;
	}

	public String getNextId(String namespace, String table, int quantity) {
		DistributedLock lock = null;
		String ret = "Error message: unknown error;" + namespace + "|" + table + "|" + quantity;
		try {
			lock = new DistributedLock(ZKURL, namespace + "|" + table);
			lock.lock();
			logger.info(Thread.currentThread().getId() + "|" + namespace + "|" + table + "|" + quantity + "|get lock");
			ret = getNextIdImpl(namespace, table, quantity);

			logger.info(Thread.currentThread().getId() + "|" + namespace + "|" + table + "|" + quantity + "|return:" + ret);
		} catch (Exception e) {
			ret = "Error message:[" + namespace + "|" + table + "|" + quantity + "]" + e.getMessage();

			throw new RuntimeException(ret, e);
		} finally {
			lock.unlock();
		}
		return ret;
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

	public String getNextIdImpl(String namespace, String table, int quantity) throws InterruptedException, KeeperException {
		String ret = "Error message:unknown exception";
		System.out.println("come in:" + Thread.currentThread().getId());
		Thread.sleep(10000L);
		System.out.println("come over:" + Thread.currentThread().getId());
		if ((quantity <= 0) || (quantity > 500)) {
			ret = "Error message:invalid parameter quantity[1-500]:" + quantity;

			throw new RuntimeException(ret);
		}
		Stat stat1 = getZk().exists(this.root + "/" + namespace, false);
		if (stat1 == null) {
			getZk().create(this.root + "/" + namespace, new String("1").getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}

		Stat stat = getZk().exists(this.root + "/" + namespace + "/" + table, false);
		String seq = "1";
		if (stat == null) {
			getZk().create(this.root + "/" + namespace + "/" + table, new String("1").getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

			ret = seq;
			for (int i = 0; i < quantity - 1; ++i) {
				seq = getOneSimpleNextId(seq);
				ret = ret + "," + seq;
			}
			getZk().setData(this.root + "/" + namespace + "/" + table, seq.getBytes(), -1);
		} else {
			byte[] b = getZk().getData(this.root + "/" + namespace + "/" + table, false, null);
			seq = new String(b);
			if (((seq.length() >= 2) && (!(seq.substring(seq.length() - 2, seq.length() - 1).equals("0"))))
					|| ((seq.length() >= 4) && (!(seq.substring(seq.length() - 4, seq.length() - 3).equals("0"))))) {
				ret = "Error Message:invalid seq:" + seq;
			} else {
				ret = "";
				for (int i = 0; i < quantity; ++i) {
					seq = getOneSimpleNextId(seq);
					ret = ret + seq + ",";
				}
				ret = ret.substring(0, ret.length() - 1);
				getZk().setData(this.root + "/" + namespace + "/" + table, seq.getBytes(), -1);
			}
		}

		logger.info("seq:" + seq + "|ret:" + ret);
		return ret;
	}

	public void process(WatchedEvent watchedEvent) {
	}
}