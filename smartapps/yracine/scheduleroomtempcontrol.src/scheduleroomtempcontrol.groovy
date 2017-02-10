/**
 *  ScheduleRoomTempControl
 *
 *  Copyright 2015 Yves Racine
 *  LinkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
 *
 *  Developer retains all right, title, copyright, and interest, including all copyright, patent rights, trade secret 
 *  in the Background technology. May be subject to consulting fees under the Agreement between the Developer and the Customer. 
 *  Developer grants a non exclusive perpetual license to use the Background technology in the Software developed for and delivered 
 *  to Customer under this Agreement. However, the Customer shall make no commercial use of the Background technology without
 *  Developer's written consent.
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *
 *  Software Distribution is restricted and shall be done only with Developer's written approval.
 */
 
definition(
	name: "${get_APP_NAME()}",
	namespace: "yracine",
	author: "Yves Racine",
	description: "Enable better temp control in rooms based on Smart Vents",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)



preferences {

	page(name: "dashboardPage")
	page(name: "generalSetupPage")
	page(name: "roomsSetupPage")
	page(name: "zonesSetupPage")
	page(name: "schedulesSetupPage")
	page(name: "configDisplayPage")
	page(name: "NotificationsPage")
	page(name: "roomsSetup")
	page(name: "zonesSetup")
	page(name: "schedulesSetup")
	page(name: "ventSettingsSetup")
}

def dashboardPage() {
	def scale= getTemperatureScale()
	dynamicPage(name: "dashboardPage", title: "Dashboard", uninstall: true, nextPage: generalSetupPage,submitOnChange: true) {
		section("Tap Running Schedule(s) Config for latest info\nPress Next (upper right) for initial Setup") {
			if (roomsCount && zonesCount && schedulesCount) {
				paragraph image: "${getCustomImagePath()}office7.png", "ST hello mode: $location.mode" +
					"\nLast Running Schedule: $state.lastScheduleName" +
					"\nActiveZone(s): ${state?.activeZones}"
				if (state?.avgTempDiff)  { 
					paragraph "AvgTempDiffInZone: ${state?.avgTempDiff}$scale"                   
				}
				if (thermostat) {                	
					def currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1) 
					String mode = thermostat?.currentThermostatMode.toString()
					def operatingState=thermostat.currentThermostatOperatingState                
					def heatingSetpoint,coolingSetpoint
					switch (mode) { 
 						case 'cool':
							coolingSetpoint = thermostat.currentValue('coolingSetpoint')
						break                            
 						case 'auto': 
							coolingSetpoint = thermostat.currentValue('coolingSetpoint')
						case 'heat':
						case 'emergency heat':
						case 'auto': 
						case 'off': 
							try {                    
		 						heatingSetpoint = thermostat?.currentValue('heatingSetpoint')
							} catch (e) {
								traceEvent(settings.logFilter,"dashboardPage>not able to get heatingSetpoint from $thermostat,exception $e",
									settings.detailedNotif,get_LOG_INFO())                                
							}                        
							heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')?21:72                        
						break
						default:
							log.warn "dashboardPage>invalid mode $mode"
						break                        
					}        
					def dParagraph= "TstatMode: $mode" +
						"\nTstatOperatingState: $operatingState" +
						"\nTstatCurrentTemp: ${currentTempAtTstat}$scale"                
					if (coolingSetpoint)  { 
						dParagraph = dParagraph + "\nCoolingSetpoint: ${coolingSetpoint}$scale"
					}     
					if (heatingSetpoint)  { 
						dParagraph = dParagraph + "\nHeatingSetpoint: ${heatingSetpoint}$scale" 
					}     
					paragraph image: "${getCustomImagePath()}home1.png", dParagraph 
				}                        
				if ((state?.closedVentsCount) || (state?.openVentsCount)) {
					paragraph "    ** SMART VENTS SUMMARY **\n              For Active Zone(s)\n" 
					String dPar = "OpenVentsCount: ${state?.openVentsCount}" +                    
						"\nMaxOpenLevel: ${state?.maxOpenLevel}%" +
						"\nMinOpenLevel: ${state?.minOpenLevel}%" +
						"\nAvgVentLevel: ${state?.avgVentLevel}%" 
					if (state?.minTempInVents) {
						dPar=dPar +  "\nMinVentTemp: ${state?.minTempInVents}${scale}" +                    
						"\nMaxVentTemp: ${state?.maxTempInVents}${scale}" +
						"\nAvgVentTemp: ${state?.avgTempInVents}${scale}"
					}
					paragraph image: "${getCustomImagePath()}ventopen.png",dPar                    
					if (state?.totalVents) {
						paragraph image: "${getCustomImagePath()}ventclosed.png","ClosedVentsInZone: ${state?.closedVentsCount}" +
						 "\nClosedVentsTotal: ${state?.totalClosedVents}" +
						"\nRatioClosedVents: ${state?.ratioClosedVents}%" +
						"\nVentsTotal: ${state?.totalVents}" 
					}
				}                
				href(name: "toConfigurationDisplayPage", title: "Running Schedule(s) Config", page: "configDisplayPage") 
			}
		} /* end section dashboard */
		section("ABOUT") {
			paragraph "${get_APP_NAME()}, the smartapp that enables better temp control in rooms based on Smart Vents"
			paragraph "Version 3.1.1"
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.me/ecomatiqhomes",
					title:"Paypal donation..."
			paragraph "Copyright©2015 Yves Racine"
				href url:"http://www.maisonsecomatiq.com/#!home/mainPage", style:"embedded", required:false, title:"More information..."  
					description: "http://www.maisonsecomatiq.com/#!home/mainPage"
		} /* end section about  */
	}
}

def generalSetupPage() {
	dynamicPage(name: "generalSetupPage", submitOnChange: true, uninstall:false, nextPage: roomsSetupPage,
		refreshAfterSelection:true) {
		section("Main thermostat at home (used for vent adjustment) [optional]") {
			input (image: "${getCustomImagePath()}home1.png", name:"thermostat", type: "capability.thermostat", title: "Which main thermostat?",required:false)
		}
		section("Rooms count") {
			input (name:"roomsCount", title: "Rooms count (max=${get_MAX_ROOMS()})?", type: "number", range: "1..${get_MAX_ROOMS()}")
		}
		section("Zones count") {
			input (name:"zonesCount", title: "Zones count (max=${get_MAX_ZONES()})?", type:"number",  range: "1..${get_MAX_ZONES()}")
		}
		section("Schedules count") {
			input (name:"schedulesCount", title: "Schedules count (max=${get_MAX_SCHEDULES()})?", type: "number",  range: "1..${get_MAX_SCHEDULES()}")
		}
		section("Links to other setup pages") {        
			href(name: "toRoomPage", title: "Rooms Setup", page: "roomsSetupPage", description: "Tap to configure", image: "${getCustomImagePath()}room.png")
			href(name: "toZonePage", title: "Zones Setup", page: "zonesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}zoning.jpg")
			href(name: "toSchedulePage", title: "Schedules Setup", page: "schedulesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}office7.png")
			href(name: "toNotificationsPage", title: "Notification & Options Setup", page: "NotificationsPage",  description: "Tap to configure", image: "${getCustomImagePath()}notification.png")
		}      
        
		section("Enable Contact Sensors to be used for vent adjustments [optional, default=false]") {
			input (name:"setVentAdjustmentContactFlag", title: "Enable vent adjustment set in schedules based on contact sensors?", type:"bool",
				description:" if true and contact open=>vent(s) closed in schedules",required:false)
		}
        
		section("Disable or Modify the safeguards [default=some safeguards are implemented to avoid damaging your HVAC by closing too many vents]") {
			input (name:"fullyCloseVentsFlag", title: "Bypass all safeguards & allow closing the vents totally?", type:"bool",required:false)
			input (name:"minVentLevelInZone", title: "Safeguard's Minimum Vent Level", type:"number", required: false, description: "[default=10%]")
			input (name:"minVentLevelOutZone", title: "Safeguard's Minimum Vent Level Outside of the Zone", type:"number", required: false, description: "[default=25%]")
			input (name:"maxVentTemp", title: "Safeguard's Maximum Vent Temp", type:"number", required: false, description: "[default= 131F/55C]")
			input (name:"minVentTemp", title: "Safeguard's Minimum Vent Temp", type:"number", required: false, description: "[default= 45F/7C]")
		}       
		section("What do I use for the Master on/off switch to enable/disable smartapp processing? [optional]") {
			input (name:"powerSwitch", type:"capability.switch", required: false,description: "Optional")
		}
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
	}
}

