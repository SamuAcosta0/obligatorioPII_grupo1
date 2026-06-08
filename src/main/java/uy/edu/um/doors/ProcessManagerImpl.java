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
    public void executeNextProcess() { //estan en el heap, la raiz tiene mayor prioridad

        //sacamos de los procesos pendientes el que tiene mayor prioridad
        //private Process procesoEnEjecucion;
        //en el log se deben guardar distintos eventos
        while(!procesosPendientes.isEmpty()) {
            Process proceso = procesosPendientes.remove();
            //sacamos del heap el proceso de mayor prioridad

            procesoEnEjecucion = proceso;
            //guardamos el proceso como proceso actualmente en ejecucion

            //cambiar el estado del proceso a RUNNING
            proceso.setState(ProcessState.RUNNING);

            logger.escribir(String.format(
                    "EXECUTING PROCESS: PID=%d | %s | USER:%s UID:%d",
                    proceso.getPid(),
                    proceso.getName(),
                    proceso.getUser().getAlias(),
                    proceso.getUser().getUid()
            ));

            MyList<Event> events = proceso.getEvents();
            //conseguimos los eventos para poder completar la salida del log

            for (int i = 0; i < events.size(); i++) {
                Event evento = events.get(i);

                logger.escribir(
                        "EVENT: " + evento.getType()
                        + "| Instructions "
                        + evento.getInstructions()
                );

            }

           // procesosFinalizados.push(proceso);
            //agregamos a los procesos finalizados

        }


    }

    @Override
    public void finishProcessOk() throws EmptyStackException {

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

        // si el stack alcanzó su capacidad máxima
        while (procesosFinalizados.size() >= MAX_FINISHED_PROCESS_ON_RAM) {

            // registramos el overflow
            logger.escribir("Finished process stack overflow");

            // mostramos y eliminamos todos los procesos de la pila
            while (!procesosFinalizados.isEmpty()) {

                Process p = procesosFinalizados.pop();

                logger.escribir(String.format(
                        "PID=%d %s | STATE: %s | USER:%s UID:%d",
                        p.getPid(),
                        p.getName(),
                        p.getFinishType(),
                        p.getUser().getAlias(),
                        p.getUser().getUid()
                ));
            }
        }

        // guardamos el proceso recien finalizados en el stack
        procesosFinalizados.push(procesoEnEjecucion);

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

        // si el stack esta lleno
        while (procesosFinalizados.size() >= MAX_FINISHED_PROCESS_ON_RAM) {

            logger.escribir("Finished process stack overflow");

            // mostramos y vaciamos el stack
            while (!procesosFinalizados.isEmpty()) {

                Process p = procesosFinalizados.pop();

                logger.escribir(String.format(
                        "PID=%d %s | STATE: %s | USER:%s UID:%d",
                        p.getPid(),
                        p.getName(),
                        p.getFinishType(),
                        p.getUser().getAlias(),
                        p.getUser().getUid()
                ));
            }
        }

        // guardamos el proceso finalizado
        procesosFinalizados.push(procesoEnEjecucion);

        // dejamos la CPU libre
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

        // si la pila está llena
        while (procesosFinalizados.size() >= MAX_FINISHED_PROCESS_ON_RAM) {

            logger.escribir("Finished process stack overflow");

            // mostramos y vaciamos la pila
            while (!procesosFinalizados.isEmpty()) {

                Process p = procesosFinalizados.pop();

                logger.escribir(String.format(
                        "PID=%d %s | STATE: %s | USER:%s UID:%d",
                        p.getPid(),
                        p.getName(),
                        p.getFinishType(),
                        p.getUser().getAlias(),
                        p.getUser().getUid()
                ));
            }
        }

        // guardamos el proceso finalizado
        procesosFinalizados.push(procesoEnEjecucion);

        procesoEnEjecucion = null;

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
