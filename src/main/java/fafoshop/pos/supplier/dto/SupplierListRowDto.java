package fafoshop.pos.supplier.dto;

import fafoshop.common.dto.AbstractDto;

/** Dòng dữ liệu RÚT GỌN (khác SupplierRowDto đầy đủ) — phục vụ riêng dropdown chọn nhà cung cấp ở Product Master. */
public class SupplierListRowDto extends AbstractDto {

	public String supplierCode;
	public String name;
}
