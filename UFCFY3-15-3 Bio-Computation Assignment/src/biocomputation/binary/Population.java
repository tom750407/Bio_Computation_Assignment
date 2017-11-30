/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biocomputation.binary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

/**
 *
 * @author tsuochiayang
 */
public class Population {

    String dataSet2 = "src/Biocomputation/csv/ds2-pop1000-gene10-mutation0.02_1.csv";
    File f;
    FileWriter fw;
    
    //data set 1 and 2 are both use this code
    public static final int POPULATION_SIZE = 1000;
    public final static double MUTATION_RATE = 0.02;
    public static final int GENE_SIZE = 10;
    public static final int RULE_SIZE = 7; // 6 for data set 1 and 7 for data set 2

    private Individual[] population;
    private Individual[] matingPool;
    private int[][] dataset;
    int bestFitness;
    double averageFitness;
    int worstFitness;

    public Population() {
        population = new Individual[POPULATION_SIZE];
        matingPool = new Individual[POPULATION_SIZE];

        //init population
        for (int i = 0; i < population.length; i++) {
            population[i] = new Individual(GENE_SIZE, RULE_SIZE);
        }

        //randomise the genes
        setPopulation();
    }

    public void setPopulation() {
        for (int i = 0; i < population.length; i++) {
            population[i].randomiseGenes();
        }
    }

    public void readFile() {
        Scanner scan = new Scanner(Population.class.getResourceAsStream("data2.txt"));
        ArrayList<Integer[]> ruleList = new ArrayList<>();
        Integer[] rule;
        while (scan.hasNextLine()) {
            rule = new Integer[RULE_SIZE];
            String line = scan.nextLine();
            line = line.replace(" ", "");

            for (int i = 0; i < line.length(); i++) {
                rule[i] = Integer.parseInt("" + line.charAt(i));
            }
            ruleList.add(rule);
        }

        //convert rules arraylist back into a normal array
        dataset = new int[ruleList.size()][ruleList.get(0).length];
        for (int i = 0; i < ruleList.size(); i++) {
            for (int j = 0; j < ruleList.get(i).length; j++) {
                dataset[i][j] = ruleList.get(i)[j];
            }
        }

        //output dataset to check
//        for (int i = 0; i < dataset.length; i++) {
//            System.out.println(Arrays.toString(dataset[i]));
//        }
    }

    public void fitnessFunction() {
        bestFitness = 0;
        worstFitness = population[0].getFitness();
        averageFitness = 0;

        for (int i = 0; i < population.length; i++) {
            population[i].fitnessFunction(dataset);
            if (bestFitness < population[i].getFitness()) {
                bestFitness = population[i].getFitness();
            }
            if (worstFitness > population[i].getFitness()) {
                worstFitness = population[i].getFitness();
            }
            averageFitness += population[i].getFitness();
        }
        averageFitness /= population.length;
    }

    public void selection() {
        //fill the mating pool!
        for (int i = 0; i < POPULATION_SIZE; i++) {
            matingPool[i] = tournamentSelection();
        }
    }

    public void crossover() {
        //create new population from mating pool
        for (int i = 0; i < matingPool.length / 2; i++) {
            int offset = i * 2;
            Individual[] children = singlePointCrossover(matingPool[offset], matingPool[offset + 1]);
            matingPool[i] = children[0];
            matingPool[i + 1] = children[1];
        }
    }

    public void mutation() {
        for (int i = 0; i < matingPool.length; i++) {
            matingPool[i].mutation(MUTATION_RATE);
        }
    }

    private void nextGeneration() {
        for (int i = 0; i < population.length; i++) {
            population[i] = new Individual(matingPool[i]);
        }
    }

    private Individual[] singlePointCrossover(Individual p1, Individual p2) {
        Individual[] children = new Individual[2];
        children[0] = new Individual(p1);
        children[1] = new Individual(p2);

        int crossingPoint = new Random().nextInt(GENE_SIZE * RULE_SIZE);
        int pointer = 0;
        for (int i = 0; i < p1.getGenes().length; i++) {
            for (int j = 0; j < p1.getGene(i).length; j++) {
                if (pointer < crossingPoint) {
                    //swap
                    int temp = children[0].getGenes()[i][j];
                    children[0].getGenes()[i][j] = children[1].getGenes()[i][j];
                    children[1].getGenes()[i][j] = temp;
                } else {
                    break;
                }
                pointer++;
            }
            if (pointer >= crossingPoint) {
                break;
            }
        }

        return children;
    }

    private Individual tournamentSelection() {
        Random rand = new Random();
        Individual m1 = new Individual(population[rand.nextInt(POPULATION_SIZE)]);
        Individual m2 = new Individual(population[rand.nextInt(POPULATION_SIZE)]);
        if (m1.getFitness() > m2.getFitness()) {
            return m1;
        } else {
            return m2;
        }
    }

    public void bestIndividualStats() {
        Individual bestInd = population[0];
        for (int i = 1; i < population.length; i++) {
            if (population[i].getFitness() > bestInd.getFitness()) {
                bestInd = population[i];
            }
        }

        System.out.println("Best Individual stats!");
        for (int i = 0; i < bestInd.getGenes().length; i++) {
            System.out.println(Arrays.toString(bestInd.getGene(i)));
        }
    }

    private void newCSV() {
        f = new File(dataSet2);
        try {
            if (!f.exists()) {
                f.createNewFile();
            } else {
                f.delete();
                f.createNewFile();
            }
            fw = new FileWriter(f);
            fw.append("Best Fitness");
            fw.append(", ");
            fw.append("Average Fitness");
            fw.append(", ");
            fw.append("Worst Fitness");
            fw.append("\n");
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void csvData(String best, String average, String worst) {
        try {
            fw.append(best);
            fw.append(", ");
            fw.append(average);
            fw.append(", ");
            fw.append(worst);
            fw.append("\n");
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void close() {
        try {
            fw.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void main(String[] args) {
        Population p = new Population();
        p.readFile();
        p.newCSV();
        //ga stuff
        int generations = 1;
        while (generations <= 1000) {
            /* fitness function */
            p.fitnessFunction();

            System.out.println(generations + " best fitness: " + p.bestFitness);
            p.csvData(p.bestFitness + "", p.averageFitness + "", p.worstFitness + "");

            /* selection */
            p.selection();

            /* crossover */
            p.crossover();

            /* mutation */
            p.mutation();

            /* select individuals for next generation */
            p.nextGeneration();

            generations++;
        }
        //print out the best individual
        System.out.println("Completed");
        System.out.println("========================");
        p.bestIndividualStats();
        p.close();
    }
}
