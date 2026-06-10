package uy.edu.um.doors;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.User;
import uy.edu.um.exceptions.ProcessNotFoundException;
import uy.edu.um.exceptions.UserProcessNotFoundException;
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
import uy.edu.um.exceptions.ExecutingProcessException;


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
                logger.escribirLinea(p.toStringStackOverflow());
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
                logger.escribir("NEW PENDING PROCESS: " + p.toString());

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
                throw new ExecutingProcessException("Ya hay un proceso en ejecución: PID=" + procesoEnEjecucion.getPid());

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

        } catch (NoProcessesException | ExecutingProcessException | EmptyHeapException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
    @Override
    public void finishProcessOk() {
        try {
            // vemos que exista un proceso ejecutandose
            if (procesoEnEjecucion == null)
                throw new ExecutingProcessException("No hay proceso en ejecución.");

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

        } catch (ExecutingProcessException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void finishProcessError() {
        try {
            // verificamos que exista un proceso ejecutándose
            if (procesoEnEjecucion == null)
                throw new ExecutingProcessException("No hay proceso en ejecución.");

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

        } catch (ExecutingProcessException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void terminateProcess(int uid) {
        try {
            // verificamos que exista un proceso ejecutándose
            if (procesoEnEjecucion == null)
                throw new ExecutingProcessException("No hay proceso en ejecución.");

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

        } catch (ExecutingProcessException | UserNotFoundException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void printStatus() {
        System.out.println("PROCESS STATUS");

        System.out.println("EXECUTING:");
        if (procesoEnEjecucion != null) {
            System.out.println("  " + procesoEnEjecucion);
        }

        System.out.println("PENDING:");
        recorrerPendientes(null, null, false, null);

        System.out.println("FINISHED:");
        recorrerFinalizados(null, null, false, null);
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("PROCESS STATUS");

        System.out.println("EXECUTING:");
        if (procesoEnEjecucion != null) {
            System.out.println("  " + procesoEnEjecucion);
            procesoEnEjecucion.printEvents();
        }

        System.out.println("PENDING:");
        recorrerPendientes(null, null, true ,null);

        System.out.println("FINISHED:");
        recorrerFinalizados(null, null, true, null);
    }

    @Override
    public void printStatusByUser(int uid) {
        try {
            boolean enEjecucion = (procesoEnEjecucion != null && procesoEnEjecucion.getUser().getUid() == uid);

            // Verificar existencia ANTES de imprimir cualquier header
            if (!enEjecucion && !existeEnPendientes(uid, null) && !existeEnFinalizados(uid, null)) {
                throw new UserProcessNotFoundException(uid);
            }

            System.out.println("PROCESS STATUS - USER UID:" + uid);

            System.out.println("EXECUTING:");
            if (enEjecucion) {
                System.out.println("  " + procesoEnEjecucion.toString());
                procesoEnEjecucion.printEvents();
            }

            System.out.println("PENDING:");
            recorrerPendientes(uid, null, false, null);

            System.out.println("FINISHED:");
            recorrerFinalizados(uid, null, false, null);

        } catch (UserProcessNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }




    @Override
    public void printStatusByProcess(int pid) {
        boolean enEjecucion = (procesoEnEjecucion != null && procesoEnEjecucion.getPid() == pid);

        if (enEjecucion) {
            System.out.println("PROCESS STATUS - PID:" + pid);
            System.out.println("EXECUTING:");
            System.out.println("  " + procesoEnEjecucion.toString());
            procesoEnEjecucion.printEvents();
            return;
        }
        // Recorre pendientes UNA vez; imprime header+proceso solo si lo encuentra
        if (recorrerPendientes(null, pid, true, "PROCESS STATUS - PID:" + pid + "\nPENDING:")) {
            return;
        }

        // Si no estaba en pendientes, recorre finalizados UNA vez
        if (recorrerFinalizados(null, pid, true, "PROCESS STATUS - PID:" + pid + "\nFINISHED:")) {
            return;
        }

        // No apareció en ninguna sección
        System.out.println(new ProcessNotFoundException(pid).getMessage());
    }




    //////////////////////////////////////////METODOS DE RECORRIDA//////////////////////////////////////////
    private boolean recorrerPendientes(Integer filterUid, Integer filterPid, boolean showEvents, String header) {
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
                // Imprime el header solo la primera vez que encuentra coincidencia
                if (!hayCoincidencias && header != null) {
                    System.out.println(header);
                }
                System.out.println("  " + p.toString());
                if (showEvents) {
                    p.printEvents();
                }
                hayCoincidencias = true;
            }
        }

        for (int i = 0; i < count; i++) {
            procesosPendientes.insert(temp[i]);
        }

        return hayCoincidencias;
    }
    // Recorre el stack de finalizados aplicando filtros opcionales (null = sin filtro),
// imprime cada coincidencia indentada y restaura el stack. Retorna true si hubo coincidencias.
// Si header != null, lo imprime una sola vez antes de la primera coincidencia.
    private boolean recorrerFinalizados(Integer filterUid, Integer filterPid, boolean showEvents, String header) {
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
                // Imprime el header solo la primera vez que encuentra coincidencia
                if (!hayCoincidencias && header != null) {
                    System.out.println(header);
                }
                System.out.println("  " + p.toStringFinished());
                if (showEvents) {
                    p.printEvents();
                }
                hayCoincidencias = true;
            }
        }

        // Restaurar el stack en orden inverso para preservar el LIFO original
        for (int i = count - 1; i >= 0; i--) {
            procesosFinalizados.push(temp[i]);
        }

        return hayCoincidencias;
    }

    // Verifica si existe algún proceso que cumpla el filtro en el heap de pendientes (vacía y restaura).
    private boolean existeEnPendientes(Integer filterUid, Integer filterPid) {
        int size = procesosPendientes.size();
        Process[] temp = new Process[size];
        int count = 0;
        boolean encontrado = false;

        while (!procesosPendientes.isEmpty()) {
            try {
                temp[count++] = procesosPendientes.remove();
            } catch (EmptyHeapException e) {
                break;
            }
        }

        for (int i = 0; i < count; i++) {
            if ((filterUid == null || temp[i].getUser().getUid() == filterUid) &&
                    (filterPid == null || temp[i].getPid() == filterPid)) {
                encontrado = true;
            }
            procesosPendientes.insert(temp[i]); // Restaurar
        }

        return encontrado;
    }

    // Verifica si existe algún proceso que cumpla el filtro en el stack de finalizados (vacía y restaura).
    private boolean existeEnFinalizados(Integer filterUid, Integer filterPid) {
        int size = procesosFinalizados.size();
        Process[] temp = new Process[size];
        int count = 0;
        boolean encontrado = false;

        while (!procesosFinalizados.isEmpty()) {
            try {
                temp[count++] = procesosFinalizados.pop();
            } catch (EmptyStackException e) {
                break;
            }
        }

        for (int i = count - 1; i >= 0; i--) {
            if ((filterUid == null || temp[i].getUser().getUid() == filterUid) &&
                    (filterPid == null || temp[i].getPid() == filterPid)) {
                encontrado = true;
            }
            procesosFinalizados.push(temp[i]); // Restaurar (LIFO)
        }

        return encontrado;
    }

}
