package de.foorcee.mappings;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;

public class Agent {
    static void addToClassPath(Path paperJar) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (!(loader instanceof java.net.URLClassLoader))
            throw new RuntimeException("System ClassLoader is not URLClassLoader");
        try {
            Method addURL = getAddMethod(loader);
            if (addURL == null) {
                System.err.println("Unable to find method to add Paper jar to System ClassLoader");
                System.exit(1);
            }
            addURL.setAccessible(true);
            addURL.invoke(loader, new Object[]{paperJar.toUri().toURL()});
        } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException | java.net.MalformedURLException e) {
            System.err.println("Unable to add Paper Jar to System ClassLoader");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Method getAddMethod(Object o) {
        Class<?> clazz = o.getClass();
        Method m = null;
        while (m == null) {
            try {
                m = clazz.getDeclaredMethod("addURL", new Class[]{URL.class});
            } catch (NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
                if (clazz == null)
                    return null;
            }
        }
        return m;
    }
}
