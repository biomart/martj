package org.ensembl.mart.explorer;

import java.sql.*;
import org.apache.log4j.*;
import java.util.*;

public class CommandLineFrontEnd {
    private Logger logger = Logger.getLogger(CommandLineFrontEnd.class.getName());
    private String[] commandLineParams;
    private Engine engine;

    /** @throws IllegalArgumentException if host or user are null. */
    public CommandLineFrontEnd(Engine engine, String host, String port, String user, String password,
        String[] commandLineParams) {
            logger.info("Constructor:" + Arrays.asList(commandLineParams));
            if (host == null) throw new IllegalArgumentException("host is not set");
            if (user == null) throw new IllegalArgumentException("user is not set");
            this.engine = engine;
            this.commandLineParams = commandLineParams;
            this.engine.init(host, port, user, password);
    }

    public void run() {
        logger.info("Running");
        try {
            System.out.println(engine.databases());
        } catch (SQLException e) {
            logger.error("", e);
        }
    }

    public void execute(Connection conn, Query query, ResultRenderer renderer) {
    }
}
