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

## Usage Tips For Self-Signed Certs

These steps assume that you have a JDK and OpenSSL installed. Done on OS X, but should be cross platform. You will also need Keystore Explorer. When you run 
Keystore Explorer for the first time, you may be prompted to upgrade to unlimited strength Java crypto.

To create a cert for self-signed modules, the process is roughly:

1. Make a module and create a .modl file
2. Create a keystore and certificate
3. Open a command prompt, Change to the directory where you would like your keystore.jks file to be saved
4. Type `keytool -genkey -alias server -keyalg RSA -keysize 2048 -keystore keystore.jks`. You will be prompted for other information needed to complete the cert. The start date of the certificate will always be today, but the end date can be changed with the -validity flag. -validity 20 will make it valid for 20 days.
5. Open your newly created keystore.jks file with Keystore Explorer. Right-click on the server alias, and choose Export > Export Certificate Chain. In the Export dialog, change Certificate Length to Entire Chain. The export format should be PKCS#7 and PEM should be checked.

With a certificate to sign with, you can now sign your module using the invocation above.

### Signing a Module in Intellij using Module Signer from Source Code

To sign a module using this Module-Signer project in Intellij:

Within an IDE, set the _main_ program to com.inductiveautomation.ignitionsdk.ModuleSigner.Main. The output will be the self-signed module. For the example above, the program arguments to configure in the IDE Run Configuration's VM Options are:

```shell
-keystore=<pathToMyJKS>/keystore.jks
-keystore-pwd=<password>
-alias=server
-alias-pwd=<password>
-chain=<pathToMyp7b>/cert.p7b
-module-in=<pathToMyModule>/MyModule_unsigned.modl
-module-out=<pathToMyModule>/MyModule.modl
```
This self-signed module can now be installed in Ignition.
