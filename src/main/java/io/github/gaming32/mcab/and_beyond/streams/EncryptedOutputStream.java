package io.github.gaming32.mcab.and_beyond.streams;

import java.io.IOException;
import java.io.OutputStream;

public class EncryptedOutputStream extends OutputStream {
    protected final OutputStream next;
    protected final byte[] key;
    protected final int mod;
    protected int i;

    public EncryptedOutputStream(OutputStream next, byte[] key) {
        this.next = next;
        this.key = key;
        this.i = 0;
        this.mod = key.length - 1;
    }

    @Override
    public void close() throws IOException {
        next.close();
    }

    @Override
    public void flush() throws IOException {
        next.flush();
    }

    @Override
    public void write(int b) throws IOException {
        next.write((b + key[i]) & 255);
        i = (i + 1) & mod;
    }
}
