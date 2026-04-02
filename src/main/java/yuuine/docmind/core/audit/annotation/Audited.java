package yuuine.docmind.core.audit.annotation;

import java.lang.annotation.*;
import yuuine.docmind.core.audit.valueobject.AuditAction;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    AuditAction action();
    String resourceType();
    String resourceIdParam() default "";
    String resourceIdFromPath() default "";
    String describe() default "";
    boolean logRequest() default true;
    boolean logResponse() default true;
    boolean async() default true;
    String[] excludeParams() default {};
    String[] sensitiveParams() default {};
}