package uy.edu.um.entities;

import uy.edu.um.tad.list.MyList;

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

    public Log() {
        String fechaHoy = LocalDate.now().format(FORMATO_FECHA);

        this.nombreArchivo = "DOORS_PROCESS_LOG_" + fechaHoy + ".txt";
        //nombre del archivo

        this.rutaArchivo = "src/main/" + nombreArchivo;
        //esta es la ruta completa del archivo, el programa se ejecuta desde el directorio del trabajo
        //desde la raíz del proyecto

        try {
            // intenta crear el archivo si no existe
            java.nio.file.Path path = java.nio.file.Paths.get(rutaArchivo);
            if (!java.nio.file.Files.exists(path)) {
                java.nio.file.Files.createFile(path);
            }
        } catch (IOException e) {
            System.err.println("Error al crear el archivo de log: " + e.getMessage());
        }
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
            //puede suceder este error, si no se pudo crear el archivo, si falla la escritura
            // o si la ruta es invalida
        }
    }

    //Sin timestamp para los eventos y stack overflow
    public void escribirLinea(String linea) {
        try {
            FileWriter fw = new FileWriter(rutaArchivo, true);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(linea);
            pw.close();
        } catch (IOException e) {
            System.out.println("Error al escribir el log");
        }
    }

}