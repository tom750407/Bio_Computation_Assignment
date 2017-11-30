/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biocomputation.floating;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

/**
 *
 * @author tsuochiayang
 */
public class GAFloat {

    String dataSet3 = "src/Biocomputation/csv/ds3-pop1000-gene10-mutation0.02_1.csv";
    File f;
    FileWriter fw;
    //HYPER PARAMETERS
    private static final int POPULATION_SIZE = 200;
    private static final int CHROMOSOME_LENGTH = 20;
    private static final double GENE_SIZE = 0.9;
    private static final double MUTATION_RATE = 0.00001;
    private static final double OMEGA_OFFSET_FIXED = 0.3;
    private static final int NUMBER_OF_GENERATIONS = 1000;
    private int CONDITION_LENGTH;
    private int OUTPUT_LENGTH;
    //input file
    private String inputFileDir;

    /* Datasets */
    private float[][] dataset;
    private float[][] trainingSet;
    private float[][] testingSet;

    /* Individuals/Population */
    private Individual[] parentPopulation;
    private Individual[] offspringPopulation;
    private Individual bestIndividual;
    private Individual worstIndividual;

    /* sort populations */
    private boolean parentPopulationSorted = false;
    private boolean offspringPopulationSorted = false;

    //constructor
    public GAFloat(String filename) {
        if (filename.equals("data3.txt")) {
            inputFileDir = "data3.txt";
        }
    }

    /*
    read file
    reads file based on inputFileDir
     */
    private void readFile() {
        Scanner scan = new Scanner(GAFloat.class.getResourceAsStream(inputFileDir));
        ArrayList<Float[]> datasetAL = new ArrayList<>();
        int conditionLengthTemp = 0;
        int outputLengthTemp = 0;

        scan.nextLine(); //read in the useless line

        while (scan.hasNextLine()) {
            //read in rule
            String ruleString = scan.nextLine();
            String[] parts = ruleString.split(" ");

            //save length of condition
            if (conditionLengthTemp == 0) {
                //output length is always 1, condition length is the rest.
                conditionLengthTemp = parts.length - 1;
            }
            if (outputLengthTemp == 0) {
                outputLengthTemp = 1;
            }

            //create set
            Float[] set = new Float[conditionLengthTemp + outputLengthTemp];
            for (int i = 0; i < set.length - 1; i++) {
                set[i] = Float.parseFloat(parts[i]);
            }
            set[set.length - 1] = Float.parseFloat(parts[parts.length - 1]);

            //add set to arraylist
            datasetAL.add(set);
        }

        scan.close();

        //populate dataset
        dataset = new float[datasetAL.size()][conditionLengthTemp + outputLengthTemp];
        for (int i = 0; i < dataset.length; i++) {
            for (int j = 0; j < dataset[i].length; j++) {
                dataset[i][j] = datasetAL.get(i)[j];
            }
        }

        //store length of condition
        CONDITION_LENGTH = conditionLengthTemp;
        OUTPUT_LENGTH = outputLengthTemp;
    }

    private void initialiseDatasets() {
        trainingSet = new float[1000][CONDITION_LENGTH + OUTPUT_LENGTH];
        testingSet = new float[1000][CONDITION_LENGTH + OUTPUT_LENGTH];

        int i = 0;
        while (i < dataset.length/2) {
            trainingSet[i] = dataset[i];
            i++;
        }
        
        while (i < dataset.length) {
            testingSet[i % trainingSet.length] = dataset[i];
            i++;
        }
    }

    private void initialisePopulations() {
        parentPopulation = new Individual[POPULATION_SIZE];
        offspringPopulation = new Individual[POPULATION_SIZE];

        //init parent and offspring
        //note: generation 0 --> parent and offspring are the same
        for (int i = 0; i < POPULATION_SIZE; i++) {
            parentPopulation[i] = offspringPopulation[i] = new Individual(CHROMOSOME_LENGTH, CONDITION_LENGTH, OUTPUT_LENGTH);
        }

        //init best individual
        bestIndividual = new Individual(CHROMOSOME_LENGTH, CONDITION_LENGTH, OUTPUT_LENGTH);
    }

