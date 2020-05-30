# Getting started with module-signer

## Prerequisites

* Java 11 installed and on your path.
* A Java Keystore (in jks or pfx format) containing either:
  * A self-generated and self-signed code signing certificate.
  * A code signing certificate, obtained from and signed by a CA, and the certificate chain that goes with it.

[Keystore Explorer](http://keystore-explorer.sourceforge.net/downloads.php) is an easy to use tool for creating and managing keystores and certificates.

## Invocation

Invocation from the command-line:
```
java -jar module-signer.jar \ 
	-keystore=<path-to-my-keystore>/keystore.jks \
	-keystore-pwd=<password> \
	-alias=server \
	-alias-pwd=<password> \
	-chain=<pathToMyp7b>/cert.p7b \
	-module-in=<path-to-my-module>/my-unsigned-module.modl \
	-module-out=<path-to-my-module>/my-signed-module.modl
```

## Parameters Explained

### keystore
The path to the keystore containing your code signing certificate. Can be either JKS or PFX format.

### keystore-pwd
The password to access the keystore.

### alias
The alias under which your code signing certificate is stored.

### alias-pwd
The password to access the alias.

### chain
The path to the certificate chain (in p7b format). This file will is generally returned along with your signed certificate after submitting a CSR to a CA.

### module-in
The path to the unsigned module.

### module-out
The path the signed module will be written to.


## Using as a Library via Maven

This library has been built and published to the Inductive Automation artifact repository.  To use this as a dependency in a JVM project, simply add the IA repo to your artifact repositories, and then add as a typical maven dependency.


#### Consuming Via Maven POM

```
    // in pom.xml

    <repositories>
        <repository>
            <id>ia-releases</id>
            <releases>true</releases>
            <snapshots>false</snapshots>
            <url>https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/</url>
        </repository>
    </repositories>
    </project>

    <dependencies>
        <dependency>
            <groupId>com.inductiveautomation.ignitionsdk</groupId>
            <artifactId>module-signer</artifactId>
            <version>YOUR_DESIRED_VERSION</version>
        </dependency>
    </dependencies>
```

#### Consuming Via Gradle buildscript

```
    // in build.gradle
    repositories {
        maven { url "https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/" }
    }

    dependencies {
        implementation("com.inductiveautomation.ignitionsdk:module-signer:YOUR_DESIRED_VERSION")
    }

```


## Artifact Publishing

To publish a copy of this artifact to a private repo, you'll need to configure your server settings as described in the [Maven deploy documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html). Specify the repo urls by directly editing the POM file and setting the urls.  Alternatively, you may set the properties through an active profile in your settings.xml file (generally located at `${user.home}/.m2/settings.xml`) as demonstrated below. 
  
Then simply execute `mvn deploy` to publish to your private repository.  For development and testing, `mvn install` will install the assembled artifact to your local maven .m2 repository.
 

```
<settings>
<profiles>
    <profile>
        <id>inject-repo-url</id>
        <properties>
            <snapshotsRepoUrl>http://your-artifact-host/repository/snapshots/</snapshotsRepoUrl>
            <releasesRepoUrl>http://your-artifact-host/repository/releases/</releasesRepoUrl>
        </properties>
    </profile>
</profiles>
<activeProfiles>
    <activeProfile>inject-repo-url</activeProfile>
</activeProfiles>

// ...

</settings>

```

