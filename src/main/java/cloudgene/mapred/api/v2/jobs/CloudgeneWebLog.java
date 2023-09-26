package cloudgene.mapred.api.v2.jobs;

import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.util.BaseResource;
import cloudgene.mapred.util.CloudgeneContextUtil;
import cloudgene.sdk.weblog.WebCommandEvent;
import cloudgene.sdk.weblog.WebCommandEventUtil;

public class CloudgeneWebLog extends BaseResource {

	@Post
	public Representation post(Representation entity) {

		String job = getAttribute("job");

		try {

			CloudgeneContext context = CloudgeneContextUtil.get(job);
			WebCommandEvent event = WebCommandEventUtil.parse(entity.getText());
			event.execute(context);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ok("");

	}

}
