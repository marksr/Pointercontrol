package br.com.marksr.subprocess;


public interface TextStreamPublisher {
    void setOnTextListener(TextStreamListener l);
    void removeOnTextListener(TextStreamListener l);
}
