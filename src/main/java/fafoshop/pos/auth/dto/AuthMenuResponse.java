package fafoshop.pos.auth.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class AuthMenuResponse extends AbstractResponse {

	/** Mã chức năng đang bật hiện menu (menu_show_flg='1', del_flg='0'). */
	public List<String> functionCodes = new ArrayList<>();
}
