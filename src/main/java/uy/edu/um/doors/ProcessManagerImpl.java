package uy.edu.um.doors;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.User;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.queue.EmptyQueueException;
import uy.edu.um.tad.stack.EmptyStackException;
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

    // Metodo auxiliar para el log cuando se llena el stack de procesos finalizados
    private void logStackOverflow() throws EmptyStackException {
        if (logger == null) return;

        // Vaciamos la pila con pop() y logueamos en cada iteración
        while (!procesosFinalizados.isEmpty()) {
            Process p = procesosFinalizados.pop();

            logger.escribir(String.format(
                    "Finished process stack overflow PID=%d %s | STATE: %s | USER:%s UID:%d",
                    p.getPid(),
                    p.getName(),
                    p.getFinishType(),
                    p.getUser().getAlias(),
                    p.getUser().getUid()
            ));
        }
    }

    // Metodo auxiliar que agrega un proceso al stack y verifica overflow
    private void pushToFinishedStack(Process process) throws EmptyStackException {
        if (procesosFinalizados.size() >= MAX_FINISHED_PROCESS_ON_RAM) {
            logStackOverflow();
        }
        procesosFinalizados.push(process);
    }

    @Override
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath) {
        DataLoader.loadUsers(usersCsvPath, usuarios); //Se ingresa en el ProcessConsole la ruta del archivo y aquí se crea el hash de usuarios
        DataLoader.loadProcesses(processCsvPath, procesosNuevos, usuarios); //Se ingresa en el ProcessConsole la ruta del archivo, y se envia la lista de usuarios llena y la lista de procesos nuevos

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
    public void finishProcessOk() throws EmptyStackException {

        // Hacer esta verificación antes del push al stack
        pushToFinishedStack(procesoEnEjecucion);
    }

    @Override
    public void finishProcessError() throws EmptyStackException {

        // Hacer esta verificación antes del push al stack
        pushToFinishedStack(procesoEnEjecucion);
    }

    @Override
    public void terminateProcess(int uid) throws EmptyStackException {

        // Hacer esta verificación antes del push al stack
        pushToFinishedStack(procesoEnEjecucion);
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