def roomsSetupPage() {

	dynamicPage(name: "roomsSetupPage", title: "Rooms Setup", uninstall: false, nextPage: zonesSetupPage) {
		section("Press each room slot below to complete setup") {
			for (int i = 1; ((i <= settings.roomsCount) && (i <= get_MAX_ROOMS())); i++) {
				href(name: "toRoomPage$i", page: "roomsSetup", params: [indiceRoom: i], required:false, description: roomHrefDescription(i), 
					title: roomHrefTitle(i), state: roomPageState(i),image: "${getCustomImagePath()}room.png" )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

def roomPageState(i) {

	if (settings."roomName${i}" != null) {
		return 'complete'
	} else {
		return 'incomplete'
	}
  
}

def roomHrefTitle(i) {
	def title = "Room ${i}"
	return title
}

def roomHrefDescription(i) {
	def description ="Room no ${i} "

	if (settings."roomName${i}" !=null) {
		description += settings."roomName${i}"		    	
	}
	return description
}

def roomsSetup(params) {
	def indiceRoom=0    

	// Assign params to indiceZone.  Sometimes parameters are double nested.
	if (params?.indiceRoom || params?.params?.indiceRoom) {

		if (params.indiceRoom) {
			indiceRoom = params.indiceRoom
		} else {
			indiceRoom = params.params.indiceRoom
		}
	}    
 
	indiceRoom=indiceRoom.intValue()

	dynamicPage(name: "roomsSetup", title: "Rooms Setup", uninstall: false, nextPage: zonesSetupPage) {

		section("Room ${indiceRoom} Setup") {
			input "roomName${indiceRoom}", title: "Room Name", "string",image: "${getCustomImagePath()}room.png"
		}
		section("Room ${indiceRoom}-TempSensor [optional]") {
			input image: "${getCustomImagePath()}IndoorTempSensor.png", "tempSensor${indiceRoom}", title: "Temp sensor for better temp adjustment", "capability.temperatureMeasurement", 
				required: false, description: "Optional"

		}
		section("Room ${indiceRoom}-ContactSensor [optional]") {
			input image: "${getCustomImagePath()}contactSensor.png", "contactSensor${indiceRoom}", title: "Contact sensor for better vent adjustment", "capability.contactSensor", 
				required: false, description: "Optional,if open=>vent is closed"

		}
        
		section("Room ${indiceRoom}-Vents Setup [optional]")  {
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				input image: "${getCustomImagePath()}ventclosed.png","ventSwitch${j}${indiceRoom}", title: "Vent switch no ${j} in room", "capability.switch", 
					required: false, description: "Optional"
				input "ventLevel${j}${indiceRoom}", title: "set vent no ${j}'s level in room [optional, range 0-100]", "number", range: "0..100",
						required: false, description: "blank:calculated by smartapp"
			}           
		}           
		section("Room ${indiceRoom}-Motion Detection parameters [optional]") {
			input image: "${getCustomImagePath()}MotionSensor.png","motionSensor${indiceRoom}", title: "Motion sensor (if any) to detect if room is occupied", "capability.motionSensor", 
				required: false, description: "Optional"
			input "needOccupiedFlag${indiceRoom}", title: "Will do vent adjustement only when Occupied [default=false]", "bool",  
				required: false, description: "Optional"
			input "residentsQuietThreshold${indiceRoom}", title: "Threshold in minutes for motion detection [default=15 min]", "number", 
				required: false, description: "Optional"
			input "occupiedMotionOccNeeded${indiceRoom}", title: "Motion counter for positive detection [default=1 occurence]", "number", 
				required: false, description: "Optional"
		}
		section {
			href(name: "toRoomsSetupPage", title: "Back to Rooms Setup Page", page: "roomsSetupPage")
		}
	}
}


def configDisplayPage() {
	def key 
	def fullyCloseVents = (settings.fullyCloseVentsFlag) ?: false 	
    
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone) 
	float desiredTemp 
	def scale=getTemperatureScale()     
	def currTime = now()	 
	boolean foundSchedule=false 
	String bypassSafeguardsString= (fullyCloseVents)?'true':'false'                             
	String setpointFlagString= (noSetPoints=='false')?'true':'false'                             
	float currentTempAtTstat =(scale=='C')?21:72 	// set a default value 
	String mode, operatingState
	int nbClosedVents=0, nbOpenVents=0, totalVents=0, nbRooms=0 
	int min_open_level=100, max_open_level=0, total_level_vents=0     
	float min_temp_in_vents=200, max_temp_in_vents=0, total_temp_diff=0, target_temp     
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def MIN_OPEN_LEVEL_OUT_ZONE=(minVentLevelOutZone!=null)?((minVentLevelOutZone>=0 && minVentLevelOutZone <100)?minVentLevelOutZone:25):25
	def MAX_TEMP_VENT_SWITCH = (settings.maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (settings.minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch
	def heatingSetpoint,coolingSetpoint
	def desiredHeatDelta, desiredCoolDelta,desiredCool,desiredHeat
	if (thermostat) { 
		currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1) 
		mode = thermostat.currentThermostatMode.toString()
		operatingState=thermostat.currentThermostatOperatingState
	}         

	traceEvent(settings.logFilter,"configDisplayPage>About to display Running Schedule(s) Configuration",settings.detailedNotif)
	dynamicPage(name: "configDisplayPage", title: "Running Schedule(s) Config", nextPage: generalSetupPage,submitOnChange: true) {
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
        
		section("General") {
			paragraph image: "${getCustomImagePath()}notification.png", "Notifications" +
					"\n  >Detailed Notification: $detailedNotifString" +
					"\n  >AskAlexa Notifications: $askAlexaString"             
			paragraph image: "${getCustomImagePath()}home1.png", "ST hello mode: $location.mode" 
			if (thermostat) {                	
				switch (mode) { 
					case 'cool':
 						coolingSetpoint = thermostat.currentValue('coolingSetpoint')
						target_temp= coolingSetpoint.toFloat()                        
					break                        
					case 'auto': 
 						coolingSetpoint = thermostat.currentValue('coolingSetpoint')
					case 'heat':
					case 'emergency heat':
					case 'auto': 
					case 'off': 
						try {                    
		 					heatingSetpoint = thermostat?.currentValue('heatingSetpoint')
						} catch (e) {
							traceEvent(settings.logFilter,"ConfigDisplayPage>not able to get heatingSetpoint from $thermostat, exception $e",
								settings.detailedNotif, get_LOG_INFO())                            
						}                        
						heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')?21:72   
						if (mode=='auto') {                        
							float median= ((coolingSetpoint + heatingSetpoint)/2).toFloat().round(1)
							if (currentTempAtTstat > median) {
								target_temp =coolingSetpoint.toFloat()                   
							} else {
								target_temp =heatingSetpoint.toFloat()                   
							}
						} else {
							target_temp=  heatingSetpoint  
						}                        
                            
					break                        
					default:
						log.warn "ConfigDisplayPage>invalid mode $mode"
					break                        
				}                        
				def detailedNotifString=(settings.detailedNotif)?'true':'false'			            
				def askAlexaString=(settings.askAlexaFlag)?'true':'false'			            
				def setVentAdjustmentContactString=(settings.setVentAdjustmentContactFlag)?'true':'false'
				paragraph "  >TstatMode: $mode" +
						"\n  >TstatOperatingState: $operatingState" +
						"\n  >TstatCurrentTemp: ${currentTempAtTstat}$scale"                
				if (coolingSetpoint)  { 
					paragraph "  >TstatCoolingSetpoint: ${coolingSetpoint}$scale"
				}                        
				if (heatingSetpoint)  { 
					paragraph "  >TstatHeatingSetpoint: ${heatingSetpoint}$scale"
				}    
			}                
			if (state?.avgTempDiff)  {   
				paragraph " >AvgTempDiffInZone: ${state?.avgTempDiff.toFloat().round(1)}$scale"                     
			}  
			paragraph image: "${getCustomImagePath()}safeguards.jpg","Safeguards"
 			paragraph "  >BypassSafeguards: ${bypassSafeguardsString}" +
					"\n  >MinVentLevelInZone: ${MIN_OPEN_LEVEL_IN_ZONE}%" +
					"\n  >MinVentLevelOutZone: ${MIN_OPEN_LEVEL_OUT_ZONE}%" +
					"\n  >MinVentTemp: ${MIN_TEMP_VENT_SWITCH}${scale}" +
					"\n  >MaxVentTemp: ${MAX_TEMP_VENT_SWITCH}${scale}" 
  		}
		for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
        
			key = "selectedMode$i"
			def selectedModes = settings[key]
			key = "scheduleName$i"
			def scheduleName = settings[key]
			traceEvent(settings.logFilter,"configDisplayPage>looping thru schedules, now at $scheduleName",settings.detailedNotif)
			boolean foundMode=selectedModes.find{it == (location.currentMode as String)} 
			if ((selectedModes != null) && (!foundMode)) {
        
				traceEvent(settings.logFilter,"configDisplayPage>schedule=${scheduleName} does not apply,location.mode= $location.mode, selectedModes=${selectedModes},foundMode=${foundMode}, continue",
					settings.detailedNotif)                
				continue			
			}
			key = "begintime$i"
			def startTime = settings[key]
			if (startTime == null) {
        			continue
			}
			def startTimeToday = timeToday(startTime,location.timeZone)
			key = "endtime$i"
			def endTime = settings[key]
			def endTimeToday = timeToday(endTime,location.timeZone)
			if ((currTime < endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
				startTimeToday = startTimeToday -1        
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, subtracted - 1 day, new startTime=${startTimeToday.time}",
					settings.detailedNotif)                
			}            
			if ((currTime > endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
				endTimeToday = endTimeToday +1        
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)
			}   
            
			String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
			String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
            
			if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange(i))) {
				foundSchedule=true
                
				traceEvent(settings.logFilter,"configDisplayPage>$scheduleName is good to go..",settings.detailedNotif)
				key = "givenClimate${i}"
				def climate = settings[key]
                
				key = "includedZones$i"
				def zones = settings[key]
				key = "desiredCool${i}"
				def desiredCoolTemp = (settings[key])?: ((scale=='C') ? 23:75)
				key = "desiredHeat${i}"
				def desiredHeatTemp = (settings[key])?: ((scale=='C') ? 21:72)
				key = "adjustVentsEveryCycleFlag${i}"
				String adjustVentsEveryCycleString = (settings[key])?'true':'false'
				key = "openVentsFanOnlyFlag${i}"                
				def openVentsWhenFanOnlyString = (settings[key])?'true':'false'                
				key = "setVentLevel${i}"
				def setLevel = settings[key]
				key = "resetLevelOverrideFlag${i}"
				def resetLevelOverrideString=(settings[key])?'true':'false'
  
				traceEvent(settings.logFilter,"configDisplayPage>about to display schedule $scheduleName..",settings.detailedNotif)
				                
				section("Running Schedule(s)") {
					paragraph image: "${getCustomImagePath()}office7.png","Schedule $scheduleName" 
						"\n >StartTime: $startInLocalTime" +                    
						"\n >EndTime: $endInLocalTime"                   
					if (setLevel) {
						paragraph " >DefaultSetLevelForAllVentsInZone(s): ${setLevel}%"
					}                        
					paragraph " >BypassSetLevelOverrideinZone(s): ${resetLevelOverrideString}" +
						"\n >AdjustVentsEveryCycle: $adjustVentsEveryCycleString" + 
						"\n >OpenVentsWhenFanOnly: $openVentsWhenFanOnlyString"                        
					key = "noSetpointsFlag$i"
					def noSetpointInSchedule = settings[key]?: false
					def setpointsAtThermostat = (noSetpointInSchedule==true)?'false':'true'                    
					paragraph " >SetpointsAtThermostat: $setpointsAtThermostat"  
					if (!noSetpointInSchedule) {
						if (climate) {
							paragraph " >EcobeeProgramSet: $climate" 
						} else {
							if (desiredCoolTemp) {                            
								paragraph " >DesiredCool: ${desiredCoolTemp}$scale" 
							}                                
							if (desiredHeatTemp) {                            
								paragraph " >DesiredHeat: ${desiredHeatTemp}$scale"
							}
						}                                
					}                            
                    
					if (selectedModes) {                    
						paragraph " >STHelloModes: $selectedModes"
					}                        
					paragraph " >Includes: $zones" 
				}
				state?.activeZones = zones // save the zones for the dashboard                
				for (zone in zones) { 
						def zoneDetails=zone.split(':') 
	 					def indiceZone = zoneDetails[0] 
						def zoneName = zoneDetails[1] 
						key = "desiredCoolTemp${indiceZone}"
						desiredCool = settings[key]
						key = "desiredHeatTemp${indiceZone}"
						desiredHeat = settings[key]
                        
						key = "includedRooms$indiceZone" 
						def rooms = settings[key] 
						if (mode=='cool') { 
						
							key = "desiredCoolDeltaTemp$indiceZone" 
							desiredCoolDelta= settings[key] 
							if (desiredCool) { 
								desiredTemp= desiredCool.toFloat() 
							} else { 
 								desiredTemp = ((coolingSetpoint)?:(scale=='C')?23:75) + (desiredCoolDelta?:0)
	 						}                 
                            
						} else { 
 
							key = "desiredHeatDeltaTemp$indiceZone" 
							desiredHeatDelta= settings[key] 
 
							if (desiredHeat) { 
								desiredTemp= desiredHeat.toFloat()
							} else {
                            
 								desiredTemp = ((heatingSetpoint)?:(scale=='C')?21:72) + (desiredHeatDelta?:0)
							}                 
						} 
						section("Active Zone(s) in Schedule $scheduleName") { 
							paragraph image: "${getCustomImagePath()}zoning.jpg", "Zone $zoneName" 
							if (desiredTemp) {                         
								paragraph " >TempThresholdForVents: ${desiredTemp}$scale"  
							}   
							if (desiredCoolDelta) {                         
								paragraph " >DesiredCoolDeltaSP: ${desiredCoolDelta}$scale"  
							}   
							if (desiredHeatDelta) {                         
								paragraph " >DesiredHeatDeltaSP: ${desiredHeatDelta}$scale"  
							}   
                            
							paragraph " >Includes: $rooms" 
						} 
						for (room in rooms) { 
							def roomDetails=room.split(':') 
							def indiceRoom = roomDetails[0] 
							def roomName = roomDetails[1] 
							key = "needOccupiedFlag$indiceRoom" 
							def needOccupied = (settings[key]) ?: false 
							traceEvent(settings.logFilter,"configDisplayPage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
								settings.detailedNotif)                            
							key = "motionSensor${indiceRoom}" 
							def motionSensor = (settings[key])  
							key = "contactSensor${indiceRoom}"
							def contactSensor = settings[key]
							key = "tempSensor${indiceRoom}" 
							def tempSensor = (settings[key])  
							def tempAtSensor =getSensorTempForAverage(indiceRoom)			 
							if (tempAtSensor == null) { 
								tempAtSensor= currentTempAtTstat				             
							} 
							section("Room(s) in Zone $zoneName") { 
	 							nbRooms++                         
								paragraph image: "${getCustomImagePath()}room.png","$roomName" 
								if (tempSensor) {                            
									paragraph image: "${getCustomImagePath()}IndoorTempSensor.png", "TempSensor: $tempSensor" 
								}                                
								if (tempAtSensor) { 
 									if (desiredTemp) {                             
	 									float temp_diff = (tempAtSensor.toFloat() - desiredTemp).round(1)  
										paragraph " >CurrentTempOffSet: ${temp_diff.round(1)} $scale"  
										total_temp_diff = total_temp_diff + temp_diff                                     
									}                                     
									paragraph " >CurrentTempInRoom: ${tempAtSensor}$scale" 
	 							}                                 
								if (contactSensor) {      
									def contactState = contactSensor.currentState("contact")
									paragraph image: "${getCustomImagePath()}contactSensor.png", " ContactSensor: $contactSensor" + 
										"\n >ContactState: ${contactState.value}"                                
								}                            
		 
								if (motionSensor) {      
									def countActiveMotion=isRoomOccupied(motionSensor, indiceRoom)
									String needOccupiedString= (needOccupied)?'true':'false'
									if (!needOccupied) {                                
										paragraph " >MotionSensor: $motionSensor" +
											"\n ->NeedToBeOccupied: ${needOccupiedString}" 
									} else {                                        
										key = "residentsQuietThreshold${indiceRoom}"
										def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 
										String thresholdString = threshold   
										key = "occupiedMotionOccNeeded${indiceRoom}"
										def occupiedMotionOccNeeded= (settings[key]) ?:1
										key = "occupiedMotionTimestamp${indiceRoom}"
										def lastMotionTimestamp = (state[key])
										String lastMotionInLocalTime                                     
										def isRoomOccupiedString=(countActiveMotion>=occupiedMotionOccNeeded)?'true':'false'                                
										if (lastMotionTimestamp) {                                    
											lastMotionInLocalTime= new Date(lastMotionTimestamp).format("yyyy-MM-dd HH:mm", location.timeZone)
										}						                                    
                                    
										paragraph image: "${getCustomImagePath()}MotionSensor.png", "MotionSensor: $motionSensor" 
										paragraph "  >IsRoomOccupiedNow: ${isRoomOccupiedString}" + 
											"\n  >NeedToBeOccupied: ${needOccupiedString}" + 
											"\n  >OccupiedThreshold: ${thresholdString} minutes"+ 
											"\n  >MotionCountNeeded: ${occupiedMotionOccNeeded}" + 
											"\n  >OccupiedMotionCounter: ${countActiveMotion}" +
											"\n  >LastMotionTime: ${lastMotionInLocalTime}"
									}
								}                                
								paragraph "** VENTS in $roomName **" 
								float total_temp_in_vents=0                            
								for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
								key = "ventSwitch${j}$indiceRoom"
								def ventSwitch = settings[key]
								if (ventSwitch != null) {
									float temp_in_vent=getTemperatureInVent(ventSwitch)                               
									// compile some stats for the dashboard                    
									if (temp_in_vent) {                                   
										min_temp_in_vents=(temp_in_vent < min_temp_in_vents)? temp_in_vent.toFloat().round(1) : min_temp_in_vents
										max_temp_in_vents=(temp_in_vent > max_temp_in_vents)? temp_in_vent.toFloat().round(1) : max_temp_in_vents
										total_temp_in_vents=total_temp_in_vents + temp_in_vent
									}                                        
									def switchLevel = getCurrentVentLevel(ventSwitch)							                        
									totalVents++                                    
									paragraph image: "${getCustomImagePath()}ventopen.png","$ventSwitch"
									paragraph " >CurrentVentLevel: ${switchLevel}%"                                    
									if (switchLevel) {                                    
										// compile some stats for the dashboard                    
										min_open_level=(switchLevel.toInteger() < min_open_level)? switchLevel.toInteger() : min_open_level
										max_open_level=(switchLevel.toInteger() > max_open_level)? switchLevel.toInteger() : max_open_level
										total_level_vents=total_level_vents + switchLevel.toInteger()                                    
										if (switchLevel > MIN_OPEN_LEVEL_IN_ZONE) {
											nbOpenVents++                                    
										} else {
											nbClosedVents++                                    
										}                                        
									}                                        
                            
									input "ventLevel${j}${indiceRoom}", title: "  >override vent level [Optional,0-100]", "number", range: "0..100",
										required: false, description: "  blank:calculated by smartapp"
								}                            
							}  
						} /* end section rooms */
					} /* end for rooms */
				} /* end for zones */
			} /* end if current schedule */ 
		} /* end for schedules */
		state?.closedVentsCount= nbClosedVents                                  
		state?.openVentsCount= nbOpenVents         
		state?.minOpenLevel= min_open_level
		state?.maxOpenLevel= max_open_level
		state?.minTempInVents=min_temp_in_vents
		state?.maxTempInVents=max_temp_in_vents
		traceEvent(settings.logFilter,"configDisplayPage>foundSchedule=$foundSchedule",settings.detailedNotif)
		if (total_temp_in_vents) {
			state?.avgTempInVents= (total_temp_in_vents/totalVents).toFloat().round(1)
		}		        
		if (total_level_vents) {    
			state?.avgVentLevel= (total_level_vents/totalVents).toFloat().round(1)
		}		        
		nbClosedVents=0        
		nbOpenVents=0    
		totalVents=0        
		// Loop thru all smart vents to get the total count of vents (open,closed)
		for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					totalVents++                
					def switchLevel = getCurrentVentLevel(ventSwitch)							                        
					if ((switchLevel!=null) && (switchLevel > MIN_OPEN_LEVEL_IN_ZONE)) {
						nbOpenVents++                                    
					} else {
						nbClosedVents++                                    
					}                                        
				} /* end if ventSwitch != null */
			} /* end for switches null */
		} /* end for vent rooms */

		// More stats for dashboard
		if (total_temp_diff ) {
			state?.avgTempDiff = (total_temp_diff/nbRooms).round(1)			        
		}            
		state?.totalVents=totalVents
		state?.totalClosedVents=nbClosedVents
		if (nbClosedVents) {
			float ratioClosedVents=((nbClosedVents/state?.totalVents).toFloat()*100)
			state?.ratioClosedVents=ratioClosedVents.round(1)
		} else {
			state?.ratioClosedVents=0
		}
		if (!foundSchedule) {         
			section {
				paragraph "\n\nNo Schedule running at this time $nowInLocalTime" 
			}	                
		}
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
	} /* end dynamic page */                
}


