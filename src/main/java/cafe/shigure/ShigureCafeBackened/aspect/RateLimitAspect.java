package cafe.shigure.ShigureCafeBackened.aspect;

import cafe.shigure.ShigureCafeBackened.annotation.RateLimit;
import cafe.shigure.ShigureCafeBackened.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Before("@annotation(rateLimit)")
    public void before(JoinPoint joinPoint, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder(rateLimit.key());

        if (!rateLimit.expression().isEmpty()) {
            String resolvedExpression = resolveExpression(joinPoint, rateLimit.expression());
            if (key.length() > 0) {
                key.append(":");
            }
            key.append(resolvedExpression);
        }

        if (rateLimit.useIp()) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                if (key.length() > 0) {
                    key.append(":");
                }
                key.append(rateLimitService.getClientIp(request));
            }
        }

        rateLimitService.checkRateLimit(key.toString(), rateLimit.milliseconds());
    }

    private String resolveExpression(JoinPoint joinPoint, String expressionStr) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = nameDiscoverer.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Expression expression = parser.parseExpression(expressionStr);
        Object value = expression.getValue(context);
        return value != null ? value.toString() : "";
    }
}
