package uy.edu.um.entities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private String nombreArchivo;

    public Log() throws IOException {
        this.nombreArchivo = "DOORS_PROCESS_LOG_FECHA" + java.time.LocalDate.now();
        //ponemos el nombre del archivo
    }

    private String generarTimestamp(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss"));
    }
    FileWriter fw = new FileWriter(nombreArchivo + ".txt", true);
    //agregar al final del archivo existente

    public void escribir(String mensaje) {
        //IOException para problemas de leer archivos, escribir archivos, abrir archivos, etc
        try {
            FileWriter fw = new FileWriter(nombreArchivo + ".txt", true);
            //escribir en el archivo, si no existe lo crea, si exsite lo sobreescrbe
            PrintWriter pw = new PrintWriter(fw);
            //imprimir el archivo
            pw.println("[" + generarTimestamp() + "]: " + mensaje);;
            pw.close();
            //close para cerrar el archivo, esto evitará problems

        } catch (IOException e) {
            System.out.println("Error al escribir el log");
            //puede suceder este error, si no se oudo crear el archivo, si falla la escritura
            // o si la ruta es invalida
        }

    }
}
