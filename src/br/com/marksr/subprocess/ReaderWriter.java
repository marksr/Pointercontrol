package br.com.marksr.subprocess;

import br.com.marksr.pointercontrol.commons.GenericPublisher;

import java.io.IOException;
import java.io.OutputStreamWriter;


public abstract class ReaderWriter implements
        TextStreamPublisher,
        ServiceStatusPublisher,
        OutputWriter,
        AsyncReaderPost {
    protected GenericPublisher<TextStreamListener, String> mTextStream;
    protected GenericPublisher<ServiceStatusListener, Boolean> mServiceStatus;
    protected String TAG = this.getClass().getSimpleName();
    protected OutputStreamWriter mOutputStream;
    protected AsyncReader mStdinAsyncReader;
    protected boolean mReady;

    protected ReaderWriter() {
        mTextStream = new GenericPublisher<TextStreamListener, String>() {
            @Override
            public void action(TextStreamListener l, String d) {
                l.onText(d);
            }
        };
        mServiceStatus = new GenericPublisher<ServiceStatusListener, Boolean>() {
            @Override
            public void action(ServiceStatusListener l, Boolean d) {
                if (d)
                    l.onStart();
                else
                    l.onStop();
            }
        };
    }

    public abstract void create();

    public abstract void destroy();

    public boolean isReady() {
        return mReady;
    }

    @Override
    public void setOnTextListener(TextStreamListener l) {
        mTextStream.setOnEvent(l);
    }

    @Override
    public void removeOnTextListener(TextStreamListener l) {
        mTextStream.removeOnEvent(l);
    }

    @Override
    public void setOnStatusChangeListener(ServiceStatusListener l) {
        mServiceStatus.setOnEvent(l);
    }

    @Override
    public void removeOnStatusChangeListener(ServiceStatusListener l) {
        mServiceStatus.removeOnEvent(l);
    }

    @Override
    public void stdOutputWrite(String data) throws IOException {
        if (mReady)
            mOutputStream.write(data);
    }

    @Override
    public void stdOutputFlush() throws IOException {
        if (mReady)
            mOutputStream.flush();
    }

    @Override
    public void post(String data) {
        mTextStream.fire(data);
    }

    public void sendCommand(String cmd) {
        try {
            stdOutputWrite(cmd + "\n");
            stdOutputFlush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String sendCommandResult(String cmd, int stopWhenInativeFor) {

        TextStreamListener listener = new TextStreamListener() {
            private String buffer = "";

            @Override
            public void onText(String data) {
                buffer += data;
            }

            @Override
            public String toString() {
                return buffer;
            }
        };    
        setOnTextListener(listener);
        sendCommand(cmd);
        waitAtivityFor(stopWhenInativeFor);
        waitInativityFor(stopWhenInativeFor);
        removeOnTextListener(listener);
        return listener.toString();
    }           

    public void waitInativityFor(int miliseconds) {
        mStdinAsyncReader.waitInativityFor(miliseconds);
    }

    public void waitAtivityFor(int miliseconds) {
        mStdinAsyncReader.waitAtivityFor(miliseconds);
    }

    public boolean checkForInativity(int miliseconds) {
        return mStdinAsyncReader.checkForInativity(miliseconds);
    }         

    public String getTAG() {
        return TAG;
    }

    public void setTAG(String TAG) {
        if (null != mStdinAsyncReader)
            mStdinAsyncReader.setTAG(mStdinAsyncReader.getClass().getSimpleName() + "_[" + TAG + "]");
        this.TAG = TAG;
    }
}
