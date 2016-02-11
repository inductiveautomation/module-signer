package com.inductiveautomation.ignitionsdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Copied from SRCommon. The ZipMap provides a convenient way of working with zip files by providing a {@link Map} that
 * is backed by a zip file.
 * <p/>
 * A variety of constructors are available for creating the ZipMap. Once loaded, you can work with file objects as you
 * would work with any normal map. To save changes, you must call writeToFile.
 *
 * @author Kevin
 */
public class ZipMap implements Map<String, ZipMapFile> {

    private Map<String, ZipMapFile> fileMap;

    /**
     * Creates an empty ZipMap. Changes need to be explicitly written using {@link #writeToFile(File)} or {@link
     * #writeToFile(OutputStream)}.
     */
    public ZipMap() {
        fileMap = new HashMap<String, ZipMapFile>();
    }

    /**
     * Create a ZipMap with the contents of the given zip file. Changes need to be explicitly written using {@link
     * #writeToFile(File)} or {@link #writeToFile(OutputStream)}.
     *
     * @param zipFile
     * @throws IOException
     */
    public ZipMap(File zipFile) throws IOException {
        this(new FileInputStream(zipFile));
    }

    /**
     * Create a ZipMap with the contents of the given zip file. Changes need to be explicitly written using {@link
     * #writeToFile(File)} or {@link #writeToFile(OutputStream)}.
     *
     * @param zipFile
     * @throws IOException
     */
    public ZipMap(InputStream zipFile) throws IOException {
        fileMap = new HashMap<String, ZipMapFile>();

        ZipInputStream zipIn = null;

        try {
            zipIn = new ZipInputStream(zipFile);
            ZipEntry entry = null;

            byte[] bytes = new byte[4096];

            while ((entry = zipIn.getNextEntry()) != null) {

                String entryName = entry.getName();

                ByteArrayOutputStream bOut = new ByteArrayOutputStream();

                int bytesRead;
                while ((bytesRead = zipIn.read(bytes)) >= 0) {
                    bOut.write(bytes, 0, bytesRead);
                }
                bOut.close();

                final byte[] fileBytes = bOut.toByteArray();
                final boolean directory = entry.isDirectory();

                fileMap.put(entryName, new ZipMapFile(fileBytes, directory));
            }
        } finally {
            if (zipIn != null) {
                try {
                    zipIn.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Write the contents of this ZipMap to a zip file using the supplied File.
     *
     * @param outputFile
     * @throws IOException
     */
    public void writeToFile(File outputFile) throws IOException {
        writeToFile(new FileOutputStream(outputFile));
    }

    /**
     * Write the contents of this ZipMap to a zip file using the supplied OutputStream.
     *
     * @param outputStream
     * @throws IOException
     */
    public void writeToFile(OutputStream outputStream) throws IOException {
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(outputStream);

            byte[] buffer = new byte[4096];

            for (Entry<String, ZipMapFile> file : fileMap.entrySet()) {
                String name = file.getKey();
                ZipMapFile zipMapFile = file.getValue();
                ZipEntry zipEntry = new ZipEntry(name);

                zos.putNextEntry(zipEntry);

                ByteArrayInputStream bytesIn = new ByteArrayInputStream(zipMapFile.getBytes());

                int length;
                while ((length = bytesIn.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();
                bytesIn.close();
            }
        } finally {
            if (zos != null) {
                zos.close();
            }
        }
    }

    public void clear() {
        fileMap.clear();
    }

    public boolean containsKey(Object key) {
        return fileMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return fileMap.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, ZipMapFile>> entrySet() {
        return fileMap.entrySet();
    }

    public ZipMapFile get(Object key) {
        return fileMap.get(key);
    }

    public boolean isEmpty() {
        return fileMap.isEmpty();
    }

    public Set<String> keySet() {
        return fileMap.keySet();
    }

    /**
     * Puts an entry into the ZipMap. Directory names should end with a "/".
     */
    public ZipMapFile put(String key, ZipMapFile value) {
        return fileMap.put(key, value);
    }

    /**
     * Convenience method for putting a file into the ZipMap.
     *
     * @param key   Name of the file. Example: "hello.txt" or "dir/world.txt".
     * @param value The file itself. A ZipMapFile will be created from this File.
     * @return The ZipMapFile added to the ZipMap.
     */
    public ZipMapFile put(String key, File value) {
        return fileMap.put(key, new ZipMapFile(value));
    }

    /**
     * Convenience method for putting a file into the ZipMap.
     *
     * @param key   Name of the file. Example: "hello.txt" or "dir/world.txt".
     * @param value The byte[] itself. A ZipMapFile will be created from this byte[] with directory=false.
     * @return The ZipMapFile added to the ZipMap.
     */
    public ZipMapFile put(String key, byte[] value) {
        return fileMap.put(key, new ZipMapFile(value, false));
    }

    public void putAll(Map<? extends String, ? extends ZipMapFile> t) {
        fileMap.putAll(t);
    }

    public ZipMapFile remove(Object key) {
        return fileMap.remove(key);
    }

    public int size() {
        return fileMap.size();
    }

    public Collection<ZipMapFile> values() {
        return fileMap.values();
    }
}
