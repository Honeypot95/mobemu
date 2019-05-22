/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import mobemu.algorithms.Epidemic;
import mobemu.algorithms.SENSE;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.SocialBlueConn;
import mobemu.parsers.StAndrews;
import mobemu.parsers.UPB;
import mobemu.trace.Parser;

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

    public static void main(String[] args) throws Exception {

        String filename = args[0]; // The first argument is the filename to write to. TODO: This may change.
        System.out.println("Starting mobemu");
        Parser parser = traceFactory("SocialBlueConn");

        System.out.println("initialize Epidemic nodes");
        long seed = 0;
        boolean dissemination = false;
        Node[] nodes = new Node[parser.getNodesNumber()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new SENSE(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
                    10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), false, nodes);
        }

        System.out.println("run the trace");
        List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), false, dissemination, seed);

        writeStatsToFile(filename, parser, messages, nodes);
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
        if (tracename == "UPB2011") {
            return new UPB(UPB.UpbTrace.UPB2011);
        } else if (tracename == "UPB2012") {
            return new UPB(UPB.UpbTrace.UPB2012);
        } else if (tracename == "UPB2015") {
            return new UPB(UPB.UpbTrace.UPB2015);
        } else if (tracename == "StAndrews") {
            return new StAndrews();
        } else if (tracename == "SocialBlueConn") {
            return new SocialBlueConn();
        }
        throw new Exception("Trace" + tracename + " not implemented");
    }
}
