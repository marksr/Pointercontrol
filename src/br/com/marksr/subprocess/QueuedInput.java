/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.marksr.subprocess;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


public class QueuedInput extends ReaderWriter {    
    public static int PRIORITY_MAX = Integer.MAX_VALUE;
    public static int PRIORITY_LOW = 5;
    public static int PRIORITY_MID = 30;
    public static int PRIORITY_DISPOSABLE = 0;

    private final ReaderWriter internal;
    private boolean active = true;
    private final BlockingQueue <QueuedInputCommand> queue;

    public QueuedInput(ReaderWriter internal) {
        if (null == internal)
            throw new NullPointerException();
        this.internal = internal;
        mTextStream = internal.mTextStream;
        mServiceStatus = internal.mServiceStatus;
        queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void create() {
        internal.create();
        mOutputStream = internal.mOutputStream;
        mStdinAsyncReader = internal.mStdinAsyncReader;
        mReady = internal.mReady;

        new Thread(new Runnable() {

            @Override
            public void run() {
                while (active) {
//                    ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
                    QueuedInputCommand cmd;
                    try {
                        cmd = queue.take();
                        if (cmd.getPriority() != PRIORITY_MAX && cmd.getPriority() < queue.size()) {
                            // Ignore current command
                            System.out.println("Command ignored");
                            continue;
                        }              
                        synchronized (mOutputStream) {
                            sendCommand(cmd.execute());
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(QueuedInput.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    @Override
    public void destroy() {
        mOutputStream = null;
        mStdinAsyncReader = null;
        mReady = false;    
        mTextStream = null;
        mServiceStatus = null;
        internal.destroy();
    }

    public void sendQueueCommand(String cmd, int priority) {
        queueCommand(new QueuedInputCommand(cmd, priority));
    }
    
    public void queueCommand(QueuedInputCommand cmd) {
        queue.add(cmd);
    }

}