def zoneHrefDescription(i) {
	def description ="Zone no ${i} "

	if (settings."zoneName${i}" !=null) {
		description += settings."zoneName${i}"		    	
	}
	return description
}

def zonePageState(i) {

	if (settings."zoneName${i}" != null) {
		return 'complete'
	} else {
		return 'incomplete'
	}
  
}

def zoneHrefTitle(i) {
	def title = "Zone ${i}"
	return title
}

def zonesSetupPage() {

	dynamicPage(name: "zonesSetupPage", title: "Zones Setup", uninstall: false, nextPage: schedulesSetupPage) {
		section("Press each zone slot below to complete setup") {
			for (int i = 1; ((i <= settings.zonesCount) && (i<= get_MAX_ZONES())); i++) {
				href(name: "toZonePage$i", page: "zonesSetup", params: [indiceZone: i], required:false, description: zoneHrefDescription(i), 
					title: zoneHrefTitle(i), state: zonePageState(i),  image: "${getCustomImagePath()}zoning.jpg" )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

def zonesSetup(params) {

	def rooms = []
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key = "roomName$indiceRoom"
		def room = "${indiceRoom}:${settings[key]}"
		rooms = rooms + room
	}
	def indiceZone=0    

	// Assign params to indiceZone.  Sometimes parameters are double nested.
	if (params?.indiceZone || params?.params?.indiceZone) {

		if (params.indiceZone) {
			indiceZone = params.indiceZone
		} else {
			indiceZone = params.params.indiceZone
		}
	}    
	indiceZone=indiceZone.intValue()
	dynamicPage(name: "zonesSetup", uninstall: false, title: "Zones Setup") {
		section("Zone ${indiceZone} Setup") {
			input (name:"zoneName${indiceZone}", title: "Zone Name", type: "text",
				defaultValue:settings."zoneName${indiceZone}")
		}
		section("Zone ${indiceZone}-Included rooms") {
			input (name:"includedRooms${indiceZone}", title: "Rooms included in the zone", type: "enum",
				options: rooms,
				multiple: true,
				defaultValue:settings."includedRooms${indiceZone}")
		}
		section("Zone ${indiceZone}-Static Cool Temp threshold in the zone (below it, when cooling, the vents are -partially- closed)") {
			input (name:"desiredCoolTemp${indiceZone}", type:"decimal", title: "Cool Temp Threshold [if blank, then dynamic threshold is used with thermostat only]", 
				required: false,defaultValue:settings."desiredCoolTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Static Heat Temp threshold in the zone (above it, when heating, the vents are -partially- closed)") {
			input (name:"desiredHeatTemp${indiceZone}", type:"decimal", title: "Heat Temp Threshold [if blank, then dynamic threshold is used with thermostat only]", 
				required: false, defaultValue:settings."desiredHeatTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Dynamic Cool Temp threshold based on the coolSP at thermostat (above it, when cooling, the vents are -partially- closed)") {
			input (name:"desiredCoolDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Cool Temp Threshold [default = +/-0F or +/-0C]", 
				required: false, defaultValue:settings."desiredCoolDeltaTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Dynamic Heat Temp threshold based on the heatSP at thermostat (above it, when heating, the vents are -partially- closed)") {
			input (name:"desiredHeatDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Heat Temp Threshold [default = +/-0F or +/-0C]", 
				required: false, defaultValue:settings."desiredHeatDeltaTemp${indiceZone}")			                
		}
		section {
			href(name: "toZonesSetupPage", title: "Back to Zones Setup Page", page: "zonesSetupPage")
		}
	}            
}

def scheduleHrefDescription(i) {
	def description ="Schedule no ${i} " 
	if (settings."scheduleName${i}" !=null) {
		description += settings."scheduleName${i}"		    
	}
	return description
}

def schedulePageState(i) {

	if (settings."scheduleName${i}"  != null) {
		return 'complete'
	} else {
		return 'incomplete'
	}	
    
}

def scheduleHrefTitle(i) {
	def title = "Schedule ${i}"
	return title
}

def schedulesSetupPage() {
	dynamicPage(name: "schedulesSetupPage", title: "Schedules Setup", uninstall: false, nextPage: NotificationsPage) {
		section("Press each schedule slot below to complete setup") {
			for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
				href(name: "toSchedulePage$i", page: "schedulesSetup", params: [indiceSchedule: i],required:false, description: scheduleHrefDescription(i), 
					title: scheduleHrefTitle(i), state: schedulePageState(i),image: "${getCustomImagePath()}office7.png" )
			}
		}            
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}        

def schedulesSetup(params) {
    
	def ecobeePrograms=[]
	// try to get the thermostat programs list (ecobee)
	try {
		ecobeePrograms = thermostat?.currentClimateList.toString().minus('[').minus(']').tokenize(',')
		ecobeePrograms.sort()        
	} catch (any) {
		traceEvent(settings.logFilter,"Not able to get the list of climates (ecobee)",settings.detailedNotif)
	}    
    
    
	traceEvent(settings.logFilter,"programs: $ecobeePrograms",settings.detailedNotif)

	def zones = []
    
	for (int i = 1; ((i <= settings.zonesCount) && (i<= get_MAX_ZONES())); i++) {
		def key = "zoneName$i"
		def zoneName =  "${i}:${settings[key]}"   
		zones = zones + zoneName
	}

	
	def enumModes=location.modes.collect{ it.name }
	def indiceSchedule=1
	// Assign params to indiceSchedule.  Sometimes parameters are double nested.
	if (params?.indiceSchedule || params?.params?.indiceSchedule) {

		if (params.indiceSchedule) {
			indiceSchedule = params.indiceSchedule
		} else {
			indiceSchedule = params.params.indiceSchedule
		}
	}    
	indiceSchedule=indiceSchedule.intValue()
	dynamicPage(name: "schedulesSetup", title: "Schedule Setup",uninstall: false) {
		section("Schedule ${indiceSchedule} Setup") {
			input (name:"scheduleName${indiceSchedule}", title: "Schedule Name", type: "text",
				defaultValue:settings."scheduleName${indiceSchedule}", image: "${getCustomImagePath()}office7.png" )
		}
		section("Schedule ${indiceSchedule}-Included zones") {
			input (name:"includedZones${indiceSchedule}", title: "Zones included in this schedule", type: "enum",
				defaultValue:settings."includedZones${indiceSchedule}",
				options: zones,
 				multiple: true)
		}
		section("Schedule ${indiceSchedule}- Day & Time of the desired Heating/Cooling settings for the selected zone(s)") {
			input (name:"dayOfWeek${indiceSchedule}", type: "enum",
				title: "Which day of the week to trigger the zoned heating/cooling settings?",
				defaultValue:settings."dayOfWeek${indiceSchedule}",                 
				multiple: false,
				metadata: [
					values: [
						'All Week',
						'Monday to Friday',
						'Saturday & Sunday',
						'Monday',
						'Tuesday',
						'Wednesday',
						'Thursday',
						'Friday',
						'Saturday',
						'Sunday'
					]
				])
			input (name:"begintime${indiceSchedule}", type: "time", title: "Beginning time to trigger the zoned heating/cooling settings",
				defaultValue:settings."begintime${indiceSchedule}")
			input (name:"endtime${indiceSchedule}", type: "time", title: "End time",
				defaultValue:settings."endtime${indiceSchedule}")
		}
		section("Schedule ${indiceSchedule}-Select the program/climate at ecobee thermostat to be applied [optional,for ecobee only]") {
			input (name:"givenClimate${indiceSchedule}", type:"enum", title: "Which ecobee program? ", options: ecobeePrograms, 
				required: false, defaultValue:settings."givenClimate${indiceSchedule}", description: "Optional")
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Cooling setpoint during the schedule [optional, when no ecobee climate is specified]") {
			input (name:"desiredCool${indiceSchedule}", type:"decimal", title: "Cooling Setpoint, default = 75F/23C", 
				required: false,defaultValue:settings."desiredCool${indiceSchedule}", description: "Optional")			                
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Heating setpoint during the schedule [optional, when no ecobee climate is specified]") {
			input (name:"desiredHeat${indiceSchedule}", type:"decimal", title: "Heating Setpoint, default=72F/21C", 
				required: false, defaultValue:settings."desiredHeat${indiceSchedule}", description: "Optional")			                
		}
		section("Schedule ${indiceSchedule}-Vent Settings for the Schedule") {
			href(name: "toVentSettingsSetup", page: "ventSettingsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: ventSettingsHrefTitle(indiceSchedule), image: "${getCustomImagePath()}ventopen.png" ) 
		}
		section("Schedule ${indiceSchedule}-Set for specific mode(s) [default=all]")  {
			input (name:"selectedMode${indiceSchedule}", type:"enum", title: "Choose Mode", options: enumModes, 
				required: false, multiple:true,defaultValue:settings."selectedMode${indiceSchedule}", description: "Optional")
		}
		section("Do not set the thermostat setpoints in this schedule [optional, default=The thermostat setpoints are set]") {
			input (name:"noSetpointsFlag${indiceSchedule}", title: "Do not set the thermostat setpoints?", type:"bool", 
				required:false, defaultValue:settings."noSetpointsFlag${indiceSchedule}")
		}
		section {
			href(name: "toSchedulesSetupPage", title: "Back to Schedules Setup Page", page: "schedulesSetupPage")
		}
	}        
}

def ventSettingsSetup(params) {
	def indiceSchedule=1
	// Assign params to indiceSchedule.  Sometimes parameters are double nested.
	if (params?.indiceSchedule || params?.params?.indiceSchedule) {

		if (params.indiceSchedule) {
			indiceSchedule = params.indiceSchedule
		} else {
			indiceSchedule = params.params.indiceSchedule
		}
	}    
	indiceSchedule=indiceSchedule.intValue()
    
	dynamicPage(name: "ventSettingsSetup", title: "Vent Settings for schedule " + settings."scheduleName${indiceSchedule}", uninstall: false, 
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Vent Settings for the Schedule [optional]") {
			input (name: "setVentLevel${indiceSchedule}", type:"number",  title: "Set all Vents in Zone(s) to a specific Level during the Schedule [range 0-100]", 
				required: false, defaultValue:settings."setVentLevel${indiceSchedule}", range: "0..100", description: "blank: calculated by smartapp")
			input (name: "resetLevelOverrideFlag${indiceSchedule}", type:"bool",  title: "Bypass all vents overrides in zone(s) during the Schedule (default=false)?", 
				required: false, defaultValue:settings."resetLevelOverrideFlag${indiceSchedule}", description: "Optional")
			input (name: "adjustVentsEveryCycleFlag${indiceSchedule}", type:"bool",  title: "Adjust vent settings every 5 minutes (default=only when heating/cooling/fan running)?", 
				required: false, defaultValue:settings."adjustVentsEveryCycleFlag${indiceSchedule}", description: "Optional")
			input (name: "openVentsFanOnlyFlag${indiceSchedule}", type:"bool", title: "Open all vents when HVAC's OperatingState is Fan only",
				required: false, defaultValue:settings."openVentsFanOnlyFlag${indiceSchedule}", description: "Optional")
		}
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule no ${indiceSchedule} Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   

def ventSettingsHrefTitle(i) {
	def title = "Vent Settings for Schedule ${i}"
	return title
}


def NotificationsPage() {
	dynamicPage(name: "NotificationsPage", title: "Other Options", install: true) {
		section("Notifications & Logging") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:
				false
			input("recipients", "contact", title: "Send notifications to", required: false)
			input "phoneNumber", "phone", title: "Send a text message?", required: false
			input "detailedNotif", "bool", title: "Detailed Logging & Notifications?", required:false
			input "logFilter", "enum", title: "log filtering [Level 1=ERROR only,2=<Level 1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>]?",required:false, metadata: [values: [1,2,3,4,5]]
				          
		}
		section("Enable Amazon Echo/Ask Alexa Notifications [optional, default=false]") {
			input (name:"askAlexaFlag", title: "Ask Alexa verbal Notifications?", type:"bool",
				description:"optional",required:false)
		}
		section([mobileOnly: true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
		section {
			href(name: "toGeneralSetupPage", title: "Back to General Setup Page", page: "generalSetupPage")
		}
	}
}



def installed() {
	state?.closedVentsCount= 0
	state?.openVentsCount=0
	state?.totalVents=0
	state?.ratioClosedVents=0
	state?.avgTempDiff=0.0
	state?.activeZones=[]
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def offHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value",settings.detailedNotif)
}

def onHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value",settings.detailedNotif)
	setZoneSettings()    
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
}



def ventTemperatureHandler(evt) {
	traceEvent(settings.logFilter,"vent temperature: $evt.value",settings.detailedNotif)
	float ventTemp = evt.value.toFloat()
	def scale = getTemperatureScale()
	def MAX_TEMP_VENT_SWITCH = (maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch
	def currentHVACMode = thermostat?.currentThermostatMode.toString()
	currentHVACMode=(currentHVACMode)?:'auto'	// set auto by default

	if ((currentHVACMode in ['heat','auto','emergency heat']) && (ventTemp >= MAX_TEMP_VENT_SWITCH)) {
		if (fullyCloseVentsFlag) {
			// Safeguards are not implemented as requested     
			traceEvent(settings.logFilter, "ventTemperatureHandler>vent temperature is not within range ($evt.value>$MAX_TEMP_VENT_SWITCH) ,but safeguards are not implemented as requested",
				true,get_LOG_WARN(),true)        
			return    
		}    
    
		// Open all vents just to be safe
		open_all_vents()
		traceEvent(settings.logFilter,"current HVAC mode is ${currentHVACMode}, found one of the vents' value too hot (${evt.value}), opening all vents to avoid any damage", 
			true,get_LOG_ERROR(),true)        
        
	} /* if too hot */           
	if ((currentHVACMode in ['cool','auto']) && (ventTemp <= MIN_TEMP_VENT_SWITCH)) {
		if (fullyCloseVentsFlag) {
			// Safeguards are not implemented as requested     
			traceEvent(settings.logFilter, "ventTemperatureHandler>vent temperature is not within range, ($evt.value<$MIN_TEMP_VENT_SWITCH) but safeguards are not implemented as requested",
				true,get_LOG_WARN(),true)        
			return    
		}    
		// Open all vents just to be safe
		open_all_vents()
		traceEvent(settings.logFilter,"current HVAC mode is ${currentHVACMode}, found one of the vents' value too cold (${evt.value}), opening all vents to avoid any damage",
			true,get_LOG_ERROR(),true)        
	} /* if too cold */ 
}


def thermostatOperatingHandler(evt) {
	traceEvent(settings.logFilter,"Thermostat Operating now: $evt.value",settings.detailedNotif)
	state?.operatingState=evt.value    
	setZoneSettings()
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
}

def heatingSetpointHandler(evt) {
	traceEvent(settings.logFilter,"heating Setpoint now: $evt.value",settings.detailedNotif)
}
def coolingSetpointHandler(evt) {
	traceEvent(settings.logFilter,"cooling Setpoint now: $evt.value",settings.detailedNotif)
}

def changeModeHandler(evt) {
	traceEvent(settings.logFilter,"Changed mode, $evt.name: $evt.value",settings.detailedNotif)
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
	state.lastStartTime=null    
	setZoneSettings()    
}

def contactEvtHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value",settings.detailedNotif)
	setZoneSettings()
	rescheduleIfNeeded(evt)   // Call rescheduleIfNeeded to work around ST scheduling issues
}


def motionEvtHandler(evt, indice) {
	if (evt.value == "active") {
		def key= "roomName${indice}"    
		def roomName= settings[key]
		key = "occupiedMotionTimestamp${indice}"       
		state[key]= now()        
		traceEvent(settings.logFilter,"Motion at home in ${roomName},occupiedMotionTimestamp=${state[key]}",settings.detailedNotif, get_LOG_INFO(),true)
   
	}
}


def motionEvtHandler1(evt) {
	int i=1
	motionEvtHandler(evt,i)    
}

def motionEvtHandler2(evt) {
	int i=2
	motionEvtHandler(evt,i)    
}

def motionEvtHandler3(evt) {
	int i=3
	motionEvtHandler(evt,i)    
}

def motionEvtHandler4(evt) {
	int i=4
	motionEvtHandler(evt,i)    
}

def motionEvtHandler5(evt) {
	int i=5
	motionEvtHandler(evt,i)    
}

def motionEvtHandler6(evt) {
	int i=6
	motionEvtHandler(evt,i)    
}

def motionEvtHandler7(evt) {
	int i=7
	motionEvtHandler(evt,i)    
}

def motionEvtHandler8(evt) {
	int i=8
	motionEvtHandler(evt,i)    
}

def motionEvtHandler9(evt) {
	int i=9
	motionEvtHandler(evt,i)    
}

def motionEvtHandler10(evt) {
	int i=10
	motionEvtHandler(evt,i)    
}

def motionEvtHandler11(evt) {
	int i=11
	motionEvtHandler(evt,i)    
}

def motionEvtHandler12(evt) {
	int i=12
	motionEvtHandler(evt,i)    
}

def motionEvtHandler13(evt) {
	int i=13
	motionEvtHandler(evt,i)    
}

def motionEvtHandler14(evt) {
	int i=14
	motionEvtHandler(evt,i)    
}

def motionEvtHandler15(evt) {
	int i=15
	motionEvtHandler(evt,i)    
}

def motionEvtHandler16(evt) {
	int i=16
	motionEvtHandler(evt,i)    
}

def initialize() {

	if (powerSwitch) {
		subscribe(powerSwitch, "switch.off", offHandler, [filterEvents: false])
		subscribe(powerSwitch, "switch.on", onHandler, [filterEvents: false])
	}
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)    
	subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
	subscribe(thermostat, "thermostatOperatingState", thermostatOperatingHandler)
    
	subscribe(location, "mode", changeModeHandler)
    
	// Initialize state variables
	state.lastScheduleName=""
	state.lastStartTime=null 
	state.scheduleHeatSetpoint=0  
	state.scheduleCoolSetpoint=0    
	state.operatingState=""
    
	subscribe(app, appTouch)

	// subscribe all vents to check their temperature on a regular basis
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
			if (vent) {
				subscribe(vent, "temperature", ventTemperatureHandler)
			} /* end if vent != null */
		} /* end for vent switches */
	} /* end for rooms */

	// subscribe all motion sensors to check for active motion in rooms
    
	for (int i = 1;
		((i <= settings.roomsCount) && (i <= get_MAX_ROOMS())); i++) {
		def key = "motionSensor${i}"
		def motionSensor = settings[key]
        
		if (motionSensor) {
			// associate the motionHandler to the list of motionSensors in rooms   	 
			subscribe(motionSensor, "motion", "motionEvtHandler${i}", [filterEvents: false])
		}   
		key ="contactSensor${i}"
		def contactSensor = settings[key]
       
		if (contactSensor) {
			// associate the contactHandler to the list of contactSensors in rooms   	 
			subscribe(contactSensor, "contact.closed", "contactEvtHandler", [filterEvents: false])
			subscribe(contactSensor, "contact.open", "contactEvtHandler", [filterEvents: false])
		}            
        
	}        
   
    
	state?.poll = [ last: 0, rescheduled: now() ]

	Integer delay =5 				// wake up every 5 minutes to apply zone settings if any
	traceEvent(settings.logFilter,"initialize>scheduling setZoneSettings every ${delay} minutes to check for zone settings to be applied",settings.detailedNotif,
		get_LOG_INFO())

	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)

	rescheduleIfNeeded()
}

def rescheduleIfNeeded(evt) {
	if (evt) traceEvent(settings.logFilter,"rescheduleIfNeeded>$evt.name=$evt.value",settings.detailedNotif)
	Integer delay = 5 // By default, schedule SetZoneSettings() every 5 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (state?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		traceEvent(settings.logFilter, "rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago",settings.detailedNotif)
	}
	if (((state?.poll["last"]?:0) + (delay * 60000) < currentTime) && canSchedule()) {
		traceEvent(settings.logFilter, "setZoneSettings>scheduling rescheduleIfNeeded() in ${delay} minutes..",settings.detailedNotif, get_LOG_INFO())
		try {        
			runEvery5Minutes(setZoneSettings)
		} catch (e) {
 			traceEvent(settings.logFilter,"rescheduleIfNeeded>exception $e while rescheduling",settings.detailedNotif, get_LOG_ERROR(),true)        
		}
		setZoneSettings()    
	}
    
    
	// Update rescheduled state
    
	if (!evt) state.poll["rescheduled"] = now()
}


def appTouch(evt) {
	state.lastScheduleName=""	// force reset of the zone settings
	state.lastStartTime=null    
	setZoneSettings()
	rescheduleIfNeeded()    
}

def setZoneSettings() {

	traceEvent(settings.logFilter,"Begin of setZoneSettings Fcn",settings.detailedNotif, get_LOG_TRACE())
	def todayDay = new Date().format("dd",location.timeZone)
	if ((!state?.today) || (todayDay != state?.today)) {
		state?.exceptionCount=0   
		state?.sendExceptionCount=0        
		state?.today=todayDay        
	}   
	Integer delay = 5 // By default, schedule SetZoneSettings() every 5 min.

	//schedule the rescheduleIfNeeded() function
	state?.poll["last"] = now()
    
	if (((state?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		traceEvent(settings.logFilter, "setZoneSettings>scheduling rescheduleIfNeeded() in ${delay} minutes..",settings.detailedNotif, get_LOG_INFO())
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		state?.poll["rescheduled"] = now()
	}
	if (powerSwitch?.currentSwitch == "off") {
		traceEvent(settings.logFilter, "${powerSwitch.name} is off, schedule processing on hold...",true, get_LOG_INFO())
		return
	}

	def currTime = now()
	boolean initialScheduleSetup=false        
	boolean foundSchedule=false

	if (thermostat) {
		/* Poll or refresh the thermostat to get latest values */
		if  (thermostat?.hasCapability("Polling")) {
			try {        
				thermostat.poll()
			} catch (e) {
				traceEvent(settings.logFilter,"setZoneSettings>not able to do a poll() on ${thermostat}, exception ${e}",settings.detailedNotif,get_LOG_ERROR())
			}                    
		}  else if  (thermostat?.hasCapability("Refresh")) {
			try {        
				thermostat.refresh()
			} catch (e) {
				traceEvent(settings.logFilter,"setZoneSettings>not able to do a refresh() on ${thermostat}, exception ${e}",settings.detailedNotif,get_LOG_ERROR())
			}                
		}                    
	}                    

	def ventSwitchesOn = []
    
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
	for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
        
		def key = "selectedMode$i"
		def selectedModes = settings[key]
		key = "scheduleName$i"
		def scheduleName = settings[key]
        
		key = "noSetpointsFlag$i"
		def noSetpointInSchedule = settings[key]?: false
        
		boolean foundMode=selectedModes.find{it == (location.currentMode as String)} 
		if ((selectedModes != null) && (!foundMode)) {
			traceEvent(settings.logFilter,"setZoneSettings>schedule=${scheduleName} does not apply,location.mode= $location.mode, selectedModes=${selectedModes},foundMode=${foundMode}, continue",
				settings.detailedNotif)
			continue			
		}
		key = "begintime$i"
		def startTime = settings[key]
		if (startTime == null) {
        		continue
		}
		def startTimeToday = timeToday(startTime,location.timeZone)
		key = "endtime$i"
		def endTime = settings[key]
		def endTimeToday = timeToday(endTime,location.timeZone)
		if ((currTime < endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
			startTimeToday = startTimeToday -1        
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName}, subtracted - 1 day, new startTime=${startTimeToday.time}",
				settings.detailedNotif)
		}            
		if ((currTime > endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
			endTimeToday = endTimeToday +1        
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName}, added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)

		}        
		String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
		String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)

		traceEvent(settings.logFilter,"setZoneSettings>found schedule ${scheduleName},original startTime=$startTime,original endTime=$endTime,nowInLocalTime= ${nowInLocalTime},startInLocalTime=${startInLocalTime},endInLocalTime=${endInLocalTime}," +
       		"currTime=${currTime},begintime=${startTimeToday.time},endTime=${endTimeToday.time},lastScheduleName=$state.lastScheduleName, lastStartTime=$state.lastStartTime",
			settings.detailedNotif)
		def ventSwitchesZoneSet = []        
		if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (state.lastStartTime != startTimeToday.time) && (IsRightDayForChange(i))) {
        
			// let's set the given schedule
			initialScheduleSetup=true
			foundSchedule=true

			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},currTime= ${currTime}, current date & time OK for execution", detailedNotif)
            
			if ((thermostat) && (!noSetpointInSchedule)){
				traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},about to set the thermostat setpoint", settings.detailedNotif)
 				set_thermostat_setpoint_in_zone(i)
			}            
			// set the zoned vent switches to 'on' and adjust them according to the ambient temperature
               
			ventSwitchesZoneSet= adjust_vent_settings_in_zone(i)
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},list of Vents turned 'on'= ${ventSwitchesZoneSet}",settings.detailedNotif)

 			ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet              
			state.lastScheduleName = scheduleName
			state?.lastStartTime = startTimeToday.time
		}
		else if ((state.lastScheduleName == scheduleName) && (currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange)) {
			// We're in the middle of a schedule run
        
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},currTime= ${currTime}, current time is OK for execution, we're in the middle of a schedule run",
				settings.detailedNotif)
			foundSchedule=true
			// let's adjust the vent settings according to desired Temp only if thermostat is not idle or was not idle at the last run
			key = "adjustVentsEveryCycleFlag$i"
			def adjustVentSettings = (settings[key]) ?: false
			traceEvent(settings.logFilter,"setZoneSettings>adjustVentsEveryCycleFlag=$adjustVentSettings",settings.detailedNotif)
			
			if (thermostat) {
				// Check the operating State before adjusting the vents again.
				String operatingState = thermostat.currentThermostatOperatingState           
				if ((adjustVentSettings) || ((operatingState?.toUpperCase() !='IDLE') ||
					((state?.operatingState.toUpperCase() =='HEATING') || (state?.operatingState.toUpperCase() =='COOLING'))))
				{            
					traceEvent(settings.logFilter,"setZoneSettings>thermostat ${thermostat}'s Operating State is ${operatingState} or was just recently " +
						"${state?.operatingState}, adjusting the vents for schedule ${scheduleName}",settings.detailedNotif, get_LOG_INFO())
					ventSwitchesZoneSet=adjust_vent_settings_in_zone(i)
					ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet     
				}                    
				state?.operatingState =operatingState            
			}  else if (adjustVentSettings) {
				ventSwitchesZoneSet=adjust_vent_settings_in_zone(i)
				ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet     
            
			}   
		} else {
			traceEvent(settings.logFilter,"schedule: ${scheduleName},change not scheduled at this time ${nowInLocalTime}...",settings.detailedNotif)
		}

	} /* end for */
    
	if ((ventSwitchesOn !=[]) || (initialScheduleSetup)) {
		traceEvent(settings.logFilter,"setZoneSettings>list of Vents turned on= ${ventSwitchesOn}",settings.detailedNotif)
		turn_off_all_other_vents(ventSwitchesOn)
	}
	if (!foundSchedule) {
		traceEvent(settings.logFilter, "No schedule applicable at this time ${nowInLocalTime}",settings.detailedNotif,get_LOG_INFO())
	} 
}


