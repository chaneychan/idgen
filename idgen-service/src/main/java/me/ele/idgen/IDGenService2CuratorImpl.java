package me.ele.idgen;

import java.util.concurrent.TimeUnit;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;
import me.ele.idgen.client.IDGenService;
import me.ele.idgen.common.Constant;
import me.ele.idgen.common.CuratorClient;
import me.ele.idgen.common.IDConfig;
import me.ele.idgen.common.IDUtil;
import me.ele.idgen.model.Seq;

import com.alibaba.fastjson.JSON;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.recipes.locks.InterProcessMutex;

public class IDGenService2CuratorImpl implements IDGenService {
	private static final Log logger = LogFactory.getLog(IDGenService2CuratorImpl.class);
	private static CuratorFramework client = CuratorClient.getClient();

	public String getOnePolicyNextId(String ns, String object, Seq seq, String newrule) {
		return IDUtil.getNextId(ns, object, seq, newrule);
	}

	public String getNextIdImpl(String namespace, String table, int quantity, Seq seq, String newrule) throws Exception {
		String ret = "";
		String seqString = "";
		for (int i = 0; i < quantity; ++i) {
			seqString = getOnePolicyNextId(namespace, table, seq, newrule);
			ret = ret + seqString + ",";
		}
		ret = ret.substring(0, ret.length() - 1);
		logger.info("seq:" + seq + "|ret:" + ret);
		return ret;
	}

	public String getNextId(String namespace, String table, int quantity) {
		long tStart = System.currentTimeMillis();
		long tWaitLockStart = 0L;
		long tWaitLockEnd = 0L;
		long lockClients = 0L;
		String ret = "Error message: unknown error;" + namespace + "|" + table + "|" + quantity;
		if ((quantity <= 0) || (quantity > Constant.IDGEN_MAX_COUNT)) {
			ret = "Error message:invalid parameter quantity[1-"+Constant.IDGEN_MAX_COUNT+"]:" + quantity;
			IDUtil.monitor(namespace, table, quantity, tStart, tWaitLockStart, tWaitLockEnd, System.currentTimeMillis(), lockClients, ret);
			throw new RuntimeException(ret);
		}

		String key = Thread.currentThread().getId() + "|" + namespace + "|" + table + "|" + quantity;
		logger.debug(Thread.currentThread().getId() + key + " come in:");

		InterProcessMutex lock = new InterProcessMutex(client, IDConfig.lockroot + "/" + namespace + "|" + table);
		try {
			logger.info("----------" + key + " acquire lock----------");
			tWaitLockStart = System.currentTimeMillis();
			lockClients = getLockClients(lock);

			if (lock.acquire(30L, TimeUnit.SECONDS)) {
				Seq seq;
				Seq seqNew;
				try {
					tWaitLockEnd = System.currentTimeMillis();
					logger.info("---------->>>>" + key + " get lock----------");
					seq = IDUtil.getCurrentSeq(namespace, table);
					seqNew = IDUtil.calculateNewSeqAfterRequest(namespace, table, quantity, seq);
					String seqNewStr = JSON.toJSONString(seqNew);
					client.setData().forPath(IDConfig.seqroot + "/" + namespace + "/" + table, seqNewStr.getBytes());
					logger.info("---------->>>>" + key + " set new seq----------" + seqNewStr);
				} finally {
					lock.release();
					logger.info("----------" + key + " release----------");
				}
				ret = getNextIdImpl(namespace, table, quantity, seq, seqNew.getRule());
				logger.info(key + "|return:" + ret);
			} else {
				throw new IllegalStateException(" could not acquire the lock");
			}
		} catch (Exception e) {
			ret = "Error message:[" + key + "]" + e.getMessage();
			logger.error(ret, e);
			IDUtil.monitor(namespace, table, quantity, tStart, tWaitLockStart, tWaitLockEnd, System.currentTimeMillis(), lockClients, ret);
			throw new RuntimeException(ret, e);
		}
		logger.debug(Thread.currentThread().getId() + key + " come over:");
		IDUtil.monitor(namespace, table, quantity, tStart, tWaitLockStart, tWaitLockEnd, System.currentTimeMillis(), lockClients, ret);
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
}