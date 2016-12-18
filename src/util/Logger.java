package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {
    public static PrintWriter out;

    static {

        try {
            out = new PrintWriter(new FileWriter("debugv15.log"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