private def isRoomOccupied(sensor, indiceRoom) {
	def key ="occupiedMotionOccNeeded${indiceRoom}"
	def nbMotionNeeded = (settings[key]) ?: 1
    // If mode is Night, then consider the room occupied.
    
	if (location.mode == "Night") {
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, ST hello mode ($location.mode) == Night",settings.detailedNotif)
		return nbMotionNeeded
    
	}    
	if (thermostat) {
		try {    
			String currentProgName = thermostat.currentSetClimate
			if (currentProgName?.toUpperCase().contains('SLEEP')) { 
				// Rooms are considered occupied when the ecobee program is set to 'SLEEP'    
				traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, ecobee ($currentProgName) == Sleep",settings.detailedNotif)
				return nbMotionNeeded
			} 
		} catch (any) {
			traceEvent(settings.logFilter,"isRoomOccupied>not an ecobee thermostat, continue",settings.detailedNotif)           
		}        
	}    
   
	key = "residentsQuietThreshold$indiceRoom"
	def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 

	key = "roomName$indiceRoom"
	def roomName = settings[key]


	def t0 = new Date(now() - (threshold * 60 * 1000))
	def recentStates = sensor.statesSince("motion", t0)
	def countActive =recentStates.count {it.value == "active"}
 	if (countActive>0) {
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} has been occupied, motion was detected at sensor ${sensor} in the last ${threshold} minutes",settings.detailedNotif)
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName}, is motion counter (${countActive}) for the room >= motion occurence needed (${nbMotionNeeded})?",settings.detailedNotif)
		if (countActive >= nbMotionNeeded) {
			return countActive
		}            
 	}
	return 0
}

