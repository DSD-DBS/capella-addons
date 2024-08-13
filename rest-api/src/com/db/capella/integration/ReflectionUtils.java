// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella.integration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectionUtils {
    public static void copyProperties(Object source, Object target) {
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        Map<String, Method> getters = getGetters(sourceClass);
        Map<String, Method> setters = getSetters(targetClass);

        for (Map.Entry<String, Method> getterEntry : getters.entrySet()) {
            String propName = getterEntry.getKey();
            Method getterMethod = getterEntry.getValue();
            if (setters.containsKey(propName)) {
                Method setterMethod = setters.get(propName);
                try {
                    Object value = getterMethod.invoke(source);
                    setterMethod.invoke(target, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Map<String, Method> getGetters(Class<?> clazz) {
        Map<String, Method> getters = new HashMap<>();
        populateGetters(getters, clazz);
        return getters;
    }

    public static Map<String, Method> getSetters(Class<?> clazz) {
        Map<String, Method> setters = new HashMap<>();
        populateSetters(setters, clazz);
        return setters;
    }

    public static void populateGetters(Map<String, Method> getters, Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (isGetter(method)) {
                String propName = extractPropertyName(method.getName());
                getters.putIfAbsent(propName, method);
            }
        }
        populateGetters(getters, clazz.getSuperclass());
    }

    public static void populateSetters(Map<String, Method> setters, Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (isSetter(method)) {
                String propName = extractPropertyName(method.getName());
                setters.putIfAbsent(propName, method);
            }
        }
        populateSetters(setters, clazz.getSuperclass());
    }

    public static boolean isGetter(Method method) {
        return method.getParameterTypes().length == 0
                && (method.getName().startsWith("get") || method.getName().startsWith("is"));
    }

    public static boolean isSetter(Method method) {
        return method.getName().startsWith("set") &&
                method.getParameterTypes().length == 1;
    }

    public static String extractPropertyName(String methodName) {
        String prefix = "";
        if (methodName.startsWith("get")) {
            prefix = "get";
        } else if (methodName.startsWith("is")) {
            prefix = "is";
        } else if (methodName.startsWith("set")) {
            prefix = "set";
        }
        String propName = methodName.substring(prefix.length(), prefix.length()
                + 1).toLowerCase() + methodName.substring(prefix.length() + 1);
        return propName;
    }
}
