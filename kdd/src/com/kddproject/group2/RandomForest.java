
package com.kddproject.group2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

// Class which initiates map reduce tasks
public class RandomForest
{
  public static ArrayList<String> attributeArrayList = new ArrayList();
  public static ArrayList<String> dataFileList = new ArrayList();
  public static ArrayList<String> originalAttributes = new ArrayList();
  public static ArrayList<String> stableAttributes = new ArrayList();
  public static ArrayList<String> decisionAttributes = new ArrayList();
  public static ArrayList<String> decisionFromAndTo = new ArrayList();
  public static String decisionFrom = new String();
  public static String decisionTo = new String();
  public static int minSupport = 0;
  public static int minConfidence = 0;
  public static Scanner inputScanner = new Scanner(System.in);
  public static int index = 0;
  
  public static void main(String[] args)
    throws IOException, ClassNotFoundException, InterruptedException
  {
    // Read attributes from attributes file
    File attribute = new File(args[0]);
    FileReader attribute_reader = new FileReader(attribute);
    BufferedReader attribute_buffer = new BufferedReader(attribute_reader);
    String att = new String();
    while ((att = attribute_buffer.readLine()) != null)
    {
      attributeArrayList.addAll(Arrays.asList(att.split("\\s+")));
      originalAttributes.addAll(Arrays.asList(att.split("\\s+")));
    }
    int count = 0;
    attribute_reader.close();
    attribute_buffer.close();
    File data = new File(args[1]);
    FileReader data_reader = new FileReader(data);
    BufferedReader data_buffer = new BufferedReader(data_reader);
    String d = new String();
    while ((d = data_buffer.readLine()) != null) {
      count++;
    }
    data_reader.close();
    data_buffer.close();
    // Ask for stable attributes
    setStableAttributes();
    // Ask for decision attributes
    setDecisionAttribute();
    // Ask for decision from and to attributes
    setDecisionFromAndTo(args[1]);

    // Ask for minimim support and confidence
    System.out.println("Please enter minimum Support: ");
    minSupport = inputScanner.nextInt();
    System.out.println("Please enter minimum Confidence %: ");
    minConfidence = inputScanner.nextInt();
    inputScanner.close();
    
    Configuration configuration = new Configuration();
    
    configuration.set("mapred.max.split.size", data.length() / 5L + "");
    configuration.set("mapred.min.split.size", "0");
    
    configuration.setInt("count", count);
    configuration.setStrings("attributes", (String[])Arrays.copyOf(originalAttributes.toArray(), originalAttributes.toArray().length, String[].class));
    
    configuration.setStrings("stable", (String[])Arrays.copyOf(stableAttributes.toArray(), stableAttributes.toArray().length, String[].class));
    
    configuration.setStrings("decision", (String[])Arrays.copyOf(decisionAttributes.toArray(), decisionAttributes.toArray().length, String[].class));
    
    configuration.setStrings("decisionFrom", new String[] { decisionFrom });
    configuration.setStrings("decisionTo", new String[] { decisionTo });
    configuration.setStrings("support", new String[] { minSupport + "" });
    configuration.setStrings("confidence", new String[] { minConfidence + "" });
    

    // MapReduce job to calculate Action Rules
    Job actionRulesJob = Job.getInstance(configuration);
    
    actionRulesJob.setJarByClass(ActionRules.class);
    
    actionRulesJob.setMapperClass(ActionRules.JobMapper.class);
    actionRulesJob.setReducerClass(ActionRules.JobReducer.class);
    
    actionRulesJob.setNumReduceTasks(1);
    
    actionRulesJob.setOutputKeyClass(Text.class);
    actionRulesJob.setOutputValueClass(Text.class);
    
    FileInputFormat.addInputPaths(actionRulesJob, args[1]);
    
    FileOutputFormat.setOutputPath(actionRulesJob, new Path(args[2]));
    
    actionRulesJob.waitForCompletion(true);
    
    // MapReduce job to calculate Association Action Rules
    Job associationActionRulesJob = Job.getInstance(configuration);
    
    associationActionRulesJob.setJarByClass(AssociationActionRules.class);
    
    associationActionRulesJob.setMapperClass(AssociationActionRules.JobMapper.class);
    
    associationActionRulesJob.setReducerClass(AssociationActionRules.JobReducer.class);
    
    associationActionRulesJob.setNumReduceTasks(1);
    
    associationActionRulesJob.setOutputKeyClass(Text.class);
    associationActionRulesJob.setOutputValueClass(Text.class);
    
    FileInputFormat.addInputPaths(associationActionRulesJob, args[1]);
    
    FileOutputFormat.setOutputPath(associationActionRulesJob, new Path(args[3]));
    
    associationActionRulesJob.waitForCompletion(true);
  }
  
