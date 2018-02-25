package com.voxelgameslib.assets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.mineskin.MineskinClient;
import org.mineskin.Model;
import org.mineskin.SkinOptions;
import org.mineskin.Visibility;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@SuppressWarnings("MissingJSR305")
public class Test {

    private static File ids;

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        File inputFile = new File("tools\\assets\\src\\main\\resources\\skulls\\template.ora");
        File outputFolder = new File("tools\\assets\\src\\main\\resources\\skulls\\generated\\");
        Font font = new Font("6px2bus", Font.PLAIN, 6);
        ids = new File(outputFolder, "ids.json");

        for (char text : new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'}) {
            try {
                makeFile(inputFile, new File(outputFolder, text + ".png"), Color.GRAY, Color.RED, font, text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(30);
        MineskinClient mineskinClient = new MineskinClient(executor);
        File[] files = outputFolder.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".png")) {
                File skins = new File(outputFolder, "skins");
                if (!skins.exists()) skins.mkdirs();
                File out = new File(skins, file.getName().replace(".png", "") + ".json");
                if (out.exists()) {
                    System.out.println(file.getName() + " exists, skipping");
                    handle(out);
                    continue;
                } else {
                    System.out.println("uploading " + file.getName());
                }
                mineskinClient.generateUpload(file, SkinOptions.create(file.getName().replace(".png", ""), Model.DEFAULT, Visibility.PRIVATE), new SkinCallback() {

                    @Override
                    public void done(Skin skin) {
                        try {
                            System.out.println("saving " + file.getName());
                            FileWriter fw = new FileWriter(out);
                            gson.toJson(skin, fw);
                            fw.close();
                            handle(out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void exception(Exception exception) {
                        exception.printStackTrace();
                    }

                    @Override
                    public void uploading() {
                        System.out.println("uploading.......  " + file.getName());
                    }

                    @Override
                    public void error(String errorMessage) {
                        System.out.println(file.getName() + " error: " + errorMessage);
                    }

                    @Override
                    public void waiting(long delay) {
                        System.out.println(file.getName() + " waiting for " + delay);
                    }

                    @Override
                    public void parseException(Exception exception, String body) {
                        System.out.println(file.getName() + " parse exception for " + body);
                    }
                });
            }
        }
        try {
            System.out.println("waiting for termination");
            executor.awaitTermination(100, TimeUnit.MINUTES);
            System.out.println("done");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized static void handle(File out) {
        try {
            Skin skin = new Gson().fromJson(new FileReader(out), Skin.class);
            Map<String, Integer> ids = null;
            try {
                ids = new Gson().fromJson(new FileReader(Test.ids), Map.class);
            } catch (Exception ex) {
                System.out.println("err");
            }
            if (ids == null) {
                ids = new HashMap<>();
                System.out.println("ids was null");
            }
            ids.put(skin.name, skin.id);
            System.out.println("+++++++++++++++++++++++++++++++++        id =  " + skin.id + " name = " + skin.name);
            FileWriter fw = new FileWriter(Test.ids);
            new Gson().toJson(ids, fw);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void makeFile(File inputFile, File outputFile, Color backgroundColor, Color textColor, Font font, char text) throws Exception {
        makeFile(inputFile,
            outputFile,
            new Color[]{backgroundColor, backgroundColor, backgroundColor, backgroundColor, backgroundColor, backgroundColor},
            new Color[]{textColor, textColor, textColor, textColor, textColor, textColor},
            font,
            new char[]{text, text, text, text, text, text});
    }

    private static void makeFile(File inputFile, File outputFile, Color[] backgroundColors, Color[] textColors, Font font, char[] text) throws Exception {
        Map<Layer, BufferedImage> composition = readOpenRaster(inputFile);

        BufferedImage finalImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics g = finalImage.createGraphics();

        int bgCounter = 0;
        int textCounter = 0;
        for (Map.Entry<Layer, BufferedImage> entry : composition.entrySet()) {
            System.out.println("layer " + entry.getKey().name);
            BufferedImage image = entry.getValue();
            if (entry.getKey().name.endsWith("_background")) {
                Graphics2D graphics2D = image.createGraphics();

                graphics2D.setColor(backgroundColors[bgCounter]);
                graphics2D.fillRect(0, 0, 8, 8);

                graphics2D.dispose();
                bgCounter++;
            } else if (entry.getKey().name.endsWith("_text")) {
                Graphics2D graphics2D = image.createGraphics();
                // clear
                graphics2D.setBackground(new Color(0, 0, 0, 0));
                graphics2D.clearRect(0, 0, 7, 7);
                // draw
                graphics2D.setFont(font);
                graphics2D.setColor(textColors[textCounter]);
                graphics2D.drawString(text[textCounter] + "", 1, 5);

                graphics2D.dispose();
                textCounter++;
            }

            g.drawImage(image, entry.getKey().x, entry.getKey().y, null);
        }

        ImageIO.write(finalImage, "png", outputFile);
    }

    public static Map<Layer, BufferedImage> readOpenRaster(File file) throws IOException, ParserConfigurationException, SAXException {
        Map<Layer, BufferedImage> result = new TreeMap<>();
        String stackXML = null;
        Map<String, BufferedImage> images = new HashMap<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> fileEntries = zipFile.entries();
            while (fileEntries.hasMoreElements()) {
                ZipEntry entry = fileEntries.nextElement();
                String name = entry.getName();

                if (name.equalsIgnoreCase("stack.xml")) {
                    Scanner s = new Scanner(zipFile.getInputStream(entry)).useDelimiter("\\A");
                    stackXML = s.hasNext() ? s.next() : "";
                } else if (name.equalsIgnoreCase("mergedimage.png")) {
                    // no need for that
                } else {
                    String extension = getExt(name);
                    if ("png".equalsIgnoreCase(extension)) {
                        BufferedImage image = ImageIO.read(zipFile.getInputStream(entry));
                        images.put(name, image);
                    }
                }
            }
        }

        if (stackXML == null) {
            throw new IllegalStateException("No stack.xml found.");
        }

        Document doc = loadXMLFromString(stackXML);
        Element docElement = doc.getDocumentElement();
        docElement.normalize();

        String w = docElement.getAttribute("w");
        int compWidth = Integer.parseInt(w);
        String h = docElement.getAttribute("h");
        int compHeight = Integer.parseInt(h);

        NodeList layers = docElement.getElementsByTagName("layer");
        for (int i = layers.getLength() - 1; i >= 0; i--) { // stack.xml contains layers in reverse order
            Node node = layers.item(i);
            Element element = (Element) node;

            String layerName = element.getAttribute("name");
            String layerImageSource = element.getAttribute("src");
            String layerX = element.getAttribute("x");
            String layerY = element.getAttribute("y");

            BufferedImage image = images.get(layerImageSource);
            image = toSysCompatibleImage(image);
            result.put(new Layer(i, layerName, parseInt(layerX, 0), parseInt(layerY, 0)), image);
        }
        return result;
    }

    private static Document loadXMLFromString(String xml) throws ParserConfigurationException, IOException, SAXException {
        if (xml.startsWith("\uFEFF")) {
            xml = xml.substring(1);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }


    private static BufferedImage toSysCompatibleImage(BufferedImage input) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice().getDefaultConfiguration();

        if (input.getColorModel().equals(gc.getColorModel())) {
            return input;
        }

        int transparency = Transparency.TRANSLUCENT;
        BufferedImage output = gc.createCompatibleImage(input.getWidth(), input.getHeight(), transparency);
        Graphics2D g = output.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        return output;
    }

    private static int parseInt(String input, int defaultValue) {
        if ((input != null) && !input.isEmpty()) {
            return Integer.parseInt(input);
        }
        return defaultValue;
    }

    private static String getExt(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return null;
        }
        return fileName
            .substring(lastIndex + 1, fileName.length())
            .toLowerCase();
    }

    static class Layer implements Comparable {

        String name;
        int x;
        int y;
        int id;

        public Layer(int id, String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.id = id;
        }

        @Override
        public int compareTo(Object o) {
            return Integer.compare(((Layer) o).id, id);
        }
    }
}
