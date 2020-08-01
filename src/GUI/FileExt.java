package GUI;

import java.io.File;

class FileExt {
    static String getFilenameWithoutExtension(File toGet) throws Exception {
        String[] possibleExtensions = new String[]{".s", ".S", ".asm"};

        String filename = toGet.getAbsolutePath();
        int numExtension = 0;
        int indexOfExtension = -1;
        while (numExtension < possibleExtensions.length && indexOfExtension == -1) {
            indexOfExtension = filename.indexOf(possibleExtensions[numExtension]);
            numExtension++;
        }

        if (indexOfExtension == -1) {
            throw new Exception("Unsupported file type");
        } else {
            return filename.substring(0, indexOfExtension);
        }
    }
}
