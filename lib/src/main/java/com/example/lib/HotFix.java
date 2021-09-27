package com.example.lib;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HotFix {

    /**
     *  字节码插桩
     * @param context
     * @return
     */
    public static File initHack(Context context) {
        File hackFile = new File(context.getExternalFilesDir(""), "hack.dex");
        FileOutputStream fos = null;
        InputStream is = null;

        try {
            fos = new FileOutputStream(hackFile);
            is = context.getAssets().open("hack.dex");
            int len;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return hackFile;
    }

    /**
     * 1、获取程序的PathClassLoader对象
     * 2、反射获得PathClassLoader父类BaseDexClassLoader的pathList对象
     * 3、反射获取pathList的dexElements对象 （oldElement）
     * 4、把补丁包变成Element数组：patchElement（反射执行makePathElements）
     * 5、合并patchElement+oldElement = newElement （Array.newInstance）
     * 6、反射把oldElement赋值成newElement
     *
     * @param application  Application上下文
     * @param path  patch文件
     */
    public static void install(Application application, File path) {
        File hackDex = initHack(application);
        List<File> pathListFile = new ArrayList<>();
        pathListFile.add(hackDex);
        if (path.exists()) {
            pathListFile.add(path);
        }

        //获取程序的PathClassLoader对象
        ClassLoader classLoader = application.getClassLoader();

        // 兼容Android N 混合编译  修改系统PathClassLoader，使用我们自己的PathClassLoader
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                NewClassLoaderInjector.inject(application, classLoader, pathListFile);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } else {
            try {
                Log.d("TAG", "程序的PathClassLoader对象：" + classLoader.getClass());

                Class<?> superclass = classLoader.getClass().getSuperclass();
                Log.d("TAG", "父类的class对象为：" + superclass);
                Field pathListField = superclass.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathList = pathListField.get(classLoader);
                Log.d("TAG", "pathList对象为：" + pathList);

                Class<?> pathListClass = pathList.getClass();
                Field dexElementsField = pathListClass.getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object[] oldDexElements = (Object[]) dexElementsField.get(pathList);
                Log.d("TAG", "oldDexElements对象为：" + Arrays.toString(oldDexElements));

                Object[] patchDexElements = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Method makePathElementsMethod = pathListClass.getDeclaredMethod("makePathElements", List.class, File.class, List.class);
                    makePathElementsMethod.setAccessible(true);
                    Log.d("TAG", "makePathElementsMethod方法为：" + makePathElementsMethod);

                    ArrayList<IOException> ioExceptions = new ArrayList<>();
                    // 这里invoke的时候注意，如果该方法是静态方法，则obj可以传null，如果不是静态方法，obj必须是该方法的对象
                    patchDexElements = (Object[]) makePathElementsMethod.invoke(null, pathListFile, application.getCacheDir(), ioExceptions);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Method makePathElementsMethod = pathListClass.getDeclaredMethod("makeDexElements", ArrayList.class, File.class, ArrayList.class);
                    makePathElementsMethod.setAccessible(true);
                    Log.d("TAG", "makePathElementsMethod方法为：" + makePathElementsMethod);

                    ArrayList<IOException> ioExceptions = new ArrayList<>();
                    // 这里invoke的时候注意，如果该方法是静态方法，则obj可以传null，如果不是静态方法，obj必须是该方法的对象
                    patchDexElements = (Object[]) makePathElementsMethod.invoke(null, pathListFile, application.getCacheDir(), ioExceptions);

                }

                Object[] newDexElements = (Object[]) Array.newInstance(oldDexElements.getClass().getComponentType(), oldDexElements.length + patchDexElements.length);

                System.arraycopy(patchDexElements, 0, newDexElements, 0, patchDexElements.length);
                System.arraycopy(oldDexElements, 0, newDexElements, patchDexElements.length, oldDexElements.length);

                Log.d("TAG", "newDexElements对象为：" + Arrays.toString(newDexElements));
                dexElementsField.set(pathList, newDexElements);

                Log.d("TAG", "新的dexElementsField为 : " + pathList);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
