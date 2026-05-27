package uy.edu.um.importer;

import uy.edu.um.entities.User;
import uy.edu.um.entities.UserType;
import uy.edu.um.entities.Process;
import uy.edu.um.entities.ProcessState;
import uy.edu.um.entities.Event;
import uy.edu.um.entities.Event.EventType;
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

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] fields = line.split(";");
                if (fields.length < 3) continue;

                int uid        = Integer.parseInt(fields[0].trim());
                String alias   = fields[1].trim();
                UserType type  = UserType.valueOf(fields[2].trim());

                userTable.put(uid, new User(uid, alias, type));
            }

        } catch (IOException e) {
            System.err.println("Error leyendo usuarios: " + e.getMessage());
        }
    }

    // ─── Carga procesos ────────────────────────────────────────
    // Formato: pid;uid;name;{TIPO:[i1, i2]# TIPO:[i1, i2]}
    public static void loadProcesses(String csvPath, MyQueue newQueue, MyHash userTable) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {


            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                Process process = parseLine(line, userTable);
                if (process != null) {
                    newQueue.enqueue(process);
                }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo procesos: " + e.getMessage());
        }
    }

    // ─── Parsea una línea completa → Process ───────────────────
    private static Process parseLine(String line, MyHash userTable) {
        try {
            // split por ; con límite 4 para no partir el bloque de eventos
            // "26362;98;python.exe;{RAM:[...]# CPU:[...]}"
            String[] fields = line.split(";", 4);
            if (fields.length < 4) return null;

            int pid      = Integer.parseInt(fields[0].trim());
            int uid      = Integer.parseInt(fields[1].trim());
            String name  = fields[2].trim();
            String block = fields[3].trim(); // "{RAM:[...]# CPU:[...]}"

            User owner = (User) userTable.get(uid);
            if (owner == null) {
                System.err.println("UID " + uid + " no encontrado, proceso " + pid + " omitido.");
                return null;
            }

            Process process = new Process(pid, name, owner, ProcessState.NEW);

            // quitar llaves externas: {RAM:[...]# CPU:[...]}  →  RAM:[...]# CPU:[...]
            block = block.substring(1, block.length() - 1).trim();

            // separar eventos por #
            String[] eventTokens = block.split("#");

            for (String token : eventTokens) {
                token = token.trim();
                // token = "RAM:[release, cache, cache]"

                int bracketOpen  = token.indexOf(":[");
                if (bracketOpen == -1) continue;

                String typeName = token.substring(0, bracketOpen).trim();
                // instrucciones entre [ y ]
                String instrPart = token.substring(bracketOpen + 2, token.length() - 1);
                // instrPart = "release, cache, cache"

                EventType eType = EventType.valueOf(typeName);
                Event event = new Event(eType);

                String[] instrs = instrPart.split(",");
                for (String instr : instrs) {
                    event.addInstruction(instr.trim());
                }

                process.addEvent(event);
            }

            return process;

        } catch (Exception e) {
            System.err.println("Línea malformada, se omite: " + line);
            return null;
        }
    }
}