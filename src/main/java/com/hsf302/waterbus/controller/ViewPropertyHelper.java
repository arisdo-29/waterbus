package com.hsf302.waterbus.controller;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dùng chung cho view (FE chính - Khuyên). Đọc 1 field của DTO/Map mà KHÔNG lỗi khi DTO chưa có field đó.
 * Dùng khi hợp đồng trong docs/PAGES.md đã chốt field mới nhưng module khác chưa thêm vào DTO:
 * {@code ${@view.prop(ticket, 'passengerName')}} trả null thay vì lỗi 500.
 * Khi DTO đã có field thì có thể đổi lại thành {@code ${ticket?.passengerName}}.
 */
@Component("view")
public class ViewPropertyHelper {

    public Object prop(Object target, String name) {
        if (target == null || name == null || name.isBlank()) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(name);
        }
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(target);
        return wrapper.isReadableProperty(name) ? wrapper.getPropertyValue(name) : null;
    }
}
