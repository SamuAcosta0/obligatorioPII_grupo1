package uy.edu.um.doors;

import uy.edu.um.tad.queue.EmptyQueueException;
import uy.edu.um.tad.stack.EmptyStackException;

public interface ProcessManager {
    public static final int MAX_FINISHED_PROCESS_ON_RAM = 3;
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath);
    public void prepareProcesses() throws EmptyQueueException;
    public void executeNextProcess();
    public void finishProcessOk() throws EmptyStackException;
    public void finishProcessError() throws EmptyStackException;
    public void terminateProcess(int uid) throws EmptyStackException;
    public void printStatus();
    public void printStatusVerbose();
    public void printStatusByUser(int uid);
    public void printStatusByProcess(int pid);
}
