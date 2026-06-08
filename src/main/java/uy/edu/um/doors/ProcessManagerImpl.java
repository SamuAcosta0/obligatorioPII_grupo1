package uy.edu.um.doors;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.User;
import uy.edu.um.entities.Event;
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

    // Metodo auxiliar para el log cuando se llena el stack de procesos finalizados
    private void logStackOverflow() {
        try {
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
        } catch (EmptyStackException e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

    // Metodo auxiliar que agrega un proceso al stack y verifica overflow
    private void pushToFinishedStack(Process process) {
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
    public void prepareProcesses() {
        try {
            while (!procesosNuevos.isEmpty()) {
                Process p = procesosNuevos.dequeue();

                // Calcula la prioridad y cambia el valor del atributo del proceso
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
        } catch (EmptyQueueException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    public void executeNextProcess() {
        //estan en el heap, la raiz tiene mayor prioridad
        //sacamos de los procesos pendientes el que tiene mayor prioridad
        if (procesosPendientes.isEmpty()) {
            System.out.println("No hay procesos pendientes para ejecutar.");
            return;
        }

        if (procesoEnEjecucion != null) {
            System.out.println("Ya hay un proceso en ejecución.");
            return;
        }

        try {
            // Extrae el de mayor prioridad
            procesoEnEjecucion = procesosPendientes.remove();
            procesoEnEjecucion.setState(ProcessState.RUNNING);

            // Registrar en log y mostrar en consola
            String logLine = procesoEnEjecucion.toStringExecuting();
            logger.escribir(logLine);
            System.out.println(logLine);

            // Imprimir eventos en consola
            procesoEnEjecucion.printEvents();


        } catch (EmptyHeapException e) {
            System.err.println("Error al extraer proceso del heap: " + e.getMessage());
        }
    }
    @Override
    public void finishProcessOk() {

        // vemos quee exista un proceso ejecutandose
        if (procesoEnEjecucion == null) {
            System.out.println("No hay proceso en ejecución.");
            return;
        } // crear una excepcion para esto


        // el proceso pasa a estado FINISHED
        procesoEnEjecucion.setState(ProcessState.FINISHED);

        // guardamos el tipo de finalización
        procesoEnEjecucion.setFinishType(FinishType.OK);

        // registramos el evento en el log
        logger.escribir(String.format(
                "ENDING PROCESS: PID=%d | STATE: OK",
                procesoEnEjecucion.getPid()
        ));

        // Hacer esta verificación antes del push al stack
        pushToFinishedStack(procesoEnEjecucion);

        //como es uno a la vez, queda null
        procesoEnEjecucion = null;
    }

    @Override
    public void finishProcessError() {

        // verificamos que exista un proceso ejecutándose
        if (procesoEnEjecucion == null) {
            System.out.println("No hay proceso en ejecución.");
            return;
        }

        // el proceso pasa a estado FINISHED
        procesoEnEjecucion.setState(ProcessState.FINISHED);

        // indicamos que terminó por ERROR
        procesoEnEjecucion.setFinishType(FinishType.ERROR);

        // registramos el evento en el log
        logger.escribir(String.format(
                "ENDING PROCESS: PID=%d | STATE: ERROR",
                procesoEnEjecucion.getPid()
        ));

        // Hacer esta verificación antes del push al stack
        pushToFinishedStack(procesoEnEjecucion);

        //limpiamos
        procesoEnEjecucion = null;
    }

    @Override
    public void terminateProcess(int uid) {
        // verificamos que exista un proceso ejecutándose
        if (procesoEnEjecucion == null) {
            System.out.println("No hay proceso en ejecución.");
            return;
        } //excepcion

        // buscamos el usuario que forzó la terminación
        User user = usuarios.get(uid);

        // si no existe, abortamos la operación
        if (user == null) {
            System.out.println("No existe usuario con UID=" + uid);
            return;
        } //excepcion

        // el proceso pasa a estado FINISHED
        procesoEnEjecucion.setState(ProcessState.FINISHED);

        // indicamos que fue terminado manualmente
        procesoEnEjecucion.setFinishType(FinishType.TERMINATED);

        // guardamos quién lo terminó
        procesoEnEjecucion.setTerminatedBy(user);

        // registramos el evento en el log
        logger.escribir(String.format(
                "ENDING PROCESS: PID=%d | STATE: TERMINATED by USER:%s UID:%d",
                procesoEnEjecucion.getPid(),
                user.getAlias(),
                user.getUid()
        ));

        // Hacer esta verificación antes del push al stack
        pushToFinishedStack(procesoEnEjecucion);

        procesoEnEjecucion = null;
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