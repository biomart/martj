package org.ensembl.mart.explorer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;

public class martShell {

    protected BufferedReader reader;

    protected final String exit = "exit";
    protected final String quit = "quit";
    protected final String help = "help";
    protected final String lineEnd = ";";
    protected final String listStart = "list";
    protected final String qStart = "select";
    protected final String qFrom = "from";
    protected final String qWhere = "where";
    protected final String qLimit = "limit";
    protected final String qInto = "into";
    protected final String lStart = "(";
    protected final String lEnd = ")";
    protected final String id = "id";
    protected final int sublevel = 0;
    protected List qualifiers = Arrays.asList(new String[] {"=", "<", ">", "<=", ">=", "exclusive", "excluded", "in"});

    protected boolean issub = false;
    protected StringBuffer conline = new StringBuffer();

    public static void main (String[] args) {
        new martShell().run();
    }

    public martShell() {
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void run(){
        String thisline = null;
        mainPrompt();
        while (true) {
            try {
                  thisline = reader.readLine();
                  if (thisline.equals(exit) || thisline.equals(quit))
                      break;
	          parse(thisline);
                  thisline = null;
            } catch (IOException e) {
		System.out.println("Couldnt get that one"+e);
                break;
	    }

	}
	ExitShell();
    }

    public void parse(String line) {
        if (line.length() == 0) {
            if (issub)
                subPrompt();
            else
                mainPrompt();
	}
        else if (line.equals(help)) {
            if (issub) {
                System.out.println("help not applicable in the middle of a query");
                subPrompt();
	    }
            else {
                System.out.println(help());
                mainPrompt();
	    }
	}
        else if (line.equals(lineEnd)) {
            String command = conline.append(line).toString().trim();
	    parseCommand(command);
            mainPrompt();
            conline = new StringBuffer();
	}
        else if (line.endsWith(lineEnd)) {
            String command = conline.append(" "+line).toString().trim();
	    parseCommand(command);
            mainPrompt();
            conline = new StringBuffer();
	}
	else {
            conline.append(" "+line);
            subPrompt();
	}
    }

    public void ExitShell() {
        System.exit(0);
    }

    public void mainPrompt() {
        issub = false;
        System.out.print(">");
    }

    public void subPrompt() {
        issub = true;
        System.out.print("%");
    }

    public String help() {
        return "JUST TYPE SOMETHING IN, SILLY\n";
    }

    public void parseCommand(String command) {
        int cLen = command.length();

        if (cLen == 0) {
            return;
        }
        else if (command.startsWith(qStart)) {
           List atts = new ArrayList();
           Hashtable filts = new Hashtable();

           String dataset = null;
           String where = null;
           String limit = null;
           String into = null;

           int fIndex = command.indexOf(qFrom);
           if (fIndex < 0) {
             System.out.println("You must supply a from with a query: "+command);
             return;
           }
           String attList = command.substring(qStart.length(), fIndex - 1).trim();

           StringTokenizer att = new StringTokenizer(attList, ",", false);
           if (att.countTokens() > 1) {
             while (att.hasMoreTokens()) {
                atts.add(att.nextToken().trim());
             }
           }
           else
             atts.add(attList);

           int lineEndIndex = command.indexOf(lineEnd);
           int whereIndex = command.indexOf(qWhere);
           int limitIndex = command.lastIndexOf(qLimit);
           int intoIndex = command.lastIndexOf(qInto);

           if (limitIndex > -1 && intoIndex > -1 && limitIndex > intoIndex) {
	       System.out.println("Invalid Query, limit must precede into: "+command);
               return;
           }

           if (whereIndex > -1) {
             dataset = command.substring(fIndex + qFrom.length(), whereIndex - 1).trim();

             if (limitIndex > -1) {
               if( whereIndex > limitIndex) {
                 System.out.println("Invalid Query: where must precede the limit clause: "+command);
                 return;
               }
               else
                  where = command.substring(whereIndex + qWhere.length(), limitIndex).trim();

               if (intoIndex > -1) {
                 limit = command.substring(limitIndex + qLimit.length(), intoIndex).trim();
                 into = command.substring(intoIndex + qInto.length(), lineEndIndex).trim();
	       }
               else
                 limit = command.substring(limitIndex + qLimit.length(), lineEndIndex).trim();

	     }
             else if (intoIndex > -1) {
		 if (whereIndex > intoIndex) {
		     System.out.println("Invalid Query: Where must precede the into clause: "+command);
                     return;
                 }
                 else {
		   where = command.substring(whereIndex + qWhere.length(), intoIndex).trim();
                   into = command.substring(intoIndex + qInto.length(), lineEndIndex).trim();
		 }
	     }
             else
		 where = command.substring(whereIndex + qWhere.length(), lineEndIndex).trim();

             StringTokenizer whereTokens = new StringTokenizer(where, " ", false);
                  
             String filterType = null;
             String filtCond = null;
             boolean islist = false;
             boolean issubr = false;
             StringBuffer sub = new StringBuffer();
             StringBuffer thisList = new StringBuffer();
             int liststart = 0;
             int listend = 0;
             int open = 0;

             while (whereTokens.hasMoreTokens()) {
               String nextWhere = whereTokens.nextToken();

               if (nextWhere.endsWith(","))
                 nextWhere = nextWhere.substring(0, nextWhere.length() - 1);

               if (filterType == null)
                 filterType = nextWhere;
               else {
		 if (nextWhere.equals("excluded"))
                   filts.put(filterType, nextWhere);
		 else if (nextWhere.equals("exclusive"))
                   filts.put(filterType, nextWhere);
                 else if (islist) {
		   if (nextWhere.startsWith(lStart)) {
		     open++;
		     
                     nextWhere = nextWhere.substring(1);

                     if (nextWhere.startsWith(qStart)) {
		       //if (sublevel > 0) {
		       //System.out.println("Can only have one nested subroutine");
		       //return;
		       //}
		       islist = false;
		       issubr = true;
                       sub.append(nextWhere);
		     }
		     else {
                       // single token surrounded by perenthesis
		       if (nextWhere.endsWith(lEnd)) {
                         islist = false;
                         filts.put(filterType, filtCond+" LIST "+thisList.append(nextWhere.substring(0, nextWhere.indexOf(lEnd)).toString()));
                         thisList = new StringBuffer();
                         filterType = null;
                         open = 0;
		       }
                       else
                         thisList.append(nextWhere+",");
                     }
                   }
		   else if (nextWhere.endsWith(lEnd)) {
                       // last token in a series within perenthesis

                       islist = false;
                       filts.put(filterType, filtCond+" LIST "+thisList.append(nextWhere.substring(0, nextWhere.indexOf(lEnd)).toString()));
                       thisList = new StringBuffer();
                       filterType = null;
		       open = 0;
		   }
                   else if (nextWhere.indexOf("file:") > -1) {
		       //URL
                       filts.put(filterType, filtCond+" URL "+nextWhere);
                       filterType = null;
		   }
                   else
		       thisList.append(nextWhere+",");
		 }	   
                 else if(issubr) {
                     char operen = '(';
                     char cperen = ')';
                     int lastindex = 0;
                     for (int i = 0, n = nextWhere.length(); i < n; i++) {
                         char c = nextWhere.charAt(i);
			 if ( c == cperen) {
			     open--;
                             if (open > 0)
				 lastindex++;
			 }
                         else {
                             if (c == operen)
				 open++;
			     lastindex++;
			 }
		     }

                     nextWhere = nextWhere.substring(0, lastindex);
                     if (open == 0) {
			issubr = false;
                        sub.append(" "+nextWhere);
                        filts.put(filterType, filtCond+" SubQuery "+sub.toString());
                        filterType = null;
                        sub = new StringBuffer();
		     }
                     else
		        sub.append(" "+nextWhere);
		 }
                 else if (nextWhere.equals("in")) {
		   islist = true;
                   filtCond = nextWhere;
		 }
                 else {
		   String filtValue = whereTokens.nextToken();

                   if (filtValue.endsWith(","))
		     filtValue = filtValue.substring(0, filtValue.indexOf(","));

                   filts.put(filterType, nextWhere+" "+filtValue);
                   filterType = null;
		 }
	       }
	     }
	   }
           else if (limitIndex > -1) {
	     System.out.println("parsing limit");

             dataset = command.substring(fIndex + qFrom.length(), limitIndex - 1).trim();
             if (intoIndex > -1) {
 	       System.out.println("Parsing into");

               limit = command.substring(limitIndex + qLimit.length(), intoIndex).trim();
               into = command.substring(intoIndex + qInto.length(), lineEndIndex).trim();
             }
             else
               limit = command.substring(limitIndex + qLimit.length(), lineEndIndex).trim();
           }
           else if (intoIndex > -1) {
              System.out.println("Parsing into");
              dataset = command.substring(fIndex + qFrom.length(), intoIndex - 1).trim();
              into = command.substring(intoIndex + qInto.length(), lineEndIndex).trim();
           }
           else {
	     System.out.println("Just select and dataset");

             dataset = command.substring(fIndex + qFrom.length(), lineEndIndex).trim();
           }
           // process it all

           System.out.println("Requested query from "+dataset);

           int n = atts.size();
           System.out.println("\nwith "+n+" Attributes\n");
           for (int i = 0; i < n; i++) {
             System.out.println((String) atts.get(i)+"\n");
           }

           if (where != null) {
             System.out.println("\nand "+filts.size()+" Filters:\n");

             for(Iterator iter = filts.keySet().iterator(); iter.hasNext();) {
               String filterType = (String) iter.next();
               String filtValue = (String) filts.get(filterType);
               System.out.println(filterType+" "+filtValue+"\n");
             }
           }

           if (limit != null)
             System.out.println("Limit "+limit+"\n");

           if (into != null)
             System.out.println("into "+into);
	}
        else {
          System.out.println("Invalid Command, please try again "+command);
        }
    }
}
