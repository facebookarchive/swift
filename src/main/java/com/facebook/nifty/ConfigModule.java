package com.facebook.nifty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.commons.configuration.*;

import java.io.File;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/18/12
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigModule extends AbstractModule{
    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public Configuration getConfig() throws ConfigurationException {
        File file = new File("./conf/nifty.properties");
        if (file.exists()) {
            return new CompositeConfiguration(
                    Arrays.asList(
                        new PropertiesConfiguration(file),
                        new SystemConfiguration()
                    )
            );
        }
        return new SystemConfiguration();
    }
}
