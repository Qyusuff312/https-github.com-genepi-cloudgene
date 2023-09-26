package cloudgene.mapred.util;

import java.util.HashMap;
import java.util.Map;

import cloudgene.mapred.jobs.CloudgeneContext;

public class CloudgeneContextUtil {

	public static Map<String, CloudgeneContext> index = new HashMap<String, CloudgeneContext>();

	public static CloudgeneContext get(String job) {
		return index.get(job);
	}

	public static void put(String job, CloudgeneContext context) {
		index.put(job, context);
	}

}
