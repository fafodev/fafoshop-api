package fafoshop;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 * Đăng ký Jersey ResourceConfig cho Tomcat nhúng (embedded) của Spring Boot
 * lúc dev (xem {@link FafoshopApplication}) — quét CÙNG package gốc với
 * `jersey.config.server.provider.packages` trong web.xml, để 2 cách chạy
 * (WAR thật trên Tomcat và embedded server lúc dev) luôn thấy đúng 1 tập
 * `@Path`/`@Provider` như nhau, không lệch cấu hình.
 *
 * `@ApplicationPath("api")` khớp `url-pattern` "/api/*" của
 * `ServletContainer` trong web.xml — cùng 1 base path dù chạy cách nào.
 */
@Component
@ApplicationPath("api")
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		packages("fafoshop");
	}
}
