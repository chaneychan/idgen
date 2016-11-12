package me.ele.idgen.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.netflix.curator.framework.CuratorFramework;

import me.ele.idgen.model.Policy;
import me.ele.idgen.model.Rule;
import me.ele.idgen.model.Seq;

public class IDUtil {

	private static final Logger logger = LoggerFactory.getLogger(IDUtil.class);
	private static final Logger cfgLogger = LoggerFactory.getLogger("cfg");
	private static CuratorFramework client = CuratorClient.getClient();
	private static int USABLE_LIMIT = Integer.valueOf(System.getProperty("USABLE_LIMIT", "100000")).intValue();

	public static Policy getPolicyById(String id) {
		return ((Policy) IDConfig.policyMap.get(id));
	}

	public static Rule getRuleById(String id) {
		return ((Rule) IDConfig.ruleMap.get(id));
	}

	public static String getNextId(String ns, String object, Seq seq, String newrule) {
		Rule rule = getRuleById(seq.getRule());
		if ((seq.getSeq() > rule.getSeq_max()) || (seq.getSeq() < 0L)) {
			Seq.replicate(getFirstSeqOfARule(newrule), seq);
			rule = getRuleById(newrule);
		}
		String id = mergeByRule(rule, seq.getDb(), seq.getTb(), seq.getSeq());
		// fix
		if (!rule.isHasdbtb()) {
			seq.setSeq(seq.getSeq() + 1L);
			return id;
		}

		if (seq.getTb() < rule.getTb_max()) {
			seq.setTb(seq.getTb() + 1);
		} else if (seq.getDb() < rule.getDb_max()) {
			seq.setTb(rule.getTb_min());
			seq.setDb(seq.getDb() + 1);
		} else {
			seq.setTb(rule.getTb_min());
			seq.setDb(rule.getDb_min());
			seq.setSeq(seq.getSeq() + 1L);
		}
		return id;
	}

	public static Seq getCurrentSeq(String namespace, String table) throws Exception {
		Seq ret = null;

		Stat stat1 = (Stat) client.checkExists().forPath(IDConfig.seqroot + "/" + namespace);
		if (stat1 == null) {
			client.create().withMode(CreateMode.PERSISTENT).forPath(IDConfig.seqroot + "/" + namespace, new byte[0]);
		}
		Stat stat = (Stat) client.checkExists().forPath(IDConfig.seqroot + "/" + namespace + "/" + table);

		if (stat == null) {
			Rule rule = IDConfig.getInitialRule(namespace, table);
			Seq seq = new Seq();
			seq.setDb(rule.getDb_min());
			seq.setTb(rule.getTb_min());
			seq.setSeq(rule.getSeq_min());
			seq.setRule(rule.getRuleno());
			client.create().withMode(CreateMode.PERSISTENT).forPath(IDConfig.seqroot + "/" + namespace + "/" + table,
					JSON.toJSONString(seq).getBytes());
			ret = seq;
		} else {
			byte[] b = (byte[]) client.getData().forPath(IDConfig.seqroot + "/" + namespace + "/" + table);
			ret = (Seq) JSON.parseObject(new String(b), Seq.class);
		}
		logger.debug(ret.toString());
		return ret;
	}

