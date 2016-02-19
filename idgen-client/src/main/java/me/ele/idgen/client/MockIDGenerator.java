package me.ele.idgen.client;

import java.util.ArrayList;
import java.util.List;

import me.ele.elog.LogFactory;
import me.ele.elog.Log;

public class MockIDGenerator implements IDGenService {
	private static final Log log = LogFactory.getLog(MockIDGenerator.class);
	private Long idGenerator;

	public MockIDGenerator() {
		this.idGenerator = Long.valueOf(0L);
	}

	public synchronized String getNextId(String s, String s2, int i) {
		List<Long> Ids = new ArrayList();
		for (int j = 0; j < i; ++j) {
			while ((this.idGenerator.longValue() / 10L % 10L != 0L) || (this.idGenerator.longValue() / 1000L % 10L != 0L)) {
				this.idGenerator = Long.valueOf(this.idGenerator.longValue() + 1L);
			}
			Ids.add(this.idGenerator);
			this.idGenerator = Long.valueOf(this.idGenerator.longValue() + 1L);
		}

		StringBuilder sb = new StringBuilder();
		for (Long id : Ids) {
			sb.append(id).append(',');
		}
		String out = sb.substring(0, sb.length() - 1);
//		Log.debug("generated {} ids, raw string: {}", Integer.valueOf(i), out);
		return out;
	}
}