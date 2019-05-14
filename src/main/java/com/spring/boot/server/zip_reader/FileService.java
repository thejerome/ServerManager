package com.spring.boot.server.zip_reader;

import com.spring.boot.server.exceptions.FileNotUploadException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

  public static String libDir = null;
  public static String rscDir = null;
  public static String jarDir = null;

//  public static void main(String[] args) throws IOException {
//    String fileZip = "src/main/resources/unzipTest/server.zip";
//    File file = new File(fileZip);
//    unZipFile(file);
//    System.out.println(jarDir);
//    System.out.println(rscDir);
//    System.out.println(libDir);
//  }

//  public static boolean unZip(String zipFilePath, File destDir) throws IOException {
//    try {
//      System.out.println("zipFilePath = " + zipFilePath);
//      ZipFile zipFile = new ZipFile(zipFilePath);
//
//      Enumeration<? extends ZipEntry> entries = zipFile.entries();
//
//      while (entries.hasMoreElements()) {
//        ZipEntry entry = entries.nextElement();
//        if (entry.isDirectory()) {
//          System.out.print("dir  : " + entry.getName());
//          String destPath = destDir + File.separator + entry.getName();
//          System.out.println(" => " + destPath);
//          checkDir(entry, destDir);
//          File file = new File(destPath);
//          file.mkdirs();
//        } else {
//          String destPath = destDir + File.separator + entry.getName();
//
//          try (InputStream inputStream = zipFile.getInputStream(entry);
//              FileOutputStream outputStream = new FileOutputStream(destPath);
//          ) {
//            int data = inputStream.read();
//            while (data != -1) {
//              outputStream.write(data);
//              data = inputStream.read();
//            }
//          }
//          System.out.println("file : " + entry.getName() + " => " + destPath);
//          if (entry.getName().contains("server.jar")) {
//            jarDir = destDir + File.separator + entry.getName();
//          }
//        }
//      }
//    } catch (IOException e) {
//      throw new RuntimeException("Error unzipping file " + zipFilePath, e);
//    }
//    return true;
//  }

  public static void checkDir(ZipEntry entry, File destDir) {
    if (entry.getName().equals("server/lib/")) {
      libDir = destDir + File.separator + entry.getName() + "*";
    }

    if (entry.getName().equals("server/rsc/")) {
      rscDir = destDir + File.separator + entry.getName() + "*";
    }
  }


  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  @Autowired
  private Environment env;

  public void upload(MultipartFile uploadFile) throws IOException {
    final String path =
        System.getProperty("user.dir") + File.separator + env.getProperty("paths.uploadedFiles");
    File serverDir = new File(path, env.getProperty("paths.dir"));
    if (serverDir.getParentFile().listFiles().length != 0) {
      PathMatcher requestPathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.desc");
      Stream.of(serverDir.listFiles()).filter(p -> !requestPathMatcher.matches(p.toPath()))
          .forEach(f -> deleteFile(f));
    }
    String name = uploadFile.getOriginalFilename();
    name.lastIndexOf('.');
    File newServerZip = new File(serverDir, uploadFile.getOriginalFilename());
    String ex = name.substring(name.lastIndexOf('.') + 1);
    if (ex.equalsIgnoreCase("zip")) {
      if (!uploadFile.isEmpty()) {
        if (uploadedFiles(uploadFile, newServerZip)) {
          if (unZipFile(newServerZip)) {
            newServerZip.delete();
          }
        }
      } else {
        throw new FileNotUploadException("Empty zip file " + uploadFile.getName());
//        logger.error("Empty zip file " + uploadFile.getName());
      }
    } else {
      throw new FileNotUploadException(
          "Invalid file extension expected 'zip', actually '" + ex + "'");
//      logger.error("Invalid file extension expected 'zip', actually '" + ex + "'");
    }
  }

  private boolean uploadedFiles(MultipartFile uploadFile, File newServer) {
    try {
      byte[] bytes = uploadFile.getBytes();
      BufferedOutputStream stream =
          new BufferedOutputStream(new FileOutputStream(newServer));
      stream.write(bytes);
      stream.close();
//      logger.model("File " + uploadFile.getOriginalFilename() + " uploaded");
      return true;
    } catch (Exception e) {
//      logger.error("Error upload file " + uploadFile.getName(), e.fillInStackTrace());
      return false;
    }
  }

  private static boolean unZipFile(File zipFile) {
    try {
      String property = "lab.desc";
      ZipFile zip = new ZipFile(zipFile);
      Enumeration entries = zip.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();
        if (entry.isDirectory()) {
          checkDir(entry, zipFile.getParentFile().getCanonicalFile());
          new File(zipFile.getParent(), entry.getName()).mkdirs();
        } else {
          if (!entry.getName().equals(property)) {
            write(zip.getInputStream(entry),
                new BufferedOutputStream(new FileOutputStream(
                    new File(zipFile.getParent(), entry.getName()))));
          }
          if (entry.getName().contains("server.jar")) {
            jarDir = zipFile.getParentFile().getCanonicalFile() + File.separator + entry.getName();
          }
        }
        System.out.println(entry.getName());
//        logger.model(entry.getName());
      }
//      logger.model(zipFile.getName() + " unzip");

      zip.close();
      return true;
    } catch (IOException e) {
//      logger.error("Error unzip file " + zipFile.getName(), e.fillInStackTrace());
    }
    return false;
  }

  private static void write(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer)) >= 0) {
      out.write(buffer, 0, len);
    }
    out.close();
    in.close();
  }

  private void deleteFile(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        File f = new File(dir, children[i]);
        deleteFile(f);
      }
      dir.delete();
      logger.info("Delete dir " + dir.getAbsolutePath());
    } else {
      dir.delete();
      logger.info("Delete file " + dir.getAbsolutePath());
    }
  }

}