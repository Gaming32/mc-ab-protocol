package io.github.gaming32.mcab.and_beyond.streams;

import java.io.IOException;
import java.io.InputStream;

public class EncryptedInputStream extends InputStream {
    protected final InputStream next;
    protected final byte[] key;
    protected final int mod;
    protected int i, markOffset;

    public EncryptedInputStream(InputStream next, byte[] key) {
        this.next = next;
        this.key = key;
        this.mod = key.length - 1;
        this.i = 0;
    }

    @Override
    public int available() throws IOException {
        return next.available();
    }

    @Override
    public void close() throws IOException {
        next.close();
    }

    @Override
    public void mark(int readlimit) {
        next.mark(readlimit);
        markOffset = 0;
    }

    @Override
    public boolean markSupported() {
        return next.markSupported();
    }

    @Override
    public void reset() throws IOException {
        next.reset();
        this.i = (i - markOffset) & mod;
    }

    @Override
    public int read() throws IOException {
        int b = (next.read() - key[i]) & 255;
        i = (i - 1) & mod;
        markOffset++;
        return b;
    }

    @Override
    public long skip(long n) throws IOException {
        return next.skip(n);
    }
}
