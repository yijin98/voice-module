    
package model;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
// import edu.cmu.sphinx.result.WordResult;

import java.net.*;
import java.io.*;
import javax.swing.*;

public class SpeechRecognizerMain{
	
	// Necessary
	private LiveSpeechRecognizer recognizer;
	
	// Logger
	private Logger logger = Logger.getLogger(getClass().getName());
	
	/**
	 * This String contains the Result that is coming back from SpeechRecognizer
	 */
	private String speechRecognitionResult;
	
	//-----------------Lock Variables-----------------------------
	
	/**
	 * This variable is used to ignore the results of speech recognition cause actually it can't be stopped...
	 * 
	 * <br>
	 * Check this link for more information: <a href=
	 * "https://sourceforge.net/p/cmusphinx/discussion/sphinx4/thread/3875fc39/">https://sourceforge.net/p/cmusphinx/discussion/sphinx4/thread/3875fc39/</a>
	 */
	private boolean ignoreSpeechRecognitionResults = false;
	
	/**
	 * Checks if the speech recognize is already running
	 */
	private boolean speechRecognizerThreadRunning = false;
	
	/**
	 * Checks if the resources Thread is already running
	 */
	private boolean resourcesThreadRunning;
	
	//---
	
	/**
	 * This executor service is used in order the playerState events to be executed in an order
	 */
	private ExecutorService eventsExecutorService = Executors.newFixedThreadPool(2);
	
	private ArrayList<String> cmdLst = new ArrayList<String>();
	
	private ServerSocket server;
	private Socket s;
	
	private window frame;
	
	class MyThread extends Thread {
		public void run() {
			try {
//				ServerSocket server = new ServerSocket(4999);
//				Socket s = server.accept();
				
				
//				InetAddress host = InetAddress.getLocalHost();
//				Socket s = new Socket(host.getHostName(), 4999);
				
//				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				OutputStream out = s.getOutputStream();
				DataOutputStream oout = new DataOutputStream(out);
				
				while (true) {
					while(cmdLst.isEmpty())
						try { Thread.sleep(100); } catch(Exception e) {}
					String output = cmdLst.get(0);
					frame.appendA("sending command: " + output + "\n");
					oout.writeUTF(output);
					cmdLst.remove(0);
					frame.appendA("sending successfully!\n");
					if (output.contains("low speed")) {
						frame.setLabel("<html>Speed:<br>LOW</html>");
					} else if(output.contains("normal speed")) {
						frame.setLabel("<html>Speed:<br>NORMAL</html>");
					} else if(output.contains("high speed")) {
						frame.setLabel("<html>Speed:<br>HIGH</html>");
					}
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("IO Exception setting up server");
				e.printStackTrace();
			}
			
		}
	}
	