    private void getBestIndividualNoSort() {
        //no sorting, so we will sequential search for best
        int bestFitnessIndex = 0;
        for (int i = 1; i < offspringPopulation.length; i++) {
            if (offspringPopulation[i].getFitness() > offspringPopulation[bestFitnessIndex].getFitness()) {
                bestFitnessIndex = i;
            }
        }
        bestIndividual = new Individual(offspringPopulation[bestFitnessIndex]);
        
    }
    
    private void getWorstIndividualNoSort() {
        int worstFitnessIndex = 0;
        for (int i = 1; i < offspringPopulation.length; i++) {
            if (offspringPopulation[i].getFitness() < offspringPopulation[worstFitnessIndex].getFitness()) {
                worstFitnessIndex = i;
            }
        }
        worstIndividual = new Individual(offspringPopulation[worstFitnessIndex]);
    }

    private String showAverageFitness() {
        double totalFitness = 0;
        for (Individual individual : offspringPopulation) {
            totalFitness += individual.getFitness();
        }
//        System.out.println("Average Fitness: " + (totalFitness / offspringPopulation.length));
        return (totalFitness / offspringPopulation.length) + "";
    }

    private String showBestFitness() {
        return bestIndividual.getFitness() + "";
    }
    
    private String showWorstFitness() {
        return worstIndividual.getFitness() + "";
    }

    private void generatOffspring(float[][] set) {

        /* Selection / Crossover /  Mutation */
        for (int i = 0; i < offspringPopulation.length; i++) {

            /* Selection */
            Individual[] parents = new Individual[2];
            parents[0] = Selection.tornamentSelection(parentPopulation);
            parents[1] = Selection.tornamentSelection(parentPopulation);

            /* Crossover */
            Individual[] children = Crossover.singlePointCrossover(parents[0], parents[1], GENE_SIZE);

            /* Mutation and add to offspring pop */
            children[0].mutationCreepConditions(MUTATION_RATE, OMEGA_OFFSET_FIXED);
            children[0].mutationOutput(MUTATION_RATE);
            offspringPopulation[i] = children[0];

            if (i + 1 < offspringPopulation.length) {
                i++;
                children[1].mutationCreepConditions(MUTATION_RATE, OMEGA_OFFSET_FIXED);
                children[1].mutationOutput(MUTATION_RATE);
                offspringPopulation[i] = children[1];
            }
        }
        
        getBestIndividualNoSort();
        getWorstIndividualNoSort();
    }

        private void newCSV() {
        f = new File(dataSet3);
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
    
    private void sortPopulations() {
        Comparator<Individual> comp = new Comparator<Individual>() {
            @Override
            public int compare(Individual o1, Individual o2) {
                double result = o1.getFitness() - o2.getFitness();
                if (result > 0) {
                    return 1;
                }
                if (result < 0) {
                    return -1;
                }
                return 0;
            }
        };
        
        Arrays.sort(parentPopulation, comp);
        Arrays.sort(offspringPopulation, comp);
        
        parentPopulationSorted = true;
        offspringPopulationSorted = true;
    }
    
    private void copyChildrenToParents() {
        for (int i = 0; i < parentPopulation.length; i++) {
            parentPopulation[i] = new Individual(offspringPopulation[i]);
        }
    }

    public void run() {
        readFile();
        initialiseDatasets();
        newCSV();

        initialisePopulations();

        System.out.println("Hello!");
        /* GA loop */
        int numberOfGenerations = 0;
        float[][] set = dataset;
        while (numberOfGenerations < NUMBER_OF_GENERATIONS) {

            parentPopulationSorted = false;
            offspringPopulationSorted = false;
            
            //every 10 generations, use testingSet
            if (numberOfGenerations % 10 == 0) {
                set = testingSet;
                System.out.println("*"); //just to make a note that the next output is on testing set
            } else {
                set = trainingSet;
            }
            
            /* Fitness function on parents */
            for (int i = 0; i < parentPopulation.length; i++) {
                parentPopulation[i].evaluate(set);
            }

            /* Generate offspring */
            generatOffspring(set);

            /* Show Best Fitness */
            System.out.println("Best Fitness: " + bestIndividual.getFitness()
                    + " Generations: " + numberOfGenerations);

            csvData(showBestFitness(), showAverageFitness() , showWorstFitness());
            
            /* copy children to parents */
            copyChildrenToParents();
            
            /* Increment generations */
            numberOfGenerations++;
        }
        close();
        
    }

    public static void main(String[] args) {
        GAFloat run = new GAFloat("data3.txt");
        run.run();
    }
}
