package com.inductiveautomation.ignitionsdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ModuleSigner {

    private final RSAPrivateKey privateKey;
    private final InputStream chainInputStream;

    public ModuleSigner(RSAPrivateKey privateKey, InputStream chainInputStream) {
        this.privateKey = privateKey;
        this.chainInputStream = chainInputStream;
    }

    public void signModule(File moduleFileIn, File moduleFileOut) throws IOException {
        /** Filename -> Base64-encoded SHA-256 hash of file contents. */
        Properties signatures = new Properties();

        try (FileSystem zfsIn = zipFileSystem(moduleFileIn);
             FileSystem zfsOut = zipFileSystem(moduleFileOut)) {

            Path root = zfsIn.getPath("/");

            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String filename = file.toString();

                        byte[] fbs = Files.readAllBytes(file);
                        byte[] sha = sha256(privateKey, fbs);
                        String b64 = Base64.getEncoder().encodeToString(sha);

                        Files.copy(
                                file, zfsOut.getPath(filename),
                                StandardCopyOption.REPLACE_EXISTING);

                        signatures.put(filename, b64);

                        return super.visitFile(file, attrs);
                    } catch (GeneralSecurityException e) {
                        throw new IOException("signing failed", e);
                    }
                }
            });

            OutputStream signaturesOutputStream = zfsOut.provider().newOutputStream(
                    zfsOut.getPath("/signatures.properties"), StandardOpenOption.CREATE_NEW);

            signatures.store(signaturesOutputStream, null);
            signaturesOutputStream.flush();
            signaturesOutputStream.close();

            Files.copy(chainInputStream, zfsOut.getPath("/certificates.p7b"));
        }
    }


    private static byte[] sha256(PrivateKey privateKey, byte[] bs) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);

        signature.update(bs);

        return signature.sign();
    }

    private static FileSystem zipFileSystem(File file) throws IOException {
        URI uri = URI.create("jar:file:" + file.getAbsolutePath());

        Map<String, String> env = new HashMap<>();
        if (!file.exists()) {
            env.put("create", "true");
        }

        return FileSystems.newFileSystem(uri, env);
    }

    public static class Main {

        public static final String OPT_KEY_STORE = "keystore";
        public static final String OPT_KEY_STORE_PWD = "keystore-pwd";
        public static final String OPT_ALIAS = "alias";
        public static final String OPT_ALIAS_PWD = "alias-pwd";
        public static final String OPT_CHAIN = "chain";
        public static final String OPT_MODULE_IN = "module-in";
        public static final String OPT_MODULE_OUT = "module-out";

        public static void main(String[] args) throws Exception {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(makeOptions(), args);

            File keyStoreFile = new File(commandLine.getOptionValue(OPT_KEY_STORE));
            String keyStorePwd = commandLine.getOptionValue(OPT_KEY_STORE_PWD, "");
            String keyStoreType = keyStoreFile.toPath().endsWith("pfx") ? "pfx" : "jks";

            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(new FileInputStream(keyStoreFile), keyStorePwd.toCharArray());

            String alias = commandLine.getOptionValue(OPT_ALIAS);
            String aliasPwd = commandLine.getOptionValue(OPT_ALIAS_PWD, "");
            Key privateKey = keyStore.getKey(alias, aliasPwd.toCharArray());

            if (!(privateKey instanceof RSAPrivateKey)) {
                System.out.println("no RSAPrivateKey found for alias '" + alias + "'.");
                System.exit(-1);
            }

            InputStream chainInputStream = new FileInputStream(commandLine.getOptionValue(OPT_CHAIN));

            File moduleIn = new File(commandLine.getOptionValue(OPT_MODULE_IN));
            File moduleOut = new File(commandLine.getOptionValue(OPT_MODULE_OUT));

            ModuleSigner moduleSigner = new ModuleSigner((RSAPrivateKey) privateKey, chainInputStream);

            moduleSigner.signModule(moduleIn, moduleOut);
        }

        private static Options makeOptions() {
            Option keyStore = Option.builder().longOpt(OPT_KEY_STORE).required().hasArg().build();
            Option keyStorePassword = Option.builder().longOpt(OPT_KEY_STORE_PWD).hasArg().build();
            Option alias = Option.builder().longOpt(OPT_ALIAS).required().hasArg().build();
            Option aliasPassword = Option.builder().longOpt(OPT_ALIAS_PWD).hasArg().build();
            Option chain = Option.builder().longOpt(OPT_CHAIN).required().hasArg().build();
            Option moduleIn = Option.builder().longOpt(OPT_MODULE_IN).required().hasArg().build();
            Option moduleOut = Option.builder().longOpt(OPT_MODULE_OUT).required().hasArg().build();

            return new Options()
                    .addOption(keyStore)
                    .addOption(keyStorePassword)
                    .addOption(alias)
                    .addOption(aliasPassword)
                    .addOption(chain)
                    .addOption(moduleIn)
                    .addOption(moduleOut);
        }

    }

}
