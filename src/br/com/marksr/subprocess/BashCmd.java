/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.marksr.subprocess;

public class BashCmd extends ExternalExec {
    private String startDiretory;

    public BashCmd() {
        super("sh");
    }

    public BashCmd(String startDiretory) {
        super("sh");
        this.startDiretory = startDiretory;
    }

    @Override
    public void create() {
        super.create();

        if (null != startDiretory && !startDiretory.isEmpty())
            sendCommand("cd " + startDiretory);
    }
}
