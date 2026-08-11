package fafoshop;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
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

		// jersey-media-json-jackson TỰ auto-discover JacksonFeature mặc định
		// (kèm JsonParseExceptionMapper/JsonMappingExceptionMapper riêng của
		// Jackson) TRƯỚC khi request kịp chạm AbstractProcess - 2 mapper đó
		// khớp kiểu cụ thể hơn ExceptionMapper<Throwable> của
		// SystemExceptionMapper nên JAX-RS luôn chọn chúng trước, trả nguyên
		// văn exception (lộ tên package/class Java nội bộ) thẳng ra client mỗi
		// khi JSON request hỏng - vi phạm luật không lộ thông tin nội bộ
		// (CLAUDE.md). Tắt auto-discovery rồi tự đăng ký JacksonFeature bản
		// KHÔNG kèm 2 exception mapper đó (registerExceptionMappers=false),
		// để JsonRequestExceptionMapper (đăng ký qua packages() ở trên) là
		// mapper duy nhất xử lý lỗi parse JSON, trả cùng khuôn lỗi MC000001
		// như mọi lỗi hệ thống khác. Dự án không dùng Bean Validation/MultiPart
		// hay auto-discoverable Feature nào khác (đã rà pom.xml + code) nên tắt
		// auto-discovery không ảnh hưởng gì thêm.
		property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
		register(JacksonFeature.withoutExceptionMappers());
	}
}