	class MyThread2 extends Thread {
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				while(true) {
					String input = in.readLine();
					System.out.println(input);
					frame.setTextField(input.substring(2));
					Voice voice = new Voice("kevin16");
					voice.say(input.substring(2));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("ERROR!");
				e.printStackTrace();
			}
			
		}
	}
	//------------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public SpeechRecognizerMain() throws IOException{
		frame = new window();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 540);
        frame.setTitle("Voice Recognition Module");
        frame.setVisible(true);
		
		server = new ServerSocket(4999);
		s = server.accept();
		frame.setTextA("\nClient Connected!\n");
		MyThread th = new MyThread();
		th.start();
		MyThread2 th2 = new MyThread2();
		th2.start();
		
		// Loading Message
		logger.log(Level.INFO, "Loading Speech Recognizer...\n");
				
		// Configuration
		Configuration configuration = new Configuration();
				
		// Load model from the jar
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
			
		//configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
				
		// Grammar
		configuration.setGrammarPath("resource:/grammars");
		configuration.setGrammarName("grammar");
		configuration.setUseGrammar(true);
			
		try {
			recognizer = new LiveSpeechRecognizer(configuration);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		// Start recognition process pruning previously cached data.
		// recognizer.startRecognition(true);
				
		//Check if needed resources are available
		startResourcesThread();
		//Start speech recognition thread
		startSpeechRecognition();
				
	}
	
	/**
	 * Starts the Speech Recognition Thread
	 */
	public synchronized void startSpeechRecognition() {
		
		//Check lock
		if (speechRecognizerThreadRunning)
			logger.log(Level.INFO, "Speech Recognition Thread already running...\n");
		else
			//Submit to ExecutorService
			eventsExecutorService.submit(() -> {
				
				//locks
				speechRecognizerThreadRunning = true;
				ignoreSpeechRecognitionResults = false;
				
				//Start Recognition
				recognizer.startRecognition(true);
				
				//Information			
				logger.log(Level.INFO, "You can start to speak...\n");
				
				try {				
					ArrayList<String> lst = new ArrayList<String>();
					// double check boolean when execute
					//boolean check = false;
					int changeCmdIndex = -1;
					int correctionCmdIndex = -1;
					boolean pause = false;
					while (speechRecognizerThreadRunning) {
						/*
						 * This method will return when the end of speech is reached. 
						 * Note that the end pointer will determine the end of speech.
						 */
						SpeechResult speechResult = recognizer.getResult();
						
						// Check if we ignore the speech recognition results
						if (!ignoreSpeechRecognitionResults) {
							
							// Check the result
							if (speechResult == null)
								logger.log(Level.INFO, "I can't understand what you said.\n");
							else {
								
								//Get the hypothesis
								speechRecognitionResult = speechResult.getHypothesis();
								speechRecognitionResult = convertNumber(speechRecognitionResult);
								speechRecognitionResult = wipeSpace(speechRecognitionResult);
								
								if (!pause) {
									frame.setTextA("You said: [" + speechRecognitionResult + "]\n");
									
									/*
									 * if(check) { check = false; if
									 * (speechRecognitionResult.equals("confirm command")) { sendCmdLst(lst); } }
									 * else
									 */if (changeCmdIndex >= 0) {
										lst.remove(changeCmdIndex);
										lst.add(changeCmdIndex, speechRecognitionResult);
										changeCmdIndex = -1;
									} else if (correctionCmdIndex >= 0) {
										String newNum;
										if (!speechRecognitionResult.contains("replace with")) {
											frame.appendA("Wrong Answer!\n");
										} else {
											newNum = speechRecognitionResult.substring(13);
											if(newNum.length() > 4) {
												frame.appendA("the Number you said is not valid!\n");
											} else {
												String cmd = lst.get(lst.size() - 1);
												String[] arrCmd = cmd.split(" ", -1);
												ArrayList<String> parsedCmd = new ArrayList<String>(Arrays.asList(arrCmd));
												ArrayList<Integer> arguments = new ArrayList<Integer>();
												for (int i = 0; i < parsedCmd.size(); i++) {
													if(Character.isDigit(parsedCmd.get(i).charAt(0)) && i != parsedCmd.size() - 1) {
														arguments.add(i);
														while (Character.isDigit(parsedCmd.get(i+1).charAt(0)) && i != parsedCmd.size() - 1) {
															parsedCmd.set(i, parsedCmd.get(i) + parsedCmd.get(i + 1));
															parsedCmd.remove(i + 1);
														}
													}	
												}
												if (correctionCmdIndex > arguments.size()) {
													frame.appendA("the No." + correctionCmdIndex + " argument does not exists\n");
												} else {
													parsedCmd.set(arguments.get(correctionCmdIndex), newNum);
													String temp = parsedCmd.get(0);
													for (int i = 1; i < parsedCmd.size(); i++) {
														temp += " " + parsedCmd.get(i);
													}
													lst.set(lst.size() - 1, temp);
												}
											}
										}
										correctionCmdIndex = -1;
									} else if (speechRecognitionResult.equals("clear command list")) {
										lst.clear();
									} else if (speechRecognitionResult.equals("execute command")) {
										sendCmdLst(lst);
//										frame.appendA("If you are sure to execute command, speak \"Confirm Command\"\n");
//										check = true;
//									} else if (speechRecognitionResult.equals("shut down")) {
//										System.exit(0);
									} else if (speechRecognitionResult.contains("change command")) {
										try {
											changeCmdIndex = Integer.parseInt(speechRecognitionResult.substring(15,16)) - 1;
											if(changeCmdIndex >= lst.size()) {
												changeCmdIndex = -1;
												frame.appendA("Warning: target command does not exist!\n");
											} else {
												frame.appendA("You want to change Command No." + (changeCmdIndex + 1) + 
														" in the command list \nWhat would you like to change it into?\n");
											}
										} catch (Exception e) {
											frame.appendA("Target is not a simple number\n");
										}
									} else if (speechRecognitionResult.contains("correction argument")) {
										if (lst.isEmpty())
											frame.appendA("there are no command in the command list YET\n");
										else {
											correctionCmdIndex = Integer.parseInt(speechRecognitionResult.substring(20,21)) - 1;
											frame.appendA("You want to correct No." + (changeCmdIndex + 1) + 
													" argument in the command \nWhat would you like to change it into?\n");
										}
									} else if (speechRecognitionResult.contains("delete command")){
										try {
											int deleteIndex = Integer.parseInt(speechRecognitionResult.substring(15,16)) - 1;
											if (deleteIndex >= 0 && deleteIndex < lst.size()) {
												lst.remove(deleteIndex);
												frame.appendA("Command No." + (deleteIndex + 1) + " deleted successfully!\n");
											} else {
												System.out.println("Target command does not exist!");
											}
										} catch (Exception e) {
											frame.appendA("Target is not a simple number!\n");
										}									
									} else if (speechRecognitionResult.equals("program pause")) {
										pause = true;
										frame.appendA("The program is now paused, say \"Program resume\" to bring it back!\n");
									} else {
										if(!speechRecognitionResult.contains("unk"))
											lst.add(speechRecognitionResult);
										System.out.println(speechRecognitionResult);
//										if(speechRecognitionResult != "<unk>")
//											frame.setTextA("unknown");
									}
									
									int n = lst.size();
									frame.setTextB("This is the current command list:\n");
									for (int i = 0; i < n; i++) {
										frame.appendB("Command No." + (i + 1) + " " + lst.get(i) + "\n");
									}
									//Call the appropriate method 
									//makeDecision(speechRecognitionResult, speechResult.getWords());
								} else {
									if (speechRecognitionResult.equals("program resume")) {
										pause = false;
										frame.setTextA("Program Continues!!\n");
									}
								}
							}
						} else
							logger.log(Level.INFO, "Ingoring Speech Recognition Results...");
	
					}
				} catch (Exception ex) {
					logger.log(Level.WARNING, null, ex);
					speechRecognizerThreadRunning = false;
				}
				
				logger.log(Level.INFO, "SpeechThread has exited...");
				
			});
	}
	
	/**
	 * Wipe the space and add underscore between certain words
	 */
	public void sendCmdLst(ArrayList<String> lst){
		int n = lst.size();
		frame.appendA("Executimg Command List:\n");
		for (int i = 0; i < n; i++) {
			frame.appendA("Command No." + (i + 1) + " " + lst.get(i) + "\n");
			cmdLst.add(lst.get(i) + ";");
		}
		lst.clear();
	}
	
	/**
	 * Wipe the space and add underscore between certain words
	 */
	public String wipeSpace(String input) {
		String result = input;
		result = wipeSpaceHelper(result, "current pose", 7);
		result = wipeSpaceHelper(result, "screw tip", 5);
		result = wipeSpaceHelper2(result, "marker ", 6);
		result = wipeSpaceHelper(result, "rotate move", 6);
		result = wipeSpaceHelper2(result, ". ", 1);
		return result;
	}
	
	/**
	 * helper method to help wipe the space
	 */
	public String wipeSpaceHelper(String input, String target, int n) {
		String result = input;
		int pos = result.indexOf(target);
		while (pos >= 0) {
			result = result.substring(0, pos + n) + "_" + result.substring(pos + n + 1);
			pos = result.indexOf(target);
		}
		return result;
	}
	
	/**
	 * helper method No.2 to help wipe the space
	 */
	public String wipeSpaceHelper2(String input, String target, int n) {
		String result = input;
		int pos = result.indexOf(target);
		while (pos >= 0) {
			result = result.substring(0, pos + n) + result.substring(pos + n + 1);
			pos = result.indexOf(target);
		}
		return result;
	}
	
	/**
	 * Convert the numbers in the text from English to integer in string
	 */
	public String convertNumber(String input) {
		String delims = "[ ]+";
		String[] tokens = input.split(delims);
		String result = "";
		int num = 0;
		boolean isNumber = false;
		String[][] listNumber1 = {{"one", "1"},{"two", "2"},{"three", "3"},
					{"four", "4"},{"five", "5"},{"six", "6"},{"seven", "7"},
					{"eight", "8"},{"nine", "9"},{"ten", "10"}, {"eleven", "11"},
					{"twelve", "12"},{"thirteen", "13"},{"fourteen", "14"},{"fifteen", "15"},
					{"sixteen", "16"},{"seventeen", "17"},{"eighteen", "18"},{"nineteen", "19"}, {"zero", "0"}};
		String[][] listNumber2 = {{"twenty", "20"}, {"thirty", "30"}, {"forty", "40"}, {"fifty", "50"},
								{"sixty", "60"}, {"seventy", "70"}, {"eighty", "80"}, {"ninety", "20"}};
		int i = 0;
		while(i < tokens.length) {
			isNumber = false;
			for(int j = 0; j < listNumber1.length; j++) {
				if(tokens[i].equals(listNumber1[j][0])){
					num = Integer.parseInt(listNumber1[j][1]);
					result += " " + num;
					isNumber = true;
					break;
				}
			}
			for (int j = 0; j < listNumber2.length; j++) {
				if(tokens[i].equals(listNumber2[j][0])){
					num = Integer.parseInt(listNumber2[j][1]);
					if(i + 1 < tokens.length) {
						for(int k = 0; k < 9; k++) {
							if(tokens[i + 1].equals(listNumber1[k][0])){
								num += Integer.parseInt(listNumber1[k][1]);
								i++;
								break;
							}
						}
					}
					result += " " + num;
					isNumber = true;
					break;
				}
			}
			if (!isNumber) {
				if(tokens[i].equals("point"))
					result += ".";
				else
					result += " " + tokens[i];
			}
			i++;
		}
		return result.substring(1);
	}
	
	/**
	 * Stops ignoring the results of SpeechRecognition
	 */
	public synchronized void stopIgnoreSpeechRecognitionResults() {
		
		//Stop ignoring speech recognition results
		ignoreSpeechRecognitionResults = false;
	}
	
	/**
	 * Ignores the results of SpeechRecognition
	 */
	public synchronized void ignoreSpeechRecognitionResults() {
		
		//Instead of stopping the speech recognition we are ignoring it's results
		ignoreSpeechRecognitionResults = true;
		
	}
		
	/**
	 * Starting a Thread that checks if the resources needed to the SpeechRecognition library are available
	 */
	public void startResourcesThread() {
		
		//Check lock
		if (resourcesThreadRunning)
			logger.log(Level.INFO, "Resources Thread already running...\n");
		else
			//Submit to ExecutorService
			eventsExecutorService.submit(() -> {
				try {
					
					//Lock
					resourcesThreadRunning = true;
					
					// Detect if the microphone is available
					while (true) {
						
						//Is the Microphone Available
						if (!AudioSystem.isLineSupported(Port.Info.MICROPHONE))
							logger.log(Level.INFO, "Microphone is not available.\n");
						
						// Sleep some period
						Thread.sleep(350);
					}
					
				} catch (InterruptedException ex) {
					logger.log(Level.WARNING, null, ex);
					resourcesThreadRunning = false;
				}
			});
	}
	
	
	public boolean getIgnoreSpeechRecognitionResults() {
		return ignoreSpeechRecognitionResults;
	}
	
	public boolean getSpeechRecognizerThreadRunning() {
		return speechRecognizerThreadRunning;
	}
	
	/**
	 * Main Method
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		new SpeechRecognizerMain();
	}
}