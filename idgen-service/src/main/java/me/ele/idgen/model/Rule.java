package me.ele.idgen.model;

import com.alibaba.fastjson.JSON;

public class Rule {
	String original;
	int db_max;
	int db_min;
	int tb_max;
	int tb_min;
	long seq_max;
	long seq_min;
	String ruleno;
	boolean seqfirst;
	boolean hasdbtb;
	int db_pace;
	int tb_pace;
	long seq_pace;

	public Rule() {
		this.seqfirst = false;
		this.hasdbtb = true;
	}

	public long getSeq_pace() {
		return this.seq_pace;
	}

	public void setSeq_pace(long seq_pace) {
		this.seq_pace = seq_pace;
	}

	public boolean isHasdbtb() {
		return this.hasdbtb;
	}

	public void setHasdbtb(boolean hasdbtb) {
		this.hasdbtb = hasdbtb;
	}

	public long getSeq_max() {
		return this.seq_max;
	}

	public void setSeq_max(long seq_max) {
		this.seq_max = seq_max;
	}

	public long getSeq_min() {
		return this.seq_min;
	}

	public void setSeq_min(int seq_min) {
		this.seq_min = seq_min;
	}

	public int getDb_pace() {
		return this.db_pace;
	}

	public void setDb_pace(int db_pace) {
		this.db_pace = db_pace;
	}

	public int getTb_pace() {
		return this.tb_pace;
	}

	public void setTb_pace(int tb_pace) {
		this.tb_pace = tb_pace;
	}

	public boolean isSeqfirst() {
		return this.seqfirst;
	}

	public void setSeqfirst(boolean seqfirst) {
		this.seqfirst = seqfirst;
	}

	public static Rule parse(String original) {
		Rule rule = (Rule) JSON.parseObject(original, Rule.class);
		rule.setDb_pace(rule.getDb_max() - rule.getDb_min() + 1);
		rule.setTb_pace(rule.getTb_max() - rule.getTb_min() + 1);
		rule.setSeq_pace(rule.getSeq_max() - rule.getSeq_min() + 1L);
		return rule;
	}

	public String getOriginal() {
		return this.original;
	}

	public void setOriginal(String original) {
		this.original = original;
	}

	public int getDb_max() {
		return this.db_max;
	}

	public void setDb_max(int db_max) {
		this.db_max = db_max;
	}

	public int getDb_min() {
		return this.db_min;
	}

	public void setDb_min(int db_min) {
		this.db_min = db_min;
	}

	public int getTb_max() {
		return this.tb_max;
	}

	public void setTb_max(int tb_max) {
		this.tb_max = tb_max;
	}

	public int getTb_min() {
		return this.tb_min;
	}

	public void setTb_min(int tb_min) {
		this.tb_min = tb_min;
	}

	public String getRuleno() {
		return this.ruleno;
	}

	public void setRuleno(String ruleno) {
		this.ruleno = ruleno;
	}

	public String toString() {
		return "Rule:" + JSON.toJSONString(this);
	}
}