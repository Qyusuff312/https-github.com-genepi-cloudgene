package cloudgene.mapred.util;

import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.ClusterStatus;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;

import cloudgene.mapred.Main;
import genepi.hadoop.HdfsUtil;

public class DevelopmentServer {

	private static String WORKING_DIRECTORY = "test-cluster";
	
	public static void main(String[] args) throws Exception {
		
		File testDataCluster1 = new File(WORKING_DIRECTORY);
		if (testDataCluster1.exists()) {
			testDataCluster1.delete();
		}
		HdfsConfiguration conf = new HdfsConfiguration();
		conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, testDataCluster1.getAbsolutePath());
		MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
		DistributedFileSystem fs = cluster.getFileSystem();

		// set mincluster as default config
		HdfsUtil.setDefaultConfiguration(conf);
		System.setProperty("hadoop.log.dir", "test-log-dir");
		MiniMRCluster mrCluster = new MiniMRCluster(1, fs.getUri().toString(), 1, null, null, new JobConf(conf));
		JobConf mrClusterConf = mrCluster.createJobConf();
		HdfsUtil.setDefaultConfiguration(new Configuration(mrClusterConf));

		System.out.println("------");

		JobClient client = new JobClient(mrClusterConf);
		ClusterStatus status = client.getClusterStatus(true);
		System.out.println(status.getActiveTrackerNames());
		Main.main(new String[] {});
		
	}
}
