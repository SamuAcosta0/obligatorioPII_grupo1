package uy.edu.um.doors;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.User;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.entities.*;

public class ProcessManagerImpl implements ProcessManager{

    private MyQueue<Process> procesosNuevos;
    private MyHeap<Process> procesosPendientes;
    private Process procesoEnEjecucion;

    private MyStack<Process> procesosFinalizados;

    private MyHash<Integer, User> usuarios;

    public ProcessManagerImpl(MyQueue<Process> procesosNuevos, Process procesoEnEjecucion, MyHeap<Process> procesosPendientes, MyStack<Process> procesosFinalizados, MyHash<Integer, User> usuarios) {
        this.procesosNuevos = procesosNuevos;
        this.procesoEnEjecucion = procesoEnEjecucion;
        this.procesosPendientes = procesosPendientes;
        this.procesosFinalizados = procesosFinalizados;
        this.usuarios = usuarios;
    }

    public ProcessManagerImpl() {

    }

    @Override
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void prepareProcesses() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void executeNextProcess() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessOk() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessError() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void terminateProcess(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatus() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByUser(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("IMPLEMENTAR");
    }
}
