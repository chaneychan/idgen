package me.ele.idgen.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.alibaba.fastjson.JSON;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.api.CuratorEvent;
import com.netflix.curator.framework.api.CuratorListener;

import me.ele.idgen.model.Policy;
import me.ele.idgen.model.Rule;

public class IDConfig {
	private static final Logger logger = LoggerFactory.getLogger(IDConfig.class);
	private static CuratorFramework client = CuratorClient.getClient();
	public static final String root = "/idgen";
	public static final String policyroot = "/idgen/policy";
	public static final String ruleroot = "/idgen/rule";
	public static final String lockroot = "/idgen/locks";
	public static final String seqroot = "/idgen/seq";
	public static Map<String, Policy> policyMap = new HashMap<String, Policy>();
	public static Map<String, Rule> ruleMap = new HashMap<String, Rule>();
	private static CuratorListener listener = null;

	public static synchronized void initial() throws Exception {
		Resource resource = new ClassPathResource("/config.properties");
		Properties props = PropertiesLoaderUtils.loadProperties(resource);
		String auth = props.getProperty("idgen.zk.auth");
		Id id1 = new Id("digest", DigestAuthenticationProvider.generateDigest(auth));

		List<ACL> acls = new ArrayList<ACL>(1);
		ACL acl1 = new ACL(ZooDefs.Perms.ALL, id1);
		acls.add(acl1);

		Stat stat1 = null;

		stat1 = (Stat) client.checkExists().forPath(IDConfig.root);
		if (stat1 == null) {
			client.create().withMode(CreateMode.PERSISTENT).withACL(acls).forPath(IDConfig.root, new byte[0]);
		}
		stat1 = (Stat) client.checkExists().forPath(IDConfig.policyroot);
		if (stat1 == null) {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(acls).forPath(IDConfig.policyroot, new byte[0]);
		}
		stat1 = (Stat) client.checkExists().forPath(IDConfig.ruleroot);
		if (stat1 == null) {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(acls).forPath(IDConfig.ruleroot, new byte[0]);
		}
		stat1 = (Stat) client.checkExists().forPath(IDConfig.lockroot);
		if (stat1 == null) {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(acls).forPath(IDConfig.lockroot, new byte[0]);
		}
		stat1 = (Stat) client.checkExists().forPath(IDConfig.seqroot);
		if (stat1 == null) {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(acls).forPath(IDConfig.seqroot, new byte[0]);
		}

		if (listener != null) {
			client.getCuratorListenable().removeListener(listener);
		}
		listener = new CuratorListener() {
			public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
				if ((event != null) && (event.getPath() != null))
					if (event.getPath().equals(policyroot)) {
						IDConfig.logger.info("policyroot event received");
						IDConfig.initialPolicy(client);
					} else if (event.getPath().equals(ruleroot)) {
						IDConfig.logger.info("ruleroot event received");
						IDConfig.initialRule(client);
					} else {
						IDConfig.logger.info("event received" + event);
					}
				else
					IDConfig.logger.info("event received" + event);
			}
		};
		client.getCuratorListenable().addListener(listener);
		initialPolicy(client);
		initialRule(client);
	}

	public static void initialPolicy(CuratorFramework client) throws Exception {
		byte[] policydata = client.getData().watched().forPath(IDConfig.policyroot);
		if (policydata.length == 0) {
			return;
		}
		List<Policy> policy = JSON.parseArray(new String(policydata), Policy.class);
		IDUtil.monitorCfg("policy", new String(policydata));
		policyMap.clear();
		for (Policy pol : policy) {
			pol.setRulenos(pol.getRulelist().split(","));
			policyMap.put(pol.getNs() + "|" + pol.getObject(), pol);
		}
	}

	public static void initialRule(CuratorFramework client) throws Exception {
		byte[] ruledata = client.getData().watched().forPath(IDConfig.ruleroot);
		if (ruledata.length == 0) {
			return;
		}
		List<Rule> rule = JSON.parseArray(new String(ruledata), Rule.class);
		IDUtil.monitorCfg("rule", new String(ruledata));
		ruleMap.clear();
		for (Rule rul : rule) {
			rul.setDb_pace(rul.getDb_max() - rul.getDb_min() + 1);
			rul.setTb_pace(rul.getTb_max() - rul.getTb_min() + 1);
			rul.setSeq_pace(rul.getSeq_max() - rul.getSeq_min() + 1L);
			ruleMap.put(rul.getRuleno(), rul);
		}
	}

	public static Rule getInitialRule(String ns, String table) {
		try {
			return ((Rule) ruleMap.get(((Policy) policyMap.get(ns + "|" + table)).getRulenos()[0]));
		} catch (Exception e) {
			String msg = "Please check the rule&policy of " + ns + "|" + table + ", exception:" + e.getMessage();
			logger.error(msg, e);
			IDUtil.monitorNotifyError(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	public static Rule getNextRule(String ns, String table, String currentRuleNo) {
		try {
			return ((Rule) ruleMap.get(((Policy) policyMap.get(ns + "|" + table)).getNextRuleNo(currentRuleNo)));
		} catch (Exception e) {
			String msg = "Please check the next rule&policy of " + ns + "|" + table;
			IDUtil.monitorNotifyError(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	static {
		try {
			initial();
		} catch (Exception e) {
			logger.error("initial configuration error:", e);
			IDUtil.monitorNotifyError("initial configuration error", e);
		}
	}
}