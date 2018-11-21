package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.Builder;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.Attributes;

class CarrierPlanXmlParserV2 extends MatsimXmlParser {

	public static Logger logger = Logger.getLogger(CarrierPlanXmlParserV2.class);
	
	public static String ID = "id";
	
	public static String FROM = "from";
	public static String TO = "to";
	
	private static final String XX = "x" ;
	private static final String YY = "y" ;

	public static String CARRIERS = "carriers";
	public static String CARRIER = "carrier";

//	public static String LINKID = "linkId";

	public static String SERVICES = "services";
	public static String SERVICE = "service";
	private static final String SERVICE_ID = "serviceId";
	public static final String SERVICE_DEMAND_SIZE = "capacityDemand";
	public static final String SERVICE_DURATION = "serviceDuration";
	public static final String SERVICE_LATEST_START = "latestEnd";
	public static final String SERVICE_EARLIEST_START = "earliestStart";

	public static String SHIPMENTS = "shipments";
	public static String SHIPMENT = "shipment";
	public static String SHIPMENT_ID = "shipmentId";
	public static String SHIPMENT_DEMAND_SIZE = "size";
	
	private static final String PICKUP = "pickup";
	private static final String PICKUP_DURATION = "pickupServiceTime";
	private static final String START_PICKUP = "startPickup";
	private static final String END_PICKUP = "endPickup";

	private static final String DELIVERY = "delivery";
	private static final String DELIVERY_DURATION = "deliveryServiceTime";
	private static final String START_DELIVERY = "startDelivery";
	private static final String END_DELIVERY = "endDelivery";
	
	public static String VEHICLES = "vehicles";
	public static String VEHICLE = "vehicle";
	private static final String VEHICLE_ID = "vehicleId";
	private static final String VEHICLE_TYPE = "vehicleType";
	private static final String VEHICLE_TYPE_ID = "typeId";
	private static final String DEPOT_LINK_ID = "depotLinkId";
	private static final String VEHICLE_EARLIEST_START = "earliestStart";
	private static final String VEHICLE_LATEST_END = "latestEnd";
	private static final String VEHICLE_CAPACITY = "capacity";
	
	private static final String CAPABILITIES = "capabilities";
	
	private static final String FLEET_SIZE = "fleetSize";
	
	private static final String COST_INFORMATION = "costInformation";
	private static final String COSTS_PER_SECOND = "perSecond";
	private static final String COSTS_PER_METER = "perMeter";
	private static final String COSTS_FIX = "fix";
	
	private static final String ENGINE_INFORMATION = "engineInformation";
	private static final String GAS_CONSUMPTION = "gasConsumption";
	private static final String FUEL_TYPE = "fuelType";

	private static final String DESCRIPTION = "description";

	private static final String TOUR = "tour";
	private static final String PLAN = "plan";
	private static final String SELECTED = "selected";
	private static final String SCORE = "score";
	
	public static String ACTIVITY = "act";
	public static String ACT_TYPE = "type";
	private static final String ACT_END_TIME = "end_time";
	public static String START = "start";
	private static final String END = "end";
	
	private static final String ROUTE = "route";
	private static final String LEG = "leg";
	private static final String EXPECTED_TRANSPORT_TIME = "expected_transp_time";
	private static final String TRANSPORT_TIME = "transp_time";
	private static final String DEPARTURE_TIME = "dep_time";
	private static final String EXPECTED_DEPARTURE_TIME = "expected_dep_time";


	private Carrier currentCarrier = null;

	private CarrierVehicle currentVehicle = null;

	private Tour.Builder currentTourBuilder = null;

	private Id<Link> previousActLoc = null;

	private String previousRouteContent;

	public Map<String, CarrierShipment> currentShipments = null;

	public Map<String, CarrierVehicle> vehicles = null;

	public Collection<ScheduledTour> scheduledTours = null;

	public CarrierPlan currentPlan = null;
	
