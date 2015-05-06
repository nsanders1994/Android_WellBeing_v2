package com.example.natalie.android_wellbeing;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Created by Natalie on 2/8/2015.
**/

public class InstallationID {
    /**
     *  This class retrieves the installation ID of the app which is created once in the app's lifetime
    **/

    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    public synchronized static String id(Context context) {
        if (sID == null) {
            // Retrieve the installation file
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                // If the installation file doesn't exist, create it and the app ID
                if (!installation.exists())
                    writeInstallationFile(installation);

                // Read installation file for the sID
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        /**
         * Read the installation file for the app ID
        **/

        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        /* Writes a random number as the app ID to the newly created Installation file */

        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }
}
