package com.example.lib;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import dalvik.system.DelegateLastClassLoader;
import dalvik.system.DexFile;

public class NewClassLoaderInjector {

    public static void inject(Application app, ClassLoader oldClassLoader, List<File> patchs) throws Throwable {

        ClassLoader newClassLoader = createNewClassLoader(app, oldClassLoader, patchs);

        doInject(app, newClassLoader);
    }

    private static ClassLoader createNewClassLoader(Context context, ClassLoader oldClassLoader, List<File> patchs) throws Throwable {

        /**
         * 1、先把补丁包的dex拼起来
         */
        // 获得原始的dexPath用于构造classloader
        StringBuilder dexPathBuilder = new StringBuilder();
        String packageName = context.getPackageName();
        boolean isFirstItem = true;
        for (File patch : patchs) {
            if (patch == null) {
                continue;
            }
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                dexPathBuilder.append(File.pathSeparator);
            }
            dexPathBuilder.append(patch);
        }

        /**
         * 2、把apk中的dex拼起来
         */
        //得到原本的pathList
        final Field pathListField = findField(
                Class.forName("dalvik.system.BaseDexClassLoader", false, oldClassLoader),
                "pathList");
        final Object oldPathList = pathListField.get(oldClassLoader);

        //得到oldDexElements
        final Field dexElementsField = findField(oldPathList.getClass(), "dexElements");
        final Object[] oldDexElements = (Object[]) dexElementsField.get(oldPathList);

        //从Element上得到 dexFile
        Field dexFileField = findField(oldDexElements[0].getClass(), "dexFile");
        for (Object oldDexElement : oldDexElements) {
            String dexPath = null;
            DexFile dexFile = (DexFile) dexFileField.get(oldDexElement);
            if (dexFile != null) {
                dexPath = dexFile.getName();
            }
            if (dexPath == null || dexPath.isEmpty()) {
                continue;
            }
            if (!dexPath.contains("/" + packageName)) {
                continue;
            }
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                dexPathBuilder.append(File.pathSeparator);
            }
            dexPathBuilder.append(dexPath);
        }
        String combinedDexPath = dexPathBuilder.toString();

        /**
         * 3、获取apk中的so加载路径
         */
        //  app的native库（so） 文件目录 用于构造classloader
        final Field nativeLibraryDirectoriesField = findField(oldPathList.getClass(), "nativeLibraryDirectories");
        List<File> oldNativeLibraryDirectories = null;
        if (nativeLibraryDirectoriesField.getType().isArray()) {
            oldNativeLibraryDirectories = Arrays.asList((File[]) nativeLibraryDirectoriesField.get(oldPathList));
        } else {
            oldNativeLibraryDirectories = (List<File>) nativeLibraryDirectoriesField.get(oldPathList);
        }
        final StringBuilder libraryPathBuilder = new StringBuilder();
        isFirstItem = true;
        for (File libDir : oldNativeLibraryDirectories) {
            if (libDir == null) {
                continue;
            }
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                libraryPathBuilder.append(File.pathSeparator);
            }
            libraryPathBuilder.append(libDir.getAbsolutePath());
        }

        final String combinedLibraryPath = libraryPathBuilder.toString();

        ClassLoader result = new MyClassLoader(combinedDexPath, combinedLibraryPath, ClassLoader.getSystemClassLoader());

        return result;


    }


    private static void doInject(Application app, ClassLoader classLoader) throws Throwable {
        Thread.currentThread().setContextClassLoader(classLoader);
        final Context baseContext = (Context) findField(app.getClass(), "mBase").get(app);

        try {
            findField(baseContext.getClass(), "mClassLoader").set(baseContext, classLoader);
        } catch (Throwable ignored) {
            // There's no mClassLoader field in ContextImpl before Android O.
            // However we should try our best to replace this field in case some
            // customized system has one.
        }

        final Object basePackageInfo = findField(baseContext.getClass(), "mPackageInfo").get(baseContext);
        findField(basePackageInfo.getClass(), "mClassLoader").set(basePackageInfo, classLoader);

        if (Build.VERSION.SDK_INT < 27) {
            final Resources res = app.getResources();
            try {
                findField(res.getClass(), "mClassLoader").set(res, classLoader);

                final Object drawableInflater = findField(res.getClass(), "mDrawableInflater").get(res);
                if (drawableInflater != null) {
                    findField(drawableInflater.getClass(), "mClassLoader").set(drawableInflater, classLoader);
                }
            } catch (Throwable ignored) {
                // Ignored.
            }
        }
    }


    private static Field findField(Class<?> clazz, String name) throws Throwable {
        Class<?> currClazz = clazz;
        while (true) {
            try {
                final Field result;
                if (currClazz != null) {
                    result = currClazz.getDeclaredField(name);
                    result.setAccessible(true);
                    return result;
                }
            } catch (Throwable ignored) {
                if (currClazz == Object.class) {
                    throw new NoSuchFieldException("Cannot find field "
                            + name + " in class " + clazz.getName() + " and its super classes.");
                } else {
                    currClazz = currClazz.getSuperclass();
                }
            }
        }
    }

}