  public static void setStableAttributes()
  {
    // Function requestion stable attributes
    boolean flag = false;
    String[] stable = null;
    System.out.println("-------------Random Forest Algorithm------------");
    System.out.println("Available attributes are: " + attributeArrayList.toString());
    
    System.out.println("Please enter the Stable Attribute(s):");
    String s = inputScanner.next();
    if (s.split(",").length > 1)
    {
      stable = s.split(",");
      for (int j = 0; j < stable.length; j++) {
        if (!attributeArrayList.contains(stable[j]))
        {
          System.out.println("Invalid Stable attribute(s)");
          flag = true;
          break;
        }
      }
      if (!flag)
      {
        stableAttributes.addAll(Arrays.asList(stable));
        attributeArrayList.removeAll(stableAttributes);
      }
    }
    else if (!attributeArrayList.contains(s))
    {
      System.out.println("Invalid Stable attribute(s)");
    }
    else
    {
      stableAttributes.add(s);
      attributeArrayList.removeAll(stableAttributes);
    }
    System.out.println("Stable Attribute(s): " + stableAttributes.toString());
    
    System.out.println("Available Attribute(s): " + attributeArrayList.toString());
  }
  
  public static void setDecisionAttribute()
  {
    // Function requestion decision attributes
    System.out.println("1. Enter the Decision Attribute:");
    String s = inputScanner.next();
    if (!attributeArrayList.contains(s))
    {
      System.out.println("Invalid Decision attribute(s)");
    }
    else
    {
      decisionAttributes.add(s);
      index = originalAttributes.indexOf(s);
      attributeArrayList.removeAll(decisionAttributes);
    }
  }
  
  public static void setDecisionFromAndTo(String args)
  {
    // Function requestion secision from and to attributes
    HashSet<String> set = new HashSet();
    File data = new File(args);
    try
    {
      FileReader fileReader = new FileReader(data);
      BufferedReader data_buffer = new BufferedReader(fileReader);
      String str = new String();
      while ((str = data_buffer.readLine()) != null) {
        set.add(str.split(",")[index]);
      }
      fileReader.close();
      data_buffer.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    System.out.println();
    Iterator<String> iterator = set.iterator();
    while (iterator.hasNext()) {
      decisionFromAndTo.add((String)originalAttributes.get(index) + (String)iterator.next());
    }
    System.out.println("Available decision attributes are: " + decisionFromAndTo.toString());
    
    System.out.println("Enter decision FROM attribute: ");
    decisionFrom = inputScanner.next();
    System.out.println("Enter decision TO Attribute: ");
    decisionTo = inputScanner.next();
    System.out.println("Stable attributes are: " + stableAttributes.toString());
    
    System.out.println("Decision attribute is: " + decisionAttributes.toString());
    
    System.out.println("Decision FROM : " + decisionFrom);
    System.out.println("Decision TO : " + decisionTo);
    System.out.println("Flexible Attribute(s) are: " + attributeArrayList.toString());
  }
}
