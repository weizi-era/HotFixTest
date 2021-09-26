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

//    public static File initHack(Context context) {
//        File hackFile = new File(context.getExternalFilesDir(""), "hack.dex");
//        FileOutputStream fos = null;
//        InputStream is = null;
//
//        try {
//            fos = new FileOutputStream(hackFile);
//            is = context.getAssets().open("hack.dex");
//            int len;
//            byte[] buffer = new byte[1024];
//            while ((len = is.read(buffer)) != -1) {
//                fos.write(buffer, 0, len);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (fos != null) {
//                try {
//                    fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        return hackFile;
//    }

    public static void install(Application application, File path) {

        ClassLoader classLoader = application.getClassLoader();

        if (!path.exists()) {
            return;
        }

        List<File> pathListFile = new ArrayList<>();
        pathListFile.add(path);

        // 兼容5.0以下  修改系统ClassLoader，使用我们自己的ClassLoader
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            try {
//                NewClassLoaderInjector.inject(application, classLoader, pathListFile);
//            } catch (Throwable throwable) {
//                throwable.printStackTrace();
//            }
//        } else {
            try {

                Field pathListField = ShareReflectUtil.findField(classLoader, "pathList");
                Object pathList = pathListField.get(classLoader);
                //3、反射获取pathList的dexElements对象 （oldElement）
                Field dexElementsField = ShareReflectUtil.findField(pathList, "dexElements");
                Object[] oldDexElements = (Object[]) dexElementsField.get(pathList);
                Method makePathElementsMethod = ShareReflectUtil.findMethod(pathList, "makePathElements",
                        List.class, File.class, List.class);
                ArrayList<IOException> ioExceptions = new ArrayList<>();
//                Log.d("TAG", "程序的PathClassLoader对象：" + classLoader.getClass());
//
//                Class<?> superclass = classLoader.getClass().getSuperclass();
//                Log.d("TAG", "父类的class对象为：" + superclass);
//                Field pathListField = superclass.getDeclaredField("pathList");
//                pathListField.setAccessible(true);
//                Object pathList = pathListField.get(classLoader);
//                Log.d("TAG", "pathList对象为：" + pathList);
//
//                Class<?> pathListClass = pathList.getClass();
//                Field dexElementsField = pathListClass.getDeclaredField("dexElements");
//                dexElementsField.setAccessible(true);
//                Object[] oldDexElements = (Object[]) dexElementsField.get(pathList);
//                Log.d("TAG", "oldDexElements对象为：" + Arrays.toString(oldDexElements));
//
//                Method makePathElementsMethod = pathListClass.getDeclaredMethod("makePathElements", List.class, File.class, List.class);
//                makePathElementsMethod.setAccessible(true);
//                Log.d("TAG", "makePathElementsMethod方法为：" + makePathElementsMethod);

                // 这里invoke的时候注意，如果该方法是静态方法，则obj可以传null，如果不是静态方法，obj必须是该方法的对象
                Object[] patchDexElements = (Object[]) makePathElementsMethod.invoke(null, pathListFile, application.getCacheDir(), ioExceptions);

                Object[] newDexElements = (Object[]) Array.newInstance(oldDexElements.getClass().getComponentType(), oldDexElements.length + patchDexElements.length);

                System.arraycopy(patchDexElements, 0, newDexElements, 0, patchDexElements.length);
                System.arraycopy(oldDexElements, 0, newDexElements, patchDexElements.length, oldDexElements.length);

                Log.d("TAG", "newDexElements对象为：" + Arrays.toString(newDexElements));
                dexElementsField.set(pathList, newDexElements);

                Log.d("TAG", "新的dexElementsField为 : " + pathList);

            } catch (Exception e) {
                e.printStackTrace();
            }
     //   }

    }
}
