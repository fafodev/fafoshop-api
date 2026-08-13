package fafoshop.pos.report.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/** 1 dòng trong danh sách top sản phẩm bán chạy — dùng cho màn Tổng quan. */
public class TopProductDto extends AbstractDto {

	public String productCode;

	public String name;

	public long quantity;

	public BigDecimal revenue;
}
