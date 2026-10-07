package cn.bugstack.types.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 5317680961212299217L;

    /** 异常码 */
    private String code;

    /** 异常信息 */
    private String info;

    /**
     * 说明：所有构造函数都要把文案交给 RuntimeException，否则 getMessage() 恒为 null，
     * 日志里只会打印「AppException: null」，排查问题时看不到任何原因。
     */
    public AppException(String code) {
        super(code);
        this.code = code;
        this.info = code;
    }

    public AppException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
        this.info = code;
    }

    public AppException(String code, String message) {
        super(message);
        this.code = code;
        this.info = message;
    }

    public AppException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.info = message;
    }

    @Override
    public String toString() {
        return "cn.bugstack.types.exception.AppException{" +
                "code='" + code + '\'' +
                ", info='" + info + '\'' +
                '}';
    }

}
