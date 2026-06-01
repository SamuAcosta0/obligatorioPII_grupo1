package uy.edu.um;

import uy.edu.um.doors.ProcessConsole;
import uy.edu.um.doors.ProcessManagerImpl;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        ProcessConsole pc = new ProcessConsole(new ProcessManagerImpl());
        pc.init();

    }
}