private def verify_presence_based_on_motion_in_rooms() {

	def result=false
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		def key = "roomName$indiceRoom"
		def roomName = settings[key]
		key = "motionSensor$indiceRoom"
		def motionSensor = settings[key]
		if (motionSensor != null) {

			if (isRoomOccupied(motionSensor,indiceRoom)) {
				traceEvent(settings.logFilter,"verify_presence_based_on_motion>in ${roomName},presence detected, return true",settings.detailedNotif)
				return true
			}                
		}
	} /* end for */        
	return result
}

private def getSensorTempForAverage(indiceRoom) {
	def key = "tempSensor$indiceRoom"
	def currentTemp=null
	    
	def tempSensor = settings[key]
	if (tempSensor != null) {
		traceEvent(settings.logFilter,"getTempSensorForAverage>found sensor ${tempSensor}",settings.detailedNotif)
		if ((tempSensor.hasCapability("Refresh")) || (tempSensor.hasCapability("Polling"))) {
			// do a refresh to get the latest temp value
			try {        
				tempSensor.refresh()
			} catch (e) {
				traceEvent(settings.logFilter,"getSensorTempForAverage>not able to do a refresh() on $tempSensor",settings.detailedNotif, get_LOG_INFO())
			}                
		}        
		currentTemp = tempSensor.currentTemperature?.toFloat().round(1)
	}
	return currentTemp
}


