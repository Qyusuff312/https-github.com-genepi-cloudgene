package cloudgene.mapred.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.reactivestreams.Publisher;

import genepi.io.FileUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
public class FormUtil {

	@Inject
	protected cloudgene.mapred.server.Application application;

	public synchronized Publisher<HttpResponse<Object>> processMultipartBody(MultipartBody body,
			Function<List<Parameter>, HttpResponse<Object>> callback) {

		return Mono.<HttpResponse<Object>>create(emitter -> {

			List<Parameter> form = new Vector<Parameter>();
			Mono.from(body).map(completedPart -> {
				System.out.println(completedPart.getName());
				Parameter formParameter = proessCompletedPart(completedPart);
				if (formParameter != null) {
					form.add(formParameter);
				}
				return completedPart;

			}).doOnTerminate(new Runnable() {

				@Override
				public void run() {
					HttpResponse<Object> result = callback.apply(form);
					emitter.success(result);

				}

			}).subscribe();
		});

	}

	public Parameter proessCompletedPart(CompletedPart completedPart) {

		String partName = completedPart.getName();

		if (completedPart instanceof CompletedFileUpload) {

			String originalFileName = ((CompletedFileUpload) completedPart).getFilename();
			String tmpFile = application.getSettings().getTempFilename(originalFileName);
			File file = new File(tmpFile);

			try {
				InputStream stream = completedPart.getInputStream();
				FileUtils.copyInputStreamToFile(stream, file);
				stream.close();
				return new Parameter(partName, file);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {

			try {
				String value = FileUtil.readFileAsString(completedPart.getInputStream());
				return new Parameter(partName, value);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;

	}

	public static class Parameter {

		private String name;

		private Object value;

		public Parameter(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return name + " = " + value;
		}
	}

}
