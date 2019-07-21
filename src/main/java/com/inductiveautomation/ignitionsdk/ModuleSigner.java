package com.inductiveautomation.ignitionsdk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

@SuppressWarnings("restriction")
public class ModuleSigner {

    private final PrivateKey privateKey;
    private final InputStream chainInputStream;

    public ModuleSigner(PrivateKey privateKey, InputStream chainInputStream) {
        this.privateKey = privateKey;
        this.chainInputStream = chainInputStream;
    }

    public void signModule(File moduleFileIn, File moduleFileOut) throws IOException {
        signModule(System.out, moduleFileIn, moduleFileOut);
    }

    public void signModule(PrintStream printStream, File moduleFileIn, File moduleFileOut) throws IOException {
        /** Filename -> Base64-encoded SHA256withRSA asymmetric signature of file contents. */
        Properties signatures = new Properties();

        ZipMap zipMap = new ZipMap(moduleFileIn);

        for (String fileName : zipMap.keySet()) {
            ZipMapFile file = zipMap.get(fileName);
            if (!file.isDirectory()) {
                fileName = "/" + fileName;
                printStream.println("--- signing ---");
                printStream.println(fileName);

                try {
                    byte[] sig = asymmetricSignature(privateKey, file.getBytes());
                    String b64 = Base64.getEncoder().encodeToString(sig);

                    signatures.put(fileName, b64);

                    printStream.println("signature: " + Arrays.toString(sig));
                    printStream.println("signature_b64: " + b64);
                } catch (GeneralSecurityException e) {
                    throw new IOException("signing failed", e);
                }
            }
        }

        // Write out the signatures properties to the zip file
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        signatures.store(pw, null);
        pw.flush();
        pw.close();

        zipMap.put("signatures.properties", sw.toString().getBytes());

        // Write out the cert chain to the zip file
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(chainInputStream, bos);
        bos.flush();

        ZipMapFile certFile = new ZipMapFile(bos.toByteArray(), false);
        zipMap.put("certificates.p7b", certFile);

        // Finally, write out the full signed module
        zipMap.writeToFile(moduleFileOut);
    }


    private static byte[] asymmetricSignature(PrivateKey privateKey, byte[] bs) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);

        signature.update(bs);

        return signature.sign();
    }

    public static class Main {

        public static final String OPT_KEY_STORE = "keystore";
        public static final String OPT_KEY_STORE_PWD = "keystore-pwd";
        public static final String OPT_ALIAS = "alias";
        public static final String OPT_ALIAS_PWD = "alias-pwd";
        public static final String OPT_CHAIN = "chain";
        public static final String OPT_MODULE_IN = "module-in";
        public static final String OPT_MODULE_OUT = "module-out";
        public static final String OPT_PKCS11_CFG = "pkcs11-cfg";
        public static final String OPT_VERBOSE = "verbose";

        public static void main(String[] args) throws Exception {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(makeOptions(), args);

            KeyStore keyStore;
            String keyStorePwd = commandLine.getOptionValue(OPT_KEY_STORE_PWD, "");
            String alias = commandLine.getOptionValue(OPT_ALIAS);
            String aliasPwd = commandLine.getOptionValue(OPT_ALIAS_PWD, "");

            if (commandLine.hasOption(OPT_PKCS11_CFG)) {
                Provider p = Security.getProvider("SunPKCS11");
                p = p.configure(commandLine.getOptionValue(OPT_PKCS11_CFG));
                Security.addProvider(p);
                keyStore = KeyStore.getInstance("PKCS11");
                keyStore.load(null, keyStorePwd.toCharArray());
            } else {
                File keyStoreFile = new File(commandLine.getOptionValue(OPT_KEY_STORE));
                String keyStoreType = keyStoreFile.toPath().endsWith("pfx") ? "pkcs12" : "jks";

                keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(new FileInputStream(keyStoreFile), keyStorePwd.toCharArray());
            }

            Key privateKey = keyStore.getKey(alias, aliasPwd.toCharArray());

            if (privateKey == null || !privateKey.getAlgorithm().equalsIgnoreCase("RSA")) {
                System.out.println("no RSA PrivateKey found for alias '" + alias + "'.");
                System.exit(-1);
            }

            InputStream chainInputStream = new FileInputStream(commandLine.getOptionValue(OPT_CHAIN));

            File moduleIn = new File(commandLine.getOptionValue(OPT_MODULE_IN));
            File moduleOut = new File(commandLine.getOptionValue(OPT_MODULE_OUT));

            ModuleSigner moduleSigner = new ModuleSigner((PrivateKey) privateKey, chainInputStream);

            PrintStream printStream = commandLine.hasOption(OPT_VERBOSE) ?
                    System.out : new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM);

            moduleSigner.signModule(printStream, moduleIn, moduleOut);
        }

        private static Options makeOptions() {
            Option keyStore = Option.builder()
                    .longOpt(OPT_KEY_STORE)
                    .required(false)
                    .hasArg()
                    .build();

            Option keyStorePassword = Option.builder()
                    .longOpt(OPT_KEY_STORE_PWD)
                    .hasArg()
                    .build();

            Option alias = Option.builder()
                    .longOpt(OPT_ALIAS)
                    .required()
                    .hasArg()
                    .build();

            Option aliasPassword = Option.builder()
                    .longOpt(OPT_ALIAS_PWD)
                    .hasArg()
                    .build();

            Option chain = Option.builder()
                    .longOpt(OPT_CHAIN)
                    .required()
                    .hasArg()
                    .build();

            Option moduleIn = Option.builder()
                    .longOpt(OPT_MODULE_IN)
                    .required()
                    .hasArg()
                    .build();

            Option moduleOut = Option.builder()
                    .longOpt(OPT_MODULE_OUT)
                    .required()
                    .hasArg()
                    .build();

            Option pkcs11Cfg = Option.builder()
                    .longOpt(OPT_PKCS11_CFG)
                    .required(false)
                    .hasArg()
                    .build();

            Option verbose = Option.builder("v")
                    .longOpt(OPT_VERBOSE)
                    .required(false)
                    .build();

            return new Options()
                    .addOption(keyStore)
                    .addOption(keyStorePassword)
                    .addOption(alias)
                    .addOption(aliasPassword)
                    .addOption(chain)
                    .addOption(moduleIn)
                    .addOption(moduleOut)
                    .addOption(pkcs11Cfg)
                    .addOption(verbose);
        }

    }

}
