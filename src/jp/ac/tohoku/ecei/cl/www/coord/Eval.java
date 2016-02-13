package jp.ac.tohoku.ecei.cl.www.coord;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import jp.ac.tohoku.ecei.cl.www.eval.*;
import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.io.*;

public class Eval {

    public static File goldFile = null;
    public static File sysFile = null;
    public static File goldDir = null;
    public static File sysDir = null;

    // evaluators 
    public Evaluator labE;
    public Alphabet labelAlphabet;

    public enum DepType {DEP, ZERO_INTRA, ZERO_INTER, SAME_PHRASE}

    public Eval () {
        // evaluators 
        this.labE = new Evaluator();
        this.labelAlphabet = new Alphabet();
    }

    public Evaluator getLabE() {
        return this.labE;
    }

    public Alphabet getLabelAlphabet() {
        return this.labelAlphabet;
    }

    public void getResults(DependencyTree goldTree, DependencyTree systemTree) {

        Bunsetsu[] goldBs = goldTree.getBunsetsuList();
        Bunsetsu[] sysBs = systemTree.getBunsetsuList();
        
        for (int i = 0; i < goldBs.length; i++) {
            Bunsetsu goldB = goldBs[i];
            Bunsetsu sysB = sysBs[i];
            int goldBDepLabelAsInt = labelAlphabet.lookupIndex(goldB.getDepLabel(), true);
            int sysBDepLabelAsInt = labelAlphabet.lookupIndex(sysB.getDepLabel(), true);
            this.labE.add(goldBDepLabelAsInt, sysBDepLabelAsInt);
        }
    }

    public static void main(String[] args) {
        processArguments(args);

        // label(String) 2 label(Int)
        Eval eval = new Eval();

        try {
            if (goldFile != null && sysFile != null) {
                CaboCha2Dep goldPipe = new CaboCha2Dep(new FileInputStream(goldFile));
                CaboCha2Dep systemPipe = new CaboCha2Dep(new FileInputStream(sysFile));	

                while (!goldPipe.eof()) {
                    DependencyTree goldTree = goldPipe.pipePerSentence();
                    DependencyTree systemTree = systemPipe.pipePerSentence();
                    if (goldTree == null || systemTree == null) { break; }
                    eval.getResults(goldTree, systemTree);
                }
            } else if (goldDir != null && sysDir != null) {
                File[] goldFiles = goldDir.listFiles();
                File[] sysFiles = sysDir.listFiles();
                if (goldFiles.length != sysFiles.length) {
                    System.err.println("the number of examples different between gold and system results.");
                    System.err.println("gold: "+goldFiles.length+"  sys: "+sysFiles.length);
                } else {
                    System.err.println("the number of examples: "+goldFiles.length);
                }

                TObjectIntHashMap<String> sysFilesHash = new TObjectIntHashMap<String>();
                for (int i = 0; i < sysFiles.length; i++) {
                    String[] sysE = sysFiles[i].toString().split("/");
                    String sysBaseName = sysE[sysE.length-1];
                    //sysBaseName.
                    sysFilesHash.put(sysBaseName, i);
                }

                for (int i = 0; i < goldFiles.length; i++) {
                    String[] goldE = goldFiles[i].toString().split("/");
                    String goldBaseName = goldE[goldE.length-1];
                    if (!sysFilesHash.contains(goldBaseName)) {
                        System.err.println("error!");
                        //System.exit(1);
                    }
                    System.err.println("processsing "+goldBaseName);
                    int idx = sysFilesHash.get(goldBaseName);
                    CaboCha2Dep sysPipe = new CaboCha2Dep(new FileInputStream(sysFiles[idx]));	
                    CaboCha2Dep goldPipe = new CaboCha2Dep(new FileInputStream(goldFiles[i]));
                    DependencyTree sysTree = sysPipe.pipePerSentence();
                    DependencyTree goldTree = goldPipe.pipePerSentence();
                    if (goldTree == null) { continue; }
                    eval.getResults(goldTree,sysTree);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Alphabet labelAlphabet = eval.getLabelAlphabet();

        Evaluator labE = eval.getLabE();
        int[] labels = labE.getLabels();

        System.out.print("Labeled precision:\t");
        System.out.print("("+labE.getNumberOfCorrect()+")");
        System.out.print(" / ("+labE.getNumberOfReturned()+")");
        System.out.printf(" * 100 = %3.2f %% \n", labE.getTotalPrecision());

        System.out.print("Labeled recall:\t\t");
        System.out.print("("+labE.getNumberOfCorrect()+")");
        System.out.print(" / ("+labE.getNumberOfTarget()+")");
        System.out.printf(" * 100 = %3.2f %%\n", labE.getTotalRecall());

        System.out.print("Labeled F1:\t\t");
        System.out.printf("%3.2f \n", labE.getTotalF());
        System.out.print("Macro F1:\t\t");
        System.out.printf("%3.2f \n", labE.getTotalMacroF());
        System.out.print("\n");


        System.out.print("--- Total Performance ---------------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(labE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%6d/%6d)\t|", labE.getPrecision(labels[i]), labE.getNumberOfCorrect(labels[i]), labE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%6d/%6d)\t|", labE.getRecall(labels[i]), labE.getNumberOfCorrect(labels[i]), labE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", labE.getF(labels[i]));
            System.out.print("\n");
        }
        System.out.print("-------------------------------------------------------------------------\n");

        System.out.println("Confusion Matrix");
        System.out.print("--------------");
        for (int i = 0; i < labels.length; i++) {
            System.out.printf("%5s", "-----");
        }
        System.out.print("-\n");

        System.out.printf("| gold \\ sys |", "");            
        for (int i = 0; i < labels.length; i++) {
            System.out.printf("%5s", labelAlphabet.lookupObject(labels[i]));
        }
        System.out.print("|\n");
        System.out.print("--------------");
        for (int i = 0; i < labels.length; i++) {
            System.out.printf("%5s", "-----");
        }
        System.out.print("-\n");

        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.printf("%12s|", labelAlphabet.lookupObject(labels[i]));
            for (int j = 0; j < labels.length; j++) {
                System.out.printf("%5d", labE.getNum(labels[i], labels[j]));
            }
            System.out.print("|\n");
        }
        System.out.print("--------------");
        for (int j = 0; j < labels.length; j++) {
            System.out.printf("%5s", "-----");
        }
        System.out.print("-\n");

    }

    private static void processArguments(String[] args) {
        int idx = 0;
        while (idx < args.length) {
            if(args[idx].equals("--gold-file") || args[idx].equals("-g")) {
                idx++;
                goldFile = new File(args[idx++]);
            } else if(args[idx].equals("--goldn-dir") || args[idx].equals("-gd")) {
                idx++;
                goldDir = new File(args[idx++]);
            } else if(args[idx].equals("--sys-file") || args[idx].equals("-s")) {
                idx++;
                sysFile = new File(args[idx++]);
            } else if(args[idx].equals("--sys-dir") || args[idx].equals("-sd")) {
                idx++;
                sysDir = new File(args[idx++]);
            } else {
                idx++;
            }
        }
    }

}
