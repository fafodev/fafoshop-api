package fafoshop.common.health.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Request rỗng cho endpoint kiểm tra sống (health check) — GET không có body,
 * chỉ cần accessInfo/isFirstCall kế thừa từ AbstractRequest để đi đúng khung
 * AbstractProcess.execute().
 */
public class HealthRequest extends AbstractRequest {
}
