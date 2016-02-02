/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.marksr.pointercontrol;

import br.com.marksr.pointercontrol.commons.PointerCommand;
import br.com.marksr.pointercontrol.commons.PointerCommandAdapter;
import br.com.marksr.subprocess.ExternalExec;
import br.com.marksr.subprocess.QueuedInput;
import br.com.marksr.subprocess.QueuedInputCommand;
import br.com.marksr.subprocess.TextStreamListener;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcos
 */
public class PointerControl {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
            //        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(ControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(ControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(ControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(ControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
         
        final PointerCommand pong = new PointerCommandAdapter("a,pong\n");
        final PointerCommand requestScreenSize = new PointerCommandAdapter("s\n");
        CommandsClient cc = new CommandsClient("localhost", 6789);
        
        cc.registerCommand(new PointerCommandAdapter('a') {

            @Override
            public void process(String message) {
//                Logger.getLogger(PointerControl.class.getName()).log(Level.INFO, message);
                cc.sendCommand(pong);
            }
            
        });       
        
        final Integer width[] = new Integer[1];
        final Integer height[] = new Integer[1];
        final Integer my_width[] = new Integer[1];
        final Integer my_height[] = new Integer[1];
        final Float ratio[] = new Float[1];
        final PointerCommandAdapter screenSizeResult = new PointerCommandAdapter('s') {
            
            @Override
            public void process(String message) {
                Logger.getLogger(PointerControl.class.getName()).log(Level.INFO, message);
                String values[] = message.split(",");
                
                if (values.length == 3) {
                    width[0] = Integer.parseInt(values[1]);
                    height[0] = Integer.parseInt(values[2]);                    
                }
                super.process(message);
                synchronized (this) {
                    this.notify();
                }
            }            
        };
        
        cc.registerCommand(screenSizeResult);
        cc.sendCommand(requestScreenSize);
        cc.start();
        
