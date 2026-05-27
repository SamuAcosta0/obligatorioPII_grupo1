package uy.edu.um.entities;

import lombok.Setter;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.MyLinkedListImpl;

public class Process implements Comparable<Process> {

    private int pid;
    private String name;
    private User user;
    private int priority;

    @Setter
    private ProcessState state;
    @Setter
    private FinishType finishType;
    @Setter
    private User terminatedBy;

    private MyList<Event> events;

    public Process(int pid, String name, User user, ProcessState aNew) {
        this.pid        = pid;
        this.name       = name;
        this.user       = user;
        this.state      = ProcessState.NEW;
        this.priority   = 0;
        this.finishType = null;
        this.terminatedBy = null;
        this.events     = new MyLinkedListImpl<>();
    }

    public void calculatePriority() {
        int nCPU  = 0;
        int nRAM  = 0;
        int nDISK = 0;
        int nTotal = events.size();

        for (int i = 0; i < nTotal; i++) {
            Event e = events.get(i);
            switch (e.getType()) {
                case CPU:  nCPU++;  break;
                case RAM:  nRAM++;  break;
                case DISK: nDISK++; break;
            }
        }

        int W = (user.getType() == UserType.ADMIN) ? 32 : 16;

        if (nTotal == 0) {
            this.priority = 0;
        } else {
            this.priority = ((8 * nCPU + 2 * nRAM + 2 * nDISK) / nTotal)
                    + (W * nTotal);
        }
    }


    @Override
    public int compareTo(Process other) {
        return Integer.compare(this.priority, other.priority);
    }


    public int getPid()                  { return pid; }
    public String getName()              { return name; }
    public User getUser()                { return user; }
    public int getPriority()             { return priority; }
    public ProcessState getState()       { return state; }
    public FinishType getFinishType()    { return finishType; }
    public User getTerminatedBy()        { return terminatedBy; }
    public MyList<Event> getEvents()     { return events; }

    public void addEvent(Event event)                 { this.events.add(event); }

    @Override
    public String toString() {
        return "PID=" + pid + " | " + name
                + " | USER:" + user.getAlias()
                + " UID:" + user.getUid()
                + " | P=" + priority;
    }

}
