package cloudgene.mapred.plugins.weblog;

import java.io.File;

import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;

public class WebLogRunner {

	private String binary = "";

	public static WebLogRunner build(Settings settings) {
		String javaPath = System.getProperty("java.home");

		File path = new File(javaPath);

		if (!path.exists()) {
			return new WebLogRunner(null);
		}

		String binary = "";

		if (path.isDirectory()) {
			binary = FileUtil.path(javaPath, "bin", "java");
		} else {
			binary = javaPath;
		}

		File file = new File(binary);

		if (!file.exists()) {
			return new WebLogRunner(null);
		}

		if (!file.canExecute()) {
			return new WebLogRunner(null);
		}
		return new WebLogRunner(binary);
	}

	private WebLogRunner(String binary) {
		this.binary = binary;
	}

	public String getBinary() {
		return binary;
	}

	public boolean isInstalled() {
		if (binary != null) {
			String binary = getBinary();
			return (new File(binary)).exists();
		} else {
			return false;
		}
	}

	public String getVersion() {
		if (isInstalled()) {
			return cloudgene.sdk.weblog.WebLogRunner.VERSION;
		} else {
			return "java is not installed.";
		}
	}

	public String getStarterScript(String jar, String params) {
		StringBuffer script = new StringBuffer();
		script.append("HADOOP_CLASS_PATH=`` \n");
		script.append("if command -v hadoop > /dev/null 2>&1; then \n");
		script.append("  HADOOP_CLASS_PATH=`hadoop classpath` \n");
		script.append("fi \n");
		script.append("CLASS_PATH=\"" + jar + ":$HADOOP_CLASS_PATH\" \n");
		script.append(getBinary() + " -cp $CLASS_PATH " + params);
		return script.toString();
	}

}
