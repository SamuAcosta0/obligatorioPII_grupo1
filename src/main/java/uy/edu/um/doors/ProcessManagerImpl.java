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


public class ProcessManagerImpl implements ProcessManager {

    private MyQueue<Process> procesosNuevos;
    private MyHeap<Process> procesosPendientes;
    private Process procesoEnEjecucion;
    private MyStack<Process> procesosFinalizados;
    private MyHash<Integer, User> usuarios;
    private Log logger;

    public ProcessManagerImpl() throws IOException {
        this.procesosNuevos = new MyQueueImpl<>();
        this.procesosPendientes = new MyHeapImpl<>(false); //HEAP DE PENDIENTES
        this.procesosFinalizados = new MyStackImpl<>();
        this.usuarios = new MyHashImpl<>();
        this.procesoEnEjecucion = null;
        this.logger = new Log();
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

    //Uso de interfaces para funciones largas.
    //Recorrer mediante print
    @Override
    public void printStatus() {
        System.out.println("PROCESS STATUS");

        System.out.println("EXECUTING:");
        if (procesoEnEjecucion != null) {
            System.out.println(procesoEnEjecucion);
        } else {
            System.out.println("Ninguno");
        }

        System.out.println("PENDING:");
        recorrerPendientes(procesosPendientes);

        System.out.println("FINISHED:");
        recorrerFinalizados(procesosFinalizados);
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("PROCESS STATUS VERBOSE");

        System.out.println("EXECUTING:");
        if (procesoEnEjecucion != null) {
            System.out.println(procesoEnEjecucion);
            procesoEnEjecucion.printEvents();
        } else {
            System.out.println("Ninguno");
        }

        System.out.println("PENDING:");
        recorrerEventosPendientes(procesosPendientes);

        System.out.println("FINISHED:");
        recorrerEventosFinalizados(procesosFinalizados);
    }

    @Override
    public void printStatusByUser(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("IMPLEMENTAR");
    }


////////////////////////////////// MÉTODOS DE RECORRIDA //////////////////////////////////////////////////////
    private void recorrerEventosPendientes(MyHeap<Process> procesosPendientes) {
        int size = procesosPendientes.size();
        Process[] temp = new Process[size];
        int count = 0;

        while (!procesosPendientes.isEmpty()) {
            try {
                temp[count++] = procesosPendientes.remove();
            } catch (EmptyHeapException e) {
                break;
            }
        }

        for (int i = 0; i < count; i++) {
            System.out.println(temp[i]);
            temp[i].printEvents();
        }

        for (int i = 0; i < count; i++) {
            procesosPendientes.insert(temp[i]);
        }
    }

    private void recorrerEventosFinalizados(MyStack<Process> procesosFinalizados) {
        int stackSize = procesosFinalizados.size();
        Process[] temp = new Process[stackSize];
        int count = 0;

        while (!procesosFinalizados.isEmpty()) {
            try {
                temp[count++] = procesosFinalizados.pop();
            } catch (EmptyStackException e) {
                break;
            }
        }

        for (int i = 0; i < count; i++) {
            System.out.println(temp[i]);
            temp[i].printEvents();
        }

        for (int i = count - 1; i >= 0; i--) {
            procesosFinalizados.push(temp[i]);
        }
    }

    // Recorre el heap, ejecuta una acción por cada proceso, y lo restaura
    private void recorrerPendientes(MyHeap<Process> procesosPendientes) {
        int size = procesosPendientes.size();
        Process[] temp = new Process[size];
        int count = 0;

        while (!procesosPendientes.isEmpty()) {
            try {
                temp[count++] = procesosPendientes.remove();
            } catch (EmptyHeapException e) {
                break;
            }
        }

        for (int i = 0; i < count; i++) {
            System.out.println(temp[i]);
        }

        for (int i = 0; i < count; i++) {
            procesosPendientes.insert(temp[i]);
        }
    }

    // Recorre el stack, ejecuta una acción por cada proceso, y lo restaura
    private void recorrerFinalizados(MyStack<Process> procesosFinalizados) {
        int stackSize = procesosFinalizados.size();
        Process[] temp = new Process[stackSize];
        int count = 0;

        while (!procesosFinalizados.isEmpty()) {
            try {
                temp[count++] = procesosFinalizados.pop();
            } catch (EmptyStackException e) {
                break;
            }
        }

        for (int i = 0; i < count; i++) {
            System.out.println(temp[i].toStringFinished());
        }

        for (int i = count - 1; i >= 0; i--) {
            procesosFinalizados.push(temp[i]);
        }
    }
}