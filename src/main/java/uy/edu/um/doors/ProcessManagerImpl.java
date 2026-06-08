package uy.edu.um.doors;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.User;
import uy.edu.um.exceptions.NoProcessesException;
import uy.edu.um.exceptions.UserNotFoundException;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
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
import uy.edu.um.exceptions.NoRunningProcessException;
import java.io.IOException;


public class ProcessManagerImpl implements ProcessManager {

    private MyQueue<Process> procesosNuevos;
    private MyHeap<Process> procesosPendientes;
    private Process procesoEnEjecucion;
    private MyStack<Process> procesosFinalizados;
    private MyHash<Integer, User> usuarios;
    private Log logger;

    public ProcessManagerImpl() {
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
            // Registramos el encabezado del overflow en el log
            logger.escribir("Finished process stack overflow");

            // Vaciamos la pila con pop() y logueamos en cada iteración
            while (!procesosFinalizados.isEmpty()) {
                Process p = procesosFinalizados.pop();

                // Usamos toStringStackOverflow() para el formato correcto
                logger.escribir(p.toStringStackOverflow());
            }
        } catch (EmptyStackException e) {
            System.out.println("ERROR: " + e.getMessage());
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
        try {
            if (processCsvPath == null || processCsvPath.isEmpty())
                throw new IllegalArgumentException("La ruta del archivo de procesos no puede estar vacía.");
            if (usersCsvPath == null || usersCsvPath.isEmpty())
                throw new IllegalArgumentException("La ruta del archivo de usuarios no puede estar vacía.");

            DataLoader.loadUsers(usersCsvPath, usuarios); //Se ingresa en el ProcessConsole la ruta del archivo y aquí se crea el hash de usuarios
            DataLoader.loadProcesses(processCsvPath, procesosNuevos, usuarios); //Se ingresa en el ProcessConsole la ruta del archivo, y se envia la lista de usuarios llena y la lista de procesos nuevos

            System.out.println("Carga completada: " + usuarios.size()
                    + " usuarios, " + procesosNuevos.size() + " procesos nuevos.");

        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void prepareProcesses() {
        try {
            if (procesosNuevos.isEmpty())
                throw new NoProcessesException("No hay procesos nuevos para preparar.");

            while (!procesosNuevos.isEmpty()) {
                Process p = procesosNuevos.dequeue();

                // Calcula la prioridad y cambia el valor del atributo del proceso
                p.calculatePriority();

                p.setState(ProcessState.PENDING);

                // Usamos toString() de Process para el formato correcto del log
                logger.escribir("NEW->PENDING PROCESS: " + p.toString());

                procesosPendientes.insert(p);
            }
        } catch (NoProcessesException | EmptyQueueException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void executeNextProcess() {
        //estan en el heap, la raiz tiene mayor prioridad
        //sacamos de los procesos pendientes el que tiene mayor prioridad

        //NO pueden haber print y return: usar excepcion
        try {
            if (procesosPendientes.isEmpty())
                throw new NoProcessesException("No hay procesos pendientes para ejecutar.");

            if (procesoEnEjecucion != null)
                throw new NoRunningProcessException("Ya hay un proceso en ejecución: PID=" + procesoEnEjecucion.getPid());

            // Extrae el de mayor prioridad
            procesoEnEjecucion = procesosPendientes.remove();
            procesoEnEjecucion.setState(ProcessState.RUNNING);

            // Registrar en log y mostrar en consola
            String logLine = procesoEnEjecucion.toStringExecuting();
            logger.escribir(logLine);

            String eventsLog = procesoEnEjecucion.getEventsAsLogString();

            // Si hay múltiples eventos, getEventsAsLogString() devuelve saltos de línea
            // logger.escribirLinea() escribe cada línea sin timestamp
            String[] eventLines = eventsLog.split("\n");
            for (String eventLine : eventLines) {
                if (!eventLine.trim().isEmpty()) {
                    logger.escribirLinea(eventLine);
                }
            }

        } catch (NoProcessesException | NoRunningProcessException | EmptyHeapException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
    @Override
    public void finishProcessOk() {
        try {
            // vemos que exista un proceso ejecutandose
            if (procesoEnEjecucion == null)
                throw new NoRunningProcessException("No hay proceso en ejecución.");

            // el proceso pasa a estado FINISHED
            procesoEnEjecucion.setState(ProcessState.FINISHED);

            // guardamos el tipo de finalización
            procesoEnEjecucion.setFinishType(FinishType.OK);

            // registramos el evento en el log usando toStringEnding() de Process
            logger.escribir(procesoEnEjecucion.toStringEnding());

            // Hacer esta verificación antes del push al stack
            pushToFinishedStack(procesoEnEjecucion);

            //como es uno a la vez, queda null
            procesoEnEjecucion = null;

        } catch (NoRunningProcessException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void finishProcessError() {
        try {
            // verificamos que exista un proceso ejecutándose
            if (procesoEnEjecucion == null)
                throw new NoRunningProcessException("No hay proceso en ejecución.");

            // el proceso pasa a estado FINISHED
            procesoEnEjecucion.setState(ProcessState.FINISHED);

            // indicamos que terminó por ERROR
            procesoEnEjecucion.setFinishType(FinishType.ERROR);

            // registramos el evento en el log usando toStringEnding() de Process
            logger.escribir(procesoEnEjecucion.toStringEnding());

            // Hacer esta verificación antes del push al stack
            pushToFinishedStack(procesoEnEjecucion);

            //limpiamos
            procesoEnEjecucion = null;

        } catch (NoRunningProcessException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void terminateProcess(int uid) {
        try {
            // verificamos que exista un proceso ejecutándose
            if (procesoEnEjecucion == null)
                throw new NoRunningProcessException("No hay proceso en ejecución.");

            // buscamos el usuario que forzó la terminación
            User user = usuarios.get(uid);

            // si no existe, abortamos la operación
            if (user == null)
                throw new UserNotFoundException("No existe usuario con UID=" + uid);

            // el proceso pasa a estado FINISHED
            procesoEnEjecucion.setState(ProcessState.FINISHED);

            // indicamos que fue terminado manualmente
            procesoEnEjecucion.setFinishType(FinishType.TERMINATED);

            // guardamos quién lo terminó
            procesoEnEjecucion.setTerminatedBy(user);

            // registramos el evento en el log usando toStringEndingTerminated() de Process
            logger.escribir(procesoEnEjecucion.toStringEndingTerminated());

            // Hacer esta verificación antes del push al stack
            pushToFinishedStack(procesoEnEjecucion);

            procesoEnEjecucion = null;

        } catch (NoRunningProcessException | UserNotFoundException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
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
        System.out.println("PROCESS STATUS - USER UID:" + uid);

        System.out.println("EXECUTING:");
        if (procesoEnEjecucion != null) {
            System.out.println(procesoEnEjecucion);
            procesoEnEjecucion.printEvents();
        } else {
            System.out.println("Ninguno");
        }

        System.out.println("PENDING:");
        recorrerPendientes(procesosPendientes, uid, null, false);  // ← filterUid=uid
        System.out.println("FINISHED:");
        recorrerFinalizados(procesosFinalizados, uid, null, false);
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("PROCESS STATUS - PID:" + pid);
        // Buscar en EXECUTING, PENDING y FINISHED
        if (procesoEnEjecucion != null && procesoEnEjecucion.getPid() == pid) {
            System.out.println("  " + procesoEnEjecucion.toString());
            procesoEnEjecucion.printEvents();
            return;
        }
        System.out.println("PENDING:");
        recorrerEventosPendientes(procesosPendientes, null, pid, true);  // ← filterPid=pid, showEvents=true
        System.out.println("FINISHED:");
        recorrerEventosFinalizados(procesosFinalizados, null, pid, true);
    }


    ///////////////////////////////// MÉTODOS DE RECORRIDA //////////////////////////////////////////////////////

    // Recorre el heap con eventos, aplica filtros opcionales, y lo restaura
    private void recorrerEventosPendientes(MyHeap<Process> procesosPendientes) {
        recorrerEventosPendientes(procesosPendientes, null, null, true);
    }

    // Versión completa con filtros y control de eventos
    private void recorrerEventosPendientes(MyHeap<Process> procesosPendientes,
                                           Integer filterUid,
                                           Integer filterPid,
                                           boolean showEvents) {
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

        boolean hayCoincidencias = false;
        for (int i = 0; i < count; i++) {
            Process p = temp[i];

            // Aplicar filtros: si son null, se omite el filtro (muestra todos)
            if ((filterUid == null || p.getUser().getUid() == filterUid) &&
                    (filterPid == null || p.getPid() == filterPid)) {

                System.out.println("  " + p.toString());
                if (showEvents) {
                    p.printEvents();
                }
                hayCoincidencias = true;
            }
        }

        // Mostrar "(ninguno)" solo si hay filtro activo y no hubo coincidencias
        if ((filterUid != null || filterPid != null) && !hayCoincidencias) {
            System.out.println("  (ninguno)");
        }

        // Restaurar el heap original
        for (int i = 0; i < count; i++) {
            procesosPendientes.insert(temp[i]);
        }
    }

    // Recorre el stack con eventos, aplica filtros opcionales, y lo restaura
    private void recorrerEventosFinalizados(MyStack<Process> procesosFinalizados) {
        recorrerEventosFinalizados(procesosFinalizados, null, null, true);
    }

    // Versión completa con filtros y control de eventos
    private void recorrerEventosFinalizados(MyStack<Process> procesosFinalizados,
                                            Integer filterUid,
                                            Integer filterPid,
                                            boolean showEvents) {
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

        boolean hayCoincidencias = false;
        for (int i = 0; i < count; i++) {
            Process p = temp[i];

            // Aplicar filtros
            if ((filterUid == null || p.getUser().getUid() == filterUid) &&
                    (filterPid == null || p.getPid() == filterPid)) {

                // Para finalizados: usar toStringFinished() que ya tiene el formato correcto
                System.out.println("  " + p.toStringFinished());
                if (showEvents) {
                    p.printEvents();
                }
                hayCoincidencias = true;
            }
        }

        if ((filterUid != null || filterPid != null) && !hayCoincidencias) {
            System.out.println("  (ninguno)");
        }

        // Restaurar stack: reinsertar en orden inverso para mantener LIFO original
        for (int i = count - 1; i >= 0; i--) {
            procesosFinalizados.push(temp[i]);
        }
    }

    // Recorre el heap, ejecuta una acción por cada proceso, y lo restaura
    private void recorrerPendientes(MyHeap<Process> procesosPendientes) {
        recorrerPendientes(procesosPendientes, null, null, false);
    }

    // Versión completa con filtros y control de eventos
    private void recorrerPendientes(MyHeap<Process> procesosPendientes,
                                    Integer filterUid,
                                    Integer filterPid,
                                    boolean showEvents) {
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

        boolean hayCoincidencias = false;
        for (int i = 0; i < count; i++) {
            Process p = temp[i];

            if ((filterUid == null || p.getUser().getUid() == filterUid) &&
                    (filterPid == null || p.getPid() == filterPid)) {

                System.out.println("  " + p.toString());
                if (showEvents) {
                    p.printEvents();
                }
                hayCoincidencias = true;
            }
        }

        if ((filterUid != null || filterPid != null) && !hayCoincidencias) {
            System.out.println("  (ninguno)");
        }

        for (int i = 0; i < count; i++) {
            procesosPendientes.insert(temp[i]);
        }
    }

    // Recorre el stack, ejecuta una acción por cada proceso, y lo restaura
    private void recorrerFinalizados(MyStack<Process> procesosFinalizados) {
        recorrerFinalizados(procesosFinalizados, null, null, false);
    }

    // Versión completa con filtros y control de eventos
    private void recorrerFinalizados(MyStack<Process> procesosFinalizados,
                                     Integer filterUid,
                                     Integer filterPid,
                                     boolean showEvents) {
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

        boolean hayCoincidencias = false;
        for (int i = 0; i < count; i++) {
            Process p = temp[i];

            if ((filterUid == null || p.getUser().getUid() == filterUid) &&
                    (filterPid == null || p.getPid() == filterPid)) {

                System.out.println("  " + p.toStringFinished());
                if (showEvents) {
                    p.printEvents();
                }
                hayCoincidencias = true;
            }
        }

        if ((filterUid != null || filterPid != null) && !hayCoincidencias) {
            System.out.println("  (ninguno)");
        }

        for (int i = count - 1; i >= 0; i--) {
            procesosFinalizados.push(temp[i]);
        }
    }
}