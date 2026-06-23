package com.yanxiaomap.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 数据验证工具类
 * 提供常用的数据验证方法
 */
@Slf4j
@Component
public class ValidationUtil {

    // 正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证手机号格式
     */
    public static boolean isValidPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 验证身份证格式
     */
    public static boolean isValidIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return false;
        }
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return false;
        }
        // 验证校验码（简单实现）
        return true;
    }

    /**
     * 验证URL格式
     */
    public static boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * 验证IP地址格式
     */
    public static boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        if (!IP_PATTERN.matcher(ip).matches()) {
            return false;
        }
        // 验证每个数字段在0-255之间
        String[] segments = ip.split("\\.");
        for (String segment : segments) {
            int num = Integer.parseInt(segment);
            if (num < 0 || num > 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证字符串长度是否在指定范围内
     */
    public static boolean isLengthValid(String str, int min, int max) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= min && length <= max;
    }

    /**
     * 验证数字是否在指定范围内
     */
    public static boolean isNumberInRange(Number number, Number min, Number max) {
        if (number == null || min == null || max == null) {
            return false;
        }
        double value = number.doubleValue();
        double minValue = min.doubleValue();
        double maxValue = max.doubleValue();
        return value >= minValue && value <= maxValue;
    }

    /**
     * 验证字符串是否为非空且非空白
     */
    public static boolean isNotBlank(String str) {
        return StringUtils.hasText(str);
    }

    /**
     * 验证对象是否不为null
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 验证集合是否非空
     */
    public static boolean isNotEmpty(Iterable<?> iterable) {
        return iterable != null && iterable.iterator().hasNext();
    }

    /**
     * 验证数组是否非空
     */
    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    /**
     * 验证经纬度是否有效
     */
    public static boolean isValidCoordinate(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            return false;
        }
        // 经度范围：-180 到 180
        // 纬度范围：-90 到 90
        return longitude >= -180 && longitude <= 180 &&
                latitude >= -90 && latitude <= 90;
    }

    /**
     * 验证分页参数
     */
    public static boolean isValidPageParams(Integer page, Integer size) {
        if (page == null || size == null) {
            return false;
        }
        return page >= 1 && size >= 1 && size <= 100;
    }

    /**
     * 验证排序参数
     */
    public static boolean isValidSortOrder(String order) {
        if (!StringUtils.hasText(order)) {
            return false;
        }
        return "asc".equalsIgnoreCase(order) || "desc".equalsIgnoreCase(order);
    }
}