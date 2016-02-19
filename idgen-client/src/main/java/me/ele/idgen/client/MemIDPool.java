package me.ele.idgen.client;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.ele.elog.Log;
import me.ele.elog.LogFactory;

/**
 * @Description: id池
 * @author chaney.chan
 * @date 2015年9月11日
 */
public class MemIDPool implements IDPool {
	private static final Log Log = LogFactory.getLog(MemIDPool.class);
	/** 生成接口 **/
	private IDGenService globalIdGenerator;
	/** 并发队列 **/
	private ConcurrentLinkedQueue<String> freshIds;
	/** 并发集合 **/
	private ConcurrentHashSet<String> lentIds;
	/** 每次生成数量 **/
	private int allocCount = 20;
	/** 临界调用服务数量 **/
	private int poolLowerBound = 10;
	/** 临界使用数量 **/
	private int lentPoolUpperBound = 10000;
	/** 配置域 **/
	private String idConfigDomain;
	/** 配置主键 **/
	private String idConfigKey;
	private FreshIdsHandler handler = new FreshIdsHandlerImpl();

	/** 最大尝试次数 **/
	private static final int MAX_TRY_TIMES = 3;

	/**
	 * @return the freshIds
	 */
	public ConcurrentLinkedQueue<String> getFreshIds() {
		return freshIds;
	}

	private ExecutorService threadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1),
			new ThreadPoolExecutor.DiscardPolicy());

	public MemIDPool(String configDomain, String configKey, int allocCount, IDGenService generator) {
		this.globalIdGenerator = generator;
		this.freshIds = new ConcurrentLinkedQueue<String>();
		this.lentIds = new ConcurrentHashSet<String>();

		if ((configDomain == null) || (configKey == null)) {
			throw new RuntimeException("Neither configDomain nor configKey can be null!");
		}

		this.idConfigDomain = configDomain;
		this.idConfigKey = configKey;
		this.allocCount = allocCount;
	}

	public String borrow() throws Exception {
		if (this.freshIds.size() <= this.poolLowerBound) {
			IdGen s = new IdGen(globalIdGenerator, allocCount, idConfigDomain, idConfigKey, handler);
			threadPool.execute(s);
		}
		String id = null;
		synchronized (this) {
			id = (String) this.freshIds.poll();
			if (id != null) {
				this.lentIds.add(id);
			}
		}
		if (id == null) {
			synchronized (this) {
				String rawIds = getNextIdByMaxTryTimes(this.globalIdGenerator, this.idConfigDomain, this.idConfigKey, this.allocCount);
				if (rawIds == null || rawIds.length() == 0) {
					throw new Exception("idgen error ,cause by: server shutdown");
				}
				List<String> newIds = Arrays.asList(rawIds.split(","));
				this.freshIds.addAll(newIds);
				id = (String) this.freshIds.poll();
				this.lentIds.add(id);
			}
		}

		int size = this.lentIds.size();
		if (size >= this.lentPoolUpperBound) {
			Log.warn("ID pool have lent out over {} ids! Discard these ids!", Integer.valueOf(size));
			this.lentIds.clear();
		}
		return id;
	}

	public void giveback(String id) {
		if (id == null) {
			throw new IllegalArgumentException("id can not be null!");
		}

		if (this.lentIds.contains(id))
			synchronized (this) {
				if (this.lentIds.contains(id)) {
					this.lentIds.remove(id);
					this.freshIds.add(id);
				}
			}
	}

	private final class IdGen implements Runnable {
		/** 每次生成数量 **/
		private final int allocCount;
		private final IDGenService globalIdGenerator;
		/** 配置域 **/
		private final String idConfigDomain;
		/** 配置主键 **/
		private final String idConfigKey;

		private final FreshIdsHandler handler;

		private IdGen(IDGenService globalIdGenerator, int allocCount, String idConfigDomain, String idConfigKey, FreshIdsHandler handler) {
			this.globalIdGenerator = globalIdGenerator;
			this.allocCount = allocCount;
			this.idConfigDomain = idConfigDomain;
			this.idConfigKey = idConfigKey;
			this.handler = handler;
		}

		@Override
		public void run() {
			String rawIds = getNextIdByMaxTryTimes(globalIdGenerator, idConfigDomain, idConfigKey, allocCount);
			if (rawIds == null || rawIds.length() == 0) {
				return;
			}
			List<String> newIds = Arrays.asList(rawIds.split(","));
			handler.setFreshIds(newIds);
		}
	}

	/**
	 * 最大尝试调用idgen service
	 * 
	 * @param globalIdGenerator
	 * @param idConfigDomain
	 * @param idConfigKey
	 * @param allocCount
	 * @return
	 */
	private String getNextIdByMaxTryTimes(IDGenService globalIdGenerator, String idConfigDomain, String idConfigKey, int allocCount) {
		String rawIds = "";
		for (int i = 1; i <= MAX_TRY_TIMES; i++) {
			try {
				rawIds = globalIdGenerator.getNextId(idConfigDomain, idConfigKey, allocCount);
				Log.debug("fresh ID pool size({}) is running low, allocate new {} IDs: {}", new Object[] { Integer.valueOf(this.freshIds.size()),
						Integer.valueOf(this.allocCount), rawIds });
				break;
			} catch (Exception e) {
				Log.error("调用idgen服务失败,失败原因:{},业务入口:{},尝试次数:{}", e.getMessage(), idConfigDomain + "||" + idConfigKey, i);
			}
		}
		return rawIds;
	}

	private interface FreshIdsHandler {
		public void setFreshIds(List<String> ids);
	}

	private class FreshIdsHandlerImpl implements FreshIdsHandler {
		@Override
		public void setFreshIds(List<String> ids) {
			freshIds.addAll(ids);
		}
	}

	public void consume(String id) {
		if (id == null) {
			throw new IllegalArgumentException("id can not be null!");
		}

		if (this.lentIds.contains(id)) {
			synchronized (this) {
				if (this.lentIds.contains(id))
					this.lentIds.remove(id);
			}
		}
	}

	public int getLentPoolSize() {
		return this.lentIds.size();
	}

	public int getFreshPoolSize() {
		return this.freshIds.size();
	}

	public int getAllocCount() {
		return this.allocCount;
	}

	public void setAllocCount(int allocCount) {
		this.allocCount = allocCount;
	}

	public int getPoolLowerBound() {
		return this.poolLowerBound;
	}

	public void setPoolLowerBound(int poolLowerBound) {
		this.poolLowerBound = poolLowerBound;
	}

	public int getLentPoolUpperBound() {
		return this.lentPoolUpperBound;
	}

	public void setLentPoolUpperBound(int lentPoolUpperBound) {
		this.lentPoolUpperBound = lentPoolUpperBound;
	}

	public IDGenService getGlobalIdGenerator() {
		return this.globalIdGenerator;
	}

	public void setGlobalIdGenerator(IDGenService idGenerator) {
		this.globalIdGenerator = idGenerator;
	}
	
	
}