	public static Seq calculateNewSeqAfterRequest(String ns, String object, int quantity, Seq currentSeq) {
		Seq seq = new Seq();
		Rule rule = getRuleById(currentSeq.getRule());
		if ((currentSeq.getSeq() > rule.getSeq_max()) || (currentSeq.getSeq() < 0L)) {
			seq = switchRule(ns, object, currentSeq, quantity);
		} else if (rule.isHasdbtb()) {
			int seqPlus = quantity / rule.getDb_pace() / rule.getTb_pace();
			int dbPlus = quantity / rule.getTb_pace() % rule.getDb_pace();
			int tbPlus = quantity % rule.getTb_pace();

			long seql = currentSeq.getSeq() + seqPlus;
			int dbi = currentSeq.getDb() + dbPlus;
			int tbi = currentSeq.getTb() + tbPlus;

			if (tbi > rule.getTb_max()) {
				tbi -= rule.getTb_pace();
				++dbi;
			}
			if (dbi > rule.getDb_max()) {
				dbi -= rule.getDb_pace();
				seql += 1L;
			}

			if ((seql - 1L > rule.getSeq_max()) || (seql - 1L < 0L)) {
				seq = switchRule(ns, object, currentSeq, (int) (quantity - (seql - 1L - rule.getSeq_max())));
				logger.warn("switch rule:" + ns + "|" + object + ",from:" + currentSeq.toString() + " to:"
						+ seq.toString());
				Map<String, String> mmap = new HashMap<String, String>();
				mmap.put("me.ele.idgen.KV_ITEM_KEY", "idgen-switchrule");
				mmap.put("namespace", ns);
				mmap.put("table", object);
				mmap.put("oldrule", currentSeq.getRule());
				mmap.put("newrule", seq.getRule());
				logger.info(JSON.toJSONString(mmap));
			} else {
				long usable = rule.getSeq_max() - seql + 1L
						+ (rule.getDb_max() - dbi) * (rule.getTb_max() - tbi) * rule.getSeq_pace();
				if (usable < USABLE_LIMIT) {
					Map<String, String> mmap = new HashMap<String, String>();
					mmap.put("namespace", ns);
					mmap.put("table", object);
					mmap.put("usable", String.valueOf(usable));
					logger.info(JSON.toJSONString(mmap));
					// metricClient.name(Constant.M_IDGEN_USABLE_LIMIT,ns,object).recordGaugeValue(usable);
					// MetricUtil.writeCountMetric(Constant.M_IDGEN_USABLE_LIMIT,
					// usable, mmap);
				}
				seq.setDb(dbi);
				seq.setTb(tbi);
				seq.setSeq(seql);
				seq.setRule(rule.getRuleno());
			}
		} else {
			long seql = currentSeq.getSeq() + quantity;
			if ((seql - 1L > rule.getSeq_max()) || (seql - 1L < 0L)) {
				seq = switchRule(ns, object, currentSeq, (int) (seql - 1L - rule.getSeq_max()));
				logger.warn("switch rule:" + ns + "|" + object + ",from:" + currentSeq.toString() + " to:"
						+ seq.toString());
				Map<String, Object> mmap = new HashMap<String, Object>();
				mmap.put("me.ele.idgen.KV_ITEM_KEY", "idgen-switchrule");
				mmap.put("namespace", ns);
				mmap.put("table", object);
				mmap.put("oldrule", currentSeq.getRule());
				mmap.put("newrule", seq.getRule());
				logger.info(JSON.toJSONString(mmap));
			} else {
				long usable = rule.getSeq_max() - seql + 1L;
				if (usable < USABLE_LIMIT) {
					Map<String, String> mmap = new HashMap<String, String>();
					mmap.put("me.ele.idgen.KV_ITEM_KEY", "idgen-usablelow");
					mmap.put("namespace", ns);
					mmap.put("table", object);
					mmap.put("usable", String.valueOf(usable));
					logger.info(JSON.toJSONString(mmap));
					// metricClient.name(Constant.M_IDGEN_USABLE_LIMIT, ns,
					// object).recordGaugeValue(usable);
					// MetricUtil.writeCountMetric(Constant.M_IDGEN_USABLE_LIMIT,
					// usable, mmap);
				}
				seq.setSeq(seql);
				seq.setRule(rule.getRuleno());
			}
		}
		return seq;
	}

