/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.initialdemandgeneration.empiricalsocnet.toy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSamplerUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSamplingConfigGroup;

import java.util.Random;

/**
 * @author thibautd
 */
public class GenerateToySocialNetwork {
	public static void main( final String... args ) {
		final ToySocialNetworkConfigGroup configGroup = new ToySocialNetworkConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , configGroup , new SocialNetworkSamplingConfigGroup() );

		MoreIOUtils.initOut( config.controler().getOutputDirectory() , config );

		new ConfigWriter( config ).write( config.controler().getOutputDirectory()+"/output_config.xml" );

		try ( final AutocloserModule closer = new AutocloserModule() ){
			final Scenario scenario = ToySocialNetworkUtils.generateRandomScenario( new Random( 8 ) , config );

			final SocialNetwork socialNetwork =
					SocialNetworkSamplerUtils.sampleSocialNetwork(
							scenario,
							closer,
							new ToySocialNetworkModule() );

			new SocialNetworkWriter( socialNetwork ).write( config.controler().getOutputDirectory() + "/output_socialNetwork.xml.gz" );
		}
		catch ( RuntimeException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}
}
