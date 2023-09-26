package cloudgene.mapred.plugins.weblog;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import com.google.gson.JsonIOException;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.jobs.Message;
import cloudgene.mapred.util.CloudgeneContextUtil;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.wdl.WdlStep;
import cloudgene.sdk.internal.WorkflowContextDTO;
import cloudgene.sdk.internal.WorkflowContextUtil;
import genepi.io.FileUtil;

public class WebLogStep extends CloudgeneStep {

	public WebLogStep() {
	}

	@Override
	public void setup(CloudgeneContext context) {
		super.setup(context);
	}

	@Override
	public void kill() {
		super.kill();
	}

	@Override
	public void updateProgress() {

	}

	@Override
	public boolean run(WdlStep step, CloudgeneContext context) {
		context.setConfig(step);

		try {

			WebLogRunner weblogRunner = WebLogRunner.build(context.getSettings());

			String contextJsonFilename = FileUtil.path(context.getLocalTemp(), "context.json");
			writeWorkflowContext(contextJsonFilename, context);

			String hostname = context.getSettings().getServerUrl();

			String collectorUrl = hostname + "/api/v2/weblog/" + makeSecretJobId(context.getJobId());
			CloudgeneContextUtil.put(makeSecretJobId(context.getJobId()), context);

			System.out.println(context.getConfig());

			String params = cloudgene.sdk.weblog.WebLogRunner.class.getCanonicalName() + " "
					+ context.getConfig("classname") + " " + contextJsonFilename + " " + collectorUrl;
			String script = weblogRunner.getStarterScript(context.getConfig().get("jar"), params);

			String scriptFileName = FileUtil.path(context.getLocalTemp(), "webrunner.sh");
			context.log(script);
			FileUtil.writeStringBufferToFile(scriptFileName, new StringBuffer(script));

			List<String> command = new Vector<String>();
			command.add("/bin/bash");
			command.add(scriptFileName);

			try {
				boolean successful = executeCommand(command, context, null);
				if (successful) {
					return true;
				} else {
					return false;
				}

			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			// TODO: params, data, config,counters, ...
		} catch (JsonIOException | IOException e) {
			context.log("Execution failed.", e);
			context.endTask("Execution failed.", Message.ERROR);
			return false;
		}

	}

	public String makeSecretJobId(String job) {
		return HashUtil.getSha256(job);
	}

	private void writeWorkflowContext(String contextJsonFilename, CloudgeneContext context)
			throws JsonIOException, IOException {

		WorkflowContextDTO dto = new WorkflowContextDTO();
		dto.setJobId(context.getJobId());
		dto.setJobName(context.getJobName());
		dto.setHdfsTemp(context.getHdfsTemp());
		dto.setInputs(context.getInputs());
		dto.setLocalTemp(context.getLocalTemp());
		dto.setWorkingDirectory(context.getWorkingDirectory());
		dto.setConfig(context.getConfig());
		dto.setCounters(context.getCounters());
		dto.setData(context.getData());
		dto.setParams(context.getParams());

		WorkflowContextUtil.writeToJson(dto, contextJsonFilename);

	}

}
