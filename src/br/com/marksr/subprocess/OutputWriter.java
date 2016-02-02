package br.com.marksr.subprocess;

import java.io.IOException;

public interface OutputWriter {
    void stdOutputWrite(String data) throws IOException;
    void stdOutputFlush() throws IOException;
}
