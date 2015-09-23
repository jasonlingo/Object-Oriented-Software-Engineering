//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

/**
 * @author Li-Yi Lin
 */

package com.oose2015.llin34.hareandhounds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

import static spark.Spark.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Bootstrap {
    public static final String IP_ADDRESS = "localhost";
    public static final int PORT = 8080;

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) throws Exception {
        //Check if the database file exists in the current directory. Abort if not
        DataSource dataSource = configureDataSource();
        if (dataSource == null) {
            System.out.printf("Could not find hareandhounds.db in the current directory (%s). Terminating\n",
                    Paths.get(".").toAbsolutePath().normalize());
            System.exit(1);
        }

        //Specify the IP address and Port at which the server should be run
        ipAddress(IP_ADDRESS);
        port(PORT);

        //Specify the sub-directory from which to serve static resources (like html and css)
        staticFileLocation("/public");

        //Create the model instance and then configure and start the web service
        try {
            GameService model = new GameService(dataSource);
            new GameController(model);
        } catch (GameService.GameServiceException ex) {
            logger.error("Failed to create a GameService instance. Aborting");
        }
    }

    /**
     * Check if the database file exists in the current directory. If it does
     * create a DataSource instance for the file and return it.
     * @return javax.sql.DataSource corresponding to the hareandhounds database
     */
    private static DataSource configureDataSource() {
        Path gamePath = Paths.get(".", "hareandhounds.db");
        if ( !(Files.exists(gamePath) )) {
            try { Files.createFile(gamePath); }
            catch (java.io.IOException ex) {
                logger.error("Failed to create hareandhounds.db file in current directory. Aborting");
            }
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:hareandhounds.db");
        return dataSource;

    }
}
