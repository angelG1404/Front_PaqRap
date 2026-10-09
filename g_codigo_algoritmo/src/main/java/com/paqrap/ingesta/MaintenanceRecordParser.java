package com.paqrap.ingesta;

import com.paqrap.model.Maintenance;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MaintenanceRecordParser implements CsvRecordParser<Maintenance> {
    private final LocalDate epoch;
    private int lineNumber = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public MaintenanceRecordParser(LocalDate epoch) {
        this.epoch = epoch;
    }

    @Override
    public Maintenance parse(String line) {
        lineNumber++;
        String[] parts = line.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato inválido en línea " + lineNumber);
        }

        String dateToken = parts[0];
        String vehicleId = parts[1]; // Expected TTNN (e.g., TA01)

        LocalDate date = LocalDate.parse(dateToken, DATE_FORMATTER);
        double startTimeHours = TimeConverter.daysBetweenAsHours(epoch, date);
        double endTimeHours = startTimeHours + 24.0;

        return new Maintenance(vehicleId, startTimeHours, endTimeHours);
    }
}
