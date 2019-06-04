/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import mobemu.algorithms.Epidemic;
import mobemu.algorithms.SENSE;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.*;
import mobemu.trace.Parser;

/**
 * External libraries
 */
import mjson.Json;

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

    public static void main(String[] args) throws Exception {

        String input = args[0]; // First argument is the input file
        String output = args[1]; // Second argument is the output file

        // Parse the input file to json object
        Json runs = readInputFile(input);
        System.out.println("Configuration to run: ");
        System.out.println(runs);
        // Read the first trace
        String tracename = runs.at("runs").at(0).at("trace").asString();
        System.out.println(tracename);

        System.out.println("Starting mobemu");
        Parser parser = traceFactory(tracename);

        System.out.println("initialize SENSE nodes");
        long seed = 0;
        boolean dissemination = false;
        Node[] nodes = new Node[parser.getNodesNumber()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new SENSE(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
                    10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), true, nodes);
        }

        System.out.println("run the trace");
        List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), false, dissemination, seed);

        writeStatsToFile(output, parser, messages, nodes);
        System.out.println("Finishing mobemu");
    }

    /**
     * Utility function to print trace statistics and trace run to file. Should be converted to something better, like
     * json
     * @param filename
     * @param parser
     * @param messages
     * @param nodes
     */
    public static void writeStatsToFile(String filename, Parser parser, List<Message> messages, Node[] nodes) {
        try {

            File f = new File(filename);

            PrintWriter writer = null;
            if ( f.exists() && !f.isDirectory() ) {
                writer = new PrintWriter(new FileOutputStream(new File(filename), true));
            }
            else {
                writer = new PrintWriter(filename);
            }

            // current date
            writer.append("Current date: " + System.currentTimeMillis() / 1000L + "\n");

            // print some trace statistics
            writer.append("Trace name: " + parser.getTraceData().getName() + "\n");
            double duration = (double) (parser.getTraceData().getEndTime() - parser.getTraceData().getStartTime()) / (Parser.MILLIS_PER_MINUTE * 60);
            writer.append("Trace duration in hours: " + duration + "\n");
            writer.append("Trace contacts: " + parser.getTraceData().getContactsCount() + "\n");
            writer.append("Trace contacts per hour: " + (parser.getTraceData().getContactsCount() / duration) + "\n");
            writer.append("Nodes: " + parser.getNodesNumber() + "\n");

            writer.append("Algorithm: " + nodes[0].getName() + "\n");
            writer.append("Seed: " + nodes[0].getSeed() + "\n");

            writer.append("Messages: " + messages.size() + "\n");

            // print opportunistic algorithm statistics
            writer.append("Hit rate: " + Stats.computeHitRate(messages, nodes, false) + "\n");
            writer.append("Delivery cost: " + Stats.computeDeliveryCost(messages, nodes, false) + "\n");
            writer.append("Delivery latency: " + Stats.computeDeliveryLatency(messages, nodes, false) + "\n");
            writer.append("Hop count: " + Stats.computeHopCount(messages, nodes, false) + "\n");
            writer.append("***" + "\n");
            writer.close();
        } catch (Exception e) {

        }
    }

    /**
     * Returns a new trace object of the specified type. It maps strings to trace objects.
     * @param tracename
     */
    public static Parser traceFactory(String tracename) throws Exception {
        if (tracename.equals("UPB2011")) {
            return new UPB(UPB.UpbTrace.UPB2011);
        } else if (tracename.equals("UPB2012")) {
            return new UPB(UPB.UpbTrace.UPB2012);
        } else if (tracename.equals("UPB2015")) {
            return new UPB(UPB.UpbTrace.UPB2015);
        } else if (tracename.equals("StAndrews")) {
            return new StAndrews();
        } else if (tracename.equals("SocialBlueConn")) {
            return new SocialBlueConn();
        } else if (tracename.equals("Sigcomm")) {
            return new Sigcomm();
        } else if (tracename.equals("NUS")) {
            return new NUS();
        } else if (tracename.equals("NCCU")) {
            return new NCCU();
        } else if (tracename.equals("GeoLife")) {
            return new GeoLife();
        }

        // TODO: There are still some traces not here, those need to be further analysed
        throw new Exception("Trace " + tracename + " not implemented");
    }

    /**
     * Parses the filename to a json object. Filename has to be the path of a UTF-8 encoded file.
     * @param filename
     * @return
     */
    public static Json readInputFile(String filename) throws IOException {
        return Json.read(new String(Files.readAllBytes(Paths.get(filename)), "UTF-8"));
    }

}
