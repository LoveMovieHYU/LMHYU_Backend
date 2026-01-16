package Recommend.Movie.Config.Exception;


import Recommend.Movie.Config.Exception.Dto.ErrorDTO;
import Recommend.Movie.Config.Exception.Dto.ErrorDetail;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 비즈니스 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex) {
        return toResponse(ex.getCode(), ex.getMessage(), List.of());
    }

    /**
     * 쿠키(required = true) 없음
     */
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<Object> handleMissingRequestCookie(MissingRequestCookieException ex) {
        return toResponse(ErrorCode.MISSING_COOKIE, "필수 쿠키가 없습니다.", List.of());
    }

    /**
     * 최종 Fallback(핸들러가 없는 알 수 없는 예외)
     * 메시지는 클라이언트 노출 X
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request)
    {
        log.error("⚠️ Internal server error occurred: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorDTO.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message("서버 에러가 발생했습니다.")
                        .errors(List.of())
                        .build());
    }

    /**
     * @RequestBody 바인딩 실패, validation 실패(@NotNull 등)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return toResponse(ErrorCode.VALIDATION_FAILED, "입력값 검증에 실패했습니다.", details);
    }

    /**
     * @RequestParam(required=true) 이 아예 누락된 경우.
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        var details = List.of(new ErrorDetail(ex.getParameterName(), "필수 요청 파라미터가 누락되었습니다."));
        return toResponse(ErrorCode.BIND_FAILED, "요청 파라미터 처리에 실패했습니다.", details);
    }


    /**
     * @RequestParam / @ModelAttribute 타입 변환 실패 또는 검증 실패.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBind(BindException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return toResponse(ErrorCode.BIND_FAILED, "요청 파라미터 처리에 실패했습니다.", details);
    }

    /**
     * @PathVariable 타입 불일치 / @Validated(파라미터) 제약 위반
     * MethodArgumentTypeMismatchException: 주로 @PathVariable 타입 불일치.
     * ConstraintViolationException: 메서드 파라미터 수준의 @Validated 위반.
     */
    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConstraintViolationException.class })
    public ResponseEntity<Object> handlePathAndConstraint(Exception ex) {
        List<ErrorDetail> details = new ArrayList<>();
        if (ex instanceof MethodArgumentTypeMismatchException e) {
            String required = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "알 수 없음";
            details.add(new ErrorDetail(e.getName(), "'" + e.getValue() + "' 값은 '" + required + "' 타입으로 변환할 수 없습니다."));
        } else if (ex instanceof ConstraintViolationException e) {
            e.getConstraintViolations().forEach(v ->
                    details.add(new ErrorDetail(v.getPropertyPath().toString(), v.getMessage())));
        }
        return toResponse(ErrorCode.BIND_FAILED, "쿼리/경로 파라미터 바인딩에 실패했습니다.", details);
    }

    /**
     * JSON 파싱 불가/본문 읽기 실패.
     * */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        return toResponse(ErrorCode.MALFORMED_JSON, "요청 본문의 형식이 올바르지 않습니다.", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다.", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다.", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(
            org.springframework.web.bind.MissingPathVariableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var details = List.of(new ErrorDetail(ex.getVariableName(), "필수 경로 변수가 누락되었습니다."));
        return toResponse(ErrorCode.BIND_FAILED, "경로 변수 처리에 실패했습니다.", details);
    }

    private ResponseEntity<Object> toResponse(ErrorCode code, String message, List<ErrorDetail> details) {
        return ResponseEntity.status(code.httpStatus).body(
                ErrorDTO.builder().code(code.code).message(message).errors(details).build()
        );
    }

}