package fafoshop.pos.supplier.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class SupplierUpdateRequest extends AbstractRequest {

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
}
