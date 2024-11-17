package com.project.common.aspect;

import com.project.common.annotation.SkipTokenVerification;
import com.project.common.controller.BaseController;
import com.project.common.enums.CommonCode;
import com.project.common.enums.CommonStatus;
import com.project.common.exception.BaseException;
import com.project.common.exception.helper.ValidationExceptionHelper;
import com.project.common.response.ApiRes;
import com.project.common.util.MessageFormatterUtil;
import com.project.common.util.TokenUtil;
import com.project.common.util.UUIDUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Aspect
@Component
public class ManageAspect {

    @Autowired
    private TokenUtil tokenUtil;

    private static final String RES_ERROR_MESSAGE = "服务器错误，请联系管理员，错误识别码:%s";
    private static final String LOG_ERROR_MESSAGE = "错误识别码: %s%n%s";

    // 修改 Pointcut，避免直接引用 user 和 article 模块的包
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPoint() {
    }

    /**
     * 1. 标准化返回
     * 2. 错误信息处理
     * 3. 统计 API 使用时间
     * 4. 验证 token
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("controllerPoint()")
    @Order(9)
    public Object doAroundAccessCheck(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        ApiRes apiRes = new ApiRes();
        HttpServletRequest request = null;
        HttpServletResponse response = getCurrentHttpResponse();
        Object controller = pjp.getTarget();
        stopWatch.start();

        MethodSignature signature = (MethodSignature) pjp.getSignature();

        try {
            request = getCurrentHttpRequest();

            // 检查是否需要进行 Token 验证
            if (isTokenVerificationRequired(controller)) {
                String bearerToken = getBearerToken();
                Map<String, Object> decodedMap = tokenUtil.decodedBearerToken(bearerToken);
                if (controller instanceof BaseController baseController) {
                    setBaseController(decodedMap, baseController);
                }
            }

            apiRes = handleProceedResult(apiRes, pjp.proceed());

            setSuccessResponse(apiRes);
        } catch (ConstraintViolationException e) {
            apiRes = ValidationExceptionHelper.constraintViolationExceptionHandle(new ApiRes<>(), e);
        } catch (BaseException e) {
            log.error(e.getMessage(), e);
            setResponseStatusCode(e, response);

            String message = MessageFormatterUtil.formatMessage(e.getCode().getMsg(), e.getArgs());

            handleBaseExceptionResponse(apiRes, e, message);
        } catch (Exception e) {
            String errorUuid = UUIDUtil.generateUuid();
            String resErrorMessage = String.format(RES_ERROR_MESSAGE, errorUuid);
            log.error(String.format(LOG_ERROR_MESSAGE, errorUuid, e.getMessage()), e);

            response.setStatus(500);

            handleOtherExceptionResponse(apiRes, resErrorMessage);
        } finally {
            stopWatch.stop();
            log.info("[API Process] Controller name: {}, 执行时间(纳秒): {}", pjp.getSignature().getName(), stopWatch.getNanoTime());
        }

        return apiRes;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private HttpServletResponse getCurrentHttpResponse() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return ((ServletRequestAttributes) attributes).getResponse();
    }

    private String getBearerToken() {
        return getCurrentHttpRequest().getHeader(AUTHORIZATION);
    }

    /**
     * 判断是否需要验证 token
     *
     * @param controller
     * @return
     */
    private boolean isTokenVerificationRequired(Object controller) {
        // 检查控制器类上是否有 @SkipTokenVerification 注解
        Class<?> controllerClass = controller.getClass();
        return !controllerClass.isAnnotationPresent(SkipTokenVerification.class);
    }

    public void setBaseController(Map<String, Object> tokenMap, BaseController controller) {
        controller.setUserId((String) tokenMap.get(TokenUtil.USERID));
    }

    private ApiRes handleProceedResult(ApiRes apiRes, Object result) {
        if (result instanceof ApiRes responseResult) {
            apiRes = responseResult;
        } else {
            apiRes.setResult(new HashMap<>());
        }
        return apiRes;
    }

    private void setSuccessResponse(ApiRes apiRes) {
        CommonCode commonCode = CommonCode.SUCCESS;
        CommonStatus commonStatus = CommonStatus.SUCCESS;
        apiRes.setStatus(commonStatus.getStatus());
        apiRes.setCode(commonCode.getCode());
        apiRes.setMessage(commonCode.getMsg());
    }

    private void handleBaseExceptionResponse(ApiRes apiRes, BaseException e, String msg) {
        handleExceptionResponse(apiRes, e.getCode().getCode(), msg, CommonStatus.ERROR.getStatus());
    }

    private void handleOtherExceptionResponse(ApiRes apiRes, String resErrorMessage) {
        handleExceptionResponse(apiRes, CommonCode.ERROR.getCode(), resErrorMessage, CommonStatus.ERROR.getStatus());
    }

    private void handleExceptionResponse(ApiRes apiRes, int errorCode, String message, String errorStatus) {
        apiRes.setStatus(errorStatus);
        apiRes.setCode(errorCode);
        apiRes.setMessage(message);
        apiRes.setResult(new HashMap<>());
    }

    private void setResponseStatusCode(BaseException e, HttpServletResponse response) {
        CommonCode code = e.getCode();
        int status = getFirstThreeDigits(code.getCode());
        response.setStatus(status);
    }

    private int getFirstThreeDigits(int number) {
        int absNumber = Math.abs(number);
        String numberStr = String.valueOf(absNumber);
        String firstThreeDigits = numberStr.length() > 3 ? numberStr.substring(0, 3) : numberStr;
        return Integer.parseInt(firstThreeDigits);
    }
}
