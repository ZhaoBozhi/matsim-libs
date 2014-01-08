/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data;

import org.matsim.api.core.v01.Scenario;


public class MatsimVrpData
{
    private final VrpData vrpData;
    private final Scenario scenario;
    private final MobsimAgentMappings agentMappings = new MobsimAgentMappings();


    public MatsimVrpData(VrpData vrpData, Scenario scenario)
    {
        this.vrpData = vrpData;
        this.scenario = scenario;
    }


    public VrpData getVrpData()
    {
        return vrpData;
    }


    public Scenario getScenario()
    {
        return scenario;
    }


    public MobsimAgentMappings getMobsimAgentMappings()
    {
        return agentMappings;
    }
}
