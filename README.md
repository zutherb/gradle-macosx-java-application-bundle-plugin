Introduction
============

The gradle-macappbundle plugin creates Mac OSX .app applications from within Gradle. The runSetFile and createDmg tasks
will probably only run on under OSX as they use the SetFile and hdiutil applications. It is a fork of the Google code
project [gradle-macappbundle](http://code.google.com/p/gradle-macappbundle/wiki/Intro) which does not support Java 7+
yet. The core plugin was developed by crotwell@seis.sc.edu. In the implementation the JavaAppStub was replaced by the
JavaAppLauncher which is provided by [Oracle](http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/packagingAppsForMac.html).

Tasks
=====

There are 8 tasks:

* configMacApp - configures defaults
* generatePlist - creates the Info.plist file
* generatePkgInfo - create the PkgInfo file
* copyToResourcesJava - copies the jars into the .app
* copyJavaAppLauncher - copies the JavaAppLauncher used to start Java to the .app
* copyJavaRuntime - copies the Java Runtime used to start the .app
* runSetFile - runs SetFile to toggle the magic bit (not run by default)
* createApp - empty task that depends on the above
* codeSign - digital signature using codesign
* createDmg - creates a .dmg disk image containing the app

An example configuration within your build.gradle might look like:

```groovy
apply plugin: 'macAppBundle'

macAppBundle {
    mainClassName = "com.example.myApp.Start"
    icon = "myIcon.icns"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.github.zutherb.gradle:gradle-macappbundle:0.1'
    }
}
```

Configuration
=============

The configuration for the tasks is done via the Extension mechanism in Gradle. The MacAppBundlePluginExtension source
code shows the list of items that can be configured. Most map in a straightforward way into the values used in the Mac
OSX Application bundle. See [Apple's documentation](https://developer.apple.com/library/mac/documentation/java/conceptual/java14development/03-javadeployment/javadeployment.html)
for more information on .app directory structure.