private def set_thermostat_setpoint_in_zone(indiceSchedule) {
	def scale = getTemperatureScale()
	float desiredHeat, desiredCool

	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	key = "includedZones$indiceSchedule"
	def zones = settings[key]
	key = "givenClimate$indiceSchedule"
	def climateName = settings[key]

	float currentTemp = thermostat?.currentTemperature.toFloat().round(1)
	String mode = thermostat?.currentThermostatMode.toString()
	if (mode in ['heat', 'auto', 'emergency heat']) {
		if ((climateName) && (thermostat.hasCommand("setClimate"))) {
			try {
				thermostat.setClimate("", climateName)
				thermostat.refresh() // to get the latest setpoints
			} catch (any) {
				traceEvent(settings.logFilter,"schedule ${scheduleName}:not able to set climate ${climateName} for heating at the thermostat ${thermostat}",
					true, get_LOG_ERROR(),true)                
			}                
			desiredHeat = thermostat.currentHeatingSetpoint
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredHeat=${desiredHeat}",
					settings.detailedNotif)                
            
		} else {
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for heatingSetpoint",
					settings.detailedNotif)                
			key = "desiredHeat$indiceSchedule"
			def heatTemp = settings[key]
			if (!heatTemp) {
				traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName}:about to apply default heat settings",
					settings.detailedNotif)                
                
				desiredHeat = (scale=='C') ? 21:72 					// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredHeat=${desiredHeat}",
					settings.detailedNotif)                

			thermostat?.setHeatingSetpoint(desiredHeat)
		} 
		traceEvent(settings.logFilter,"schedule ${scheduleName},in zones=${zones},heating setPoint now =${targetTstatTemp}",
			settings.detailedNotif, get_LOG_INFO(),true)
		if (scheduleName != state.lastScheduleName) {
			state.scheduleHeatSetpoint=desiredHeat 
		}  
	}        
	if (mode in ['cool', 'auto']) {
		if ((climateName) && (thermostat.hasCommand("setClimate"))) {
			try {
				thermostat?.setClimate("", climateName)
				thermostat.refresh() // to get the latest setpoints
			} catch (any) {
				traceEvent(settings.logFilter,"schedule ${scheduleName},not able to set climate ${climateName} for cooling at the thermostat(s) ${thermostat}",
					true, get_LOG_ERROR(),true)                
			}                
			desiredCool = thermostat.currentCoolingSetpoint
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredCool=${desiredCool}",
					settings.detailedNotif)                
            
		} else {
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for coolingSetpoint",
					settings.detailedNotif)                

			key = "desiredCool$indiceSchedule"
			def coolTemp = settings[key]
			if (!coolTemp) {
				traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},about to apply default cool settings",
					settings.detailedNotif)                
                
				desiredCool = (scale=='C') ? 23:75					// by default, 23C/75F is the target cool temp
			} else {
				desiredCool = coolTemp.toFloat()
			}
            
			traceEvent(settings.logFilter,"set_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredCool=${desiredCool}",
					settings.detailedNotif)                
            
		} 
		traceEvent(settings.logFilter,"schedule ${scheduleName}, in zones=${zones},cooling setPoint now =${targetTstatTemp}",
			settings.detailedNotif, get_LOG_INFO(),true)
		if (scheduleName != state.lastScheduleName) {
			state.scheduleCoolSetpoint=desiredCool 
		}        
		thermostat?.setCoolingSetpoint(desiredCool)
	} /* else if mode == 'cool' */

}


