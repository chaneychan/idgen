package me.ele.idgen.client;

public abstract interface IDPool {
	
	/**
	 * 当业务方每次通过borrow方法拿到id的时候，idgen并不知道业务方是否最终使用了此id，所以会存在如下两种情况： <li>
	 * 正常情况下，业务方borrow一个id，并且用于自己的业务中，插入数据库。 <li>
	 * 异常情况下，业务方borrow一个id之后，自己的业务出错，这个时候id可能并没有用插入数据库
	 * ，为了不浪费id建议程序调用giveback()方法，将id还回来以备下次继续使用。
	 * 
	 * @throws Exception
	 */
	public abstract String borrow() throws Exception;

	/**
	 * 归还
	 * @param id
	 */
	public abstract void giveback(String id);

	/**
	 * 消费
	 * @param id
	 */
	public abstract void consume(String id);
}