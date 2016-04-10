package com.rida.behaviours;

import com.google.common.collect.Sets;
import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.SubsetGenerator;
import com.rida.tools.Trip;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by daine on 03.04.2016.
 */
public class RequestRecieveServer extends CyclicBehaviour {
    private DriverAgent driverAgent;

    private static final Logger LOG = LoggerFactory.getLogger(RequestRecieveServer.class);

    private static void testSubsetGenerator(Set<DriverDescription> set) {
        SubsetGenerator<DriverDescription> subsetGenerator = new SubsetGenerator<>();
        Set<Set<DriverDescription>> sets = subsetGenerator.powerSet(set, 4);
        for (Set<DriverDescription> pass : sets) {
            for (DriverDescription d : pass) {
                Trip trip = d.getTrip();
                System.out.print(String.format("(%d,%d) ", trip.getFrom(), trip.getTo()));
            }
            System.out.println();
        }
    }

    private Set<DriverDescription> calcPassengersProfit(Set<DriverDescription> passengers) {
        Set<DriverDescription> bestPassengeres;
        int passengersCount = passengers.size();
        int limitSize = (passengersCount > 4) ? 4 : passengersCount;
        Set<Set<DriverDescription>> passengerCombinations = new SubsetGenerator<DriverDescription>().powerSet(passengers, limitSize);
        int maxProfit = Integer.MIN_VALUE;
        bestPassengeres = null;
        for (Set<DriverDescription> passengerCombination : passengerCombinations) {
            int size = passengerCombination.size();
            if (size > 0 && size <= 4) {
                int profit = getMaxProfit(passengerCombination);
                if (maxProfit < profit) {
                    maxProfit = profit;
                    bestPassengeres = passengerCombination;
                }
            }
        }
        return bestPassengeres;
    }

    public static void main(String[] args) {
        Set<DriverDescription> driverDescriptions = new HashSet<>();
        driverDescriptions.add(new DriverDescription("bla", null, new Trip(0, 1)));
        driverDescriptions.add(new DriverDescription("bla", null, new Trip(1, 2)));
        driverDescriptions.add(new DriverDescription("bla", null, new Trip(3, 4)));
        driverDescriptions.add(new DriverDescription("bla", null, new Trip(5, 6)));
        testSubsetGenerator(driverDescriptions);

    }

    private int getMaxProfit(Set<DriverDescription> cont) {
        DriverAgent driverAgent = (DriverAgent) myAgent;
        Graph g = driverAgent.getMapGraph();

        Set<Integer> vert = new HashSet<>();
        Map<Integer, Boolean> end = new HashMap<Integer, Boolean>();
        for (DriverDescription dd : cont) {
            vert.add(dd.getTrip().getFrom());
        }

        int cur = 0;
        int sum = 0;
        int min = Integer.MAX_VALUE;
        for (Integer t : vert) {
            if (min > g.bfs(driverAgent.getDescription().getTrip().getFrom(), t)) {
                min = g.bfs(driverAgent.getDescription().getTrip().getFrom(), t);
                cur = t;
            }
        }

        vert.remove(cur);
        sum += min;
        end.put(cur, true);
        for (DriverDescription dd : cont) {
            if (dd.getTrip().getFrom() == cur)
                vert.add(dd.getTrip().getTo());
        }

        while (vert.size() > 1) {
            min = Integer.MAX_VALUE;
            int curcur = 0;
            for (Integer t : vert) {
                if (min > g.bfs(cur, t)) {
                    min = g.bfs(cur, t);
                    curcur = t;
                }
            }
            vert.remove(curcur);
            sum += min;
            cur = curcur;
            for (DriverDescription dd : cont) {
                if (dd.getTrip().getFrom() == cur)
                    vert.add(dd.getTrip().getTo());
            }
            end.put(cur, true);
        }
        sum += g.bfs(cur, driverAgent.getDescription().getTrip().getTo());

        int maxway = 0;
        for (DriverDescription dd : cont) {
            maxway += g.bfs(dd.getTrip().getFrom(), dd.getTrip().getTo());
        }
        maxway += g.bfs(driverAgent.getDescription().getTrip().getFrom(), driverAgent.getDescription().getTrip().getTo());

        //System.out.println(maxway - sum);
        return maxway - sum;
    }

    private int calcMaxPassengersProfit(Set<DriverDescription> best, Set<DriverDescription> q, Set<DriverDescription> inq) {
        DriverAgent driverAgent = (DriverAgent) myAgent;
        Set<DriverDescription> result = new HashSet<>();
        Graph g = driverAgent.getMapGraph();

        int max = Integer.MIN_VALUE;
        for (DriverDescription p1 : q) {
            inq.add(p1);
            int temp_res1 = getMaxProfit(inq);
            if (max < temp_res1) {
                max = temp_res1;
                result = new HashSet<>(inq);
            }
            if (inq.size() < 4) {
                Set<DriverDescription> temp_result2 = new HashSet<>();
                Set<DriverDescription> qq = new HashSet<>(q);
                qq.remove(p1);
                int temp_res2 = calcMaxPassengersProfit(temp_result2, qq, inq);
                if (max < temp_res2) {
                    max = temp_res2;
                    result = new HashSet<>(temp_result2);
                }
                qq.add(p1);
            }
            inq.remove(p1);
        }
        for (DriverDescription dd : result)
            best.add(dd);
        //best = new HashSet<>(result);

        return max;
    }

    @Override
    public void action() {
        DriverAgent driverAgent = (DriverAgent) myAgent;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            AID driverName = msg.getSender();
            driverAgent.addPassenger(driverAgent.getDriverDescriptionByName(driverName));
            driverAgent.getPaseengerDescriptionByName(driverName).setCost(Double.parseDouble(msg.getContent()));
            /*Set<DriverDescription> result = new HashSet<>();
            Set<DriverDescription> q = new HashSet<>();
            for (DriverDescription dd : driverAgent.getPassengers())
                q.add(new DriverDescription(dd));
            Set<DriverDescription> inq = new HashSet<>();
            calcMaxPassengersProfit(result, q, inq);*/
            // if (driverAgent.getMapGraph().bfs(driverAgent.getFrom(), driverAgent.getTo()) < getMaxProfit(result)){
            Set<DriverDescription> result = calcPassengersProfit(driverAgent.getPassengers());
            String s = "";
            for (DriverDescription dd : result)
                s += "\r\n\t\t" + dd.toString();
            LOG.info("{}", s);
            //  }

        }
    }
}