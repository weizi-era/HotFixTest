package com.example.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.api.ApplicationVariant;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PatchPlugin implements Plugin<Project> {

    private Project project;
    @Override
    public void apply(Project project) {
        this.project = project;

        if (!project.getPlugins().hasPlugin(AppPlugin.class)) {
            throw new GradleException("必须结合android application插件使用热修复插件！");
        }

        project.getExtensions().create("patch", PluginExt.class);

        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                PluginExt pluginExt = project.getExtensions().findByType(PluginExt.class);
                AppExtension android = project.getExtensions().getByType(AppExtension.class);

                android.getApplicationVariants().all(new Action<ApplicationVariant>() {
                    @Override
                    public void execute(ApplicationVariant applicationVariant) {

                        if (applicationVariant.getName().equals("debug") && !pluginExt.isDebugOn()) {
                            return;
                        }

                        configTask(applicationVariant, pluginExt);
                    }
                });
            }
        });
    }

    void configTask(ApplicationVariant applicationVariant, PluginExt pluginExt) {

        // debug/release
        String name = applicationVariant.getName();

        File outputDir;
        String output = pluginExt.getOutput();
        if (!Utils.isEmpty(output)) {
            outputDir = new File(output, name);
        } else {
            outputDir = new File(project.getBuildDir(), "patch/" + name);
        }
        outputDir.mkdirs();

        // 类md5值的缓存文件
        File hexFile = new File(outputDir, "hex.txt");
        // 缓存的md5数据
        Map<String, String> oldHex = Utils.readHex(hexFile);
        Map<String, String> newHex = new HashMap<>();

        // Debug/Release
        String capitalizeName = Utils.capitalize(name);
        Task dexBuild = project.getTasks().findByName("dexBuilderDebug");
        //  在这个任务执行之前执行我们的Action，也就是在产生dex之前
        dexBuild.doFirst(new Action<Task>() {
            @Override
            public void execute(Task task) {
                // 目录名
                String dirName = applicationVariant.getName();
                // 拿到的是所有要打包成dex的class与jar包
                FileCollection files = task.getInputs().getFiles();
                for (File file: files) {
                    String filePath = file.getAbsolutePath();
                    if (filePath.endsWith(".jar")) {
                        processJar(file);
                    } else if (filePath.endsWith(".class")) {
                        processClass(file, newHex, dirName);
                    }
                }

                // 没有缓存
                if (oldHex.isEmpty()) {
                    return;
                }

                for (String className : newHex.keySet()) {
                    // 如果缓存中找不到，就是新文件
                    // 如果缓存中找到了，开始比较md5值
                    //      md5值一致：不管
                    //      md5值不一致：打包进补丁包
                    String md5 = oldHex.get(className);
                    if (Utils.isEmpty(md5)) {
                        // 新文件
                    } else if (!md5.equals(newHex.get(className))) {
                        // 改动过

                    }
                }
            }
        });
    }

    private void processClass(File file, Map<String, String> newHex, String dirName) {
        byte[] bytes = Utils.readFile(file);
        // 1.比较缓存的md5值
        // 2.缓存class的md5值到hex.txt
        String hex = Utils.hex(bytes);

        // F:/xxx/xxx/xxx
        String filePath = file.getAbsolutePath();
        // 去掉目录名
        String className = filePath.split(dirName)[1].substring(1);

        newHex.put(className, hex);
    }

    private void processJar(File file) {

    }
}
