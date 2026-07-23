package fafoshop.common.dto;

/**
 * 1 lỗi nghiệp vụ.
 */
public class ErrorDto extends AbstractDto {

	/** Mã lỗi (tra trong systemerror.properties) */
	public String errId;

	/** Nội dung lỗi (tiếng Việt) */
	public String errMsg;

	/** Mã control/field liên quan lỗi (nếu có) */
	public String controlID;
}
