package me.ele.idgen.model;

import com.alibaba.fastjson.JSON;

public class Seq {
	int db;
	int tb;
	long seq;
	String rule;

	public int getDb() {
		return this.db;
	}

	public void setDb(int db) {
		this.db = db;
	}

	public int getTb() {
		return this.tb;
	}

	public void setTb(int tb) {
		this.tb = tb;
	}

	public long getSeq() {
		return this.seq;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	public String getRule() {
		return this.rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public static Seq parse(String original) {
		return ((Seq) JSON.parseObject(original, Seq.class));
	}

	public String toString() {
		return "Seq:" + JSON.toJSONString(this);
	}

	public static void replicate(Seq fromseq, Seq toseq) {
		toseq.setDb(fromseq.getDb());
		toseq.setRule(fromseq.getRule());
		toseq.setSeq(fromseq.getSeq());
		toseq.setTb(fromseq.getTb());
	}

	public static void main(String[] args) {
		Seq seq = new Seq();
		seq.setDb(2);
		seq.setTb(2);
		seq.setSeq(8990L);
		seq.setRule("33");
		String str = JSON.toJSONString(seq);
		System.out.println(str);

		Seq seq2 = (Seq) JSON.parseObject(str, Seq.class);

		System.out.println(seq2.getRule());
	}
}