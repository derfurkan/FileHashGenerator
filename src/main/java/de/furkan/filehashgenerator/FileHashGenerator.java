package de.furkan.filehashgenerator;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class FileHashGenerator {
  public static HashMap<String, String> fileHash = new HashMap<>();

  public static List<File> allFiles = new ArrayList<>(),
      generatedFiles = new ArrayList<>(),
      failedFiles = new ArrayList<>();

  public static void main(String[] args) {
    boolean includeSubFolders = false;
    boolean multiThreading = false;
    int threads = 1;
    File startFolder;
    try {
      startFolder =
          new File(
                  FileHashGenerator.class
                      .getProtectionDomain()
                      .getCodeSource()
                      .getLocation()
                      .toURI())
              .getParentFile();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    Scanner scanner = new Scanner(System.in);
    System.out.println("\n");
    System.out.println("Hashes will be generated for " + startFolder.getAbsolutePath());
    System.out.println("Should we include sub-folders? (y/n) ");
    String answer = scanner.nextLine();
    if (answer.equalsIgnoreCase("y")) {
      includeSubFolders = true;
    }
    System.out.println("\n");
    System.out.println("Please specify an algorithm (MD5, SHA1, SHA256, SHA512, ADLER32, CRC32)");
    String algorithm = scanner.nextLine();
    try {
      Algorithm.valueOf(algorithm);
    } catch (Exception e) {
      System.out.println(" Invalid algorithm!");
      return;
    }

    System.out.println("\n");
    System.out.println(
        "Please specify a path and file name for the output file that contains all hashes (ex. C:\\Users\\USER\\Desktop\\hashes.json)");
    String fileName = scanner.nextLine();
    if (fileName.isEmpty() || new File(fileName).exists()) {
      System.out.println(" Invalid file name or file already exists!");
      return;
    }
    System.out.println("\n");
    System.out.println("Do you want to use multithreading? (y/n)");
    answer = scanner.nextLine();
    if (answer.equalsIgnoreCase("y")) {
      multiThreading = true;
    }

    if (multiThreading) {
      System.out.println("\n");
      System.out.println("How many threads should be used?");
      threads = scanner.nextInt();
    }

    new HashThread(
        true,
        new File(fileName),
        startFolder,
        Algorithm.valueOf(algorithm),
        null,
        multiThreading,
        threads,
        includeSubFolders);
  }
}
