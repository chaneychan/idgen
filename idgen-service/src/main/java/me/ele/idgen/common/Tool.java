package me.ele.idgen.common;

import java.util.ArrayList;
import java.util.List;

import me.ele.idgen.model.Policy;
import me.ele.idgen.model.Rule;
import me.ele.idgen.model.Seq;

import com.alibaba.fastjson.JSON;

public class Tool {
	public static String generatePolicy() {
		List pols = new ArrayList();
		Policy policy = new Policy();
		policy.setNs("n8");
		policy.setObject("t8");
		policy.setRulelist("1,2");
		pols.add(policy);

		return JSON.toJSONString(pols);
	}

	public static String generateRule() {
		List ruls = new ArrayList();
		Rule rule = new Rule();
		rule.setHasdbtb(false);
		rule.setRuleno("1");
		rule.setSeq_max(10000000L);
		rule.setSeq_min(1);
		ruls.add(rule);
		return JSON.toJSONString(ruls);
	}

	public static String generateSeq() {
		Seq seq = new Seq();
		seq.setRule("1");
		seq.setDb(0);
		seq.setTb(0);
		seq.setSeq(20L);
		return JSON.toJSONString(seq);
	}

	public static void main(String[] args) throws Exception {
		long l = -9223372036854775789L;

		long l2 = l + 1L;

		int i = 2147483647;
		int i2 = i + 1;
		long l3 = 94608000000L;

		System.out.println(l3);
	}
}