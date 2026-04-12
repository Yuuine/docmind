package yuuine.docmind.core.audit.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import yuuine.docmind.core.audit.annotation.Audited;

import java.lang.reflect.Parameter;
import java.net.InetAddress;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AuditDataCollector {

    public HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    public Map<String, Object> buildRequestData(org.aspectj.lang.ProceedingJoinPoint joinPoint, 
                                                  HttpServletRequest request, 
                                                  Audited annotation) {
        Map<String, Object> data = new HashMap<>();

        if (request != null) {
            data.put("method", request.getMethod());
            data.put("uri", request.getRequestURI());
            data.put("queryString", request.getQueryString());
        }

        if (!annotation.logRequest()) {
            return data;
        }

        String[] excludeParams = annotation.excludeParams();
        Set<String> excludeSet = excludeParams != null ? Set.of(excludeParams) : Set.of();

        org.aspectj.lang.reflect.MethodSignature signature = 
            (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        if (parameters != null && args != null) {
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                String paramName = parameters[i].getName();
                if (excludeSet.contains(paramName)) {
                    continue;
                }
                if (args[i] != null && isSimpleType(args[i])) {
                    data.put(paramName, args[i]);
                } else if (args[i] != null) {
                    data.put(paramName, args[i].getClass().getSimpleName() + "@" + System.identityHashCode(args[i]));
                }
            }
        }

        return data;
    }

    private boolean isSimpleType(Object obj) {
        return obj instanceof String || obj instanceof Number || obj instanceof Boolean ||
               obj instanceof Character || obj instanceof Enum || obj.getClass().isPrimitive();
    }

    public Object resolveResourceId(org.aspectj.lang.ProceedingJoinPoint joinPoint, Audited annotation) {
        String paramName = annotation.resourceIdParam();
        if (paramName != null && !paramName.isBlank()) {
            return resolveFromParameter(joinPoint, paramName);
        }

        String pathVar = annotation.resourceIdFromPath();
        if (pathVar != null && !pathVar.isBlank()) {
            return resolveFromPath(pathVar);
        }

        return null;
    }

    private Object resolveFromParameter(org.aspectj.lang.ProceedingJoinPoint joinPoint, String paramName) {
        org.aspectj.lang.reflect.MethodSignature signature = 
            (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        if (parameters != null && args != null) {
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                if (parameters[i].getName().equals(paramName)) {
                    return args[i];
                }
            }
        }
        return null;
    }

    private Object resolveFromPath(String pathVar) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> uriTemplateVariables = 
            (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriTemplateVariables != null && uriTemplateVariables.containsKey(pathVar)) {
            return uriTemplateVariables.get(pathVar);
        }
        return null;
    }

    public Long resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException ignored) {
            }
        }

        Principal principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null) {
            String name = principal.getName();
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public static Long toLong(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case Long l -> {
                return l;
            }
            case Number number -> {
                return number.longValue();
            }
            case String s -> {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignored) {
                }
            }
            default -> {
            }
        }
        return null;
    }

    public String getServerHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String getTraceId() {
        String traceId = org.slf4j.MDC.get("traceId");
        return traceId != null ? traceId : "no-trace";
    }
}
