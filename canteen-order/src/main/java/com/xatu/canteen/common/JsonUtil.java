package com.xatu.canteen.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 统一 JSON 工具，缓存序列化与业务序列化共用一套规则。
 */
public final class JsonUtil {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME));
        MAPPER.registerModule(module);
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JsonUtil() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(ResultCode.SERVER_ERROR, "JSON 序列化失败");
        }
    }

    public static <T> T parse(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new BizException(ResultCode.SERVER_ERROR, "JSON 解析失败");
        }
    }

    public static <T> List<T> parseList(String json, Class<T> type) {
        try {
            JavaType listType = MAPPER.getTypeFactory().constructCollectionType(List.class, type);
            return MAPPER.readValue(json, listType);
        } catch (Exception e) {
            throw new BizException(ResultCode.SERVER_ERROR, "JSON 解析失败");
        }
    }
}
