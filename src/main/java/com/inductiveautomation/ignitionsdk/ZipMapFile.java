package com.inductiveautomation.ignitionsdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by mattg on 2/10/16. Copied from SRCommon.
 */
public class ZipMapFile {

    private byte[] bytes;
    private boolean directory;

    public ZipMapFile(File file) {
        this(getBytes(file), file.isDirectory());
    }

    public ZipMapFile(byte[] bytes, boolean directory) {
        this.bytes = bytes;
        this.directory = directory;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean isDirectory() {
        return directory;
    }

    private static byte[] getBytes(File file) {
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);

            long length = file.length();

            if (length > Integer.MAX_VALUE) {
                // ?
                return new byte[]{};
            }

            byte[] bytes = new byte[(int) length];

            fis.read(bytes);

            return bytes;
        } catch (Exception e) {
            return new byte[]{};
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
