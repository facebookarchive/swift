package com.facebook.nifty.client;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

public class Test {
  public static void main(String[] args) {
      File[] files = new File("/Users/jaxlaw").listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().startsWith("screenshot-");
        }
      });
      Arrays.sort(files, new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
          return o2.getName().compareTo(o1.getName());
        }
      });
      for (File file : files) {
        System.out.println(file.getName());
      }
    }
}