private def adjust_vent_settings_in_zone(indiceSchedule) {
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	float desiredTemp,total_temp_in_vents=0
	def indiceRoom
	boolean closedAllVentsInZone=true
	int nbVents=0, openVentsCount=0,total_level_vents=0
	def switchLevel    
	def ventSwitchesOnSet=[]
	def scale= getTemperatureScale()
	def currentHeatingSetpoint,currentCoolingSetpoint
    
	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key= "openVentsFanOnlyFlag$indiceSchedule"
	def openVentsWhenFanOnly = (settings[key])?:false
	String operatingState = thermostat?.currentThermostatOperatingState           

	if ((thermostat) && (openVentsWhenFanOnly) && (operatingState.toUpperCase().contains("FAN ONLY"))) { 
 		// If fan only and the corresponding flag is true, then set all vents to 100% and finish the processing
		traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}:set all vents to 100% in fan only mode,exiting",
			settings.detailedNotif, get_LOG_INFO())     
		open_all_vents()
		return ventSwitchesOnSet      
	}
    

	key = "includedZones$indiceSchedule"
	def zones = settings[key]
 
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}: zones= ${zones}",
		settings.detailedNotif)     

	float currentTempAtTstat =(scale=='C')?21:72
	String mode='auto' // By default, set to auto
	if (thermostat) {
		currentTempAtTstat = thermostat.currentTemperature.toFloat().round(1)
 		mode = thermostat.currentThermostatMode.toString()
		currentHeatingSetpoint=thermostat.currentHeatingSetpoint        
		currentCoolingSetpoint=thermostat.currentCoolingSetpoint        
	}        
	key = "setVentLevel${indiceSchedule}"
	def defaultSetLevel = settings[key]
	key = "resetLevelOverrideFlag${indiceSchedule}"	
	boolean resetLevelOverrideFlag = settings[key]
	def fullyCloseVents = (fullyCloseVentsFlag) ?: false
	state?.activeZones=zones
	def min_open_level=100, max_open_level=0, nbRooms=0    
	float min_temp_in_vents=200, max_temp_in_vents=0, total_temp_diff=0, median    
	def adjustmentBasedOnContact=(settings.setVentAdjustmentContactFlag)?:false
	state?.activeZones = zones // save the zones for the dashboard                
  	
	for (zone in zones) {

		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		if (mode=='cool') {
			key = "desiredCoolTemp$indiceZone"
			def desiredCool= settings[key]
			if (!desiredCool) {            
				desiredCool = (currentCoolingSetpoint?:(scale=='C')?23:75)
			}                
			key  = "desiredCoolDeltaTemp$indiceZone"
			def desiredCoolDelta =  settings[key]           
			desiredTemp= desiredCool.toFloat() + (desiredCoolDelta?:0)                
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
		} else if (mode == 'auto') {
			key = "desiredCoolTemp$indiceZone"
			def desiredCool= settings[key]
			if (!desiredCool) {            
				desiredCool = (currentCoolingSetpoint?:(scale=='C')?23:75)
			}                
			key = "desiredHeatTemp$indiceZone"
			def desiredHeat= settings[key]
			if (!desiredHeat) {            
				desiredHeat = (currentHeatingSetpoint?:(scale=='C')?21:72)
			}                
			key  = "desiredHeatDeltaTemp$indiceZone"
			def desiredHeatDelta =  settings[key]           
			key  = "desiredCoolDeltaTemp$indiceZone"
			def desiredCoolDelta =  settings[key]           
			median = ((desiredHeat + desiredCool)/2).toFloat().round(1)
			if (currentTempAtTstat > median) {
				desiredTemp =desiredCool.toFloat().round(1) + (desiredCoolDelta?:0)           
			} else {
				desiredTemp =desiredHeat.toFloat().round(1)  + (desiredHeatDelta?:0)              
			}                        
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredCoolDelta}",			
				settings.detailedNotif)     
		} else {
			key = "desiredHeatTemp$indiceZone"
			def desiredHeat= settings[key]
			if (!desiredHeat) {            
				desiredHeat = (currentHeatingSetpoint?:(scale=='C')?21:72)
			}       
			key  = "desiredHeatDeltaTemp$indiceZone"
			def desiredHeatDelta =  settings[key]           
			desiredTemp= desiredHeat.toFloat() +  (desiredHeatDelta?:0)
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
            
		}
		for (room in rooms) {
        
			nbRooms++        
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredTemp=${desiredTemp}",			
				settings.detailedNotif)     


			switchLevel =null	// initially set to null for check later
			def roomDetails=room.split(':')
			indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]
			key = "needOccupiedFlag$indiceRoom"
			def needOccupied = (settings[key]) ?: false
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
				settings.detailedNotif)     
            

			if (needOccupied) {
				key = "motionSensor$indiceRoom"
				def motionSensor = settings[key]
				if (motionSensor != null) {
					if (!isRoomOccupied(motionSensor, indiceRoom)) {
						switchLevel = (fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE // setLevel at a minimum as the room is not occupied.
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, ${roomName} is not occupied,vents set to mininum level=${switchLevel}",
							settings.detailedNotif, get_LOG_INFO(),true)     
                        
					}
				}
			} 
            
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>AdjustmentBasedOnContact=${adjustmentBasedOnContact}",settings.detailedNotif)
            
			if (adjustmentBasedOnContact) { 
				key = "contactSensor$indiceRoom"
				def contactSensor = settings[key]
				traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>contactSensor=${contactSensor}",settings.detailedNotif)
				if (contactSensor != null) {
					def contactState = contactSensor.currentState("contact")
					if (contactState.value == "open") {
						switchLevel=((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE)					                
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, contact ${contactSensor} is open, the vent(s) in ${roomName} set to mininum level=${switchLevel}",
							settings.detailedNotif, get_LOG_INFO(), true)                        
					}                
				}            
			}            
	           
			if (switchLevel ==null) {
				def tempAtSensor =getSensorTempForAverage(indiceRoom)			
				if (tempAtSensor == null) {
					tempAtSensor= currentTempAtTstat				            
				}
                
				float temp_diff_at_sensor = (tempAtSensor - desiredTemp).toFloat().round(1)
				total_temp_diff =  total_temp_diff + temp_diff_at_sensor                
				traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>thermostat mode = ${mode}, schedule ${scheduleName}, in zone ${zoneName}, room ${roomName}, temp_diff_at_sensor=${temp_diff_at_sensor}",
					settings.detailedNotif)     
                
				if ((mode=='cool') || ((mode=='auto') && (currentTempAtTstat> median)))  {
					switchLevel=(temp_diff_at_sensor <=0)? ((fullyCloseVents) ? 0: MIN_OPEN_LEVEL_IN_ZONE): 100
				} else  {
					switchLevel=(temp_diff_at_sensor >=0)? ((fullyCloseVents) ? 0: MIN_OPEN_LEVEL_IN_ZONE): 100
				}                
			} 
                
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room ${roomName},switchLevel to be set=${switchLevel}",
						settings.detailedNotif)     
                    
					float temp_in_vent=getTemperatureInVent(ventSwitch)     
					if (temp_in_vent) {
						// compile some stats for the dashboard                    
						min_temp_in_vents=(temp_in_vent < min_temp_in_vents)? temp_in_vent.toFloat().round(1): min_temp_in_vents
						max_temp_in_vents=(temp_in_vent > max_temp_in_vents)? temp_in_vent.toFloat().round(1): max_temp_in_vents
						total_temp_in_vents=total_temp_in_vents + temp_in_vent
					}                        
					def switchOverrideLevel=null                 
					nbVents++
					if (!resetLevelOverrideFlag) {
						key = "ventLevel${j}$indiceRoom"
						switchOverrideLevel = settings[key]
					}                        
					if (switchOverrideLevel) {                
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} at switchOverrideLevel =${switchOverrideLevel}%",
							settings.detailedNotif)     
						switchLevel = ((switchOverrideLevel >= 0) && (switchOverrideLevel<= 100))? switchOverrideLevel:switchLevel                     
					} else if (defaultSetLevel)  {
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} at defaultSetLevel =${defaultSetLevel}%",
							settings.detailedNotif)     
						switchLevel = ((defaultSetLevel >= 0) && (defaultSetLevel<= 100))? defaultSetLevel:switchLevel                     
					}
					setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)                    
					// compile some stats for the dashboard                    
					min_open_level=(switchLevel.toInteger() < min_open_level)? switchLevel.toInteger() : min_open_level
					max_open_level=(switchLevel.toInteger() > max_open_level)? switchLevel.toInteger() : max_open_level
					total_level_vents=total_level_vents + switchLevel.toInteger()
					if (switchLevel > MIN_OPEN_LEVEL_IN_ZONE) {      // make sure that the vents are set to a minimum level, otherwise they are considered to be closed              
						ventSwitchesOnSet.add(ventSwitch)
						closedAllVentsInZone=false
						openVentsCount++    
					}                        
				}                
			} /* end for ventSwitch */                             
		} /* end for rooms */
	} /* end for zones */

	if ((!fullyCloseVents) && (closedAllVentsInZone) && (nbVents)) {
		    	
		switchLevel= MIN_OPEN_LEVEL_IN_ZONE
		ventSwitchesOnSet=control_vent_switches_in_zone(indiceSchedule, switchLevel)		    
		traceEvent(settings.logFilter,"schedule ${scheduleName}, safeguards on: set all ventSwitches at ${switchLevel}% to avoid closing all of them",
			settings.detailedNotif, get_LOG_INFO(),true)       
	}    
	// Save the stats for the dashboard
    
	state?.openVentsCount=openVentsCount
	state?.maxOpenLevel=max_open_level
	state?.minOpenLevel=min_open_level
	state?.minTempInVents=min_temp_in_vents
	state?.maxTempInVents=max_temp_in_vents
	if (total_temp_in_vents) {
		state?.avgTempInVents= (total_temp_in_vents/nbVents).toFloat().round(1)
	}		        
	if (total_level_vents) {    
		state?.avgVentLevel= (total_level_vents/nbVents).toFloat().round(1)
	}		        
	if (total_temp_diff) {
		state?.avgTempDiff = (total_temp_diff/ nbRooms).toFloat().round(1)    
	}		        
	return ventSwitchesOnSet    
}

