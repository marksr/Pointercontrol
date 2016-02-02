package br.com.marksr.pointercontrol;

import br.com.marksr.pointercontrol.commons.GenericPublisher;
import br.com.marksr.pointercontrol.commons.PointerCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandsClient {
    private Socket mClient = null;
    private SocketTask mSocketTask = null;
    private ConcurrentLinkedQueue<PointerCommand> mQueue;
    private GenericPublisher<PointerCommand, String> mCommands;
    private int mServerPort;
    private String mHost;

    public CommandsClient(String host, int serverPort) {
        mHost = host;
        mServerPort = serverPort;
        mQueue = new ConcurrentLinkedQueue<>();
        mCommands = new GenericPublisher<PointerCommand, String>() {
            @Override
            public void action(PointerCommand c, String message) {
                if (!message.isEmpty() && c.getType() == message.charAt(0)) {
                    c.process(message);
                }
            }
        };
    }
            
    public void start() {
        if (null == mSocketTask) 
            mSocketTask = new SocketTask();
            new Thread(mSocketTask).start();
    }
    
    public void stop() {
        if (null != mSocketTask) {
            mSocketTask.setActive(false);
            mSocketTask = null;
        }
    }
    
    public void registerCommand(PointerCommand command) {
        mCommands.setOnEvent(command);
    }

    public void unregisterCommand(PointerCommand command) {
        mCommands.removeOnEvent(command);
    }

    public void sendCommand(PointerCommand command) {
        mQueue.add(command);
    }

    private void dispatchCommands(String message) {
        synchronized (mCommands) {
            mCommands.fire(message);
        }
    }    
    
    private class SocketTask implements Runnable {
        private boolean mActive = true;

        public boolean isActive() {
            return mActive;
        }

        public void setActive(boolean active) {
            this.mActive = active;
        }
        
        @Override
        public void run() {
            BufferedReader in = null;
            OutputStream os = null;
            try {
                mClient = new Socket(mHost, mServerPort);
                in = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
                os = mClient.getOutputStream();
            } catch (java.net.ConnectException c)
            {
                System.out.println(c.toString());
            } catch (IOException ex) {
                Logger.getLogger(PointerControl.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (null != mClient && null != os && null != in) {
                while (mActive) {
                    PointerCommand command = mQueue.poll();

                    if (null != command) {
                        if (mQueue.size() >= 1 && command.getPriority() == 0) {
                            continue;
                        }
                        try {
                            String msg = command.execute();
                            os.write(msg.getBytes());
                            if (command.isDebug())
                                Logger.getLogger(PointerControl.class.getName()).info(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        if (in.ready()) {
                            dispatchCommands(in.readLine());
                        } else {
                            Thread.sleep(10, 0);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }                    
                }
            }
        }
    }
}
