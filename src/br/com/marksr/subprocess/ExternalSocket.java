/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.marksr.subprocess;

import android.util.Log;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ExternalSocket extends ReaderWriter {
    private final int port;
    private final String host;
    private Socket mClient;

    public ExternalSocket(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public void create() {
        Log.d(TAG, "create  begin");
        if (null == mClient) {
            mClient = createSocket(host, port);

            if (null != mClient) {
                try {
                    mOutputStream = new OutputStreamWriter(mClient.getOutputStream());
                    mStdinAsyncReader = new AsyncReader(mClient.getInputStream(), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mStdinAsyncReader.start();
                mReady = true;
            } else {
                destroy();
            }

            setTAG(getTAG());
            mServiceStatus.fire(mReady);
        }
        Log.d(TAG, "create end");
    }

    public void destroy() {
        Log.d(TAG, "destroy begin");
        if (null != mStdinAsyncReader) {
            mStdinAsyncReader.stop();
            mStdinAsyncReader = null;
        }

        try {
            if (null != mOutputStream) {
                mOutputStream.close();
                mOutputStream = null;
            }

            if (null != mClient && !mClient.isInputShutdown() && null != mClient.getInputStream())
                mClient.getInputStream().close();
        } catch (IOException e) {
        }

        if (null != mClient) {
            try {
                mClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mClient = null;
        }

        mReady = false;
        mServiceStatus.fire(mReady);
        Log.d(TAG, "destroy end");
    }

    private Socket createSocket(final String host, final int port) {
        final Socket[] socket = new Socket[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket[0] = new Socket(host, port);
                } catch (IOException e) {
                    socket[0] = null;
                    e.printStackTrace();
                }
            }
        });
        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return socket[0];
    }
}