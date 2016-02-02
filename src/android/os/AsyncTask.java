/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package android.os;

import java.util.concurrent.ExecutionException;

/**
 *
 * @author Marcos
 */
public abstract class AsyncTask<Params, Progress, Result> {
    public static int THREAD_POOL_EXECUTOR = 0;
    private Thread mThread;
    private Runnable mRunnable;
    private Result mResult = null;
    
    protected abstract Result doInBackground(Params... params);
    
    protected synchronized void publishProgress(Progress... values) {
        synchronized (this) {
            onProgressUpdate(values);
        }
    }
    
    protected void onProgressUpdate(Progress... values) {
    }
    
    public AsyncTask<Params, Progress, Result> executeOnExecutor(int type, Params... params) {     
        final Params p[] = params;
        mThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Result r = doInBackground(p);
                
                synchronized (AsyncTask.this) {
                    mResult = r;
                }
            }
        });
        mThread.start();
        
        return this;
    }

    public AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(0, params);
    }   
    
    public Result get() throws InterruptedException, ExecutionException {
        if (mThread.isAlive()) {
            mThread.join();
            return mResult;
        }
        return null;
    }
}
