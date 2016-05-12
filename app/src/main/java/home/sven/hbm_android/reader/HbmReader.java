package home.sven.hbm_android.reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import eu.chainfire.libsuperuser.Shell;

public class HbmReader {
    private String hbmFilePath;
    private BufferedReader bufferedReader;

    public HbmReader(String hbmFilePath) {
        this.hbmFilePath = hbmFilePath;
        try {
            bufferedReader = new BufferedReader(new FileReader(hbmFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean isHbmEnabled() {
        /*try {
            if (bufferedReader.readLine().equals("1")) {
                bufferedReader.reset();
                return true;
            } else {
                bufferedReader.reset();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } */
        if(Shell.SU.run("cat "+hbmFilePath).equals("1")) {
            return true;
        }
        return false;
    }
}
