package com.paqrap.ingesta;

public interface CsvRecordParser<T> {
    T parse(String line);
}
