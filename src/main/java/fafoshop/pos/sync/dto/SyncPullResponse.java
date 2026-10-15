package fafoshop.pos.sync.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class SyncPullResponse extends AbstractResponse {

	/** Thời điểm server tạo gói dữ liệu, "yyyy-MM-dd HH:mm:ss" (giờ server). */
	public String serverTime;

	public List<SyncProductDto> products = new ArrayList<>();

	public List<SyncProductUnitDto> productUnits = new ArrayList<>();

	/** true = chi nhánh đã cấu hình bank_account, các field bank* bên dưới mới có giá trị. */
	public boolean bankConfigured;
	public String bankBin;
	public String bankName;
	public String accountNo;
	public String accountName;
}
