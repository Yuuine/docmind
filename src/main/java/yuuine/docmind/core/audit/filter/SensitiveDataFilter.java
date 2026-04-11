package yuuine.docmind.core.audit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveDataFilter {

    private final ObjectMapper objectMapper;

    private static final Set<String> DEFAULT_SENSITIVE_PARAMS = Set.of(
            "password", "passwd", "secret", "token", "apiKey", "api_key",
            "accessToken", "access_token", "refreshToken", "refresh_token",
            "privateKey", "private_key", "authorization", "credential",
            "oldPassword", "newPassword", "confirmPassword",
            "creditCard", "credit_card", "cardNumber", "cvv", "ssn"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile(
            "\\b(?:\\+86)?1[3-9]\\d{9}\\b");
    private static final Pattern CHINA_PHONE_PATTERN = Pattern.compile(
            "\\b0\\d{2,3}[- ]?\\d{7,8}\\b");
    
    private static final Pattern ID_CARD_15_PATTERN = Pattern.compile(
            "\\b\\d{15}\\b");
    
    private static final Pattern ID_CARD_18_PATTERN = Pattern.compile(
            "\\b\\d{17}[\\dXx]\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");
    private static final Pattern IP_V4_PATTERN = Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\b");

    public String filterSensitiveData(String jsonString, String[] additionalSensitiveParams) {
        return filterSensitiveData(jsonString, additionalSensitiveParams, true);
    }

    public String filterSensitiveData(String jsonString, String[] additionalSensitiveParams, boolean maskIp) {
        if (jsonString == null || jsonString.isBlank()) {
            return jsonString;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) objectMapper.readValue(jsonString, Map.class);
            filterMap(data, additionalSensitiveParams, maskIp);
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Failed to filter sensitive data from JSON: {}", e.getMessage());
            return maskSensitivePatterns(jsonString, maskIp);
        }
    }

    private void filterMap(Map<String, Object> map, String[] additionalParams, boolean maskIp) {
        if (map == null) {
            return;
        }

        Set<String> sensitiveKeys = buildSensitiveKeysSet(additionalParams);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (sensitiveKeys.contains(key.toLowerCase())) {
                entry.setValue("***SENSITIVE***");
            } else if (value instanceof String stringValue) {
                entry.setValue(maskSensitivePatterns(stringValue, maskIp));
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                filterMap(mapValue, additionalParams, maskIp);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> listValue = (List<Object>) value;
                filterList(listValue, additionalParams, maskIp);
            }
        }
    }

    private void filterList(List<Object> list, String[] additionalParams, boolean maskIp) {
        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof String stringItem) {
                list.set(i, maskSensitivePatterns(stringItem, maskIp));
            } else if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapItem = (Map<String, Object>) item;
                filterMap(mapItem, additionalParams, maskIp);
            } else if (item instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> listItem = (List<Object>) item;
                filterList(listItem, additionalParams, maskIp);
            }
        }
    }

    private String maskSensitivePatterns(String text, boolean maskIp) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("***EMAIL***");
        masked = CHINA_MOBILE_PATTERN.matcher(masked).replaceAll("***CN_MOBILE***");
        masked = CHINA_PHONE_PATTERN.matcher(masked).replaceAll("***CN_PHONE***");
        masked = ID_CARD_15_PATTERN.matcher(masked).replaceAll("***ID_CARD_15***");
        masked = ID_CARD_18_PATTERN.matcher(masked).replaceAll("***ID_CARD_18***");
        masked = CREDIT_CARD_PATTERN.matcher(masked).replaceAll("***CREDIT_CARD***");
        if (maskIp) {
            masked = IP_V4_PATTERN.matcher(masked).replaceAll("***IP***");
        }
        return masked;
    }

    private Set<String> buildSensitiveKeysSet(String[] additionalParams) {
        Set<String> keys = new HashSet<>(DEFAULT_SENSITIVE_PARAMS);
        if (additionalParams != null) {
            for (String param : additionalParams) {
                keys.add(param.toLowerCase());
            }
        }
        return keys;
    }
}
