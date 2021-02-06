package me.crespel.runtastic.util;

import java.io.PrintStream;
import java.io.IOException; // Import the IOException class to handle errors
import java.io.OutputStream;

public class TeePrintStream extends PrintStream {
    private final PrintStream second;

    public TeePrintStream(OutputStream main, PrintStream second) {
        super(main);
        this.second = second;
    }

    /**
     * Closes the main stream. 
     * The second stream is just flushed but <b>not</b> closed.
     * @see java.io.PrintStream#close()
     */
    @Override
    public void close() {
        // just for documentation
        super.close();
    }

    @Override
    public void flush() {
        super.flush();
        if( second != null ) {
            second.flush();
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        if( second != null ) {
            second.write(buf, off, len);
        }
    }

    @Override
    public void write(int b) {
        super.write(b);
        if( second != null ) {
            second.write(b);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
        if( second != null ) {
            second.write(b);
        }
    }
}