        synchronized(screenSizeResult) {
            while (!screenSizeResult.isProcessDone()) {
                try {
                    screenSizeResult.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(PointerControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        final ControlScreen.ClosingEventInterface closing = new ControlScreen.ClosingEventInterface() {

            @Override
            public void closing() {
                PointerCommand command = new PointerCommandAdapter("x\n");
                cc.sendCommand(command);
            }
        };        
        
        final ControlScreen.MouseEventInterface mouseMove = new ControlScreen.MouseEventInterface() {
            private QueuedInput externalExec = null;
            private String devicename = create();
            private long lastMouseDrag = 0;
            private Point lastPoint = null;
            
            private void createSubProcess() {
                if (null == externalExec) {
                    externalExec = new QueuedInput(new ExternalExec("adb shell"));
                    
                    externalExec.setOnTextListener(new TextStreamListener() {

                        @Override
                        public void onText(String data) {
//                            System.out.print("out: "+data);
                        }
                    });
                    externalExec.create();
                }              
            }
            private String create() {
                createSubProcess();

                String out = externalExec.sendCommandResult("getevent -il", 500);
                
                System.out.println("\n\nfinish\n\n");
                
                String a[] = out.split("add device");
                
                for (String b: a) {
                    if (b.contains("ABS_MT_POSITION_X")) {
                        String c[] = b.split("\n")[0].split(" ");
                        
                        System.out.println("begin\n\n"+ c[c.length-1] + "\n\nfinish\n\n");
                        return c[c.length-1];
                    }
                    
                }
                return null;
            }
            
            private Point translatePoint(Point point) {
                point.x = (int)((point.x * width[0]) / (my_width[0] / 1.03f));
                point.y = (int)((point.y * height[0]) / (my_height[0] / 1.06f));
                return point;
            }
            
            @Override
            public void mouseMove(Point point) {
                point = translatePoint(point);
                PointerCommand command = new PointerCommandAdapter("c,"+point.x+","+point.y+"\n", 0);
//                int x = (int)((point.x * width[0]) / (my_width[0] / 1.03f));
//                int y = (int)((point.y * height[0]) / (my_height[0] / 1.06f));                
//                PointerCommand command = new PointerCommandAdapter("c,"+x+","+y+"\n", 0);
                cc.sendCommand(command);
            }

            @Override
            public void mouseClick(Point point) {
//                createSubProcess();
//                point = translatePoint(point);
//                externalExec.sendCommand("adb shell input tap "+ (point.x-1) +" "+ (point.y-1));
            }

            @Override
            public void mouseDrag(Point point) {
                createSubProcess();
                point = translatePoint(point);
                PointerCommand command = new PointerCommandAdapter("c,"+point.x+","+point.y+"\n", 0);
                cc.sendCommand(command);
                
                int d = 0;
                if (null != lastPoint) {
                    d = (int)Math.sqrt(Math.pow(point.x-lastPoint.x,2) + Math.pow(point.y-lastPoint.y,2));
                }
                System.out.println("distance: "+d);
                if (d > 20) {
                    lastPoint = point;
                    QueuedInputCommand cmd = new QueuedInputCommand(
                            "S=\"sendevent "+ devicename +"\";$S 3 47 1;$S 3 53 "+ (point.x-1) +";$S 3 54 "+ (point.y-1) +";$S 3 58 50;$S 3 48 5;$S 0 0 0;",
                            QueuedInput.PRIORITY_DISPOSABLE)
                    {

                    @Override
                    public String execute() {
                        try {
                            Thread.sleep(100,0);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(PointerControl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return super.execute();
                    }
                        
                    };
                          
                    externalExec.queueCommand(cmd);
                }
            }

            @Override
            public void mouseDown(Point point) {
                createSubProcess();
                point = translatePoint(point);
                lastPoint = point;
                externalExec.sendCommand("S=\"sendevent "+ devicename +"\";$S 3 47 1;$S 3 57 0;$S 3 53 "+ (point.x-1) +";$S 3 54 "+ (point.y-1) +";$S 3 58 50;$S 3 48 5;$S 0 57 0;$S 0 0 0;");
            }

            @Override
            public void mouseUp(Point point) {
                createSubProcess();
                point = translatePoint(point);
                externalExec.sendCommand("S=\"sendevent "+ devicename +"\";$S 3 47 1;$S 3 57 -1;$S 0 0 0;");
            }
        };
        
        if (screenSizeResult.isProcessDone()) {
            ratio[0] = (width[0] > height[0]) ? (float)width[0] / (float)height[0] : (float)height[0] / (float)width[0];
            
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                    int w = (int)(gd.getDisplayMode().getWidth() * 0.7f);
                    int h = (int)(gd.getDisplayMode().getHeight() * 0.7f);
                    my_width[0] = w;
                    my_height[0] = h;
                    ControlScreen controlScreen = new ControlScreen();
                    controlScreen.setOnMouseMoveEvent(mouseMove);
                    controlScreen.setOnClosingEvent(closing);
                    my_width[0] = (int)(h / ratio[0]);
                    if (width[0] < height[0]) {
                        controlScreen.setWindowSize(my_width[0], my_height[0]);
                    } else {
                        my_width[0] = my_width[0] ^ my_height[0];
                        my_height[0] = my_width[0] ^ my_height[0];
                        my_width[0] = my_width[0] ^ my_height[0];
                        controlScreen.setWindowSize(my_width[0], my_height[0]);                        
                    }
                    
//                    controlScreen.setWindowSize(width[0], height[0]);
//                    controlScreen.setBounds(controlScreen.getX(), controlScreen.getY(), width[0], height[0]);
                    controlScreen.setResizable(false);
//                    controlScreen.setLocation(x + controlScreen.getX(), y + controlScreen.getX());
                    controlScreen.setVisible(true);
                }
            });
        }
    }
    
}
