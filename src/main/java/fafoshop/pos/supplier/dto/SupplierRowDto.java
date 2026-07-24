package fafoshop.pos.supplier.dto;

import fafoshop.common.dto.AbstractDto;

public class SupplierRowDto extends AbstractDto {

	public String supplierCode;
	public String name;
	public String shortName;
	public String zipCode;
	public String address1;
	public String address2;
	public String address3;
	public String tel;
	public String fax;
	public String contactName;
	public String email;
	public String note;
	public String delFlg;

	/** Định dạng "yyyy-MM-dd HH:mm:ss" — dùng String để tránh phải thêm module Jackson JSR-310 chỉ cho 1 field. */
	public String updateDatetime;
}
