package cloudgene.mapred.plugins.weblog;

import cloudgene.mapred.jobs.CloudgeneStepFactory;
import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.plugins.nextflow.NextflowStep;
import cloudgene.mapred.util.Settings;

public class WebLogPlugin implements IPlugin {

	public static final String ID = "weblog";

	private Settings settings;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return "WebLog";
	}

	@Override
	public boolean isInstalled() {
		WebLogRunner binary = WebLogRunner.build(settings);
		return binary.isInstalled();
	}

	@Override
	public String getDetails() {
		WebLogRunner binary = WebLogRunner.build(settings);
		return binary.getVersion();
	}

	@Override
	public void configure(Settings settings) {
		this.settings = settings;
		CloudgeneStepFactory factory = CloudgeneStepFactory.getInstance();
		factory.register("weblog", WebLogStep.class);
	}

	@Override
	public String getStatus() {
		if (isInstalled()) {
			return "WebLogBinary support enabled.";
		} else {
			return "WebLogBinary not found. Nextflow support disabled.";
		}
	}

}
