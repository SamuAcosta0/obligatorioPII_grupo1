package uy.edu.um.doors;

import uy.edu.um.tad.queue.EmptyQueueException;

public interface ProcessManager {
    public static final int MAX_FINISHED_PROCESS_ON_RAM = 3;
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath);
    public void prepareProcesses() throws EmptyQueueException;
    public void executeNextProcess();
    public void finishProcessOk();
    public void finishProcessError();
    public void terminateProcess(int uid);
    public void printStatus();
    public void printStatusVerbose();
    public void printStatusByUser(int uid);
    public void printStatusByProcess(int pid);
}
