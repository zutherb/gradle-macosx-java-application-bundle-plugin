package com.github.zutherb.gradle.macAppBundle

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync

class MacAppBundlePlugin implements Plugin<Project> {

    static final String JRE_HOME = '/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/';

    static final String PLUGIN_NAME = "macAppBundle"
    static final String GROUP = PLUGIN_NAME

    static final String TASK_CONFIGURE_NAME = "configMacApp"
    static final String TASK_INFO_PLIST_GENERATE_NAME = "generatePlist"
    static final String TASK_PKG_INFO_GENERATE_NAME = "generatePkgInfo"

    static final String TASK_LIB_COPY_NAME = "copyToResourcesJava"
    static final String TASK_COPY_JAVA_APP_LAUNCHER_NAME = "copyJavaAppLauncher"
    static final String TASK_COPY_JAVA_RUNTIME = "copyJavaRuntime"
    static final String TASK_COPY_ICON_NAME = "copyIcon"
    static final String TASK_SET_FILE_NAME = "runSetFile"
    static final String TASK_CREATE_APP_NAME = "createApp"
    static final String TASK_CODE_SIGN_NAME = "codeSign"
    static final String TASK_CREATE_DMG = "createDmg"

    def void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        MacAppBundlePluginExtension pluginExtension = new MacAppBundlePluginExtension()
        project.extensions.macAppBundle = pluginExtension

