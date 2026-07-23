package fafoshop.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu 1 resource method KHÔNG cần token xác thực (ví dụ: đăng nhập).
 * AuthTokenFilter bỏ qua kiểm tra token cho method có annotation này.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NoAuth {
}
