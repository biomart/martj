package org.ensembl.mart.explorer;

import org.apache.log4j.*;
import java.util.*;

public class SwingFrontEnd {

private Logger logger = Logger.getLogger( SwingFrontEnd.class.getName() );

  public SwingFrontEnd(Engine engine,
                       String host,
                       String port,
                       String user,
                       String password) {
    logger.info("Constructor:");


  }  

  public void run() {
    logger.info("Running");
  }
}