private def turn_off_all_other_vents(ventSwitchesOnSet) {
	def foundVentSwitch
	int nbClosedVents=0, totalVents=0
	float MAX_RATIO_CLOSED_VENTS=50 // not more than 50% of the smart vents should be closed at once
	def MIN_OPEN_LEVEL_SMALL=(minVentLevelOutZone!=null)?((minVentLevelOutZone>=0 && minVentLevelOutZone <100)?minVentLevelOutZone:25):25
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def closedVentsSet=[]
	def fullyCloseVents = (fullyCloseVentsFlag) ?: false
    
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def ventSwitch = settings[key]
			if (ventSwitch != null) {
				totalVents++
				foundVentSwitch = ventSwitchesOnSet.find{it == ventSwitch}
				if (foundVentSwitch ==null) {
					nbClosedVents++ 
					closedVentsSet.add(ventSwitch)                        
				} else {
					def ventLevel= getCurrentVentLevel(ventSwitch)
					if ((ventLevel!=null) && (ventLevel <= MIN_OPEN_LEVEL_IN_ZONE)) { // below minimum level is considered as closed.
						nbClosedVents++ 
						closedVentsSet.add(ventSwitch)                        
						traceEvent(settings.logFilter,"turn_off_all_other_vents>${ventSwitch}'s level=${ventLevel} is lesser than minimum level ${MIN_OPEN_LEVEL_IN_ZONE}",
						 	settings.detailedNotif, get_LOG_INFO(),true)
 					}                        
				} /* else if foundSwitch==null */                    
			}   /* end if ventSwitch */                  
		}  /* end for ventSwitch */         
	} /* end for rooms */
	state?.closedVentsCount= nbClosedVents                     
	state?.totalVents=totalVents
	state?.ratioClosedVents =0   
	if (totalVents >0) {    
		float ratioClosedVents=((nbClosedVents/totalVents).toFloat()*100)
		state?.ratioClosedVents=ratioClosedVents.round(1)
		if ((!fullyCloseVents) && (ratioClosedVents > MAX_RATIO_CLOSED_VENTS)) {
			traceEvent(settings.logFilter,"ratio of closed vents is too high (${ratioClosedVents.round()}%), opening ${closedVentsSet} at minimum level of ${MIN_OPEN_LEVEL_SMALL}%",
				settings.detailedNotif, get_LOG_INFO(),true)            
		} /* end if ratioCloseVents is ratioClosedVents > MAX_RATIO_CLOSED_VENTS */            
		if (!fullyCloseVents) {
			closedVentsSet.each {
				setVentSwitchLevel(null, it, MIN_OPEN_LEVEL_SMALL)
			}                
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} using the safeguards as requested to create the desired zone(s)",
				settings.detailedNotif, get_LOG_INFO())
		}            
		if (fullyCloseVents) {
			closedVentsSet.each {
				setVentSwitchLevel(null, it, 0)
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} totally as requested to create the desired zone(s)",settings.detailedNotif,
				get_LOG_INFO())            
			}                
		}        
	} /* if totalVents >0) */        
}

private def open_all_vents() {
	// Turn on all vents        
	for (int indiceRoom =1; ((indiceRoom <= settings.roomsCount) && (indiceRoom <= get_MAX_ROOMS())); indiceRoom++) {
		for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
			def key = "ventSwitch${j}$indiceRoom"
			def vent = settings[key]
				if (vent != null) {
					setVentSwitchLevel(null, vent, 100)	
			} /* end if vent != null */
		} /* end for vent switches */
	} /* end for rooms */
}


// @ventSwitch vent switch to be used to get temperature
private def getTemperatureInVent(ventSwitch) {
	def temp=null
	try {
		temp = ventSwitch.currentValue("temperature")       
	} catch (any) {
		traceEvent(settings.logFilter,"getTemperatureInVent>Not able to current Temperature from ${ventSwitch}",settings.detailedNotif, get_LOG_WARN(),true)
	}    
	return temp    
}

// @ventSwitch	vent switch to be used to get level
private def getCurrentVentLevel(ventSwitch) {
	def ventLevel=null
	try {
		ventLevel = ventSwitch.currentValue("level")     
	} catch (any) {
		traceEvent(settings.logFilter,"getCurrentVentLevel>Not able to current vent level from ${ventSwitch}",settings.detailedNotif, get_LOG_WARN(),true)
	}    
	return ventLevel   
}

private def setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel=100) {
	def roomName
    
	if (indiceRoom) {
		def key = "roomName$indiceRoom"
		roomName = settings[key]
	}
	try {
		ventSwitch.setLevel(switchLevel)
		if (roomName) {       
			traceEvent(settings.logFilter,"set ${ventSwitch} at level ${switchLevel} in room ${roomName} to reach desired temperature",settings.detailedNotif, get_LOG_INFO())
		}            
	} catch (e) {
		if (switchLevel >0) {
			ventSwitch.on()        
			traceEvent(settings.logFilter, "setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it on",
				true, get_LOG_WARN())            
		} else {
			ventSwitch.off()        
			traceEvent(settings.logFilter, "setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it off",
				true, get_LOG_WARN())           
		}
	}
    
}



private def control_vent_switches_in_zone(indiceSchedule, switchLevel=100) {

	def key = "includedZones$indiceSchedule"
	def zones = settings[key]
	def ventSwitchesOnSet=[]
    
	for (zone in zones) {

		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
    
		for (room in rooms) {
			def roomDetails=room.split(':')
			def indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]


			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
	                
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					ventSwitchesOnSet.add(ventSwitch)
					setVentSwitchLevel(indiceRoom, ventSwitch, switchLevel)
				}
			} /* end for ventSwitch */
		} /* end for rooms */
	} /* end for zones */
	return ventSwitchesOnSet
}

def IsRightDayForChange(indiceSchedule) {

	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key ="dayOfWeek$indiceSchedule"
	def dayOfWeek = settings[key]
	def makeChange = false
	Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
	int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK);

	// Check the condition under which we want this to run now
	// This set allows the most flexibility.
	if (dayOfWeek == 'All Week') {
		makeChange = true
	} else if ((dayOfWeek == 'Monday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.MONDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Tuesday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.TUESDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Wednesday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.WEDNESDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Thursday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.THURSDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Friday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.FRIDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Saturday' || dayOfWeek == 'Saturday & Sunday') && currentDayOfWeek == Calendar.instance.SATURDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Sunday' || dayOfWeek == 'Saturday & Sunday') && currentDayOfWeek == Calendar.instance.SUNDAY) {
		makeChange = true
	}

	return makeChange
    
}




private def get_MAX_SCHEDULES() {
	return 12
}


private def get_MAX_ZONES() {
	return 8
}

private def get_MAX_ROOMS() {
	return 16
}

private def get_MAX_VENTS() {
	return 5
}

def getCustomImagePath() {
	return "http://raw.githubusercontent.com/yracine/device-type.myecobee/master/icons/"
}    

private def getStandardImagePath() {
	return "http://cdn.device-icons.smartthings.com"
}

private int get_LOG_ERROR()	{return 1}
private int get_LOG_WARN()	{return 2}
private int get_LOG_INFO()	{return 3}
private int get_LOG_DEBUG()	{return 4}
private int get_LOG_TRACE()	{return 5}

def traceEvent(filterLog, message, displayEvent=false, traceLevel=4, sendMessage=false) {
int LOG_ERROR= get_LOG_ERROR()
int LOG_WARN=  get_LOG_WARN()
int LOG_INFO=  get_LOG_INFO()
int LOG_DEBUG= get_LOG_DEBUG()
int LOG_TRACE= get_LOG_TRACE()
int filterLevel=(filterLog)?filterLog.toInteger():get_LOG_WARN()


	if (filterLevel >= traceLevel) {
		if (displayEvent) {    
			switch (traceLevel) {
				case LOG_ERROR:
					log.error "${message}"
				break
				case LOG_WARN:
					log.warn "${message}"
				break
				case LOG_INFO:
					log.info "${message}"
				break
				case LOG_TRACE:
					log.trace "${message}"
				break
				case LOG_DEBUG:
				default:            
					log.debug "${message}"
				break
			}                
		}			                
		if (sendMessage) send (message,settings.askAlexaFlag) //send message only when true
	}        
}


private send(msg, askAlexa=false) {
int MAX_EXCEPTION_MSG_SEND=5

	// will not send exception msg when the maximum number of send notifications has been reached
	if (msg.contains("exception")) {
		state?.sendExceptionCount=state?.sendExceptionCount+1         
		traceEvent(settings.logFilter,"checking sendExceptionCount=${state?.sendExceptionCount} vs. max=${MAX_EXCEPTION_MSG_SEND}", detailedNotif)
		if (state?.sendExceptionCount >= MAX_EXCEPTION_MSG_SEND) {
			traceEvent(settings.logFilter,"send>reached $MAX_EXCEPTION_MSG_SEND exceptions, exiting", detailedNotif)
			return        
		}        
	} 
	def message = "${get_APP_NAME()}>${msg}"

	if (sendPushMessage != "No") {
		if (location.contactBookEnabled && recipients) {
			traceEvent(settings.logFilter,"contact book enabled", false, get_LOG_INFO())
			sendNotificationToContacts(message, recipients)
    	} else {
			traceEvent(settings.logFilter,"contact book not enabled", false, get_LOG_INFO())
			sendPush(message)
		}            
	}
	if (askAlexa) {
		sendLocationEvent(name: "AskAlexaMsgQueue", value: "${get_APP_NAME()}", isStateChange: true, descriptionText: msg)        
	}        
	
	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, message)
	}
}


private def get_APP_NAME() {
	return "ScheduleRoomTempControl"
}