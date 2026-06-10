package uy.edu.um.entities;

import lombok.Setter;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.Node;

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



    //Se inicializa en 0 cada proceso antes de entrar a la queue de procesos, se calcula ahí
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
    //Calcular prioridad al cargar
    public void calculatePriority() {
        int nCPU  = 0;
        int nRAM  = 0;
        int nDISK = 0;
        int nTotal = 0;

        Node<Event> current = events.getFirst();
        while (current != null) {
            Event e = current.getValue();
            switch (e.getType()) {
                case CPU:  nCPU++;  break;
                case RAM:  nRAM++;  break;
                case DISK: nDISK++; break;
            }
            nTotal++;
            current = current.getNext();
        }

        int W = (user.getType() == UserType.ADMIN) ? 32 : 16;

        if (nTotal == 0) {
            this.priority = 0;
        } else {
            this.priority = ((8 * nCPU + 2 * nRAM + 2 * nDISK) / nTotal)
                    + (W * nTotal);
        }
    }

    public void printEvents() {
        Node<Event> current = events.getFirst();
        while (current != null) {
            System.out.println(current.getValue()); // delega a Event.toString()
            current = current.getNext();
        }
    }

    public String getEventsAsLogString() {
        StringBuilder result = new StringBuilder();

        Node<Event> current = events.getFirst();
        while (current != null) {
            result.append(current.getValue().toString()); // delega a Event.toString()

            // Salto de línea entre eventos (no al final del último)
            if (current.getNext() != null) {
                result.append("\n");
            }
            current = current.getNext();
        }

        return result.toString();
    }

    @Override
    public int compareTo(Process other) {
        int cmp = Integer.compare(this.priority, other.priority);
        if (cmp != 0) {
            return cmp;            // DISTINTA PRIORIDAD
        }
        return Integer.compare(this.pid, other.pid);  //IGUAL PRIORIDAD, DESEMPATE POR PID
    }


    public int getPid()                  { return pid; }
    public String getName()              { return name; }
    public User getUser()                { return user; }
    public int getPriority()             { return priority; }
    public ProcessState getState()       { return state; }
    public FinishType getFinishType()    { return finishType; }
    public User getTerminatedBy()        { return terminatedBy; }
    public MyList<Event> getEvents()     { return events; }

    public void addEvent(Event event)                 {
        this.events.add(event);
    }

    // Para PENDING y EXECUTING en pstatus
    @Override
    public String toString() {
        return "PID=" + pid + " | " + name
                + " | USER:" + user.getAlias()
                + " UID:" + user.getUid()
                + " | P=" + priority;
    }

    // Para FINISHED en pstatus
    public String toStringFinished() {
        return "PID=" + pid + " " + name
                + " | STATE: " + finishType
                + " | USER:" + user.getAlias()
                + " UID:" + user.getUid();
    }

    // Para el log de pexecute
    public String toStringExecuting() {
        return "EXECUTING PROCESS: PID=" + pid
                + " | USER:" + user.getAlias()
                + " UID:" + user.getUid();
    }

    // Para el log de pfinish OK y ERROR
    public String toStringEnding() {
        return "ENDING PROCESS: PID=" + pid
                + " | STATE: " + finishType;
    }

    // Para el log de pfinish TERM
    public String toStringEndingTerminated() {
        return "ENDING PROCESS: PID=" + pid
                + " | STATE: TERMINATED by USER:"
                + terminatedBy.getAlias()
                + " UID:" + terminatedBy.getUid();
    }

    // Para el log de stack overflow
    public String toStringStackOverflow() {
        return "PID=" + pid + " " + name
                + " | STATE: " + finishType
                + " | USER:" + user.getAlias()
                + " UID:" + user.getUid();
    }

}
