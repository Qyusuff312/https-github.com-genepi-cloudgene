package cloudgene.mapred.plugins.nextflow;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.jobs.Message;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.wdl.WdlStep;
import genepi.io.FileUtil;

public class NextflowStep extends CloudgeneStep {

	private CloudgeneContext context;

	private Map<String, Message> messages = new HashMap<String, Message>();

	@Override
	public boolean run(WdlStep step, CloudgeneContext context) {

		this.context = context;

		String script = step.get("script");

		if (script == null) {
			// no script set. try to find a main.nf. IS the default convention of nf-core.
			script = "main.nf";
		}

		String scriptPath = FileUtil.path(context.getWorkingDirectory(), script);
		if (!new File(scriptPath).exists()) {
			context.error(
					"Nextflow script '" + scriptPath + "' not found. Please use 'script' to define your nf file.");
		}

		NextflowBinary nextflow = NextflowBinary.build(context.getSettings());

		List<String> nextflowCommand = new Vector<String>();
		nextflowCommand.add("PATH=$PATH:/usr/local/bin");
		nextflowCommand.add(nextflow.getBinary());
		nextflowCommand.add("run");
		nextflowCommand.add(script);

		AbstractJob job = context.getJob();
		String appFolder = context.getSettings().getApplicationRepository().getConfigDirectory(job.getApplicationId());

		String profile = "";
		String nextflowProfile = FileUtil.path(appFolder, "nextflow.profile");
		if (new File(nextflowProfile).exists()) {
			profile = FileUtil.readFileAsString(nextflowProfile);
		}

		// set profile
		if (!profile.isEmpty()) {
			nextflowCommand.add("-profile");
			nextflowCommand.add(profile);
		}

		String nextflowConfig = FileUtil.path(appFolder, "nextflow.config");
		File nextflowConfigFile = new File(nextflowConfig);
		if (nextflowConfigFile.exists()) {
			// set custom configuration
			nextflowCommand.add("-c");
			nextflowCommand.add(nextflowConfigFile.getAbsolutePath());
		}

		String work = "";
		String nextflowWork = FileUtil.path(appFolder, "nextflow.work");
		if (new File(nextflowWork).exists()) {
			work = FileUtil.readFileAsString(nextflowWork);
		}

		// use workdir if set in settings
		if (!work.trim().isEmpty()) {
			nextflowCommand.add("-w");
			nextflowCommand.add(work);
		} else {
			String workDir = FileUtil.path(context.getLocalTemp(), "nextflow");
			FileUtil.createDirectory(workDir);
			nextflowCommand.add("-w");
			nextflowCommand.add(workDir);
		}

		// used to defined hard coded params
		for (String key : step.keySet()) {
			if (key.startsWith("params.")) {
				String param = key.replace("params.", "");
				String value = step.get(key);
				nextflowCommand.add("--" + param + " \"" + value + "\"");
			}
		}
		// add all inputs
		for (String param : context.getInputs()) {
			String value = context.getInput(param);
			//resolve app links: use all properties as input parameters
			if (value.startsWith("apps@")) {
				Map<String, Object> linkedApp = (Map<String, Object>) context.getData(param);
				for (String paramLinkedApp : linkedApp.keySet()) {
					String valueLinkedApp = linkedApp.get(paramLinkedApp).toString();
					nextflowCommand.add("--" + paramLinkedApp + " \"" + valueLinkedApp + "\"");
				}
			} else {
				nextflowCommand.add("--" + param + " \"" + value + "\"");
			}

		}

		// add all outputs
		for (String param : context.getOutputs()) {
			String value = context.getOutput(param);
			nextflowCommand.add("--" + param + " " + value);

		}

		nextflowCommand.add("-ansi-log");
		nextflowCommand.add("false");

		nextflowCommand.add("-with-weblog");
		nextflowCommand.add("http://localhost:8082/api/v2/collect/" + makeSecretJobId(context.getJobId()));

		StringBuilder output = new StringBuilder();

		List<String> command = new Vector<String>();
		command.add("/bin/bash");
		command.add("-c");
		command.add(join(nextflowCommand));

		try {
			// context.beginTask("Running Nextflow pipeline...");
			boolean successful = executeCommand(command, context, output);
			if (successful) {
				updateProgress();
				return true;
			} else {

				// set all running processes to failed
				List<NextflowProcess> processes = NextflowInfo.getInstance()
						.getProcesses(makeSecretJobId(context.getJobId()));
				for (NextflowProcess process : processes) {
					for (NextflowTask task : process.getTasks()) {
						if (task.getTrace().getString("status").equals("RUNNING")
								|| task.getTrace().getString("status").equals("SUBMITTED")) {
							task.getTrace().put("status", "KILLED");
						}
					}
				}
				updateProgress();

				// Write nextflow output into step
				context.beginTask("Running Nextflow pipeline...");
				String text = "";

				if (killed) {
					text = output + "\n\n\n" + makeRed("Pipeline execution canceled.");
				} else {
					text = output + "\n\n\n" + makeRed("Pipeline execution failed.");
				}
				context.endTask(text, Message.ERROR_ANSI);

				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	@Override
	public void updateProgress() {
		String job = makeSecretJobId(context.getJobId());

		List<NextflowProcess> processes = NextflowInfo.getInstance().getProcesses(job);

		for (NextflowProcess process : processes) {

			Message message = messages.get(process.getName());
			if (message == null) {
				message = context.createTask("<b>" + process.getName() + "</b>");
				messages.put(process.getName(), message);
			}

			String text = "<b>" + process.getName() + "</b>";
			boolean running = false;
			boolean ok = true;
			for (NextflowTask task : process.getTasks()) {
				if (task.getTrace().getString("status").equals("RUNNING")
						|| task.getTrace().getString("status").equals("SUBMITTED")) {
					running = true;
				}
				if (!task.getTrace().getString("status").equals("COMPLETED")) {
					ok = false;

				}
				text += "<br><small>";

				text += task.getTrace().getString("name");
				if (task.getTrace().getString("status").equals("RUNNING")) {
					text += "...";
				}
				if (task.getTrace().getString("status").equals("COMPLETED")) {
					text += "&nbsp;<i class=\"fas fa-check text-success\"></i>";
				}
				if (task.getTrace().getString("status").equals("KILLED")
						|| task.getTrace().getString("status").equals("FAILED")) {
					text += "&nbsp;<i class=\"fas fa-times text-danger\"></i>";
				}
				text += "</small>";
			}
			message.setMessage(text);

			if (running) {
				message.setType(Message.RUNNING);
			} else {
				if (ok) {
					message.setType(Message.OK);
				} else {
					message.setType(Message.ERROR);
				}
			}

		}
	}

	private String join(List<String> array) {
		String result = "";
		for (int i = 0; i < array.size(); i++) {
			if (i > 0) {
				result += " \\\n";
			}
			result += array.get(i);
		}
		return result;
	}

	@Override
	public String[] getRequirements() {
		return new String[] { NextflowPlugin.ID };
	}

	public String makeSecretJobId(String job) {
		return HashUtil.getSha256(job);
	}

	private String makeRed(String text) {
		return ((char) 27 + "[31m" + text + (char) 27 + "[0m");
	}

}
