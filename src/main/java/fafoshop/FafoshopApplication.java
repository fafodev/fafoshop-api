package fafoshop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Bootstrap Spring Boot CHỈ để dựng nhanh Tomcat nhúng (embedded) chạy
 * Jersey lúc dev — chạy thẳng main() này trong Eclipse (Run As > Java
 * Application) thay vì cấu hình "Run on Server", không cần quản lý Tomcat
 * server riêng trong Eclipse.
 *
 * Spring ở đây CHỈ đóng vai trò servlet container khởi động sẵn
 * {@link JerseyConfig} — không dùng Spring DI cho nghiệp vụ, mọi
 * Process/WebService vẫn JDBC thuần qua DBAccessor/DBStatement như quy ước
 * dự án (xem coding-rules.md).
 *
 * File build ra vẫn là .war (xem pom.xml) để deploy Tomcat thật — cách chạy
 * này chỉ phục vụ dev cục bộ, không ảnh hưởng bản build/deploy thật.
 */
@SpringBootApplication
public class FafoshopApplication extends SpringBootServletInitializer {

	private static final Logger logger = LoggerFactory.getLogger(FafoshopApplication.class);

	public static void main(String[] args) {
		try {
			SpringApplication.run(FafoshopApplication.class, args);
		} catch (Exception e) {
			logger.error("Không khởi động được embedded server", e);
			System.exit(1);
		}
	}
}
