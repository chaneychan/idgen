package me.ele.idgen.client;

public abstract interface IDGenService {
	
	/**
	 * 
	 * @param idConfigDomain 配置域
	 * @param idConfigKey 主键key
	 * @param allocCount  每次生成的数量
	 * @return
	 */
	public abstract String getNextId(String idConfigDomain, String idConfigKey, int allocCount);
}