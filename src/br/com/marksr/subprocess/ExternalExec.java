/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.marksr.subprocess;

import android.util.Log;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ExternalExec extends ReaderWriter {
        private Process mProcess;
        private AsyncReader mStderrAsyncReader;
        private final String mCmd;

        public ExternalExec(String cmd) {
            super();
            this.mCmd = cmd;
        }

        @Override
        public void create() {
            Log.d(TAG, "create begin");
            if (null == mProcess) {
                try {
                    mProcess = Runtime.getRuntime().exec(mCmd);
                } catch (IOException e) {
                    mProcess = null;
                    e.printStackTrace();
                }

                if (null != mProcess) {
                    mOutputStream = new OutputStreamWriter(mProcess.getOutputStream());
                    mStdinAsyncReader = new AsyncReader(mProcess.getInputStream(), this);
                    mStderrAsyncReader = new AsyncReader(mProcess.getErrorStream(), null);
                    mStdinAsyncReader.join(mStderrAsyncReader);
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

        @Override
        public void destroy() {
            Log.d(TAG, "destroy begin");
            if (null != mStdinAsyncReader) {
                mStdinAsyncReader.stop();
                mStdinAsyncReader = null;
            }
            mStderrAsyncReader = null;

            try {
                if (null != mOutputStream) {
                    mOutputStream.close();
                    mOutputStream = null;
                }

                if (null != mProcess && null != mProcess.getInputStream())
                    mProcess.getInputStream().close();

                if (null != mProcess && null != mProcess.getErrorStream())
                    mProcess.getErrorStream().close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (null != mProcess) {
                mProcess.destroy();
                mProcess = null;
            }

            mReady = false;
            mServiceStatus.fire(mReady);
            Log.d(TAG, "destroy end");
        }

        public void waitFor() {
            if (null != mProcess)
                try {
                    mProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }

    }