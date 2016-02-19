package me.ele.idgen.lock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class DistributedLock implements Lock, Watcher {
	private static final Log logger = LogFactory.getLog(DistributedLock.class);
	private ZooKeeper zk;
	private String root = "/locks";
	private String lockName;
	private String waitNode;
	private String myZnode;
	private CountDownLatch latch;
	private int sessionTimeout = 300000;
	private List<Exception> exception = new ArrayList();

	public DistributedLock(String config, String lockName) {
		this.lockName = lockName;
		try {
			this.zk = new ZooKeeper(config, this.sessionTimeout, this);
			Stat stat = this.zk.exists(this.root, false);
			if (stat == null) {
				this.zk.create(this.root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		} catch (IOException e) {
			this.exception.add(e);
		} catch (KeeperException e) {
			this.exception.add(e);
		} catch (InterruptedException e) {
			this.exception.add(e);
		}
	}

	public void process(WatchedEvent event) {
		if (this.latch != null)
			this.latch.countDown();
	}

	public void lock() {
		if (this.exception.size() > 0)
			throw new LockException((Exception) this.exception.get(0));
		try {
			if (tryLock()) {
				logger.info("Thread " + Thread.currentThread().getId() + " " + this.myZnode + " get lock true");
				return;
			}

			waitForLock(this.waitNode, this.sessionTimeout);
		} catch (KeeperException e) {
			throw new LockException(e);
		} catch (InterruptedException e) {
			throw new LockException(e);
		}
	}

	public boolean tryLock() {
		try {
			String splitStr = "_lock_";
			if (this.lockName.contains(splitStr)) {
				throw new LockException("lockName can not contains \\u000B");
			}
			this.myZnode = this.zk.create(this.root + "/" + this.lockName + splitStr, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
			logger.debug(this.myZnode + " is created ");

			List<String> subNodes = this.zk.getChildren(this.root, false);

			List lockObjNodes = new ArrayList();
			for (String node : subNodes) {
				String _node = node.split(splitStr)[0];
				if (_node.equals(this.lockName)) {
					lockObjNodes.add(node);
				}
			}
			Collections.sort(lockObjNodes);
			logger.debug(this.myZnode + "==" + ((String) lockObjNodes.get(0)));
			if (this.myZnode.equals(this.root + "/" + ((String) lockObjNodes.get(0)))) {
				return true;
			}

			String subMyZnode = this.myZnode.substring(this.myZnode.lastIndexOf("/") + 1);
			this.waitNode = ((String) lockObjNodes.get(Collections.binarySearch(lockObjNodes, subMyZnode) - 1));
		} catch (KeeperException e) {
			throw new LockException(e);
		} catch (InterruptedException e) {
			throw new LockException(e);
		}
		return false;
	}

	public boolean tryLock(long time, TimeUnit unit) {
		try {
			if (tryLock()) {
				return true;
			}
			return waitForLock(this.waitNode, time);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean waitForLock(String lower, long waitTime) throws InterruptedException, KeeperException {
		Stat stat = this.zk.exists(this.root + "/" + lower, true);

		if (stat != null) {
			logger.debug("Thread " + Thread.currentThread().getId() + " waiting for " + this.root + "/" + lower);
			this.latch = new CountDownLatch(1);
			this.latch.await(waitTime, TimeUnit.MILLISECONDS);
			this.latch = null;

			String splitStr = "_lock_";
			List<String> subNodes = this.zk.getChildren(this.root, false);

			List lockObjNodes = new ArrayList();
			for (String node : subNodes) {
				String _node = node.split(splitStr)[0];
				if (_node.equals(this.lockName)) {
					lockObjNodes.add(node);
				}
			}
			Collections.sort(lockObjNodes);
			logger.debug(this.myZnode + "==" + ((String) lockObjNodes.get(0)));
			if (this.myZnode.equals(this.root + "/" + ((String) lockObjNodes.get(0)))) {
				logger.debug("Thread " + Thread.currentThread().getId() + " getting lock " + this.root + "/" + lower);

				return true;
			}

			String subMyZnode = this.myZnode.substring(this.myZnode.lastIndexOf("/") + 1);
			this.waitNode = ((String) lockObjNodes.get(Collections.binarySearch(lockObjNodes, subMyZnode) - 1));
			logger.debug("Thread " + Thread.currentThread().getId() + " waiting Again " + this.root + "/" + lower);
			waitForLock(this.waitNode, waitTime);
		}
		return true;
	}

	public void unlock() {
		try {
			logger.info("unlock " + this.myZnode);
			this.zk.delete(this.myZnode, -1);
			this.myZnode = null;
			this.zk.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		}
	}

	public void lockInterruptibly() throws InterruptedException {
		lock();
	}

	public Condition newCondition() {
		return null;
	}

	public class LockException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public LockException(String paramString) {
			super(paramString);
		}

		public LockException(Exception paramException) {
			super(paramException);
		}
	}
}