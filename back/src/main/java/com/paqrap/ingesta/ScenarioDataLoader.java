package com.paqrap.ingesta;

import com.paqrap.model.*;
import com.paqrap.simulator.ScenarioGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScenarioDataLoader {

    public ScenarioGenerator.Scenario loadScenario(String ordersPath, String roadblocksPath, String maintenancesPath, String breakdownsPath) {
        List<Order> orders = loadFile(ordersPath, new OrderRecordParser());
        List<Roadblock> roadblocks = loadFile(roadblocksPath, new RoadblockRecordParser());
        List<Breakdown> breakdowns = loadFile(breakdownsPath, new BreakdownRecordParser());

        // Para mantenimientos necesitamos calcular el epoch
        List<Maintenance> maintenances = new ArrayList<>();
        if (maintenancesPath != null && new File(maintenancesPath).exists()) {
            List<String> lines = readLines(maintenancesPath);
            LocalDate epoch = findEpoch(lines);
            MaintenanceRecordParser parser = new MaintenanceRecordParser(epoch);
            for (String line : lines) {
                maintenances.add(parser.parse(line));
            }
        }

        return ScenarioGenerator.generateCustomScenario(orders, roadblocks, maintenances, breakdowns);
    }

    private <T> List<T> loadFile(String path, CsvRecordParser<T> parser) {
        List<T> list = new ArrayList<>();
        if (path != null && new File(path).exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    list.add(parser.parse(line));
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading file: " + path, e);
            }
        }
        return list;
    }

    private List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + path, e);
        }
        return lines;
    }

    private LocalDate findEpoch(List<String> lines) {
        LocalDate earliest = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        for (String line : lines) {
            String dateToken = line.split(":")[0];
            LocalDate date = LocalDate.parse(dateToken, formatter);
            if (earliest == null || date.isBefore(earliest)) {
                earliest = date;
            }
        }
        return earliest != null ? earliest : LocalDate.now();
    }
}