        Task configTask = configurationTask(project)
        Task plistTask = infoPlistTask(project)
        plistTask.dependsOn(configTask)
        Task copyTask = copyToLibTask(project)
        copyTask.dependsOn(configTask)
        Task stubTask = copyJavaAppLauncherTask(project)
        stubTask.dependsOn(configTask)
        Task copyIconTask = copyIconTask(project)
        copyIconTask.dependsOn(configTask)
        Task copyJavaRuntimeTask = copyJavaRuntimeTask(project)
        copyJavaRuntimeTask.dependsOn(configTask)
        Task pkgInfoTask = pkgInfoTask(project)
        pkgInfoTask.dependsOn(configTask)
        Task createAppTask = appTask(project)
        createAppTask.dependsOn(plistTask)
        createAppTask.dependsOn(copyTask)
        createAppTask.dependsOn(copyJavaRuntimeTask)
        createAppTask.dependsOn(stubTask)
        createAppTask.dependsOn(copyIconTask)
        createAppTask.dependsOn(pkgInfoTask)
        Task setFileTask = setFileTask(project)
        createAppTask.dependsOn(setFileTask)
        Task codeSignTask = codeSignTask(project)
        codeSignTask.dependsOn(createAppTask)
        Task dmgTask = dmgTask(project)
        dmgTask.dependsOn(createAppTask)
        project.getTasksByName("assemble", true).each { t -> t.dependsOn(dmgTask) }
    }

    private static Task configurationTask(Project project) {
        Task task = project.tasks.create(TASK_CONFIGURE_NAME)
        task.description = "Sets default configuration values for the extension."
        task.group = GROUP
        task.doFirst {
            project.macAppBundle.configureDefaults(project)
        }
        return task
    }

    private static Task infoPlistTask(Project project) {
        Task task = project.tasks.create(TASK_INFO_PLIST_GENERATE_NAME, GenerateInfoPlistTask)
        task.description = "Creates the Info.plist configuration file inside the mac osx .app directory."
        task.group = GROUP
        task.inputs.property("project version", project.version)
        task.inputs.property("MacAppBundlePlugin extension", { project.macAppBundle })
        task.outputs.file(project.file(project.macAppBundle.getPlistFileForProject(project)))
        return task
    }

    private static Task copyToLibTask(Project project) {
        Sync task = project.tasks.create(TASK_LIB_COPY_NAME, Sync)
        task.description = "Copies the project dependency jars in the Contents/Resorces/Java directory."
        task.group = GROUP
        task.with configureDistSpec(project)
        task.into { project.file("${project.buildDir}/${project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app/Contents/Resources/Java") }
        return task
    }

    private static Task copyJavaAppLauncherTask(Project project) {
        Task task = project.tasks.create(TASK_COPY_JAVA_APP_LAUNCHER_NAME, CopyJavaAppLauncherTask)
        task.description = "Copies the JavaAppLauncher into the Contents/MacOS directory."
        task.group = GROUP
        task.doLast { ant.chmod(dir: project.file("${project.buildDir}/${project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app/Contents/MacOS"), perm: "755", includes: "*") }
        task.inputs.property("bundle executable name", { project.macAppBundle.bundleExecutable })
        task.outputs.file("${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app/Contents/MacOS/${project.macAppBundle.bundleExecutable}")
        return task
    }

    private static Task copyIconTask(Project project) {
        Task task = project.tasks.create(TASK_COPY_ICON_NAME, Copy)
        task.description = "Copies the icon into the Contents/MacOS directory."
        task.group = GROUP
        task.from "${-> project.macAppBundle.icon}"
        task.into "${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app/Contents/Resources"
        return task
    }

    private static Task copyJavaRuntimeTask(Project project) {
        Task task = project.tasks.create(TASK_COPY_JAVA_RUNTIME, Copy)
        task.description = "Copies the java runtime into the Contents/Resources directory."
        task.group = GROUP
        task.from "${-> JRE_HOME}"
        task.into "${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app/Contents/Resources/Jre"
        task.exclude "bin/",
                "lib/deploy/",
                "lib/deploy.jar",
                "lib/javaws.jar",
                "lib/libdeploy.dylib",
                "lib/libnpjp2.dylib",
                "lib/plugin.jar",
                "lib/security/javaws.policy"
        return task
    }

    private static Task pkgInfoTask(Project project) {
        Task task = project.tasks.create(TASK_PKG_INFO_GENERATE_NAME, PkgInfoTask)
        task.description = "Creates the Info.plist configuration file inside the mac osx .app directory."
        task.group = GROUP
        task.inputs.property("creator code", { project.macAppBundle.creatorCode })
        task.outputs.file(project.macAppBundle.getPkgInfoFileForProject(project))
        return task
    }

    private static Task setFileTask(Project project) {
        def task = project.tasks.create(TASK_SET_FILE_NAME, Exec)
        task.description = "Runs SetFile to toggle the magic bit on the .app (probably not needed)"
        task.group = GROUP
        task.doFirst {
            workingDir = project.file("${project.buildDir}/${project.macAppBundle.appOutputDir}")
            commandLine "${-> project.macAppBundle.setFileCmd}", "-a", "B", "${-> project.macAppBundle.appName}.app"
        }
        task.inputs.dir("${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app")
        task.outputs.dir("${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app")
        return task
    }

    private static Task codeSignTask(Project project) {
        def task = project.tasks.create(TASK_CODE_SIGN_NAME, Exec)
        task.description = "Runs codesign on the .app (not required)"
        task.group = GROUP
        task.doFirst {
            if (!project.macAppBundle.certIdentity) {
                throw new InvalidUserDataException("No value has been specified for property certIdentity")
            }
            workingDir = project.file("${project.buildDir}/${project.macAppBundle.appOutputDir}")
            commandLine "${-> project.macAppBundle.codeSignCmd}", "-s", "${-> project.macAppBundle.certIdentity}", "-f", "${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app"
            if (project.macAppBundle.keyChain) {
                commandLine << "--keychain" << "${-> project.macAppBundle.keyChain}"
            }
        }
        task.inputs.dir("${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app")
        task.outputs.dir("${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app")
        return task
    }

    private static Task dmgTask(Project project) {
        def task = project.tasks.create(TASK_CREATE_DMG, Exec)
        task.description = "Create a dmg containing the .app"
        task.group = GROUP
        task.doFirst {
            workingDir = project.file("${project.buildDir}/${project.macAppBundle.dmgOutputDir}")
            commandLine "hdiutil", "create", "-srcfolder",
                    project.file("${project.buildDir}/${project.macAppBundle.appOutputDir}"),
                    "-volname", "${-> project.macAppBundle.volumeName}",
                    "${-> project.macAppBundle.dmgName}"
            def dmgFile = project.file("${project.buildDir}/${project.macAppBundle.dmgOutputDir}/${-> project.macAppBundle.dmgName}.dmg")
            if (dmgFile.exists()) dmgFile.delete()
        }
        task.inputs.dir("${-> project.buildDir}/${-> project.macAppBundle.appOutputDir}/${-> project.macAppBundle.appName}.app")
        task.outputs.file("${-> project.buildDir}/${project.macAppBundle.dmgOutputDir}/${-> project.macAppBundle.dmgName}.dmg")
        task.doFirst { task.outputs.files.each { it.delete() } }
        task.doFirst { project.file("${-> project.buildDir}/${project.macAppBundle.dmgOutputDir}").mkdirs() }
        return task
    }

    private static Task appTask(Project project) {
        def task = project.tasks.create(TASK_CREATE_APP_NAME)
        task.description = "Placeholder task for tasks relating to creating .app applications"
        task.group = GROUP
        return task
    }

    private static CopySpec configureDistSpec(Project project) {
        CopySpec distSpec = project.copySpec {}
        def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]

        distSpec.with {
            from(jar)
            from(project.configurations.runtime)
        }

        return distSpec
    }
}





