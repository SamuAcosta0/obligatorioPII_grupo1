package uy.edu.um.importer;
import uy.edu.um.entities.User;
import uy.edu.um.entities.UserType;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.ProcessState;
import uy.edu.um.entities.Event;
import uy.edu.um.entities.Event.EventType;
import uy.edu.um.exceptions.OwnerNotFoundException;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.queue.MyQueue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DataLoader {

    // ─── Carga usuarios ────────────────────────────────────────
    // Formato esperado: uid;alias;type
    public static void loadUsers(String csvPath, MyHash userTable) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {

            reader.readLine(); // descarta encabezado

            String line; //creo línea
            // Mientras la fila no esté vacía continuar leyendo
            while ((line = reader.readLine()) != null) {
                line = line.trim(); //retiro los espacios de cada fila
                if (line.isEmpty()) continue;// si la fila está vacía continúo
                //Separo los futuros atributos de usuarios en función de ;
                String[] fields = line.split(";");
                if (fields.length < 3) continue; // se la fila no tiene los datos completo no la uso
                //asigno a cada atributo de usuario su respectivo valor en el csv
                int uid = Integer.parseInt(fields[0].trim());
                String alias = fields[1].trim();
                UserType type = UserType.valueOf(fields[2].trim());
                //coloco en el hash en función de del uid el nuevo usuario
                userTable.put(uid, new User(uid, alias, type));
            }

        } catch (IOException e) {
            System.err.println("Error leyendo usuarios: " + e.getMessage());
        }
    }

    //  CARGA DE PROCESOS
    // Formato: pid;uid;name;{TIPO:[i1, i2]# TIPO:[i1, i2]}
    public static void loadProcesses(String csvPath, MyQueue newQueue, MyHash userTable) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {

            reader.readLine();

            String line;
            //Se recorre linea por linea el archivo
            //cuando line es null significa de que el archivo está vacío
            while ((line = reader.readLine()) != null) {
                line = line.trim(); //se eliminan los espacios de las filas
                if (line.isEmpty()) continue; //si hay una fila vacía continúa

                Process process = parseLine(line, userTable); //se manda la fila junto con la tabla de usuarios
                if (process != null) {
                    newQueue.enqueue(process);
                }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo procesos: " + e.getMessage());
        }
    }

    private static Process parseLine(String line, MyHash userTable) throws OwnerNotFoundException {
        // split por ; con límite 4 para no partir el bloque de eventos
        // "26362;98;python.exe;{RAM:[...]# CPU:[...]}"
        String[] fields = line.split(";", 4);
        if (fields.length < 4) return null; //Si la lista de procesos se encuentra incompleta, se retorna null

        //Luego de realizados los cortes con el line.split, se toma cada campo como un atributo
        //para la creación del proceso
        int pid = Integer.parseInt(fields[0].trim());
        int uid = Integer.parseInt(fields[1].trim());
        String name = fields[2].trim();
        //Se crea un atributo bloque que contiene todos los eventos del proceso aún sin separar
        String block = fields[3].trim(); // "{RAM:[...]# CPU:[...]}"

        ///---En caso de que el usuario no exista, el proceso es omitido y lanza excepción
        User owner = (User) userTable.get(uid); //En esta linea se usa el hash de usuarios del sistema y se busca si el usuario owner del proceso existe
        if (owner == null) {
            System.err.println("UID " + uid + " no encontrado, proceso " + pid + " omitido.");
            throw new OwnerNotFoundException("No se ha encontrado el usuario dueño del proceso");
        }

        try {
            //En caso de que todo salga bien se procede a la creación del proceso
            Process process = new Process(pid, name, owner, ProcessState.NEW); //Asigno al proceso el estado New

            // quitar corchetes: {RAM:[...]# CPU:[...]} → RAM:[...]# CPU:[...]
            //substring(inicio, fin), se quita el primer corchete, y el último, con el .trim() se le quitan los espacios
            block = block.substring(1, block.length() - 1).trim();

            // el block extraido del csv que contiene los eventos del proceso se le aplica la función split
            String[] eventTokens = block.split("#");
            //se recorre el array string que contiene la lista de eventos
            for (String token : eventTokens) {
                token = token.trim();
                // token = "RAM:[release, cache, cache]"

                int bracketOpen = token.indexOf(":[");
                if (bracketOpen == -1) continue;
                //extraccion de tipo de evento, va desde el inicio hasta la posicion del bracketOpen y lo extrae limpio
                String typeName = token.substring(0, bracketOpen).trim();

                String instrPart = token.substring(bracketOpen + 2, token.length() - 1);
                // instrPart = "release, cache, cache" me quedo con los eventos

                //Creo un objeto de tipo EventType y busco el valor que tiene typeName de ese enum
                EventType eType = EventType.valueOf(typeName);
                Event event = new Event(eType); //creo el nuevo evento de tipo eType encontrado

                String[] instrs = instrPart.split(","); // creo un array con las instrucciones separadas
                //recorro cada instruccion y las voy añadiendo al evento de un tipo en específico
                for (String instr : instrs) {
                    event.addInstruction(instr.trim());
                }
                //una vez creado el evento con sus instrucciones se añade a la lista de eventos en proceso
                process.addEvent(event);
            }

            return process;

        } catch (Exception e) {
            System.err.println("Línea malformada, se omite: " + line);
            return null; // ← solo atrapa errores de formato
        }
    }
}

// POR EL MOMENTO NO SE HA DICHO QUE HACER SI SE EJECUTA 2 VECES EL PLOAD, ESPERAR RESPUESTA DEL DOCENTE, POR AHORA
// SUPUESTO: EL PLOAD SE EJECUTA SOLO UNA VEZ



