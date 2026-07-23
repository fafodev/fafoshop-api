package fafoshop.common.process;

import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.ILogSender;
import fafoshop.common.LogLevel;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.LoopException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.CommonUtility;
import fafoshop.common.utility.MessageUtility;

/**
 * Lớp cha cho mọi Process — khung transaction (autoCommit=false,
 * commit/rollback tay) + tự động retry khi deadlock (LoopException) + gom
 * lỗi nghiệp vụ vào response (lstNormalError/lstFatalError).
 *
 * Xác thực token nằm ở tầng infrastructure (AuthTokenFilter, JAX-RS
 * ContainerRequestFilter), không lẫn trong business logic ở đây. checkAuth()
 * chỉ kiểm tra quyền theo FUNCID (getFuncId()) — mỗi Process tự khai FUNCID
 * của mình, không qua bảng trung gian nào (đơn giản, phù hợp quy mô 1 cửa
 * hàng).
 */
public abstract class AbstractProcess implements ILogSender {

	public AbstractProcess(ILogSender logSender) {
		this.logSender = logSender;
	}

	protected ILogSender logSender = null;

	protected String outputLevel = ConstantValue.LOG_OUTPUT_LEVEL;

	@Override
	public String getOutputLevel() {
		return outputLevel;
	}

	@Override
	public void setOutputLevel(String outputLevel) {
		this.outputLevel = outputLevel;
	}

	public AbstractResponse execute(AbstractRequest request)
			throws FatalException, DBException, ProcessCheckErrorException {
		return execute(null, request, null);
	}

	public AbstractResponse execute(DBAccessor dba, AbstractRequest request, AbstractResponse parentResponse)
			throws FatalException, DBException, ProcessCheckErrorException {

		int deadCnt = 0;
		int maxDeadCnt = 5;
		boolean isDeadLock;

		AbstractResponse response = createNewResponse(request);

		do {
			isDeadLock = false;
			logSend(LogLevel.INFOMATION, "Process:Start(" + CommonUtility.logUserCode(request.accessInfo.userCode) + ")" + getClass().getSimpleName());

			try {
				request.accessInfo.processId = getProcessId();
				boolean isParent = false;

				if (dba == null || deadCnt > 0) {
					isParent = true;
					parentResponse = response;
				}

				try {
					if (isParent) {
						dba = new DBAccessor();
						dba.setLogSender(this.logSender);
					}

					checkAuth(dba, request, parentResponse);

					beforeProcess(dba, request, response, parentResponse);
					process(dba, request, response, parentResponse);
					afterProcess(dba, request, response, parentResponse);

					if (isParent) {
						dba.commit();
						logSend(LogLevel.DEBUG, "Commit:Success");
					}

				} catch (ProcessCheckErrorException e) {

					if (!isParent) {
						throw e;
					}

					List<ErrorDto> lstNormalError = getNormalError(e);
					List<ErrorDto> lstFatalError = getFatalError(e);

					response.addFatalErrorList(lstFatalError);
					response.addNormalErrorList(lstNormalError);

					if (!lstFatalError.isEmpty()) {
						try {
							logSend(LogLevel.DEBUG, "Rollback:FatalBusinessError");
							if (dba != null) {
								dba.rollback();
							}
						} catch (DBException de) {
							logSend(LogLevel.ERROR, CommonUtility.getStackTraceString(de));
						}
					} else {
						dba.commit();
						logSend(LogLevel.DEBUG, "Commit:Success");
					}

				} catch (LoopException e) {
					if (!isParent) {
						throw e;
					}
					deadCnt++;
					isDeadLock = true;

					try {
						logSend(LogLevel.ERROR, "Deadlock:Retry " + deadCnt);
						if (dba != null) {
							dba.rollback();
						}
					} catch (DBException de) {
						logSend(LogLevel.ERROR, CommonUtility.getStackTraceString(de));
					}

					if (deadCnt >= maxDeadCnt) {
						ErrorDto error = new ErrorDto();
						error.errId = "MC000001";
						error.errMsg = MessageUtility.getSystemErrMsg(error.errId);
						response.addFatalError(error);
					} else {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							ErrorDto error = new ErrorDto();
							error.errId = "MC000001";
							error.errMsg = MessageUtility.getSystemErrMsg(error.errId);
							response.addFatalError(error);
							break;
						}
						continue;
					}

				} catch (Exception e) {
					logSend(LogLevel.FATAL, e);

					if (!isParent) {
						throw new FatalException(e);
					}

					ErrorDto error = new ErrorDto();
					error.errId = "MC000001";
					error.errMsg = MessageUtility.getSystemErrMsg(error.errId);
					response.addFatalError(error);

					try {
						if (dba != null) {
							dba.rollback();
						}
					} catch (DBException de) {
						logSend(LogLevel.ERROR, CommonUtility.getStackTraceString(de));
					}

				} finally {
					if (isParent) {
						try {
							if (dba != null) {
								dba.disconnect();
							}
						} catch (DBException ignore) {
						}
					}
				}
			} finally {
				if (!isDeadLock) {
					logSend(LogLevel.INFOMATION, "Process:Finish(" + CommonUtility.logUserCode(request.accessInfo.userCode) + ")" + getClass().getSimpleName());
				}
			}
		} while (isDeadLock && deadCnt < maxDeadCnt);

		return response;
	}

	protected void beforeProcess(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {
	}

	protected void afterProcess(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {
	}

	protected List<ErrorDto> getNormalError(ProcessCheckErrorException e) {
		return e.getNormalError();
	}

	protected List<ErrorDto> getFatalError(ProcessCheckErrorException e) {
		return e.getFatalError();
	}

	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new AbstractResponse();
	}

	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {
		return null;
	}

	@Override
	public void logSend(String level, Throwable e) {
		if (logSender != null) {
			logSender.logSend(level, e);
		}
	}

	@Override
	public void logSend(String level, String message) {
		if (logSender != null) {
			logSender.logSend(level, message);
		}
	}

	protected String getProcessId() {
		return getClass().getSimpleName();
	}

	/**
	 * Mã chức năng (function_code) dùng để kiểm tra quyền (bảng
	 * function_permission). Trả về null nghĩa là process này không cần kiểm
	 * tra quyền (ví dụ: tìm kiếm dữ liệu công khai). Mỗi Process nghiệp vụ cần
	 * quyền hạn chế phải override để khai báo mã chức năng riêng.
	 */
	protected String getFuncId() {
		return null;
	}

	private void checkAuth(DBAccessor dba, AbstractRequest request, AbstractResponse parentResponse)
			throws FatalException, DBException, ProcessCheckErrorException {

		if (request.accessInfo.processId.equals(ConstantValue.LOGIN_PROCESS_ID)) {
			return;
		}

		String funcId = getFuncId();
		if (funcId == null) {
			return;
		}

		CheckAuthProcess procCheckAuth = new CheckAuthProcess(this);
		procCheckAuth.checkAuth(dba, request.accessInfo.userCode, funcId);
	}
}
