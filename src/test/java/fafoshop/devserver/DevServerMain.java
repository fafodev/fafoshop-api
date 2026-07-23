package fafoshop.devserver;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Chạy fafoshop-api bằng HTTP server nhúng (Grizzly) để kiểm thử API qua
 * HTTP THẬT (đi qua đúng tầng Jersey routing + AuthTokenFilter + Jackson
 * JSON), không cần deploy WAR lên Tomcat.
 *
 * CHỈ dùng cho dev/test cục bộ (nằm ở src/test/java, dependency
 * jersey-container-grizzly2-http scope "test" — không đóng gói vào WAR
 * thật). Base URI khớp đúng /api/* như cấu hình web.xml để test sát với môi
 * trường Tomcat thật.
 */
public class DevServerMain {

	public static void main(String[] args) throws Exception {
		URI baseUri = URI.create("http://localhost:8089/api/");

		ResourceConfig config = new ResourceConfig();
		config.packages("fafoshop");
		config.register(JacksonFeature.class);

		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

		System.out.println("fafoshop-api dev server dang chay tai " + baseUri);
		System.out.println("Server se tu dung sau 10 phut (hoac kill process nay som hon).");
		Thread.sleep(10 * 60 * 1000L);

		server.shutdownNow();
	}
}
