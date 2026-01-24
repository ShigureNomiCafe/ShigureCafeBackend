package cafe.shigure.ShigureCafeBackend.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * Rate limit key prefix.
     */
    String key() default "";

    /**
     * Rate limit period in milliseconds.
     */
    long milliseconds() default 1000;

    /**
     * SpEL expression to generate dynamic key.
     * Example: "#request.email" or "#currentUser.id"
     */
    String expression() default "";

    /**
     * Whether to use the client's IP address as part of the key.
     */
    boolean useIp() default false;
}
