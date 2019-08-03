// package org.molgenis.emx2.io.csv;
//
// import com.fasterxml.jackson.databind.MappingIterator;
// import com.fasterxml.jackson.databind.ObjectReader;
// import com.fasterxml.jackson.dataformat.csv.CsvMapper;
// import com.fasterxml.jackson.dataformat.csv.CsvSchema;
// import com.univocity.parsers.common.record.Record;
// import com.univocity.parsers.csv.CsvParser;
// import com.univocity.parsers.csv.CsvParserSettings;
// import org.molgenis.Row;
// import org.molgenis.beans.Row;
//
// import java.io.*;
// import java.util.Iterator;
// import java.util.LinkedHashMap;
// import java.util.Map;
//
// public class RowReaderJackson {
//
//  private static ObjectReader reader = new CsvMapper().readerFor(Map.class);
//
//  /** Don't use because slower than unbuffered */
//  @Deprecated
//  public static Iterable<Row> readBuffered(File f) throws IOException {
//    return read(new BufferedReader(new FileReader(f)));
//  }
//
//  public static Iterable<Row> read(File f) throws IOException {
//    return read(new FileReader(f));
//  }
//
//  public static Iterable<Row> read(Reader in) throws IOException {
//    CsvSchema schema = CsvSchema.emptySchema().withHeader();
//    MappingIterator<Map> iterator = reader.with(schema).readValues(in);
//
//    return new Iterable<Row>() {
//      // ... some reference to data
//      public Iterator<Row> iterator() {
//        return new Iterator<Row>() {
//          final Iterator<Map> it = iterator;
//
//          public boolean hasNext() {
//            return it.hasNext();
//          }
//
//          public Row next() {
//            return new Row(it.next());
//          }
//
//          public void remove() {
//            throw new UnsupportedOperationException();
//          }
//        };
//      }
//    };
//  }
// }