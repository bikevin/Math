package com.kevin.math;

/**
 * Created by Kevin on 2/26/2016.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;

/** This class is used only if option 2 for the resource management is selected */
public abstract class SimpleResourceHelper
{
    public static boolean copyResourcesFromAssets(AssetManager am, final String subfolder, final String resourcePath,
                                                  final String[] resources)
    {
        boolean done = true;
        try
        {
            // Be sure target folder exists
            File resourceFolder = new File(resourcePath);
            resourceFolder.mkdirs();

            for (final String filename : resources)
            {
                // Files
                String srcFilename = getPathFor(subfolder, filename);
                String dstFilename = getPathFor(resourcePath, filename);

                // Reading source file.
                InputStream is = am.open(srcFilename);
                //InputStream is = am.open(am.list("")[1]);
                byte[] buffer = new byte[is.available()];
                is.read(buffer);

                // Create destination file to copy into.
                File dst = new File(dstFilename);
                if (!dst.exists())
                {
                    dst.createNewFile();
                    FileOutputStream out = new FileOutputStream(dst);
                    out.write(buffer);
                    out.close();
                }
            }
        }
        catch (IOException e)
        {
            done = false;
            e.printStackTrace();
        }
        return done;
    }

    private static String getPathFor(final String folder, final String filename)
    {
        String filepath = folder;
        if (!filepath.endsWith(java.io.File.separator))
            filepath += java.io.File.separator;
        if (filename.startsWith(java.io.File.separator))
            filepath += filename.substring(1);
        else
            filepath += filename;
        return filepath;
    }
}
