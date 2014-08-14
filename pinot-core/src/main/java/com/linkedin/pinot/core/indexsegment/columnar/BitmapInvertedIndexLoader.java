package com.linkedin.pinot.core.indexsegment.columnar;

import java.io.File;
import java.io.IOException;

import com.linkedin.pinot.common.segment.ReadMode;

public class BitmapInvertedIndexLoader {
  public static BitmapInvertedIndex load(File file, ReadMode mode, ColumnMetadata metadata) throws IOException {
    switch (mode) {
      case heap:
        return loadHeap(file, metadata);
      case mmap:
      default:
        return loadMmap(file, metadata);
    }
  }

  private static BitmapInvertedIndex loadMmap(File file, ColumnMetadata metadata) throws IOException {
    return new BitmapInvertedIndex(file, ReadMode.mmap, metadata);
  }

  private static BitmapInvertedIndex loadHeap(File file, ColumnMetadata metadata) throws IOException {
    return new BitmapInvertedIndex(file, ReadMode.heap, metadata);
  }
}