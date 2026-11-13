package com.hsf302.waterbus.controller;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

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
