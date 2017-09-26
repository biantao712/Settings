package com.android.settings.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import android.util.Log;
import android.view.View;

/**
 * A static helper class to access AMAX frameworks features.
 *@author albertjs
 */
public final class AMAXReflector {
    public static class ClassDefinition {
    }

    private static class FeatureMethod {
        Class mClass;
        String mMethodName;
        Class[] mParamTypes;
        public FeatureMethod(Class c, String name, Class[] params) {
            mClass = c;
            mMethodName = name;
            mParamTypes = params;
        }
    }

    private static HashMap<String, FeatureMethod> mMethods;
    /**
     * Definitions for setting AMAX platform specific features method
     * @author albertjs
     */
    public static class FeatureMethods {
        // Wifi
        public static final String WIFI_SETPCSC = "setPcsc";
    }
    static {
        mMethods = new HashMap<String, AMAXReflector.FeatureMethod>();
        // Wifi
        mMethods.put(FeatureMethods.WIFI_SETPCSC,
                new FeatureMethod(Object.class,FeatureMethods.WIFI_SETPCSC,
                        new Class[] { String.class }));
    }

    /**
     * Get a public member
     * @param name Field name
     * @param obj the container
     * @param defaultValue
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> T getValueByName(String name, Object obj, T defaultValue) {
        T retValue = defaultValue;
        try {
            Class<?> _class = (Class) ((obj instanceof Class) ? obj : obj.getClass());
            Field field = _class.getDeclaredField(name);
            Object val = field.get(obj);
            retValue = (T)val;
        } catch (ClassCastException e) {
            logRefectionException("Field: " + name , e);
        } catch (NoSuchFieldException e) {
            logRefectionException("Field: " + name , e);
        } catch (IllegalArgumentException e) {
            logRefectionException("Field: " + name , e);
        } catch (IllegalAccessException e) {
            logRefectionException("Field: " + name , e);
        }
        return retValue;
    }

    /**
     * Call a method of specified object by Java reflection.
     * @param name the Method name,
     * @param obj the caller object
     * @param arguments the Feature Method arguments.
     * @return 1. null: void method or no such method.<BR>2. the return value from Feature Method as type T.
     * @see AMAXReflector.FeatureMethods
     */
    @SuppressWarnings("rawtypes")
    public static <T> T callFeatureMethod(String name, Object obj, Object... arguments) {
        FeatureMethod fm = mMethods.get(name);
        Class<?> _class = (Class) ((obj instanceof Class) ? obj : obj.getClass());
        if (fm.mClass.isAssignableFrom(_class)) {
            try {
                Method method = _class.getMethod(fm.mMethodName, fm.mParamTypes);
                Object retValue = method.invoke(obj, arguments);
                return (T)retValue;
            } catch (NoSuchMethodException e) {
                logRefectionException("Method: " + name , e);
            } catch (IllegalArgumentException e) {
                logRefectionException("Method: " + name , e);
            } catch (IllegalAccessException e) {
                logRefectionException("Method: " + name , e);
            } catch (InvocationTargetException e) {
                logRefectionException("Method: " + name , e);
            }
        }
        return (T)null;
    }

    private static void logRefectionException(String method,
            Throwable p_throwable) {
        final StringBuilder result = new StringBuilder();
        result.append(method).append(", Exception:");
        result.append(p_throwable.toString());
        Log.v(AMAXReflector.class.getName(), result.toString());
//        result.append(",\n");
//        String oneElement;
//        for (StackTraceElement element : p_throwable.getStackTrace()) {
//            oneElement = element.toString();
//            result.append(oneElement);
//            result.append(",\n");
//        }
//        Log.v(AMAXReflector.class.getName(), result.toString());
    }

    /**
     * Call a method of specified object by Java reflection.
     * @param name name the Method name
     * @param paramType Customized parameter type
     * @param obj the caller object.
     * @param arguments arguments the Feature Method arguments.
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> T callFeatureMethod(String name, Class<?>[] paramType, Object obj, Object... arguments) {
        FeatureMethod fm = mMethods.get(name);
        Class<?> _class = (Class) ((obj instanceof Class) ? obj : obj.getClass());
        if (fm.mClass.isAssignableFrom(_class)) {
            try {
                Method method = _class.getMethod(fm.mMethodName, paramType);
                Object retValue = method.invoke(obj, arguments);
                return (T)retValue;
            } catch (NoSuchMethodException e) {
                logRefectionException("Method: " + name , e);
            } catch (IllegalArgumentException e) {
                logRefectionException("Method: " + name , e);
            } catch (IllegalAccessException e) {
                logRefectionException("Method: " + name , e);
            } catch (InvocationTargetException e) {
                logRefectionException("Method: " + name , e);
            }
        }
        return (T)null;
    }

    /**
     * Call a method of specified object by Java reflection.
     * @param name the Method name,
     * @param obj the caller object
     * @return 1. null: void method or no such method.<BR>2. the return value from Feature Method as type T.
     * @see AMAXReflector.FeatureMethods
     */
    public static <T> T callFeatureMethod(String name, Object obj) {
        return (T)callFeatureMethod(name, obj, new Object[] {});
    }

    /**
     * get a class by name
     * @param name class name
     * @return Class object or null (not found)
     */
    public static Class<?> getClass(String name) {
        Class<?> _class = null;
        try {
            _class = Class.forName(name);
        } catch (ClassNotFoundException e) {
            logRefectionException("Class: " + name , e);
        }
        return _class;
    }
}