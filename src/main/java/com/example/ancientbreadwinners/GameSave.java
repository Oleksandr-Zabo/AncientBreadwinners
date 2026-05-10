package com.example.ancientbreadwinners;

import java.io.*;
import java.util.*;

public class GameSave {
    public Village village;
    public double cameraX;
    public double cameraY;

    public GameSave(Village village, double cameraX, double cameraY) {
        this.village = village;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
    }

    public void saveToFile(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("cameraX=" + cameraX);
            writer.println("cameraY=" + cameraY);
            writer.println("coins=" + village.getTotalCoins());
            writer.println("macros=" + village.getMacroObjects().size());
            for (MacroObject mo : village.getMacroObjects()) {
                writer.println("macro=" + mo.getClass().getSimpleName() + "," + mo.getX() + "," + mo.getY());
            }
            writer.println("farmers=" + village.getFarmers().size());
            for (Farmer f : village.getFarmers()) {
                writer.println("farmer=" + f.getClass().getSimpleName() + "," + f.getName() + "," + f.getX() + "," + f.getY() + "," + f.getMotivation() + "," + f.getSpeed() + "," + f.getMaxLoad() + "," + f.getTool().getType() + "," + f.getCurrentLoad());
            }
        }
    }

    public static GameSave loadFromFile(File file) throws IOException {
        Village village = new Village();
        double cameraX = 0;
        double cameraY = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("cameraX=")) {
                    cameraX = Double.parseDouble(line.substring(8));
                } else if (line.startsWith("cameraY=")) {
                    cameraY = Double.parseDouble(line.substring(8));
                } else if (line.startsWith("coins=")) {
                    village.setTotalCoins(Integer.parseInt(line.substring(6)));
                } else if (line.startsWith("macro=")) {
                    String[] parts = line.substring(6).split(",");
                    MacroObject mo = createMacro(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                    village.addMacroObject(mo);
                } else if (line.startsWith("farmer=")) {
                    String[] parts = line.substring(7).split(",");
                    Farmer f = createFarmer(parts[0], parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                        Integer.parseInt(parts[4]), Double.parseDouble(parts[5]), Integer.parseInt(parts[6]),
                        ToolTypes.valueOf(parts[7]), Integer.parseInt(parts[8]));
                    village.addFarmer(f);
                }
            }
        }

        return new GameSave(village, cameraX, cameraY);
    }

    private static MacroObject createMacro(String type, double x, double y) {
        return switch (type) {
            case "WheatField" -> new WheatField(x, y);
            case "Mill" -> new Mill(x, y);
            case "Church" -> new Church(x, y);
            default -> throw new IllegalArgumentException("Unknown macro type: " + type);
        };
    }

    private static Farmer createFarmer(String type, String name, double x, double y, int motivation, double speed, int maxLoad, ToolTypes toolType, int currentLoad) {
        Tool tool = new Tool(toolType, 1.0f);
        Farmer f = switch (type) {
            case "Gardener" -> new Gardener(name, motivation, speed, maxLoad, tool, x, y);
            case "FreePeasant" -> new FreePeasant(name, motivation, speed, maxLoad, tool, x, y);
            case "MasterFarmer" -> new MasterFarmer(name, motivation, speed, maxLoad, tool, x, y);
            default -> throw new IllegalArgumentException("Unknown farmer type: " + type);
        };
        f.setCurrentLoad(currentLoad);
        return f;
    }
}
