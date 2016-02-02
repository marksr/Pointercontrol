package br.com.marksr.subprocess;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class AsyncReader implements AsyncReaderPost {
    private String TAG = this.getClass().getSimpleName();
    private final InputStream mStream;
    private final AsyncReaderPost mReaderListener;
    private MyAsyncTask mAsyncTask;
    private AsyncReaderPost mJoinListener;
    private ArrayList<AsyncReader> mAsyncReaders;
    private long mInativityTime = 0;

    public AsyncReader(InputStream stream, AsyncReaderPost readerListener) {
        mAsyncReaders = new ArrayList<>();
        this.mStream = stream;
        this.mReaderListener = readerListener;
        
        if (null == stream) {
            throw new NullPointerException("Invalid stream");
        }
    }

    public boolean join(AsyncReader asyncReader) {
        if (null == mAsyncTask && !mAsyncReaders.contains(asyncReader)) {
            mAsyncReaders.add(asyncReader);
            asyncReader.setJoinedTo(this);
        }
        return false;
    }

    public void start() {
        Log.d(TAG, "startAsyncReader begin");
        if (null == mAsyncTask) {
            mAsyncTask = new MyAsyncTask();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mStream);
            } else {
                mAsyncTask.execute(mStream);
            }
            Log.d(TAG, "startAsyncReader execute");
        }
        for (AsyncReader reader: mAsyncReaders) {
            reader.start();
        }
        Log.d(TAG, "startAsyncReader end");
    }

    public void stop() {
        Log.d(TAG, "stopAsyncReader begin");
        for (AsyncReader reader: mAsyncReaders) {
            reader.stop();
        }        
        if (null != mAsyncTask) {
            mAsyncTask.setActive(false);
            mAsyncTask = null;
            Log.d(TAG, "stopAsyncReader stop");
        }
        Log.d(TAG, "stopAsyncReader end");
    }

    public String getTAG() {
        return TAG;
    }

    public void setTAG(String TAG) {
        this.TAG = TAG;
    }

    public void waitInativityFor(int milliseconds) {
        mInativityTime = System.currentTimeMillis();
        while (checkForInativity(milliseconds)) {
            try {
                Thread.sleep(10, 0);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    public void waitAtivityFor(int milliseconds) {
        mInativityTime = System.currentTimeMillis();
        while (!checkForInativity(milliseconds)) {
            try {
                Thread.sleep(10, 0);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }    
    
    public boolean checkForInativity(int milliseconds) {
        return (System.currentTimeMillis() - mInativityTime < milliseconds);
    }    
    
    private void setJoinedTo(AsyncReaderPost readerListener) {
        this.mJoinListener = readerListener;
    }
    
    public boolean isJoined() {
        return null != mJoinListener;
    }

    @Override
    public void post(String value) {
        mInativityTime = System.currentTimeMillis();
        if (null != mReaderListener) {
            mReaderListener.post(value);
        }
        
        if (isJoined()) {
            mJoinListener.post(value);
        }
    }

    protected class MyAsyncTask extends AsyncTask<InputStream, String, Void> {
        private String TAG = MyAsyncTask.class.getSimpleName();
        private boolean active = true;

        @Override
        protected Void doInBackground(InputStream... params) {
            String line, text;
            BufferedReader m = new BufferedReader(new InputStreamReader(params[0]));
            Log.d(TAG, "doInBackground begin");
            
            try {

                while (active && (line = m.readLine()) != null) {
                    if (!line.isEmpty())
                        publishProgress(line + "\n");
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Log.d(TAG, "doInBackground end");

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            post(values[0]);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setTAG(String TAG) {
            this.TAG = TAG;
        }

        public String getTAG() {
            return TAG;
        }
    }
}
