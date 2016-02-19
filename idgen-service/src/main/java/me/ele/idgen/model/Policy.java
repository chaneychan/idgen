package me.ele.idgen.model;

import com.alibaba.fastjson.JSON;

public class Policy {
	private String ns;
	private String object;
	private String rulelist;
	private String[] rulenos;

	public String[] getRulenos() {
		return this.rulenos;
	}

	public void setRulenos(String[] rulenos) {
		this.rulenos = rulenos;
	}

	public String getNs() {
		return this.ns;
	}

	public void setNs(String ns) {
		this.ns = ns;
	}

	public String getObject() {
		return this.object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getRulelist() {
		return this.rulelist;
	}

	public void setRulelist(String rulelist) {
		this.rulelist = rulelist;
	}

	public static Policy parse(String original) {
		Policy policy = (Policy) JSON.parseObject(original, Policy.class);
		policy.setRulenos(policy.getRulelist().split(","));
		return policy;
	}

	public String getNextRuleNo(String currentRuleNo) {
		for (int i = 0; i < this.rulenos.length; ++i) {
			if (currentRuleNo.equals(this.rulenos[i])) {
				if (i == this.rulenos.length - 1) {
					return null;
				}
				return this.rulenos[(i + 1)];
			}

		}

		return this.rulenos[0];
	}

	public String toString() {
		return "Policy:" + JSON.toJSONString(this);
	}
}