	public Double currentScore;
	
	public boolean selected;

	public Carriers carriers;

	private double currentLegTransTime;

	private double currentLegDepTime;
	
	
	private Builder capabilityBuilder;

	private org.matsim.contrib.freight.carrier.CarrierVehicleType.Builder vehicleTypeBuilder;

	private Map<Id<VehicleType>, CarrierVehicleType> vehicleTypeMap = new HashMap<>();

	private double currentStartTime;
	
	private Map<Id<CarrierService>, CarrierService> serviceMap;

	/**
	 * Constructs a reader with an empty carriers-container for the carriers to be constructed. 
	 * 
	 * @param carriers which is a map that stores carriers
	 */
	public CarrierPlanXmlParserV2(Carriers carriers) {
		super();
		this.carriers = carriers;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(CARRIER)) {
			String id = atts.getValue(ID);
			if(id == null) throw new IllegalStateException("carrierId is missing.");
			currentCarrier = CarrierImpl.newInstance(Id.create(id, Carrier.class));
		}
		//services
		else if (name.equals(SERVICES)) {
			serviceMap = new HashMap<>();
		}
		else if (name.equals(SERVICE)) {
			String idString = atts.getValue(ID);
			if(idString == null) throw new IllegalStateException("service.id is missing.");
			Id<CarrierService> id = Id.create(idString, CarrierService.class);

			CarrierService.Builder serviceBuilder ;

			String toLocation = atts.getValue(TO);

			String xx = atts.getValue( XX ) ;
			String yy = atts.getValue( YY ) ;
			if ( (xx==null && yy!=null) || (xx!=null && yy==null) )  {
				throw new IllegalStateException( "coordinates are inconsistent" ) ;
			}
			if ( xx!=null ) {
				// coordinates are in xml:
				Coord coord = new Coord( Double.parseDouble(xx), Double.parseDouble(yy) ) ;
				serviceBuilder = CarrierService.Builder.newInstance( id, coord );

				// link id is _also_ in xml:
				if ( toLocation!=null ) {
					serviceBuilder.setLinkId( Id.create( toLocation, Link.class ) ) ;
				}
			} else {
				// coordinates are NOT in xml:
				if ( toLocation==null ) throw new IllegalStateException( "both service.x/y and service.link are missing" ) ;
				serviceBuilder = CarrierService.Builder.newInstance( id, Id.createLinkId( toLocation ) );
			}

			String capDemandString = atts.getValue(SERVICE_DEMAND_SIZE);
			if(capDemandString != null) serviceBuilder.setCapacityDemand(getInt(capDemandString));
			String startString = atts.getValue(SERVICE_EARLIEST_START);
			double start = parseTimeToDouble(startString);
			double end = Double.MAX_VALUE;
			String endString = atts.getValue(SERVICE_LATEST_START);
			end = parseTimeToDouble(endString);
			serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(start, end));
			String serviceTimeString = atts.getValue(SERVICE_DURATION);
			if(serviceTimeString != null) serviceBuilder.setServiceDuration(parseTimeToDouble(serviceTimeString));
			CarrierService service = serviceBuilder.build();
			serviceMap.put(service.getId(), service);
			currentCarrier.getServices().add(service);
		}
		
		//shipments
		else if (name.equals(SHIPMENTS)) {
			currentShipments = new HashMap<String, CarrierShipment>();
		}
		else if (name.equals(SHIPMENT)) {
			String idString = atts.getValue(ID);
			if(idString == null) throw new IllegalStateException("shipment.id is missing.");
			Id<CarrierShipment> id = Id.create(idString, CarrierShipment.class);
			String from = atts.getValue(FROM);
			if(from == null) throw new IllegalStateException("shipment.from is missing.");
			String to = atts.getValue(TO);
			if(to == null) throw new IllegalStateException("shipment.to is missing.");
			String sizeString = atts.getValue(SHIPMENT_DEMAND_SIZE);										//TODO: Unify shipment.size and service.capacityDemand kmt, nov'18
			if(sizeString == null) throw new IllegalStateException("shipment.size is missing.");
			int size = getInt(sizeString);
			CarrierShipment.Builder shipmentBuilder = CarrierShipment.Builder.newInstance(id, Id.create(from, Link.class), Id.create(to, Link.class), size);
			
			String startPickup = atts.getValue(START_PICKUP);
			String endPickup = atts.getValue(END_PICKUP);
			String startDelivery = atts.getValue(START_DELIVERY);
			String endDelivery = atts.getValue(END_DELIVERY);
			String pickupServiceTime = atts.getValue(PICKUP_DURATION);
			String deliveryServiceTime = atts.getValue(DELIVERY_DURATION);
			
			if (startPickup != null && endPickup != null) shipmentBuilder.setPickupTimeWindow(TimeWindow.newInstance(parseTimeToDouble(startPickup), parseTimeToDouble(endPickup)));
			if(startDelivery != null && endDelivery != null) shipmentBuilder.setDeliveryTimeWindow(TimeWindow.newInstance(parseTimeToDouble(startDelivery), parseTimeToDouble(endDelivery)));
			if (pickupServiceTime != null) shipmentBuilder.setPickupServiceTime(parseTimeToDouble(pickupServiceTime)); 
			if (deliveryServiceTime != null) shipmentBuilder.setDeliveryServiceTime(parseTimeToDouble(deliveryServiceTime));
			
			CarrierShipment shipment = shipmentBuilder.build();
			currentShipments.put(atts.getValue(ID), shipment);
			currentCarrier.getShipments().add(shipment);
		}
		
		//capabilities							//TODO: Throw exception if unknown state (neither FINITE nor INFINITE), kmt, nov18
		else if(name.equals(CAPABILITIES)){
			String fleetSize = atts.getValue(FLEET_SIZE);
			if(fleetSize == null) throw new IllegalStateException("fleetSize is missing.");
			this.capabilityBuilder = CarrierCapabilities.Builder.newInstance();
			if(fleetSize.toUpperCase().equals(FleetSize.FINITE.toString())){ 
				this.capabilityBuilder.setFleetSize(FleetSize.FINITE);
			}
			else {
				this.capabilityBuilder.setFleetSize(FleetSize.INFINITE);
			}
		}
		
		//vehicle-type
		else if(name.equals(VEHICLE_TYPE)){
			String typeId = atts.getValue(ID);
			if(typeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			this.vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(Id.create(typeId, VehicleType.class)); 
		}
		else if(name.equals(ENGINE_INFORMATION)){
			String fuelType = atts.getValue(FUEL_TYPE);
			String gasConsumption = atts.getValue(GAS_CONSUMPTION);
			EngineInformation engineInfo = new EngineInformationImpl(parseFuelType(fuelType), Double.parseDouble(gasConsumption));
			this.vehicleTypeBuilder.setEngineInformation(engineInfo);
		}
		else if(name.equals(COST_INFORMATION)){
			String fix = atts.getValue(COSTS_FIX);
			String perMeter = atts.getValue(COSTS_PER_METER);
			String perSecond = atts.getValue(COSTS_PER_SECOND);
			if(fix != null) this.vehicleTypeBuilder.setFixCost(Double.parseDouble(fix));
			if(perMeter != null) this.vehicleTypeBuilder.setCostPerDistanceUnit(Double.parseDouble(perMeter));
			if(perSecond != null) this.vehicleTypeBuilder.setCostPerTimeUnit(Double.parseDouble(perSecond));
		}
		
		//vehicle
		else if (name.equals(VEHICLES)) {
			vehicles = new HashMap<String, CarrierVehicle>();
		}
		else if (name.equals(VEHICLE)) {
			String vId = atts.getValue(ID);
			if(vId == null) throw new IllegalStateException("vehicleId is missing.");
			String depotLinkId = atts.getValue(DEPOT_LINK_ID);
			if(depotLinkId == null) throw new IllegalStateException("depotLinkId of vehicle is missing.");
			CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(Id.create(vId, Vehicle.class), Id.create(depotLinkId, Link.class));
			String typeId = atts.getValue(VEHICLE_TYPE_ID);
			if(typeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			CarrierVehicleType vehicleType = vehicleTypeMap.get(Id.create(typeId, VehicleType.class));
			vehicleBuilder.setTypeId(Id.create(typeId, VehicleType.class));
			if(vehicleType != null) vehicleBuilder.setType(vehicleType);
			String startTime = atts.getValue(VEHICLE_EARLIEST_START);
			if(startTime != null) vehicleBuilder.setEarliestStart(parseTimeToDouble(startTime));
			String endTime = atts.getValue(VEHICLE_LATEST_END);
			if(endTime != null) vehicleBuilder.setLatestEnd(parseTimeToDouble(endTime));
			
			CarrierVehicle vehicle = vehicleBuilder.build();
			capabilityBuilder.addVehicle(vehicle);
			vehicles.put(vId, vehicle);
		}
		
		//plans
		else if(name.equals(PLAN)){
			String score = atts.getValue(SCORE);
			if(score != null) currentScore = parseTimeToDouble(score);
			String selected = atts.getValue(SELECTED);
			if(selected == null ) this.selected = false;
			else if(selected.equals(Boolean.TRUE.toString())) this.selected = true;
			else this.selected = false;
			scheduledTours = new ArrayList<ScheduledTour>();
		}
		else if (name.equals(TOUR)) {
			String vehicleId = atts.getValue(VEHICLE_ID);
			if(vehicleId == null) throw new IllegalStateException("vehicleId is missing in tour.");
			currentVehicle = vehicles.get(vehicleId);
			if(currentVehicle == null) throw new IllegalStateException("vehicle to vehicleId " + vehicleId + " is missing.");
			currentTourBuilder = Tour.Builder.newInstance();
		}
		else if (name.equals(LEG)) {
			String depTime = atts.getValue(EXPECTED_DEPARTURE_TIME);
			if(depTime == null) depTime = atts.getValue(DEPARTURE_TIME);
			if(depTime == null) throw new IllegalStateException("leg.expected_dep_time is missing.");
			currentLegDepTime = parseTimeToDouble(depTime);
			String transpTime = atts.getValue(EXPECTED_TRANSPORT_TIME);
			if(transpTime == null) transpTime = atts.getValue(TRANSPORT_TIME);
			if(transpTime == null) throw new IllegalStateException("leg.expected_transp_time is missing.");
			currentLegTransTime = parseTimeToDouble(transpTime);
		}
		else if (name.equals(ACTIVITY)) {
			String type = atts.getValue(ACT_TYPE);
			if(type == null) throw new IllegalStateException("activity type is missing");
			String actEndTime = atts.getValue(ACT_END_TIME);
			if (type.equals(START)) {
				if(actEndTime == null) throw new IllegalStateException("endTime of activity \"" + type + "\" missing.");
				currentStartTime = parseTimeToDouble(actEndTime);
				previousActLoc = currentVehicle.getLocation();
				currentTourBuilder.scheduleStart(currentVehicle.getLocation(),TimeWindow.newInstance(currentVehicle.getEarliestStartTime(), currentVehicle.getLatestEndTime()));
				
			} else if (type.equals(PICKUP)) {
				String id = atts.getValue(SHIPMENT_ID);
				if(id == null) throw new IllegalStateException("pickup.shipmentId is missing.");
				CarrierShipment shipment = currentShipments.get(id);
				finishLeg(shipment.getFrom());
				currentTourBuilder.schedulePickup(shipment);
				previousActLoc = shipment.getFrom();
			} else if (type.equals(DELIVERY)) {
				String id = atts.getValue(SHIPMENT_ID);
				if(id == null) throw new IllegalStateException("delivery.shipmentId is missing.");
				CarrierShipment shipment = currentShipments.get(id);
				finishLeg(shipment.getTo());
				currentTourBuilder.scheduleDelivery(shipment);
				previousActLoc = shipment.getTo();
			} else if (type.equals(SERVICE)){
				String id = atts.getValue(SERVICE_ID);
				if(id == null) throw new IllegalStateException("act.serviceId is missing.");
				CarrierService service = serviceMap.get(Id.create(id, CarrierService.class));
				if(service == null) throw new IllegalStateException("serviceId is not known.");
				finishLeg(service.getLinkId() );
				currentTourBuilder.scheduleService(service);
				previousActLoc = service.getLinkId();
			} else if (type.equals(END)) {
				finishLeg(currentVehicle.getLocation());
				currentTourBuilder.scheduleEnd(currentVehicle.getLocation(), TimeWindow.newInstance(currentVehicle.getEarliestStartTime(),currentVehicle.getLatestEndTime()));
			}
			
		}
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals(CAPABILITIES)){
			currentCarrier.setCarrierCapabilities(capabilityBuilder.build());
		}
		else if(name.equals(VEHICLE_CAPACITY)){
			if(content == null) throw new IllegalStateException("vehicle-capacity is missing.");
			vehicleTypeBuilder.setCapacity(Integer.parseInt(content));
		}
		else if(name.equals(VEHICLE_TYPE)){
			CarrierVehicleType type = vehicleTypeBuilder.build();
			vehicleTypeMap.put(type.getId(),type);
			capabilityBuilder.addType(type);
		}
		else if (name.equals(ROUTE)) {
			this.previousRouteContent = content;
		}
		
		else if (name.equals(CARRIER)) {
			carriers.getCarriers().put(currentCarrier.getId(), currentCarrier);
		}
		else if (name.equals(PLAN)) {
			currentPlan = new CarrierPlan(currentCarrier, scheduledTours);
			currentPlan.setScore(currentScore);
			currentCarrier.getPlans().add(currentPlan);
			if(this.selected){
				currentCarrier.setSelectedPlan(currentPlan);
			}
		}
		else if (name.equals(TOUR)) {
			ScheduledTour sTour = ScheduledTour.newInstance(currentTourBuilder.build(),currentVehicle,currentStartTime);
			scheduledTours.add(sTour);
		}
		else if(name.equals(DESCRIPTION)){
			vehicleTypeBuilder.setDescription(content);
		}
	}

	private FuelType parseFuelType(String fuelType) {
		if(fuelType.equals(FuelType.diesel.toString())){
			return FuelType.diesel;
		}
		else if(fuelType.equals(FuelType.electricity.toString())){
			return FuelType.electricity;
		}
		else if(fuelType.equals(FuelType.gasoline.toString())){
			return FuelType.gasoline;
		}
		throw new IllegalStateException("fuelType " + fuelType + " is not supported");
	}

	private void finishLeg(Id<Link> toLocation) {
		NetworkRoute route = null;
		if (previousRouteContent != null) {
			List<Id<Link>> linkIds = NetworkUtils.getLinkIds(previousRouteContent);
			route = RouteUtils.createLinkNetworkRouteImpl(previousActLoc, toLocation);
			if (!linkIds.isEmpty()) {
				route.setLinkIds(previousActLoc, linkIds, toLocation);
			}
		}
		currentTourBuilder.addLeg(currentTourBuilder.createLeg(route, currentLegDepTime,currentLegTransTime));
		previousRouteContent = null;
	}

	private double parseTimeToDouble(String timeString) {
		if (timeString.contains(":")) {
			return Time.parseTime(timeString);
		} else {
			return Double.parseDouble(timeString);
		}

	}

	private int getInt(String value) {
		return Integer.parseInt(value);
	}

}
