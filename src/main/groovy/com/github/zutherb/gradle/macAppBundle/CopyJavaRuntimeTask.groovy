package com.github.zutherb.gradle.macAppBundle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


class CopyJavaRuntimeTask extends DefaultTask {

    static final String JRE_HOME = '/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/';

    @TaskAction
    def void writeStub() {
        def dest = project.file("${project.buildDir}/${project.macAppBundle.appOutputDir}/${project.macAppBundle.appName}.app/Contents/Resources/jre")
        dest.parentFile.mkdirs()
        copyFolder(JRE_HOME, dest)
    }

    def void copyFolder(File src, File dest) {

        if (src.isDirectory()) {

            //if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir();
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            def files = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                copyFolder(srcFile, destFile);
            }

        } else {
            //if file, then copy it
            //Use bytes stream to support all file types
            def inStream = new FileInputStream(src);
            def outStream = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }

            inStream.close();
            outStream.close();
        }
    }
}
