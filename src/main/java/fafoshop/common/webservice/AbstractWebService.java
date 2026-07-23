package fafoshop.common.webservice;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fafoshop.common.ConstantValue;
import fafoshop.common.ILogSender;
import fafoshop.common.LogLevel;
import fafoshop.common.auth.AuthTokenFilter;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.CommonUtility;

/**
 * Lớp cha cho mọi Jersey resource (@Path).
 *
 * Sau khi AuthTokenFilter xác thực token (trừ endpoint đăng nhập), mã người
 * dùng đã xác thực được đọc từ ContainerRequestContext và gán vào
 * accessInfo.userCode ở đây — KHÔNG tin tưởng userCode do client tự gửi lên
 * trong body request.
 */
public abstract class AbstractWebService implements ILogSender {

	protected String outputLevel = ConstantValue.LOG_OUTPUT_LEVEL;

	@Context
	protected ContainerRequestContext requestContext;

	@Override
	public String getOutputLevel() {
		return outputLevel;
	}

	@Override
	public void setOutputLevel(String outputLevel) {
		this.outputLevel = outputLevel;
	}

	protected Log log = LogFactory.getLog(this.getClass());

	protected final AbstractResponse executeProcess(AbstractRequest request) {

		if (requestContext != null) {
			Object userCode = requestContext.getProperty(AuthTokenFilter.AUTHENTICATED_USER_CODE_PROPERTY);
			if (userCode != null) {
				request.accessInfo.userCode = (String) userCode;
			}
		}

		logSend(LogLevel.INFOMATION, "WebService:Start(" + CommonUtility.logUserCode(request.accessInfo.userCode) + ")：" + getClass().getSimpleName());

		AbstractResponse response = null;
		AbstractProcess process = getProcess();

		try {
			request.isFirstCall = true;
			response = process.execute(request);
		} catch (Exception e) {
			logSend(LogLevel.ERROR, CommonUtility.getStackTraceString(e));
		} finally {
			logSend(LogLevel.INFOMATION, "WebService:Finish(" + CommonUtility.logUserCode(request.accessInfo.userCode) + ")：" + getClass().getSimpleName());
		}

		return response;
	}

	protected abstract AbstractProcess getProcess();

	@Override
	public void logSend(String level, Throwable e) {
		if (outputLevel.indexOf(level) == -1) {
			return;
		}
		logByLevel(level, null, e);
	}

	@Override
	public void logSend(String level, String message) {
		if (outputLevel.indexOf(level) == -1) {
			return;
		}
		logByLevel(level, message, null);
	}

	private void logByLevel(String level, String message, Throwable e) {
		switch (level) {
			case "T":
			case "D":
				if (e != null) log.debug("Error", e); else log.debug(message);
				break;
			case "I":
				if (e != null) log.info("Error", e); else log.info(message);
				break;
			case "W":
				if (e != null) log.warn("Error", e); else log.warn(message);
				break;
			case "E":
				if (e != null) log.error("Error", e); else log.error(message);
				break;
			case "F":
				if (e != null) log.fatal("Error", e); else log.fatal(message);
				break;
			default:
				break;
		}
	}
}
