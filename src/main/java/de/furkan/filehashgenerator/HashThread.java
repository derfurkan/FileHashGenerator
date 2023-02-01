package de.furkan.filehashgenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

public class HashThread implements Runnable {

  private final Algorithm algorithm;
  private final boolean isHostThread;
  private final Thread thread;
  Timer timer;
  private File outPutFile;
  private List<File> filesToGenerate;
  private File inputFolder;

  public HashThread(
      boolean hostThread,
      File outPutFile,
      File inputFolder,
      Algorithm algorithm,
      List<File> filesToGenerate,
      boolean multiThreading,
      int threads,
      boolean includeSubFolders) {
    this.thread = new Thread(this, "HashThread-" + (hostThread ? "Host" : "Worker"));
    this.isHostThread = hostThread;
    this.algorithm = algorithm;
    if (hostThread) {
      this.inputFolder = inputFolder;
      this.outPutFile = outPutFile;
      scanDirectory(inputFolder, includeSubFolders);
      this.thread.start();
      if (multiThreading) {
        int partitionSize = threads;
        List<List<File>> partitions = new LinkedList<>();
        List<Thread> threadList = new ArrayList<>();

        for (int i = 0; i < FileHashGenerator.allFiles.size(); i += partitionSize) {
          partitions.add(
              FileHashGenerator.allFiles.subList(
                  i, Math.min(i + partitionSize, FileHashGenerator.allFiles.size())));
        }
        partitions.forEach(
            files -> {
              threadList.add(
                  new HashThread(false, null, null, algorithm, files, false, 0, false).thread);
            });
        threadList.forEach(
            thread1 -> {
              try {
                thread1.join();
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
        endOperation();
      } else {
        HashThread hashThread =
            new HashThread(
                false, null, null, algorithm, FileHashGenerator.allFiles, false, 0, false);
        try {
          hashThread.thread.join();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        endOperation();
      }
    } else {
      this.filesToGenerate = filesToGenerate;
      this.thread.start();
    }
  }

  private void endOperation() {
    if (isHostThread) {
      timer.cancel();
      pasteStatus();
      System.out.println("\n Freeing up memory...");
      System.out.println(" Generating output file...");
      if (outPutFile.exists()) {
        outPutFile.delete();
      }
      try {
        outPutFile.createNewFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      try {
        FileWriter fileWriter = new FileWriter(outPutFile);
        HashMap<String, String> realMap = new HashMap<>();
        FileHashGenerator.fileHash.forEach(
            (s, s2) -> {
              realMap.put(s.substring(inputFolder.getAbsolutePath().length()), s2);
            });
        fileWriter.write(gson.toJson(realMap));
        fileWriter.flush();
        fileWriter.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      System.gc();
      System.runFinalization();
      System.out.println(" Successfully generated hashes!");
      System.exit(0);
    }
  }

  private void scanDirectory(File directory, boolean includeSubFolders) {
    if (!directory.isDirectory() || directory.listFiles() == null) {
      return;
    }
    for (File file : directory.listFiles()) {
      if (file.isDirectory() && includeSubFolders) {
        scanDirectory(file, true);
      } else if (file.isFile()) {
        FileHashGenerator.allFiles.add(file);
      }
    }
  }

  private void pasteStatus() {
    try {
      new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
    } catch (Exception e) {
    }
    StringBuilder stringBuilder = new StringBuilder("\n");
    for (File allFile : FileHashGenerator.allFiles) {
      stringBuilder.append(
          allFile.getName()
              + " | "
              + (FileHashGenerator.generatedFiles.contains(allFile)
                  ? "Generated"
                  : FileHashGenerator.failedFiles.contains(allFile) ? "Failed" : "...")
              + "\n");
    }
    stringBuilder.append(
        "\n "
            + (FileHashGenerator.generatedFiles.size() + FileHashGenerator.failedFiles.size())
            + "/"
            + FileHashGenerator.allFiles.size()
            + " ("
            + (int)
                (((FileHashGenerator.generatedFiles.size() + FileHashGenerator.failedFiles.size())
                        / (double) FileHashGenerator.allFiles.size())
                    * 100)
            + "%)\n  "
            + FileHashGenerator.failedFiles.size()
            + " Failed file hashes\n  "
            + FileHashGenerator.generatedFiles.size()
            + " Generated file hashes\n\n Total memory: "
            + (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE
                ? "No Limit"
                : Runtime.getRuntime().maxMemory() / 1_000_000)
            + "Mb"
            + "\n Free memory: "
            + Runtime.getRuntime().freeMemory() / 1_000_000
            + "Mb\n\n Press CTRL+C to abort!");
    System.out.println(" ");
    System.out.println(stringBuilder);
  }

  @Override
  public void run() {
    if (isHostThread) {
      TimerTask pasteStatus =
          new TimerTask() {
            public void run() {
              pasteStatus();
            }
          };
      timer = new Timer("printStatusTimer");
      timer.scheduleAtFixedRate(pasteStatus, 1000, 1000);
    } else {
      sortListByFilesSize(filesToGenerate)
          .forEach(
              file -> {
                try {
                  FileHashGenerator.fileHash.put(
                      file.getAbsolutePath(), getFileChecksum(file, algorithm));
                  FileHashGenerator.generatedFiles.add(file);
                } catch (Exception e) {
                  FileHashGenerator.failedFiles.add(file);
                }
              });
    }
  }

  private List<File> sortListByFilesSize(List<File> files) {
    files.sort(
        (o1, o2) -> {
          o1 = new File(o1.getPath());
          o2 = new File(o2.getPath());
          if (o1.length() > o2.length()) {
            return 1;
          } else if (o1.length() < o2.length()) {
            return -1;
          } else {
            return 0;
          }
        });
    return files;
  }

  private String getFileChecksum(File file, Algorithm algorithm) throws Exception {
    MessageDigest messageDigest = null;
    CRC32 crc32 = new CRC32();
    Adler32 adler32 = new Adler32();

    if (algorithm != Algorithm.CRC32 && algorithm != Algorithm.ADLER32) {
      messageDigest = MessageDigest.getInstance(algorithm.name());
    }

    // Get file input stream for reading the file content
    FileInputStream fis = new FileInputStream(file);

    // Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0;

    // Read file data and update in message digest
    while ((bytesCount = fis.read(byteArray)) != -1) {
      if (algorithm == Algorithm.CRC32) {
        crc32.update(byteArray, 0, bytesCount);
      } else if (algorithm == Algorithm.ADLER32) {
        adler32.update(byteArray, 0, bytesCount);
      } else {
        messageDigest.update(byteArray, 0, bytesCount);
      }
    }

    // close the stream; We don't need it now.
    fis.close();

    // Get the hash's bytes
    if (algorithm == Algorithm.CRC32) {
      return String.format(Locale.US, "%08X", crc32.getValue());
    } else if (algorithm == Algorithm.ADLER32) {
      return String.format(Locale.US, "%08X", adler32.getValue());
    } else {
      byte[] bytes = messageDigest.digest();
      // This bytes[] has bytes in decimal format;
      // Convert it to hexadecimal format
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < bytes.length; i++) {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }
      return sb.toString();
    }
  }
}
