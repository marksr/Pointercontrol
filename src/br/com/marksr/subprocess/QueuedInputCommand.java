/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.marksr.subprocess;

public class QueuedInputCommand {
    private final int priority;
    private final String cmd;

    public QueuedInputCommand(String cmd, int priotiry) {
        this.priority = priotiry;
        this.cmd = cmd;
    }

    public int getPriority() {
        return priority;
    }

    public String execute() {
        return cmd;
    }

}