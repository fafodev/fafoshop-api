package fafoshop.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DTO gốc cho mọi request/response DTO — bỏ field null khi serialize JSON.
 */
@JsonInclude(Include.NON_NULL)
public class AbstractDto {
}
