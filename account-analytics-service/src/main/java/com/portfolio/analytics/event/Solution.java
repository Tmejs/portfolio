package com.portfolio.analytics.event;

import java.util.Scanner;
import java.util.Stack;

public class Solution {
    public static void main(String[] args) {

        Scanner in = new Scanner(System.in);
        int testCases = Integer.parseInt(in.nextLine());
        Stack<String> stack = new Stack<>();
        while(testCases>0){
            String line = in.nextLine();
            String text = "";
            int thisIndex;
            int lastIndex=0;
            boolean printed = false;

            for (int i=0;i<line.length(); i++) {
                thisIndex = i;

                if(line.charAt(i)=='<' && line.charAt(i+1)!='/') {
                    lastIndex = line.indexOf(">",thisIndex);
                    stack.push(line.substring(thisIndex + 1, lastIndex));
                    i = lastIndex;
                } else if(line.charAt(i)=='<' && line.charAt(i+1)=='/') {
                    lastIndex = line.indexOf(">", thisIndex);
                    String closingTag = line.substring(thisIndex+1, lastIndex);
                    if(closingTag.equals("/"+stack.pop())) {
                        if(!text.isEmpty()) {
                            printed = true;
                            System.out.print(text);
                            text ="";
                        }
                    }
                    i = lastIndex;
                } else {
                    text  +=  line.charAt(i);
                }
            }
            if(!printed) {
                System.out.print("None");
            }
            testCases--;
        }
    }
}

