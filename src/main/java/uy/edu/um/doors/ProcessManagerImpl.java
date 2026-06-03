package uy.edu.um.doors;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.User;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.queue.EmptyQueueException;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.entities.*;
import uy.edu.um.importer.DataLoader;
import uy.edu.um.entities.Log;

import java.io.IOException;


public class ProcessManagerImpl implements ProcessManager{

    private MyQueue<Process> procesosNuevos;
    private MyHeap<Process> procesosPendientes;
    private Process procesoEnEjecucion;
    private MyStack<Process> procesosFinalizados;
    private MyHash<Integer, User> usuarios;
    private Log logger;

    public ProcessManagerImpl() throws IOException {
        this.procesosNuevos = new MyQueueImpl<>();
        this.procesosPendientes = new MyHeapImpl<>(false);
        this.procesosFinalizados = new MyStackImpl<>();
        this.usuarios = new MyHashImpl<>();
        this.procesoEnEjecucion = null;
        this.logger = new Log();
    }

    @Override
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath) {
        DataLoader.loadUsers(usersCsvPath, usuarios);
        DataLoader.loadProcesses(processCsvPath, procesosNuevos, usuarios);

        System.out.println("Carga completada: " + usuarios.size()
                + " usuarios, " + procesosNuevos.size() + " procesos nuevos.");
    }

    @Override
    public void prepareProcesses() throws EmptyQueueException {
        while (!procesosNuevos.isEmpty()) {
            Process p = procesosNuevos.dequeue();

            //Calcula la prioridad y cambia el valor del atributo del proceso
            p.calculatePriority();

            p.setState(ProcessState.PENDING);

            if (logger != null) {
                logger.escribir(String.format(
                        "NEW->PENDING PROCESS: PID=%d | %s | USER:%s UID:%d | P=%d",
                        p.getPid(),
                        p.getName(),
                        p.getUser().getAlias(),
                        p.getUser().getUid(),
                        p.getPriority()
                ));
            }

            procesosPendientes.insert(p);
        }
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
