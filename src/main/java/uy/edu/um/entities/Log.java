package uy.edu.um.entities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    //formato que pide la consigna para el nombre del archivo
    private static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    //formato que pide la consigna para el timestamp de cada mensaje del log fecha

    //guardamos la ruta del archivo
    private String rutaArchivo;
    private String nombreArchivo;

    public Log() throws IOException {
        String fechaHoy = LocalDate.now().format(FORMATO_FECHA);

        this.nombreArchivo = "DOORS_PROCESS_LOG_" + fechaHoy + ".txt";
        //nombre del archivo

        this.rutaArchivo = "src/main/" + nombreArchivo;
        //esta es la ruta completa del archivo, el programa se ejecuta desde el directorio del trabajo
        //desde la raíz del proyecto
    }

    private String generarTimestamp() {
        return LocalDateTime.now().format(FORMATO_TIMESTAMP);
    }

    public void escribir(String mensaje) {
        //IOException para problemas de leer archivos, escribir archivos, abrir archivos, etc
        try {
            FileWriter fw = new FileWriter(rutaArchivo,true);
            //escribir en el archivo, si no existe lo crea, si exsite lo sobreescrbe
            //le damos como parametro la ruta del archivo, que tiene la ubicación y el nombre
            //así Java sabe donde crear el archivo
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