	public static Seq switchRule(String ns, String object, Seq currentSeq, int newquantity) {
		Seq seq = new Seq();
		Rule rulenew = IDConfig.getNextRule(ns, object, currentSeq.getRule());
		if (rulenew == null) {
			String msg = "Please configure rule for switch:" + ns + "|" + object;
			monitorNotifyError(msg, null);
			throw new RuntimeException(msg);
		}
		Map<String, Object> mmap = new HashMap<String, Object>();
		mmap.put("me.ele.idgen.KV_ITEM_KEY", "idgen-switchrule");
		mmap.put("namespace", ns);
		mmap.put("table", object);
		mmap.put("oldrule", currentSeq.getRule());
		mmap.put("newrule", rulenew.getRuleno());
		logger.info(JSON.toJSONString(mmap));

		seq = calculateNewSeqAfterRequest(ns, object, newquantity, getFirstSeqOfARule(rulenew.getRuleno()));
		return seq;
	}

	public static Seq getFirstSeqOfARule(String ruleno) {
		Seq seq = new Seq();
		Rule rulenew = getRuleById(ruleno);
		if (rulenew.isHasdbtb()) {
			seq.setDb(rulenew.getDb_min());
			seq.setTb(rulenew.getTb_min());
			seq.setSeq(rulenew.getSeq_min());
			seq.setRule(rulenew.getRuleno());
		} else {
			seq.setSeq(rulenew.getSeq_min());
			seq.setRule(rulenew.getRuleno());
		}
		return seq;
	}

	public static String mergeByRule(Rule rule, int db, int tb, long seq) {
		if (!(rule.isHasdbtb()))
			return String.valueOf(seq);
		if (rule.isSeqfirst()) {
			return String.format("%d%02d%02d",
					new Object[] { Long.valueOf(seq), Integer.valueOf(db), Integer.valueOf(tb) });
		}
		return String.format("%02d%02d%d",
				new Object[] { Integer.valueOf(db), Integer.valueOf(tb), Long.valueOf(seq) });
	}

	public static void monitorNotifyError(String msg, Exception e) {
		Map<String, Object> mmap = new HashMap<String, Object>();
		mmap.put("me.ele.idgen.KV_ITEM_KEY", "idgen-monitor-notifyerror");
		mmap.put("msg", msg);
		mmap.put("error message", (e == null) ? "" : e.getMessage());
		logger.info(JSON.toJSONString(mmap));
	}

	public static void monitorCfg(String type, String content) {
		Map<String, Object> mmap = new HashMap<String, Object>();
		mmap.put("me.ele.idgen.KV_ITEM_KEY", "idgen-monitor-cfg");
		mmap.put("type", type);
		mmap.put("content", content);
		cfgLogger.info(JSON.toJSONString(mmap));
		// MetricUtil.writeCountMetric(Constant.M_IDGEN_MONITOR_CFG_C, 1);
	}

	public static void monitor(String namespace, String object, int quantity, long tStart, long tWaitLockStart,
			long tWaitLockEnd, long tEnd, long lockClients, String result) {
		Map<String, String> mmap = new HashMap<String, String>();
		mmap.put("timestamp", String.valueOf(tStart));
		mmap.put("spent_time", String.valueOf(tEnd - tStart));
		mmap.put("lock_wait_time", String.valueOf(tWaitLockEnd - tWaitLockStart));
		mmap.put("wait_lock_clients", String.valueOf(lockClients));
		mmap.put("result", result);
		// mmap.put("request address",
		// RpcContext.getContext().getRemoteAddressString());
		// mmap.put("local address",
		// RpcContext.getContext().getLocalAddressString());
		mmap.put("namespace", namespace);
		mmap.put("object", object);
		mmap.put("quantity", String.valueOf(quantity));
		logger.info(JSON.toJSONString(mmap));
		// metricClient.name(Constant.M_IDGEN_MONITOR_GETNEXTID_T).recordTimeInMillis(tEnd
		// - tStart);
		// MetricUtil.writeTimingMetric(Constant.M_IDGEN_MONITOR_GETNEXTID_T,
		// Double.valueOf(tEnd - tStart), mmap);

		// 暂时不需要统计以下两个Count
		// MetricUtil.writeCountMetric(Constant.M_IDGEN_MONITOR_GETNEXTID_C, 1);
		// MetricUtil.writeCountMetric(Constant.M_IDGEN_MONITOR_GETNEXTID_NUM_C,
		// quantity);
	}
}