/**
 *  ScheduleTstatZones
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
	description: "Enable Heating/Cooling Zoned Solutions for thermostats coupled with smart vents (optional) for better temp settings control throughout your home",
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
	page(name: "fanSettingsSetup")
	page(name: "outdoorThresholdsSetup")
	page(name: "ventSettingsSetup")
	page(name: "alternativeCoolingSetup")
}

def dashboardPage() {
	def scale= getTemperatureScale()
	dynamicPage(name: "dashboardPage", title: "Dashboard", uninstall: true, nextPage: generalSetupPage) {
		section("Tap Running Schedule(s) Config for latest info\nPress Next (upper right) for initial Setup") {
			if (roomsCount && zonesCount && schedulesCount) {
				paragraph image: "${getCustomImagePath()}office7.png", "ST hello mode: $location.mode" +
					"\nLast Running Schedule: $state.lastScheduleName" +
					"\nActiveZone(s): ${state?.activeZones}"
				if (state?.avgTempDiff)  { 
					paragraph "AvgTempDiffInZone: ${state?.avgTempDiff}$scale\n"                   
				}
				def currentTemp = thermostat?.currentTemperature
				String mode =thermostat?.currentThermostatMode   
				def operatingState=thermostat?.currentThermostatOperatingState                
				def heatingSetpoint,coolingSetpoint
				switch (mode) { 
					case 'cool':
						coolingSetpoint = thermostat?.currentValue('coolingSetpoint')
					break                        
 					case 'auto': 
						coolingSetpoint = thermostat?.currentValue('coolingSetpoint')
					case 'heat':
					case 'emergency heat':
					case 'auto': 
					case 'off':  
						try {                    
	 						heatingSetpoint = thermostat?.currentValue('heatingSetpoint')
						} catch (e) {
							traceEvent(settings.logFilter,"dashboardPage>not able to get heatingSetpoint from $thermostat,exception $e",settings.detailedNotif)                      
						}                        
						heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')?21:72                        
					break
					default:
						log.warn "dashboardPage>invalid mode $mode"
					break                        
                    
				}        
				def dParagraph = "TstatMode: $mode" +
						"\nTstatOperatingState: $operatingState" +
						"\nTstatCurrrentTemp: ${currentTemp}$scale" 
				if (coolingSetpoint)  { 
					 dParagraph = dParagraph + "\nCoolingSetpoint: ${coolingSetpoint}$scale"
				}     
				if (heatingSetpoint)  { 
					dParagraph = dParagraph + "\nHeatingSetpoint: ${heatingSetpoint}$scale" 
				}     
				paragraph image: "${getCustomImagePath()}home1.png", dParagraph 

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
			paragraph "${get_APP_NAME()}, the smartapp that enables Heating/Cooling zoned settings at selected thermostat(s) coupled with smart vents (optional) for better temp settings control throughout your home"
			paragraph "Version 7.2.3" 
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.me/ecomatiqhomes",
					title:"Paypal donation..."
			paragraph "Copyright©2015 Yves Racine"
				href url:"http://www.maisonsecomatiq.com/#!home/mainPage", style:"embedded", required:false, title:"More information..."  
 				description: "http://www.maisonsecomatiq.com/#!home/mainPage"
		} /* end section about */
	}
}

def generalSetupPage() {

	dynamicPage(name: "generalSetupPage", nextPage: roomsSetupPage,uninstall: false,refreshAfterSelection:true) {
		section(image: "${getCustomImagePath()}home1.png", "Main thermostat at home (used for temp/vent adjustment)") {
			input (name:"thermostat", type: "capability.thermostat", title: "Which main thermostat?")
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
		if (thermostat) {
			section {
				href(name: "toRoomPage", title: "Rooms Setup", page: "roomsSetupPage", description: "Tap to configure", image: "${getCustomImagePath()}room.png")
				href(name: "toZonePage", title: "Zones Setup", page: "zonesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}zoning.jpg")
				href(name: "toSchedulePage", title: "Schedules Setup", page: "schedulesSetupPage",  description: "Tap to configure",image: "${getCustomImagePath()}office7.png")
				href(name: "toNotificationsPage", title: "Notification & Options Setup", page: "NotificationsPage",  description: "Tap to configure", image: "${getCustomImagePath()}notification.png")
			}                
		}
		section("Set your main thermostat to [Away,Present] based on all Room Motion Sensors [default=false] ") {
			input (name:"setAwayOrPresentFlag", title: "Set Main thermostat to [Away,Present]?", type:"bool",required:false)
		}
		section("Outdoor temp Sensor used for adjustment or alternative cooling [optional]") {
			input (name:"outTempSensor", type:"capability.temperatureMeasurement", required: false,
				description:"Optional")
		}
		section("Enable vent settings [optional, default=false]") {
			input (name:"setVentSettingsFlag", title: "Set Vent Settings?", type:"bool",required:false)
		}
		section("Enable mode/temp adjustment based on outdoor temp sensor [optional, default=false]") {
			input (name:"setAdjustmentOutdoorTempFlag", title: "Enable mode/temp adjustment based on outdoor sensor?", type:"bool",required:false)
		}
		section("Enable temp adjustment at main thermostat based on indoor temp/motion sensor(s) [optional, default=false]") {
			input (name:"setAdjustmentTempFlag", title: "Enable temp adjustment based on collected temps at indoor sensor(s)?", type:"bool",
				description:"optional",required:false)
			input (name:"adjustmentTempMethod", title: "Calculated method to be used for setpoints adjustment", type:"enum",
				description:"optional [default=calculated avg of all sensors' temps]",required:false, options:["avg", "med", "min","max"], 
				default: "avg")
		}
		section("Enable fan adjustment based on indoor/outdoor temp sensors [optional, default=false]") {
			input (name:"setAdjustmentFanFlag", title: "Enable fan adjustment set in schedules based on sensors?", type:"bool",required:false)
		}
		section("Enable Contact Sensors to be used for vent/temp adjustments [optional, default=false]") {
			input (name:"setVentAdjustmentContactFlag", title: "Enable vent adjustment set in schedules based on contact sensors?", type:"bool",
				description:" if true and contact open=>vent(s) closed in schedules",required:false)
			input (name:"setTempAdjustmentContactFlag", title: "Enable temp adjustment set in schedules based on contact sensors?", type:"bool",
				description:"optional, true and contact open=>no temp reading in schedules",required:false)
		}
        
		section("Efficient Use of evaporative cooler/Big Fan/Damper Switch for cooling based on outdoor sensor readings [optional]") {
			input (name:"evaporativeCoolerSwitch", title: "Evaporative Cooler/Big Fan/Damper Switch(es) to be turned on/off?",
				type:"capability.switch", required: false, multiple:true, description: "Optional")
			input (name:"doNotUseHumTableFlag", title: "For alternative cooling, use it only when outdoor temp is below coolModeThreshold in schedule [default=use of ideal humidity/temp table]?", 
				type:"bool",description:"optional",required:false)
 		}
		section("Disable or Modify the safeguards [default=some safeguards are implemented to avoid damaging your HVAC by closing too many vents]") {
			input (name:"fullyCloseVentsFlag", title: "Bypass all safeguards & allow closing the vents totally?", type:"bool",required:false)
			input (name:"minVentLevelInZone", title: "Safeguard's Minimum Vent Level in Zone", type:"number", required: false, description: "[default=10%]")
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

	dynamicPage(name: "roomsSetup", title: "Rooms Setup",  uninstall: false, nextPage: zonesSetupPage) {

		section("Room ${indiceRoom} Setup") {
			input "roomName${indiceRoom}", title: "Room Name", "string",image: "${getCustomImagePath()}room.png"
		}
		section("Room ${indiceRoom}-TempSensor [optional]") {
			input image: "${getCustomImagePath()}IndoorTempSensor.png", "tempSensor${indiceRoom}", title: "Temp sensor for better temp adjustment", "capability.temperatureMeasurement", 
				required: false, description: "Optional"

		}
		section("Room ${indiceRoom}-ContactSensor [optional]") {
			input image: "${getCustomImagePath()}contactSensor.png", "contactSensor${indiceRoom}", title: "Contact sensor for better vent/temp adjustment", "capability.contactSensor", 
				required: false, description: "Optional,if open=>vent is closed"

		}
        
		section("Room ${indiceRoom}-Room Thermostat for a fireplace, baseboards, window AC, etc.  [optional]") {
			input image: "${getCustomImagePath()}home1.png", "roomTstat${indiceRoom}", title: "Thermostat for better room comfort", "capability.thermostat", 
				required: false, description: "Optional"
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
			input "needOccupiedFlag${indiceRoom}", title: "Will do temp/vent adjustement only when Occupied [default=false]", "bool",  
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
	def fullyCloseVents = (settings.fullyCloseVentsFlag) ?: false
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
	String mode = thermostat?.currentThermostatMode.toString()
	def operatingState=thermostat.currentThermostatOperatingState                
	float desiredTemp, total_temp_in_vents=0
	def key
	def scale=getTemperatureScale()    
	def currTime = now()	
	boolean foundSchedule=false
	String bypassSafeguardsString= (fullyCloseVents)?'true':'false'                            
	String setAwayOrPresentString= (setAwayOrPresentFlag)?'true':'false'                            
	String setAdjustmentTempString= (setAdjustmentTempFlag)?'true':'false'                            
	String setAdjustmentOutdoorTempString= (setAdjustmentOutdoorTempFlag)?'true':'false'                            
	String setAdjustmentFanString= (setAdjustmentFanFlag)?'true':'false'                            
	String setVentSettingsString = (setVentSettingsFlag)?'true':'false'    
	int nbClosedVents=0, nbOpenVents=0, totalVents=0,  nbRooms=0
	int min_open_level=100, max_open_level=0,total_level_vents=0       
	float min_temp_in_vents=200, max_temp_in_vents=0, total_temp_diff=0, target_temp=0 
	float currentTempAtTstat = thermostat?.currentTemperature.toFloat().round(1)
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	def MIN_OPEN_LEVEL_OUT_ZONE=(minVentLevelOutZone!=null)?((minVentLevelOutZone>=0 && minVentLevelOutZone <100)?minVentLevelOutZone:25):25
	def MAX_TEMP_VENT_SWITCH = (settings.maxVentTemp)?:(scale=='C')?55:131  //Max temperature inside a ventSwitch
	def MIN_TEMP_VENT_SWITCH = (settings.minVentTemp)?:(scale=='C')?7:45   //Min temperature inside a ventSwitch

	traceEvent(settings.logFilter,"configDisplayPage>About to display Running Schedule(s) Configuration",settings.detailedNotif)
	dynamicPage(name: "configDisplayPage", title: "Running Schedule(s) Config", nextPage: generalSetupPage,submitOnChange: true) {
		section {
			href(name: "toDashboardPage", title: "Back to Dashboard Page", page: "dashboardPage")
		}
		section("General") {
			def heatingSetpoint,coolingSetpoint
			switch (mode) { 
				case 'cool':
					coolingSetpoint = thermostat.currentValue('coolingSetpoint')
					target_temp=coolingSetpoint.toFloat()                       
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
						traceEvent(settings.logFilter,"ConfigDisplayPage>not able to get heatingSetpoint from $thermostat, exception $e",settings.detailedNotif,
							get_LOG_WARN())                        
					}   
					heatingSetpoint=  (heatingSetpoint)? heatingSetpoint: (scale=='C')?21:72                        
					if (mode == 'auto') {
						float median= ((coolingSetpoint + heatingSetpoint)/2).toFloat().round(1)
						if (currentTempAtTstat > median) {
							target_temp =coolingSetpoint.toFloat()                   
						} else {
							target_temp =heatingSetpoint.toFloat()                   
						}                        
					} else {                         
						target_temp =heatingSetpoint.toFloat()                   
					}   
				break
				default:
					log.warn "ConfigDisplayPage>invalid mode $mode"
				break                        
                
			}      
            
			def detailedNotifString=(settings.detailedNotif)?'true':'false'			            
			def askAlexaString=(settings.askAlexaFlag)?'true':'false'			            
			def setVentAdjustmentContactString=(settings.setVentAdjustmentContactFlag)?'true':'false'
			def setTempAdjustmentContactString=(settings.setTempAdjustmentContactFlag)?'true':'false'
			def setAdjustmentTempMethod=(settings.adjustmentTempMethod)?:"avg"
			paragraph image: "${getCustomImagePath()}notification.png", "Notifications" 
			paragraph "  >Detailed Notification: $detailedNotifString" +
					"\n  >AskAlexa Notifications: $askAlexaString"             
			paragraph image: "${getCustomImagePath()}home1.png", "ST hello mode: $location.mode" +
					"\nTstatMode: $mode\nTstatOperatingState: $operatingState"
			if (coolingSetpoint)  { 
				paragraph " >TstatCoolingSetpoint: ${coolingSetpoint}$scale"
			}                        
			if (heatingSetpoint)  { 
				paragraph " >TstatHeatingSetpoint: ${heatingSetpoint}$scale"
			}
            
            
			paragraph " >SetVentSettings: ${setVentSettingsString}" +
					"\n >SetAwayOrPresent: ${setAwayOrPresentString}" +
					"\n >AwayOrPresentNow: ${state?.setPresentOrAway}" + 
					"\n >AdjustTstatVs.indoorAvgTemp: ${setAdjustmentTempString}" +
					"\n >AdjustTstatTempCalcMethod: ${setAdjustmentTempMethod}" +
					"\n >AdjustTempBasedOnContact: ${setTempAdjustmentContactString}" +
					"\n >AdjustVentBasedOnContact: ${setVentAdjustmentContactString}" 

			paragraph image: "${getCustomImagePath()}safeguards.jpg","Safeguards"
 			paragraph "  >BypassSafeguards: ${bypassSafeguardsString}" +
					"\n  >MinVentLevelInZone: ${MIN_OPEN_LEVEL_IN_ZONE}%" +
					"\n  >MinVentLevelOutZone: ${MIN_OPEN_LEVEL_OUT_ZONE}%" +
					"\n  >MinVentTemp: ${MIN_TEMP_VENT_SWITCH}${scale}" +
					"\n  >MaxVentTemp: ${MAX_TEMP_VENT_SWITCH}${scale}" 
                    
			if (outTempSensor) {
				paragraph image: "${getCustomImagePath()}WeatherStation.jpg", "OutdoorTempSensor: $outTempSensor" 
				paragraph " >AdjustTstatVs.OutdoorTemp: ${setAdjustmentOutdoorTempString}"  +                         
					"\n >AdjustFanVs.OutdoorTemp: ${setAdjustmentFanString}"                            
			}				
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
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, subtracted - 1 day, new startTime=${startTimeToday.time}",settings.detailedNotif)
			}            
			if ((currTime > endTimeToday.time) && (endTimeToday.time < startTimeToday.time)) {
				endTimeToday = endTimeToday +1        
				traceEvent(settings.logFilter,"configDisplayPage>schedule ${scheduleName}, added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)
			}        
			String startInLocalTime = startTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
			String endInLocalTime = endTimeToday.format("yyyy-MM-dd HH:mm", location.timeZone)
			traceEvent(settings.logFilter,"configDisplayPage>$scheduleName is good to go..",settings.detailedNotif)
			if ((currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange(i))) {
				foundSchedule=true
                
				key = "givenClimate${i}"
				def climate = settings[key]
                
				key = "includedZones$i"
				def zones = settings[key]
				key = "heatModeThreshold${i}"
				def heatModeThreshold=settings[key]                
				key = "coolModeThreshold${i}"
				def coolModeThreshold=settings[key]                
				key = "moreHeatThreshold$i"
				def moreHeatThreshold= settings[key]
				key = "moreCoolThreshold$i"
				def moreCoolThreshold= settings[key]
				key = "givenMaxTempDiff${i}"
				def givenMaxTempDiff = settings[key]
				key = "fanMode${i}"
				def fanMode = settings[key]
				key ="moreFanThreshold${i}"
				def moreFanThreshold = settings[key]
				key = "fanModeForThresholdOnlyFlag${i}"                
				def fanModeForThresholdOnlyString = (settings[key])?'true':'false'
				key = "setRoomThermostatsOnlyFlag${i}"
				String setRoomThermostatsOnlyString = (settings[key])?'true':'false'
				key = "desiredCoolTemp${i}"
				def desiredCoolTemp = (settings[key])?: ((scale=='C') ? 23:75)
				key = "desiredHeatTemp${i}"
				def desiredHeatTemp = (settings[key])?: ((scale=='C') ? 21:72)
				key = "adjustVentsEveryCycleFlag${i}"
				def adjustVentsEveryCycleString = (settings[key])?'true':'false'
				key = "setVentLevel${i}"
				def setLevel = settings[key]
				key = "resetLevelOverrideFlag${i}"
				def resetLevelOverrideString=(settings[key])?'true':'false'
				key = "useEvaporativeCoolerFlag${i}"                
				def useAlternativeCoolingString = (settings[key])?'true':'false'
				key = "useAlternativeWhenCoolingFlag${i}"                
				def useAlternativeWhenCoolingString = (settings[key])?'true':'false'
				key = "openVentsFanOnlyFlag${i}"                
				def openVentsWhenFanOnlyString = (settings[key])?'true':'false'                
				def doNotUseHumTableString = (doNotUseHumTableFlag)?'false':'true'
				section("Running Schedule(s)") {
					paragraph image: "${getCustomImagePath()}office7.png","Schedule $scheduleName" 
						"\n >StartTime: $startInLocalTime" +                    
						"\n >EndTime: $endInLocalTime"                  
                    
					if (climate) {                    
						paragraph " >EcobeeProgramSet: $climate" 
					} else {
						if (desiredCoolTemp) {
							paragraph " >DesiredCoolTemp: ${desiredCoolTemp}$scale"
						}    
						if (desiredHeatTemp) {
							paragraph " >DesiredHeatTemp: ${desiredHeatTemp}$scale"
						}    
					}                    
					if (fanMode) {
						paragraph " >SetFanMode: $fanMode"
					}                    
					if (moreFanThreshold) {
						paragraph " >MoreFanThreshold: ${moreFanThreshold}$scale"
					}                    
					if (fanModeForThresholdOnlyString=='true') {
						paragraph " >AdjustFanWhenThresholdMetOnly: $fanModeForThresholdOnlyString"
					}
					if (heatModeThreshold) {
						paragraph " >HeatModeThreshold: ${heatModeThreshold}$scale"
					}                    
					if (coolModeThreshold) {
						paragraph " >CoolModeThreshold: ${coolModeThreshold}$scale"
					}                    
					if (moreHeatThreshold) {
						paragraph " >MoreHeatThreshold: ${moreHeatThreshold}$scale"
					}                    
					if (moreCoolThreshold) {
						paragraph " >MoreCoolThreshold: ${moreCoolThreshold}$scale"
					}                    
					if (setRoomThermostatsOnlyString=='true') {
						paragraph " >SetRoomThermostatOnly: $setRoomThermostatsOnlyString"
						if (desiredCoolTemp) {
							paragraph " >DesiredCoolTempForRoomTstat: ${desiredCoolTemp}$scale"
						}    
						if (desiredHeatTemp) {
							paragraph " >DesiredHeatTempForRoomTstat: ${desiredHeatTemp}$scale"
						}    
					}                    
					if (setLevel) {
						paragraph " >DefaultSetLevelForAllVentsInZone(s): ${setLevel}%"
					}                        
					paragraph " >BypassSetLevelOverrideinZone(s): ${resetLevelOverrideString}" +
						"\n >AdjustVentsEveryCycle: $adjustVentsEveryCycleString" + 
						"\n >OpenVentsWhenFanOnly: $openVentsWhenFanOnlyString"                        
					paragraph image: "${getCustomImagePath()}altenergy.jpg", "UseAlternativeCooling: $useAlternativeCoolingString"
        
					if (useAlternativeCoolingString=='true') {                    
						key = "diffDesiredTemp${i}"
						def diffDesiredTemp = (settings[key])?: (scale=='F')?5:2             
						key = "diffToBeUsedFlag${i}"
						def diffToBeUsedString = (settings[key])? 'true':'false'
						paragraph " >UseAlternativeWhenCooling: $useAlternativeWhenCoolingString" +
						"\n >UseHumidityTempTable: $doNotUseHumTableString" +
						"\n >DiffToBeUsedForCooling: $diffToBeUsedString" +
						"\n >DiffToDesiredTemp: $diffDesiredTemp${scale}"
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
					key = "includedRooms$indiceZone"
					def rooms = settings[key]
					key = "desiredCoolDeltaTemp$indiceZone" 
					def desiredCoolDelta= settings[key] 
					key = "desiredHeatDeltaTemp$indiceZone" 
					def desiredHeatDelta= settings[key] 
					section("Active Zone(s) in Schedule $scheduleName") {
						paragraph image: "${getCustomImagePath()}zoning.jpg", "Zone $zoneName" 
						paragraph " >Includes: $rooms" 
						if ((desiredCoolDelta) && (mode in ['cool', 'auto'])) {                         
							paragraph " >DesiredCoolDeltaSP: ${desiredCoolDelta}$scale" 
							target_temp = target_temp+ desiredCoolDelta                            
						}   
						if ((desiredHeatDelta) && (mode in ['heat','auto','off'])) {                         
							paragraph " >DesiredHeatDeltaSP: ${desiredHeatDelta}$scale"  
							target_temp = target_temp + desiredHeatDelta                            
						}   
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
						def motionSensor = settings[key] 
						key = "tempSensor${indiceRoom}"
						def tempSensor = settings[key]
						key = "contactSensor${indiceRoom}"
						def contactSensor = settings[key]
						key = "roomTstat${indiceRoom}"
						def roomTstat = settings[key] 
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
								float temp_diff = (tempAtSensor- target_temp).toFloat().round(1) 
								paragraph " >CurrentTempInRoom: ${tempAtSensor}$scale" +	
									"\n >TempOffsetVs.TargetTemp: ${temp_diff.round(1)}$scale"
								total_temp_diff = total_temp_diff + temp_diff    
							}   
							if (contactSensor) {      
								def contactState = contactSensor.currentState("contact")
								paragraph image: "${getCustomImagePath()}contactSensor.png", " ContactSensor: $contactSensor" + 
									"\n >ContactState: ${contactState.value}"                                
							}                            
							if (roomTstat) {      
								paragraph image: "${getCustomImagePath()}home1.png", " RoomTstat: $roomTstat" 
							}                            
							if (motionSensor) {      
								def countActiveMotion=isRoomOccupied(motionSensor, indiceRoom)
								String needOccupiedString= (needOccupied)?'true':'false'
								if (!needOccupied) {                                
									paragraph " >MotionSensor: $motionSensor" +
										"\n  ->NeedToBeOccupied: ${needOccupiedString}" 
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
                                    
									paragraph "  >IsRoomOccupiedNow: ${isRoomOccupiedString}" + 
										"\n  >NeedToBeOccupied: ${needOccupiedString}" + 
										"\n  >OccupiedThreshold: ${thresholdString} minutes"+ 
										"\n  >MotionCountNeeded: ${occupiedMotionOccNeeded}" + 
										"\n  >OccupiedMotionCounter: ${countActiveMotion}" +
										"\n  >LastMotionTime: ${lastMotionInLocalTime}"
								}
							}                                
							paragraph "** VENTS in $roomName **" 
							for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
								key = "ventSwitch${j}$indiceRoom"
								def ventSwitch = settings[key]
								if (ventSwitch != null) {
									def temp_in_vent=getTemperatureInVent(ventSwitch)                                
									// compile some stats for the dashboard     
									if (temp_in_vent) {                                   
										min_temp_in_vents=(temp_in_vent < min_temp_in_vents)? temp_in_vent.toFloat().round(1) : min_temp_in_vents
										max_temp_in_vents=(temp_in_vent > max_temp_in_vents)? temp_in_vent.toFloat().round(1) : max_temp_in_vents
										total_temp_in_vents=total_temp_in_vents + temp_in_vent
									}                                        
									def switchLevel = getCurrentVentLevel(ventSwitch)				                        
									totalVents++                                    
									paragraph image: "${getCustomImagePath()}ventopen.png","$ventSwitch"
									paragraph " >CurrentVentLevel: ${switchLevel}%" +
											"\n >CurrentVentStatus: ${ventSwitch.currentValue("switch")}"                                      
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
							} /* end for ventSwitch */                             
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
		if (total_temp_diff) {
			state?.avgTempDiff = (total_temp_diff/ nbRooms).toFloat().round(1)    
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

	dynamicPage(name: "zonesSetupPage", title: "Zones Setup",  uninstall: false,nextPage: schedulesSetupPage) {
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
	dynamicPage(name: "zonesSetup", title: "Zones Setup", uninstall: false) {
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
		section("Zone ${indiceZone}-Dynamic Cool Temp Adjustment for Vents based on the coolSP at thermostat - to make the zone cooler or warmer") {
			input (name:"desiredCoolDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Cool Temp Adjustment for the zone [default = +/-0F or +/-0C]", 
				required: false, defaultValue:settings."desiredCoolDeltaTemp${indiceZone}")			                
		}
		section("Zone ${indiceZone}-Dynamic Heat Temp Adjustment for Vents based on the heatSP at thermostat- to make the zone cooler or warmer") {
			input (name:"desiredHeatDeltaTemp${indiceZone}", type:"decimal", range: "*..*", title: "Dynamic Heat Temp Adjustment for the zone [default = +/-0F or +/-0C]", 
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

	dynamicPage(name: "schedulesSetup", title: "Schedule Setup", uninstall: false) {
		section("Schedule ${indiceSchedule} Setup") {
			input (name:"scheduleName${indiceSchedule}", title: "Schedule Name", type: "text",
				defaultValue:settings."scheduleName${indiceSchedule}")
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
		section("Schedule ${indiceSchedule}-Set Thermostat's Cooling setpoint in the selected zone(s) [optional,when no ecobee program/climate available]") {
			input (name:"desiredCoolTemp${indiceSchedule}", type:"decimal", title: "Cooling setpoint, default = 75F/23C", 
				required: false,defaultValue:settings."desiredCoolTemp${indiceSchedule}", description: "Optional")			                
		}
		section("Schedule ${indiceSchedule}-Set Thermostat's Heating setpoint [optional,when no ecobee program/climate available]") {
			input (name:"desiredHeatTemp${indiceSchedule}", type:"decimal", title: "Heating setpoint, default=72F/21C", 
				required: false, defaultValue:settings."desiredHeatTemp${indiceSchedule}", description: "Optional")			                
		}
		section("Schedule ${indiceSchedule}-Outdoor Thresholds Setup for switching thermostat mode (heat/cool/auto) or more heating/cooling [optional]") {
			href(name: "toOutdoorThresholdsSetup", page: "outdoorThresholdsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: outdoorThresholdsHrefTitle(indiceSchedule), image: getCustomImagePath() + "WeatherStation.jpg"  ) 
		}
		section("Schedule ${indiceSchedule}-Max Temp Adjustment Allowed for the active zone(s)") {
			input (name:"givenMaxTempDiff${indiceSchedule}", type:"decimal", title: "Max Temp adjustment to setpoints", required: false,
				defaultValue:settings."givenMaxTempDiff${indiceSchedule}", description: " [default= +/-5F/2C]")
		}        
		section("Schedule ${indiceSchedule}-Set Fan Mode [optional]") {
			href(name: "toFanSettingsSetup", page: "fanSettingsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: fanSettingsHrefTitle(indiceSchedule), image: getCustomImagePath() + "Fan.png") 
		}	
		section("Schedule ${indiceSchedule}-Alternative Cooling Setup [optional]") {
			href(name: "toAlternativeCoolingSetup", page: "alternativeCoolingSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: alternativeCoolingHrefTitle(indiceSchedule),image: getCustomImagePath() + "altenergy.jpg" ) 
		}
		section("Schedule ${indiceSchedule}-Set Zone/Room Thermostats Only Indicator [optional]") {
			input (name:"setRoomThermostatsOnlyFlag${indiceSchedule}", type:"bool", title: "Set room thermostats only [default=false,main & room thermostats setpoints are set]", 
				required: false, defaultValue:settings."setRoomThermostatsOnlyFlag${indiceSchedule}")
		}
		section("Schedule ${indiceSchedule}-Vent Settings for the Schedule") {
			href(name: "toVentSettingsSetup", page: "ventSettingsSetup", params: [indiceSchedule: indiceSchedule],required:false,  description: "Optional",
				title: ventSettingsHrefTitle(indiceSchedule), image: "${getCustomImagePath()}ventopen.png" ) 
		}
		section("Schedule ${indiceSchedule}-Set for specific mode(s) [default=all]")  {
			input (name:"selectedMode${indiceSchedule}", type:"enum", title: "Choose Mode", options: enumModes, 
				required: false, multiple:true,defaultValue:settings."selectedMode${indiceSchedule}", description: "Optional")
		}
		section {
			href(name: "toSchedulesSetupPage", title: "Back to Schedules Setup Page", page: "schedulesSetupPage")
		}
	}        
}


def fanSettingsSetup(params) {
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
    
	dynamicPage(name: "fanSettingsSetup", title: "Fan Settings Setup for schedule " + settings."scheduleName${indiceSchedule}", uninstall: false, 
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Set Fan Mode [optional]") {
			input (name:"fanMode${indiceSchedule}", type:"enum", title: "Set Fan Mode ['on', 'auto', 'circulate']", metadata: [values: ["on", "auto", "circulate"]], required: false,
				defaultValue:settings."fanMode${indiceSchedule}", description: "Optional")
			input (name:"moreFanThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for Fan Mode", required: false,
				defaultValue:settings."moreFanThreshold${indiceSchedule}", description: "Optional")			                
			input (name:"givenMaxFanDiff${indiceSchedule}", type:"decimal", title: "Max Temp Differential in the active zone(s) to trigger Fan mode above", required: false,
				defaultValue:settings."givenMaxFanDiff${indiceSchedule}", description: " [default= +/-5F/2C]")
			input (name:"fanModeForThresholdOnlyFlag${indiceSchedule}", type:"bool",  title: "Override Fan Mode only when Outdoor Threshold or Indoor Temp differential is reached(default=false)", 
				required: false, defaultValue:settings."fanModeForThresholdOnlyFlag${indiceSchedule}")
		}	
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule no ${indiceSchedule} Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   


def fanSettingsHrefTitle(i) {
	def title = "Fan Settings for Schedule ${i}"
	return title
}


def outdoorThresholdsSetup(params) {
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
    
	dynamicPage(name: "outdoorThresholdsSetup", title: "Outdoor Thresholds for schedule " + settings."scheduleName${indiceSchedule}", uninstall: false,
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Switch thermostat mode (auto/cool/heat) based on this outdoor temp range [optional]") {
			input (name:"heatModeThreshold${indiceSchedule}", type:"decimal", title: "Heat mode threshold", 
				required: false, defaultValue:settings."heatModeThreshold${indiceSchedule}", description: "Optional")			                
			input (name:"coolModeThreshold${indiceSchedule}", type:"decimal", title: "Cool mode threshold", 
				required: false, defaultValue:settings."coolModeThreshold${indiceSchedule}", description: "Optional")			               
		}			
		section("Schedule ${indiceSchedule}-More Heat/Cool Threshold in the selected zone(s) based on outdoor temp Sensor [optional]") {
			input (name:"moreHeatThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for more heating", 
				required: false, defaultValue:settings."moreHeatThreshold${indiceSchedule}", description: "Optional")			                
			input (name:"moreCoolThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for more cooling",
				required: false,defaultValue:settings."moreCoolThreshold${indiceSchedule}", description: "Optional")
		}                
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule no ${indiceSchedule} Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   

def outdoorThresholdsHrefTitle(i) {
	def title = "Outdoor Thresholds for Schedule ${i}"
	return title
}

def alternativeCoolingSetup(params) {
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
    
	dynamicPage(name: "alternativeCoolingSetup", title: "Alternative Cooling for schedule " + settings."scheduleName${indiceSchedule}" + "-switch in General Setup required", uninstall: false,
		nextPage: "schedulesSetupPage") {
		section("Schedule ${indiceSchedule}-Use of Evaporative Cooler/Big Fan/Damper For alternative cooling based on outdoor sensor's temp and humidity readings [optional]") {
			input (name:"useEvaporativeCoolerFlag${indiceSchedule}", type:"bool", title: "Use of evaporative cooler/Big Fan/Damper? [default=false]", 
				required: false, defaultValue:settings."useEvaporativeCoolerFlag${indiceSchedule}")
			input (name:"useAlternativeWhenCoolingFlag${indiceSchedule}", type:"bool", title: "Alternative cooling in conjunction with cooling? [default=false]", 
				required: false, defaultValue:settings."useAlternativeWhenCoolingFlag${indiceSchedule}")
			input (name:"coolModeThreshold${indiceSchedule}", type:"decimal", title: "Outdoor temp's threshold for alternative cooling, run when temp <= threshold [Required when not using humidity/temp table", 
				required: false, defaultValue:settings."coolModeThreshold${indiceSchedule}", description: "Optional")			               
			input (name:"diffToBeUsedFlag${indiceSchedule}", type:"bool", title: "Use of an offset value against the desired Temp for switching to cool [default=false]", 
				required: false, defaultValue:settings."diffToBeUsedFlag${indiceSchedule}")
			input (name:"diffDesiredTemp${indiceSchedule}", type:"decimal", title: "Temp Offset/Differential value vs. desired Cooling Temp", required: false,
				defaultValue:settings."diffDesiredTemp${indiceSchedule}", description: "[default= +/-5F/2C]")
		}                
		section {
			href(name: "toSchedulePage${indiceSchedule}", title: "Back to Schedule no ${indiceSchedule} Setup Page", page: "schedulesSetup", params: [indiceSchedule: indiceSchedule])
		}
	}    
}   

def alternativeCoolingHrefTitle(i) {
	def title = "Alternative Cooling Setup for Schedule ${i}"
	return title
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

private boolean is_alternative_cooling_efficient(outdoorTemp, outdoorHum) {
	def scale = getTemperatureScale()
	int outdoorTempInF= (scale=='C') ? cToF(outdoorTemp):outdoorTemp
	traceEvent(settings.logFilter,"is_alternative_cooling_efficient>outdoorTemp In Farenheit=$outdoorTempInF",settings.detailedNotif)
    
	switch (outdoorTempInF) {
    	case 75..79:
			outdoorTempInF =75        
		break            
    	case 80..84:
			outdoorTempInF =80        
		break
    	case 85..89:
			outdoorTempInF =85        
		break
    	case 90..94:
			outdoorTempInF =90        
		break
    	case 95..99:
			outdoorTempInF =95        
		break
    	case 100..104:
			outdoorTempInF =100        
		break
    	case 105..109:
			outdoorTempInF =105        
		break
    	case 110..114:
			outdoorTempInF =110        
		break
		default:
			outdoorTempInF =0        
		break        
	}        
	def temp_hum_range_table = [
		'75': '70,75,80,',
		'80': '50,55,60,65,',
		'85': '35,40,45,50,',
		'90': '20,25,30,',
		'95': '10,15,20,',
		'100': '5,10,',
		'105': '2,5,',
		'110': '2,'
	]    
	if (outdoorTempInF >= 75) {
		def max_hum_range
		try {
			max_hum_range = temp_hum_range_table.getAt(outdoorTempInF.toString())
		} catch (any) {
			traceEvent(settings.logFilter,"not able to get max humidity for temperature $outdoorTemp",settings.detailedNotif)
			return false        
		}
		def humidities  = max_hum_range.tokenize(',')
		def max_hum = humidities.last()
		traceEvent(settings.logFilter, "Max humidity $max_hum % found for temperature $outdoorTemp according to table",settings.detailedNotif)    
		    
		if ((outdoorHum) && (outdoorHum <= max_hum.toInteger())) {
			return true
		}
	} else if (outdoorTempInF <75) {
		return true    
	}
	return false
}


private boolean check_use_alternative_cooling(indiceSchedule) {
	def scale = getTemperatureScale()
	def key = "scheduleName${indiceSchedule}"
	def scheduleName=settings[key]
	def SET_LEVEL_BYPASS=99    

	def desiredCoolTemp=(state?.scheduleCoolSetpoint) ?: thermostat.currentCoolingSetpoint
    
	def outdoorTemp = outTempSensor?.currentTemperature
	def outdoorHum = outTempSensor?.currentHumidity
	def currentTemp = thermostat?.currentTemperature
	String currentMode = thermostat.latestValue("thermostatMode")
	String currentFanMode = thermostat.latestValue("thermostatFanMode")
    
	traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName, outdoorTemp=$outdoorTemp, outdoorHumidity=$outdoorHum,current mode=$currentMode, desiredCoolTemp=$desiredCoolTemp, currentTemp=$currentTemp",
		settings.detailedNotif)    
	if (evaporativeCoolerSwitch==null) {
		return false    
	}    

	def adjustmentFanFlag = (settings.setAdjustmentFanFlag)?: false
	key = "useAlternativeWhenCoolingFlag${indiceSchedule}"
	def useAlternativeWhenCooling=settings[key]
	traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName, doNotUseHumTable= $settings.doNotUseHumTableFlag, useAlternativeWhenCooling=$useAlternativeWhenCooling",
		settings.detailedNotif)    
	if (settings.doNotUseHumTableFlag) {    
		key = "coolModeThreshold$indiceSchedule"
		def lessCoolThreshold = settings[key]
		if (!lessCoolThreshold) { // if no threshold value is set, return false
			return false        
		}        
		if ((currentMode in ['cool','off', 'auto']) && ((outdoorTemp) &&
			(outdoorTemp.toFloat() <= lessCoolThreshold.toFloat())) && 
			(currentTemp.toFloat() > desiredCoolTemp.toFloat())) {
			evaporativeCoolerSwitch.on()
			if ((!useAlternativeWhenCooling) && (currentMode != 'off')) {                
				traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn off the thermostat $thermostat, saving the current thermostat's mode=$currentMode",settings.detailedNotif,
					get_LOG_WARN(),true)            
				state?.lastThermostatMode= currentMode            
				thermostat.off()
			} else if ((useAlternativeWhenCooling) && (currentMode == 'off')) {
				traceEvent(settings.logFilter,"check_use_alternative_cooling>useAlternativeWhenCooling= $useAlternativeWhenCooling,restoring $thermostat to ${state?.lastThermostatMode} mode ",settings.detailedNotif,
					get_LOG_INFO(),true)            
				if (state?.lastThermostatMode) { // by default, set it to cool
					thermostat.cool()                
				} else {                
					restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
				}                    
			}            
			if (adjustmentFanFlag) {             
				if (currentFanMode != 'auto') {            
					traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn on the thermostat's fan, saving the current Fan Mode=${currentFanMode}",settings.detailedNotif)            
					if (state?.lastThermostatFanMode) { // save the fan mode for later
						state?.lastThermostatFanMode=  currentFanMode
					}
				}
				traceEvent(settings.logFilter,"check_use_alternative_cooling>turning on the fan",settings.detailedNotif, get_LOG_INFO(),true)            
				thermostat.fanOn()                
			}            
                
			traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: alternative cooling (w/o HumTempTable); switch (${evaporativeCoolerSwitch}) is on",
				settings.detailedNotif, get_LOG_INFO(), true)
			if (settings."setVentLevel${indiceSchedule}"==null) {
				// set all vent levels to 100% temporarily
				settings."setVentLevel${indiceSchedule}"=SET_LEVEL_BYPASS                    
				traceEvent(settings.logFilter,"check_use_alternative_cooling>setLevel bypass now set to 100%",settings.detailedNotif, get_LOG_INFO(),true)            
			}  
			return true				                
		} else {
			evaporativeCoolerSwitch.off()
			if (settings."setVentLevel${indiceSchedule}"== SET_LEVEL_BYPASS) {          
				// Remove any setLevel bypass in schedule set in check_use_alternative_cooling
				settings."setVentLevel${indiceSchedule}"=null                    
				traceEvent(settings.logFilter,"check_use_alternative_cooling>removed the setLevel bypass",settings.detailedNotif, get_LOG_INFO())           
			}  
			if (adjustmentFanFlag) {             
				traceEvent(settings.logFilter,"check_use_alternative_cooling>turning off the fan",settings.detailedNotif, get_LOG_INFO(),true)            
				thermostat.fanAuto()            
			}  
			key = "diffDesiredTemp${indiceSchedule}"
			def diffDesiredTemp = (settings[key])?: (scale=='F')?5:2             
			key = "diffToBeUsedFlag${indiceSchedule}"
			def diffToBeUsed = (settings[key])?:false
			float desiredTemp = (diffToBeUsed)? (desiredCoolTemp.toFloat() - diffDesiredTemp.toFloat()) : desiredCoolTemp.toFloat()            
			if ((currentTemp.toFloat() > desiredTemp) && (currentMode=='off')) {        
				traceEvent(settings.logFilter,"check_use_alternative_cooling>diffToBeUsed=$diffToBeUsed, currentTemp ($currentTemp) > desiredTemp in schedule ($desiredTemp), switching $thermostat to cool mode",settings.detailedNotif,
					get_LOG_INFO(),true)           
				if (state?.lastThermostatMode) { // by default, set it to cool
					thermostat.cool()                
				} else {                
					restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
				}                    
			}    
			traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: alternative cooling (w/o HumTempTable); switch (${evaporativeCoolerSwitch}) is off",settings.detailedNotif,
				get_LOG_INFO(),true)
		}            
	} else if (currentMode in ['cool','off', 'auto']) {    
		if (is_alternative_cooling_efficient(outdoorTemp,outdoorHum)) {
			if (currentTemp.toFloat() > desiredCoolTemp.toFloat()) {        
				traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn on the alternative cooling Switch (${evaporativeCoolerSwitch})",settings.detailedNotif)            
				if ((!useAlternativeWhenCooling) && (currentMode != 'off')) {                
					traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn off the thermostat $thermostat",settings.detailedNotif, get_LOG_INFO(),true)                        
					state?.lastThermostatMode= currentMode             
					thermostat.off()
				} else if ((useAlternativeWhenCooling) && (currentMode == 'off')) {
    	        
					traceEvent(settings.logFilter,"check_use_alternative_cooling>useAlternativeWhenCooling= $useAlternativeWhenCooling,restoring $thermostat to ${state?.lastThermostatMode} mode ",settings.detailedNotif,
						settings.detailedNotif,get_LOG_INFO(),true )            
					if (state?.lastThermostatMode) { // by default, set it to cool
						thermostat.cool()                
					} else {                
						restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
					}            
				}                    
				evaporativeCoolerSwitch.on()	
				traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: turned on the alternative cooling switch (${evaporativeCoolerSwitch})",
					settings.detailedNotif, get_LOG_INFO(),true)
				if (adjustmentFanFlag) {             
					if (currentFanMode != 'auto') {            
						traceEvent(settings.logFilter,"check_use_alternative_cooling>about to turn on the thermostat's fan, saving the current Fan Mode=${currentFanMode}",
							settings.detailedNotif, get_LOG_INFO(),true)                                    
						if (state?.lastThermostatFanMode) { // save the fan mode for later
							state?.lastThermostatFanMode=  currentFanMode
						}
					}
					traceEvent(settings.logFilter,"check_use_alternative_cooling>turning on the fan",settings.detailedNotif, get_LOG_INFO(),true)            
					thermostat.fanOn()                
				}            
				if (settings."setVentLevel${indiceSchedule}"==null) {
					// set all vent levels to 100% temporarily while the thermostat's mode is off                  
					settings."setVentLevel${indiceSchedule}"=SET_LEVEL_BYPASS                     
					traceEvent(settings.logFilter,"check_use_alternative_cooling>setLevel bypass now set to 100%",settings.detailedNotif, get_LOG_INFO(),true)			
				}                    
				return true            
			} else { /* current temp < desiredCoolTemp */
				traceEvent(settings.logFilter,"check_use_alternative_cooling>currentTemp ($currentTemp) < desiredCoolTemp in schedule ($desiredCoolTemp), turning off alternative cooling ($evaporativeCoolerSwitch)",
					settings.detailedNotif, get_LOG_INFO(),true)            
				if (adjustmentFanFlag) {  
					traceEvent(settings.logFilter,"check_use_alternative_cooling>turning off the fan",settings.detailedNotif, get_LOG_INFO(),true)            
					thermostat.fanAuto()            
				}  
				evaporativeCoolerSwitch.off()	
				if (settings."setVentLevel${indiceSchedule}"== SET_LEVEL_BYPASS) {
					// Remove any setLevel bypass in schedule set in check_use_alternative_cooling
					settings."setVentLevel${indiceSchedule}"=null                    
					traceEvent(settings.logFilter,"check_use_alternative_cooling>removed the setLevel bypass",settings.detailedNotif, get_LOG_INFO(),true)            
				}  
			}
		} else {
			evaporativeCoolerSwitch.off()		        
			traceEvent(settings.logFilter,"check_use_alternative_cooling>schedule $scheduleName: alternative cooling not efficient, switch (${evaporativeCoolerSwitch}) is off",
				settings.detailedNotif, get_LOG_INFO(),true)
			if (settings."setVentLevel${indiceSchedule}"== SET_LEVEL_BYPASS) {
				// Remove any setLevel bypass in schedule set in check_use_alternative_cooling
				settings."setVentLevel${indiceSchedule}"=null                    
				traceEvent(settings.logFilter,"check_use_alternative_cooling>removed the setLevel bypass",settings.detailedNotif, get_LOG_INFO(),true)           
			}  
			if (adjustmentFanFlag) {             
				traceEvent(settings.logFilter,"check_use_alternative_cooling>turning off the fan",settings.detailedNotif, get_LOG_INFO(),true)            
				thermostat.fanAuto()            
			}  
			key = "diffDesiredTemp${indiceSchedule}"
			def diffDesiredTemp = (settings[key])?: (scale=='F')?5:2             
			key = "diffToBeUsedFlag${indiceSchedule}"
			def diffToBeUsed = (settings[key])?:false
			float desiredTemp = (diffToBeUsed)? (desiredCoolTemp.toFloat() - diffDesiredTemp.toFloat()) : desiredCoolTemp.toFloat()            
			if ((currentTemp.toFloat() > desiredTemp) && (currentMode=='off')) {        
				traceEvent(settings.logFilter,"check_use_alternative_cooling>diffToBeUsed=$diffToBeUsed,currentTemp ($currentTemp) > desiredCoolTemp in schedule ($desiredCoolTemp), switching $thermostat to ${state?.lastThermostatMode} mode",            
					settings.detailedNotif, get_LOG_INFO(),true)
				if (state?.lastThermostatMode) { // by default, set it to cool
					thermostat.cool()                
				} else {                
					restore_thermostat_mode()  // set the thermostat back to the mode it was before using alternative cooling           
				}                    
			}    
		} /* end if alternative_cooling efficient */            
	} /* end if settings.doNotUseHumTableFlag */
	return false    
} 


def installed() {
	state?.closedVentsCount= 0
	state?.openVentsCount=0
	state?.totalVents=0
	state?.ratioClosedVents=0
	state?.activeZones=[]
	state?.avgTempDiff= 0.0
	initialize()
}

def updated() {
	unsubscribe()
	try {
		unschedule()
	} catch (e) {	
		traceEvent(settings.logFilter,"updated>exception $e while calling unschedule()",settings.detailedNotif, get_LOG_ERROR())
	}
	initialize()
	// when updated, save the current thermostat modes for restoring them later
	if (!state?.lastThermostatMode) {
		state?.lastThermostatMode= thermostat.latestValue("thermostatMode")    
		state?.lastThermostatFanMode= thermostat.latestValue("thermostatFanMode")   
	}				        
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
	String currentHVACMode = thermostat.currentThermostatMode.toString()

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
		traceEvent(settings.logFilter,"Motion at home in ${roomName},occupiedMotionTimestamp=${state[key]}",settings.detailedNotif, get_LOG_INFO())
		if (state?.setPresentOrAway == 'Away') {
			set_main_tstat_to_AwayOrPresent('present')
		}        
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

private void restore_thermostat_mode() {

	if (state?.lastThermostatMode) {
		if (state?.lastThermostatMode == 'cool') {
			thermostat.cool()
		} else if (state?.lastThermostatMode.contains('heat')) {
			thermostat.heat()
		} else if (state?.lastThermostatMode  == 'auto') {
			thermostat.auto()
		} else if (state?.lastThermostatMode  == 'off') {
			thermostat.off()
		}            
		traceEvent(settings.logFilter, "thermostat ${thermostat}'s mode is now set back to ${state?.lastThermostatMode}",settings.detailedNotif, get_LOG_INFO(),true)
		state?.lastThermostatMode=""        
	}        
	if (state?.lastThermostatFanMode) {
		if (state?.lastThermostatFanMode == 'on') {
			thermostat.fanOn()
		} else if (state?.lastThermostatFanMode  == 'auto') {
			thermostat.fanAuto()
		} else if (state?.lastThermostatFanMode  == 'off') {
			thermostat.fanOff()
		} else if (state?.lastThermostatFanMode  == 'circulate') {
			thermostat.fanCirculate()
		}            
		traceEvent(settings.logFilter, "thermostat ${thermostat}'s fan mode is now set back to ${state?.lastThermostatFanMode}", settings.detailedNotif, get_LOG_INFO(),true)
		state?.lastThermostatFanMode="" 
	}        
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
	state.setPresentOrAway=''
	state.programSetTime = ""
	state.programSetTimestamp = null
	state.operatingState=""
	state?.lastThermostatMode=""        
	state?.lastThermostatFanMode=""        
    
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
		def key = "occupiedMotionCounter${indiceRoom}"       
		state[key]=0	 // initalize the motion counter to zero		                
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
	subscribe(location, "mode", rescheduleIfNeeded)
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
		traceEvent(settings.logFilter, "rescheduleIfNeeded>scheduling takeAction in ${delay} minutes..", settings.detailedNotif,get_LOG_INFO())
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
    
	traceEvent(settings.logFilter,"setZoneSettings>setVentSettingsFlag=$setVentSettingsFlag,setAdjustmentTempFlag=$setAdjustmentTempFlag" +
		",setAdjustmentOutdoorTempFlag=$setAdjustmentOutdoorTempFlag,setAdjustmentFanFlag=$setAdjustmentFanFlag",settings.detailedNotif)
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

	/* Poll or refresh the thermostat to get latest values */
	if  (thermostat.hasCapability("Polling")) {
		try {        
			thermostat.poll()
		} catch (e) {
			traceEvent(settings.logFilter,"setZoneSettings>not able to do a poll() on ${thermostat}, exception ${e}", settings.detailedNotif, get_LOG_ERROR())
		}                    
	}  else if  (thermostat.hasCapability("Refresh")) {
		try {        
			thermostat.refresh()
		} catch (e) {
			traceEvent(settings.logFilter,"setZoneSettings>not able to do a refresh() on ${thermostat}, exception ${e}",settings.detailedNotif, get_LOG_ERROR())
		}                    
	}                    

	if ((outTempSensor) && ((outTempSensor.hasCapability("Refresh")) || (outTempSensor.hasCapability("Polling")))) {

		// do a refresh to get latest temp value
		try {        
			outTempSensor.refresh()
		} catch (e) {
			traceEvent(settings.logFilter,"setZoneSettings>not able to do a refresh() on ${outTempSensor}, exception ${e}",settings.detailedNotif, get_LOG_INFO())
		}                    
	}
	def ventSwitchesOn = []

	def setVentSettings = (setVentSettingsFlag) ?: false
	def adjustmentOutdoorTempFlag = (setAdjustmentOutdoorTempFlag)?: false
	def adjustmentTempFlag = (setAdjustmentTempFlag)?: false
	def adjustmentFanFlag = (setAdjustmentFanFlag)?: false
    
	String nowInLocalTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
	for (int i = 1;((i <= settings.schedulesCount) && (i <= get_MAX_SCHEDULES())); i++) {
        
		def key = "selectedMode$i"
		def selectedModes = settings[key]
		key = "scheduleName$i"
		def scheduleName = settings[key]

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
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName} added + 1 day, new endTime=${endTimeToday.time}",settings.detailedNotif)            

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
			if (adjustmentFanFlag) {                
				set_fan_mode(i)
			}   
			adjust_thermostat_setpoint_in_zone(i)
		        
			key = "useEvaporativeCoolerFlag${i}"                
			def useAlternativeCooling = (settings[key]) ?: false
			if (useAlternativeCooling) {
				traceEvent(settings.logFilter,"setZoneSettings>about to call check_use_alternative_cooling()",settings.detailedNotif)
				// save the current thermostat modes for restoring them later
				if (!state?.lastThermostatMode) {
					state?.lastThermostatMode= thermostat.latestValue("thermostatMode")    
					state?.lastThermostatFanMode= thermostat.latestValue("thermostatFanMode")   
				}				        
				check_use_alternative_cooling(i)
			} else {
				if (evaporativeCoolerSwitch) {
					evaporativeCoolerSwitch.off() // Turn off the alternative cooling for the running schedule 
					restore_thermostat_mode()
				}        
			}            
			if (setVentSettings) {
				ventSwitchesZoneSet= adjust_vent_settings_in_zone(i)
				traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},list of Vents turned 'on'= ${ventSwitchesZoneSet}",settings.detailedNotif)
			}
 			ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet              
			state.lastScheduleName = scheduleName
			state?.lastStartTime = startTimeToday.time
		}
		else if ((state.lastScheduleName == scheduleName) && (currTime >= startTimeToday.time) && (currTime <= endTimeToday.time) && (IsRightDayForChange(i))) {
			// We're in the middle of a schedule run
        
			traceEvent(settings.logFilter,"setZoneSettings>schedule ${scheduleName},currTime= ${currTime}, current time is OK for execution, we're in the middle of a schedule run",
				settings.detailedNotif)            
			foundSchedule=true
			def setAwayOrPresent = (setAwayOrPresentFlag)?:false
			boolean isResidentPresent=true
            
			if (setAwayOrPresent) {
	            
				isResidentPresent=verify_presence_based_on_motion_in_rooms()
				if (isResidentPresent) {            

					if (state.setPresentOrAway != 'present') {
						set_main_tstat_to_AwayOrPresent('present')
					}
				} else {
					if (state.setPresentOrAway != 'away') {
						set_main_tstat_to_AwayOrPresent('away')
					}                
				}
			}            
			if (adjustmentFanFlag) {                
				// will override the fan settings if required (ex. more Fan Threshold is set)
				set_fan_mode(i)
			}                    
			if (isResidentPresent) {
            
				if (adjustmentTempFlag) {            	
					// adjust the temperature at the thermostat(s) based on indoor temp sensors
					adjust_thermostat_setpoint_in_zone(i)
				}                    
				if (adjustmentOutdoorTempFlag) {            	
					// check the thermsostat mode based on outdoor temp's thresholds (heat, cool) if any set                
					switch_thermostatMode(i) 
					// let's adjust the thermostat's temp & mode settings according to outdoor temperature            
                    
					adjust_tstat_for_more_less_heat_cool(i)
				}                    
                
			}
			key = "useEvaporativeCoolerFlag${i}"                
			def useAlternativeCooling = (settings[key]) ?: false
			if (useAlternativeCooling) {
				traceEvent(settings.logFilter,"setZoneSettings>about to call check_use_alternative_cooling()",settings.detailedNotif)
				check_use_alternative_cooling(i)
			}            
			String operatingState = thermostat.currentThermostatOperatingState           
			if (setVentSettings) {            

				key = "adjustVentsEveryCycleFlag$i"
				def adjustVentSettings = (settings[key]) ?: false
				traceEvent(settings.logFilter,"setZoneSettings>adjustVentsEveryCycleFlag=$adjustVentSettings",settings.detailedNotif)
				// Check the operating State before adjusting the vents again.
				// let's adjust the vent settings according to desired Temp only if thermostat is not idle or was not idle at the last run

				if ((adjustVentSettings) || ((operatingState?.toUpperCase() !='IDLE') ||
					((state?.operatingState.toUpperCase() =='HEATING') || (state?.operatingState.toUpperCase() =='COOLING'))))
				{            
					traceEvent(settings.logFilter,"setZoneSettings>thermostat ${thermostat}'s Operating State is ${operatingState} or was just recently " +
							"${state?.operatingState}, adjusting the vents for schedule ${scheduleName}",settings.detailedNotif, get_LOG_INFO())
					ventSwitchesZoneSet=adjust_vent_settings_in_zone(i)
					ventSwitchesOn = ventSwitchesOn + ventSwitchesZoneSet     
				}   
			                
			}        
			state?.operatingState =operatingState            
		} else {
			traceEvent(settings.logFilter,"setZoneSettings>No schedule applicable at this time ${nowInLocalTime}",settings.detailedNotif, get_LOG_INFO())
		}

	} /* end for */
    
	if ((setVentSettings) && ((ventSwitchesOn !=[]) || (initialScheduleSetup))) {
		traceEvent(settings.logFilter,"setZoneSettings>list of Vents turned on= ${ventSwitchesOn}",settings.detailedNotif)
		turn_off_all_other_vents(ventSwitchesOn)
	}
	if (!foundSchedule) {
		if (evaporativeCoolerSwitch) {
			evaporativeCoolerSwitch.off() // Turn off the alternative cooling for the running schedule 
			restore_thermostat_mode()
		}
		traceEvent(settings.logFilter,"setZoneSettings>No schedule applicable at this time ${nowInLocalTime}",settings.detailedNotif, get_LOG_INFO())
	} 
        
}


private def isRoomOccupied(sensor, indiceRoom) {
	def key ="occupiedMotionOccNeeded${indiceRoom}"
	def nbMotionNeeded = (settings[key]) ?: 1
	key = "roomName$indiceRoom"
	def roomName = settings[key]

  	if (location.mode == "Night") { 
		// Rooms are considered occupied when the ST hello mode is "Night"  
		traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, ST hello mode ($location.mode) == Night",settings.detailedNotif,
			get_LOG_INFO())        
		return nbMotionNeeded
	} 
    
	if (thermostat) {
		try {    
			String currentProgName = thermostat.currentSetClimate
			if (currentProgName?.toUpperCase().contains('SLEEP')) { 
				traceEvent(settings.logFilter,"isRoomOccupied>room ${roomName} is considered occupied, ecobee ($currentProgName) == Sleep",settings.detailedNotif, get_LOG_INFO())
				// Rooms are considered occupied when the ecobee program is set to 'SLEEP'    
				return nbMotionNeeded
			} 
		} catch (any) {
			traceEvent(settings.logFilter,"isRoomOccupied>not an ecobee thermostat, continue",settings.detailedNotif)
		}        
	}    
  

	key = "residentsQuietThreshold$indiceRoom"
	def threshold = (settings[key]) ?: 15 // By default, the delay is 15 minutes 

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

private def set_main_tstat_to_AwayOrPresent(mode) {

	try {
		if  (mode == 'away') {
			thermostat.away()
		} else if (mode == 'present') {	
			thermostat.present()
		}
            
		traceEvent(settings.logFilter,"set main thermostat ${thermostat} to ${mode} mode based on motion in all rooms" ,settings.detailedNotif, get_LOG_INFO(),true)
		state?.setPresentOrAway=mode    // set a state for further checking later
	 	state?.programSetTime = new Date().format("yyyy-MM-dd HH:mm", location.timeZone)
 		state?.programSetTimestamp = now()
	}    
	catch (e) {
		traceEvent(settings.logFilter,"set_tstat_to_AwayOrPresent>not able to set thermostat ${thermostat} to ${mode} mode (exception $e)",true, get_LOG_ERROR(),true)
	}

}

private def getSensorTempForAverage(indiceRoom, typeSensor='tempSensor') {
	def key 
	def currentTemp=null
	    
	if (typeSensor == 'tempSensor') {
		key = "tempSensor$indiceRoom"
	} else {
		key = "roomTstat$indiceRoom"
	}
	def tempSensor = settings[key]
	if (tempSensor != null) {
		traceEvent(settings.logFilter,"getTempSensorForAverage>found sensor ${tempSensor}",settings.detailedNotif)
		if (tempSensor.hasCapability("Refresh") || (tempSensor.hasCapability("Polling"))) {
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

private def setRoomTstatSettings(indiceSchedule,indiceZone, indiceRoom) {

	def scale = getTemperatureScale()
	float desiredHeat, desiredCool
	boolean setClimate = false
	def key = "zoneName$indiceZone"
	def zoneName = settings[key]

	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key = "givenClimate$indiceSchedule"
	def climateName = settings[key]

	key = "roomTstat$indiceRoom"
	def roomTstat = settings[key]

	key = "roomName$indiceRoom"
	def roomName = settings[key]

	traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings at ${roomTstat}",settings.detailedNotif)
	String mode = thermostat?.currentThermostatMode.toString() // get the mode at the main thermostat
	if ((climateName) && (roomTstat?.hasCommand("setClimate"))) {
		try {
			roomTstat?.setClimate("", climateName)
			setClimate = true
		} catch (any) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},not able to set climate ${climateName} at the thermostat ${roomTstat}",
				settings.detailedNotif, get_LOG_WARN())
		}                
	}
	if (mode.contains('heat')) {
		try {    
			roomTstat.heat()
		} catch (any) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
				true, get_LOG_WARN(),true)
			return            
		}                
		if (!setClimate) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings",settings.detailedNotif)
			key = "desiredHeatTemp$indiceSchedule"
			def heatTemp = settings[key]
			if (!heatTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif)
				desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			roomTstat.setHeatingSetpoint(desiredHeat)
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredHeat=${desiredHeat}",
				settings.detailedNotif, get_LOG_INFO(),true)                
		}
	} else if (mode == 'cool') {
		try {    
			roomTstat.cool()
		} catch (any) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
				true, get_LOG_WARN(),true)
			return            
		}                
		if (!setClimate) {
			traceEvent(settings.logFilter,"ecobeeSetZoneWithSchedule>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings",settings.detailedNotif)
			key = "desiredCoolTemp$indiceSchedule"
			def coolTemp = settings[key]
			if (!coolTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif)
				desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
			} else {
            
				desiredCool = coolTemp.toFloat()
			}
			roomTstat.setCoolingSetpoint(desiredCool)
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName}, ${roomTstat}'s desiredCool=${desiredCool}",settings.detailedNotif,
				get_LOG_INFO(),true)            
		}
	} else if (mode == 'auto') {
		try {    
			roomTstat.auto()
		} catch (any) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName},in room ${roomName},not able to set ${mode} mode at the thermostat ${roomTstat}",
				true, get_LOG_WARN(),true)
		}                
		if (!setClimate) {
			traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply zone's temp settings",settings.detailedNotif)
			key = "desiredHeatTemp$indiceSchedule"
			def heatTemp = settings[key]
			if (!heatTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default heat settings",settings.detailedNotif,
					get_LOG_INFO())
				desiredHeat = (scale=='C') ? 21:72				// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			roomTstat.setHeatingSetpoint(desiredHeat)
			key = "desiredCoolTemp$indiceSchedule"
			def coolTemp = settings[key]
			if (!coolTemp) {
				traceEvent(settings.logFilter,"setRoomTstatSettings>schedule ${scheduleName}, in room ${roomName},about to apply default cool settings",settings.detailedNotif,
					get_LOG_INFO(),true)
				desiredCool = (scale=='C') ? 23:75				// by default, 23C/75F is the target cool temp
			} else {
            
				desiredCool = coolTemp.toFloat()
			}
			roomTstat.setCoolingSetpoint(desiredCool)
			traceEvent(settings.logFilter,"schedule ${scheduleName},in room ${roomName},${roomTstat}'s desiredHeat=${desiredHeat},desiredCool=${desiredCool}",
				settings.detailedNotif, get_LOG_INFO(),true)
		}

	}
}

private def setAllRoomTstatsSettings(indiceSchedule,indiceZone) {
	boolean foundRoomTstat = false
	def	key= "scheduleName$indiceSchedule"
	def scheduleName = settings[key]
	key = "includedRooms$indiceZone"
	def rooms = settings[key]
	for (room in rooms) {

		def roomDetails=room.split(':')
		def indiceRoom = roomDetails[0]
		def roomName = roomDetails[1]
		key = "needOccupiedFlag$indiceRoom"
		def needOccupied = (settings[key]) ?: false
		key = "roomTstat$indiceRoom"
		def roomTstat = settings[key]

		if (!roomTstat) {
			continue
		}
		traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},found a room Tstat ${roomTstat}, needOccupied=${needOccupied} in room ${roomName}, indiceRoom=${indiceRoom}",
			settings.detailedNotif)
		foundRoomTstat = true
		if (needOccupied) {

			key = "motionSensor$indiceRoom"
			def motionSensor = settings[key]
			if (motionSensor != null) {

				if (isRoomOccupied(motionSensor, indiceRoom)) {
					traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},for occupied room ${roomName},about to call setRoomTstatSettings ",
						settings.detailedNotif, get_LOG_INFO())                    
					setRoomTstatSettings(indiceSchedule,indiceZone, indiceRoom)
				} else {
					traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},room ${roomName} not occupied,skipping it", settings.detailedNotif,
						get_LOG_INFO())                
				}
			}
		} else {
			traceEvent(settings.logFilter,"setAllRoomTstatsSettings>schedule ${scheduleName},for room ${roomName},about to call setRoomTstatSettings ",
				settings.detailedNotif)            
			setRoomTstatSettings(indiceSchedule,indiceZone, indiceRoom)
		}
	}
	return foundRoomTstat
}

private def getAllTempsForAverage(indiceZone) {
	def tempAtSensor
	def adjustmentBasedOnContact=(settings.setTempAdjustmentContactFlag)?:false

	def indoorTemps = []
	def key = "includedRooms$indiceZone"
	def rooms = settings[key]
	for (room in rooms) {

		def roomDetails=room.split(':')
		def indiceRoom = roomDetails[0]
		def roomName = roomDetails[1]

		key = "needOccupiedFlag$indiceRoom"
		def needOccupied = (settings[key]) ?: false
		traceEvent(settings.logFilter,"getAllTempsForAverage>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
			settings.detailedNotif)        
		if (adjustmentBasedOnContact) { 
			key = "contactSensor$indiceRoom"
			def contactSensor = settings[key]
			if (contactSensor != null) {
				def contactState = contactSensor.currentState("contact")
				if (contactState.value == "open") {
					continue  // do not use the temp inside the room as the associated contact is open
				}
			}
		}            
		if (needOccupied) {

			key = "motionSensor$indiceRoom"
			def motionSensor = settings[key]
			if (motionSensor != null) {

				if (isRoomOccupied(motionSensor, indiceRoom)) {

					tempAtSensor = getSensorTempForAverage(indiceRoom)
					if (tempAtSensor != null) {
						indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
						traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}",
							settings.detailedNotif)
					}
					tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
					if (tempAtSensor != null) {
						indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
						traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} due to occupied room ${roomName} based on ${motionSensor}",
							settings.detailedNotif)                        
					}
				}
			}

		} else {

			tempAtSensor = getSensorTempForAverage(indiceRoom)
			if (tempAtSensor != null) {
				traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} in room ${roomName}",settings.detailedNotif)
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
			}
			tempAtSensor = getSensorTempForAverage(indiceRoom,'roomTstat')
			if (tempAtSensor != null) {
				indoorTemps = indoorTemps + tempAtSensor.toFloat().round(1)
 				traceEvent(settings.logFilter,"getAllTempsForAverage>added ${tempAtSensor.toString()} in room ${roomName}",settings.detailedNotif)
			}

		}
	} /* end for */
	return indoorTemps

}


private def set_fan_mode(indiceSchedule, overrideThreshold=false, overrideValue=null) {

	def key = "fanMode$indiceSchedule"
	def fanMode = settings[key]
	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	if (fanMode == null) {
		return     
	}

	key = "fanModeForThresholdOnlyFlag${indiceSchedule}"
	def fanModeForThresholdOnlyFlag = settings[key]

	def fanModeForThresholdOnly = (fanModeForThresholdOnlyFlag) ?: false
	if ((fanModeForThresholdOnly) && (!overrideThreshold)) {
    
		if (outTempSensor == null) {
			return     
		}

		key = "moreFanThreshold$indiceSchedule"
		def moreFanThreshold = settings[key]
		traceEvent(settings.logFilter,"set_fan_mode>fanModeForThresholdOnly=$fanModForThresholdOnly,morefanThreshold=$moreFanThreshold",settings.detailedNotif)
		if (moreFanThreshold == null) {
			return     
		}
		float outdoorTemp = outTempSensor?.currentTemperature.toFloat().round(1)
        
		if (outdoorTemp < moreFanThreshold.toFloat()) {
			fanMode='off'	// fan mode should be set then at 'off'			
		}
	}    

	if (overrideValue != null) {
 		fanMode=overrideValue    
	}  
    
	/* Poll or refresh the thermostat to get latest values */
	if  (thermostat.hasCapability("Polling")) {
		try {        
			thermostat.poll()
		} catch (e) {
			traceEvent(settings.logFilter,"set_fan_mode>not able to do a poll() on ${thermostat}, exception ${e}", settings.detailedNotif, get_LOG_ERROR())
		}                    
	}  else if  (thermostat.hasCapability("Refresh")) {
		try {        
			thermostat.refresh()
		} catch (e) {
			traceEvent(settings.logFilter,"set_fan_mode>not able to do a refresh() on ${thermostat}, exception ${e}",settings.detailedNotif, get_LOG_ERROR())
		}                    
	}                    
    
	def currentFanMode=thermostat.latestValue("thermostatFanMode")
	if ((fanMode == currentFanMode) || ((fanMode=='off') && (currentFanMode=='auto'))) {
		traceEvent(settings.logFilter,"set_fan_mode>schedule ${scheduleName},fan already in $fanMode at thermostat ${thermostat}, exiting...",
			settings.detailedNotif)        
		return
	}    

	try {
		if (fanMode=='auto') {
			thermostat.fanAuto()        
		}
		if (fanMode=='off') {
			thermostat.fanOff()        
		}
		if (fanMode=='on') {
			thermostat.fanOn()        
		}
		if (fanMode=='circulate') {
			thermostat.fanCirculate()        
		}
        
		traceEvent(settings.logFilter,"schedule ${scheduleName},set fan mode to ${fanMode} at thermostat ${thermostat} as requested",settings.detailedNotif, get_LOG_INFO(),true)
	} catch (e) {
		traceEvent(settings.logFilter,"set_fan_mode>schedule ${scheduleName},not able to set fan mode to ${fanMode} (exception $e) at thermostat ${thermostat}",
			true, get_LOG_ERROR())        
	}        
}



private def switch_thermostatMode(indiceSchedule) {

	if (outTempSensor == null) {
		return     
	}
    
	float outdoorTemp = outTempSensor.currentTemperature.toFloat().round(1)

	def key = "heatModeThreshold$indiceSchedule"
	def heatModeThreshold = settings[key]
	key = "coolModeThreshold$indiceSchedule"
	def coolModeThreshold = settings[key]
    
	if ((heatModeThreshold == null) && (coolModeThreshold ==null)) {
		traceEvent(settings.logFilter,"switch_thermostatMode>no adjustment variables set, exiting",settings.detailedNotif)
		return
	}        
	String currentMode = thermostat.currentThermostatMode.toString()
	def currentHeatPoint = thermostat.currentHeatingSetpoint
	def currentCoolPoint = thermostat.currentCoolingSetpoint
	traceEvent(settings.logFilter,"switch_thermostatMode>currentMode=$currentMode, outdoor temperature=$outdoorTemp, coolTempThreshold=$coolTempThreshold, heatTempThreshold=$heatTempThreshold",
		settings.detailedNotif)    
	if ((heatModeThreshold != null) && (outdoorTemp < heatModeThreshold?.toFloat())) {
		if (currentMode != "heat") {
			def newMode = "heat"
			thermostat.setThermostatMode(newMode)
			traceEvent(settings.logFilter,"switch_thermostatMode>thermostat mode set to $newMode",settings.detailedNotif,get_LOG_INFO(),true)    
			state.scheduleHeatSetpoint=currentHeatPoint      // Set for later processing in adjust_more_less_heat_cool()     
		}
	} else if ((coolModeThreshold != null) && (outdoorTemp > coolModeThreshold?.toFloat())) {
		if (currentMode != "cool") {
			def newMode = "cool"
			thermostat.setThermostatMode(newMode)
			traceEvent(settings.logFilter,"switch_thermostatMode>thermostat mode set to $newMode",settings.detailedNotifget_LOG_INFO(),true)    
			state.scheduleCoolSetpoint=currentCoolPoint      // Set for later processing in adjust_more_less_heat_cool() ,     
		}
	} else if ((currentMode != "auto") && (currentMode != "off")) {
			def newMode = "auto"
			thermostat.setThermostatMode(newMode)
			traceEvent(settings.logFilter,"switch_thermostatMode>thermostat mode set to $newMode",settings.detailedNotif,get_LOG_INFO(),true)    
	}    

}
   


private def adjust_tstat_for_more_less_heat_cool(indiceSchedule) {
	def scale = getTemperatureScale()
	def key = "setRoomThermostatsOnlyFlag$indiceSchedule"
	def setRoomThermostatsOnlyFlag = settings[key]
	def setRoomThermostatsOnly = (setRoomThermostatsOnlyFlag) ?: false
	key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	if (setRoomThermostatsOnly) {
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName},all room Tstats set and setRoomThermostatsOnlyFlag= true,exiting",
			settings.detailedNotif)            
		return				    
	}    

	if (outTempSensor == null) {
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>no outdoor temp sensor set, exiting",settings.detailedNotif)    
		return     
	}
	
	key = "moreHeatThreshold$indiceSchedule"
	def moreHeatThreshold = settings[key]
	key = "moreCoolThreshold$indiceSchedule"
	def moreCoolThreshold = settings[key]
	key = "heatModeThreshold$indiceSchedule"
	def heatModeThreshold = settings[key]
	key = "coolModeThreshold$indiceSchedule"
	def coolModeThreshold = settings[key]


	if ((moreHeatThreshold == null) && (moreCoolThreshold ==null) && 
		(heatModeThreshold == null) && (coolModeThreshold ==null)) {
		traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>no adjustment variables set, exiting",settings.detailedNotif)
		return
	}
	
	float outdoorTemp = outTempSensor?.currentTemperature.toFloat().round(1)
	String currentMode = thermostat.currentThermostatMode.toString()
	float currentHeatPoint = thermostat.currentHeatingSetpoint.toFloat().round(1)
	float currentCoolPoint = thermostat.currentCoolingSetpoint.toFloat().round(1)
	float targetTstatTemp    
	traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>currentMode=$currentMode,outdoorTemp=$outdoorTemp,moreCoolThreshold=$moreCoolThreshold,  moreHeatThreshold=$moreHeatThreshold," +
		"coolModeThreshold=$coolModeThreshold,heatModeThreshold=$heatModeThreshold,currentHeatSetpoint=$currentHeatPoint,currentCoolSetpoint=$currentCoolPoint",
		settings.detailedNotif)                
	key = "givenMaxTempDiff$indiceSchedule"
	def givenMaxTempDiff = settings[key]
	def input_max_temp_diff = (givenMaxTempDiff!=null) ?givenMaxTempDiff: (scale=='C')? 2: 5 // 2C/5F temp differential is applied by default

	float max_temp_diff = input_max_temp_diff.toFloat().round(1)
	if (currentMode in ['heat', 'emergency heat']) {
		if ((moreHeatThreshold != null) && (outdoorTemp <= moreHeatThreshold?.toFloat()))  {
			targetTstatTemp = (currentHeatPoint + max_temp_diff).round(1)
			float temp_diff = (state?.scheduleHeatSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for more heat",
					settings.detailedNotif)                    
				targetTstatTemp = (state?.scheduleHeatSetpoint  + max_temp_diff).round(1)
			}
			traceEvent(settings.logFilter,"heating setPoint now= ${targetTstatTemp}, outdoorTemp <=${moreHeatThreshold}",settings.detailedNotif,get_LOG_INFO(),true)
			thermostat.setHeatingSetpoint(targetTstatTemp)
		} else if ((heatModeThreshold != null) && (outdoorTemp >= heatModeThreshold?.toFloat())) {
        	
			targetTstatTemp = (currentHeatPoint - max_temp_diff).round(1)
			float temp_diff = (state?.scheduleHeatSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for heat mode",
					settings.detailedNotif)                
				targetTstatTemp = (state?.scheduleHeatSetpoint  - max_temp_diff).round(1)
			}
			thermostat.setHeatingSetpoint(targetTstatTemp)
			traceEvent(settings.logFilter,"heating setPoint now= ${targetTstatTemp}, outdoorTemp >=${heatModeThreshold}", settings.detailedNotif,get_LOG_INFO(),true)
        
		} else {
			switch_thermostatMode(indiceSchedule)        
		}        
	}
	if (currentMode== 'cool') {
    
		if ((moreCoolThreshold != null) && (outdoorTemp >= moreCoolThreshold?.toFloat())) {
			targetTstatTemp = (currentCoolPoint - max_temp_diff).round(1)
			float temp_diff = (state?.scheduleCoolSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for more cool",
					settings.detailedNotif)                
				targetTstatTemp = (state?.scheduleCoolSetpoint  - max_temp_diff).round(1)
			}
			thermostat.setCoolingSetpoint(targetTstatTemp)
			traceEvent(settings.logFitler,"cooling setPoint now= ${targetTstatTemp}, outdoorTemp >=${moreCoolThreshold}",settings.detailedNotif,get_LOG_INFO(),true)
		} else if ((coolModeThreshold!=null) && (outdoorTemp <= coolModeThreshold?.toFloat())) {
			targetTstatTemp = (currentCoolPoint + max_temp_diff).round(1)
			float temp_diff = (state?.scheduleCoolSetpoint - targetTstatTemp).toFloat().round(1)
			if (temp_diff.abs() > max_temp_diff) {
				traceEvent(settings.logFilter,"adjust_tstat_for_more_less_heat_cool>schedule ${scheduleName}:max_temp_diff= ${max_temp_diff},temp_diff=${temp_diff},too much adjustment for cool mode",
					settings.detailedNotif)                
				targetTstatTemp = (state?.scheduleCoolSetpoint  + max_temp_diff).round(1)
			}
			thermostat.setCoolingSetpoint(targetTstatTemp)
			traceEvent(settings.logFilter,"cooling setPoint now= ${targetTstatTemp}, outdoorTemp <=${coolModeThreshold}", settings.detailedNotif,get_LOG_INFO(),true)
		} else {
        
			switch_thermostatMode(indiceSchedule)        
		}        
        
	} 
    // Check if auto mode needs to be switched to 'heat' or 'cool' based on thresholds
	if (currentMode== 'auto') {
		switch_thermostatMode(indiceSchedule)        
	}
}


private def adjust_thermostat_setpoint_in_zone(indiceSchedule) {
	float MIN_SETPOINT_ADJUSTMENT_IN_CELSIUS=0.5
	float MIN_SETPOINT_ADJUSTMENT_IN_FARENHEITS=1
	float desiredHeat, desiredCool, avg_indoor_temp
	def scale = getTemperatureScale()
	boolean setClimate=false
    
	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key = "includedZones$indiceSchedule"
	def zones = settings[key]
	key = "setRoomThermostatsOnlyFlag$indiceSchedule"
	def setRoomThermostatsOnlyFlag = settings[key]
	def setRoomThermostatsOnly = (setRoomThermostatsOnlyFlag) ?: false
	def indoor_all_zones_temps=[]
	state?.activeZones = zones // save the zones for the dashboard                

	traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}: zones= ${zones}",settings.detailedNotif)
	def adjustmentTempFlag = (setAdjustmentTempFlag)?: false

	for (zone in zones) {

		def zoneDetails=zone.split(':')
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>zone=${zone}: zoneDetails= ${zoneDetails}", settings.detailedNotif)
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		setAllRoomTstatsSettings(indiceSchedule,indiceZone) 
		if (setRoomThermostatsOnly) { // Does not want to set the main thermostat, only the room ones
			traceEvent(settings.logFilter,"schedule ${scheduleName},zone ${zoneName}: all room Tstats set and setRoomThermostatsOnlyFlag= true, continue...",
				settings.detailedNotif)            
		} else if (adjustmentTempFlag) {

			def indoorTemps = getAllTempsForAverage(indiceZone)
			indoor_all_zones_temps = indoor_all_zones_temps + indoorTemps
		}
	}
	if (setRoomThermostatsOnly) {
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},all room Tstats set and setRoomThermostatsOnlyFlag= true,exiting",
			settings.detailedNotif)        
		return				    
	}    
	//	Now will do the right temp calculation based on all temp sensors to apply the desired temp settings at the main Tstat correctly

	float currentTemp = thermostat?.currentTemperature.toFloat().round(1)
	String mode = thermostat?.currentThermostatMode.toString()
	if (indoor_all_zones_temps != [] ) {
		def adjustmentType= (settings.adjustmentTempMethod)?: "avg"    
		if (adjustmentType == "min") {
			avg_indoor_temp = (indoor_all_zones_temps.min()).round(1)
		} else if (adjustmentType == "max") {
			avg_indoor_temp = (indoor_all_zones_temps.max()).round(1)
		} else if (adjustmentType == "med") {
			float maxTemp=indoor_all_zones_temps.max()
			float minTemp=indoor_all_zones_temps.min()
			avg_indoor_temp = ((maxTemp + minTemp)/2).round(1)
		} else {        
			avg_indoor_temp = (indoor_all_zones_temps.sum() / indoor_all_zones_temps.size()).round(1)
		}            
	} else {
		avg_indoor_temp = currentTemp
	}
	traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},method=${settings.adjustmentTempMethod},all temps collected from sensors=${indoor_all_zones_temps}",
		settings.detailedNotif)    

	float temp_diff = (avg_indoor_temp - currentTemp).round(1)
	traceEvent(settings.logFilter,"schedule ${scheduleName}:avg temp= ${avg_indoor_temp},main Tstat's currentTemp= ${currentTemp},temp adjustment=${temp_diff.abs()}",
		settings.detailedNotif,get_LOG_INFO(),true)
	key = "givenMaxTempDiff$indiceSchedule"
	def givenMaxTempDiff = settings[key]
	def input_max_temp_diff = (givenMaxTempDiff!=null) ?givenMaxTempDiff: (scale=='C')? 2: 5 // 2C/5F temp differential is applied by default

	float max_temp_diff = input_max_temp_diff.toFloat().round(1)

	key = "givenMaxFanDiff$indiceSchedule"
	def givenMaxFanDiff = settings[key]
    
	def adjustmentFanFlag = (setAdjustmentFanFlag)?: false
	def input_max_fan_diff = (givenMaxFanDiff!=null) ?givenMaxFanDiff: (scale=='C')? 2: 5 // 2C/5F temp differential is applied by default for the fan diff
	float max_fan_diff = input_max_fan_diff.toFloat().round(1)
    
	if (adjustmentFanFlag) {
		// Adjust the fan mode if avg temp differential in zone is greater than max_fan_diff set in schedule
		if (( max_fan_diff>0) && (temp_diff.abs() >= max_fan_diff)) {
			traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff.abs()} > ${max_fan_diff} :adjusting fan mode as temp differential in zone is too big",
				settings.detailedNotif,get_LOG_INFO(),true)
				// set fan mode with overrideThreshold=true
			set_fan_mode(indiceSchedule, true)          
		} else if (temp_diff.abs() < max_fan_diff) {
			traceEvent(settings.logFilter,"schedule ${scheduleName},avg_temp_diff=${temp_diff.abs()} < ${max_fan_diff} :adjusting fan mode to auto as temp differential is small",
				settings.detailedNotif,get_LOG_INFO(),true)
			set_fan_mode(indiceSchedule, true, 'auto')     // set fan mode to auto as the temp diff is smaller than the differential allowed    
		}                
	}
     
	float min_setpoint_adjustment = (scale=='C') ? MIN_SETPOINT_ADJUSTMENT_IN_CELSIUS:MIN_SETPOINT_ADJUSTMENT_IN_FARENHEITS
	if ((adjustmentTempFlag) && (scheduleName == state.lastScheduleName) && (temp_diff.abs() < min_setpoint_adjustment)) {  // adjust the temp only if temp diff is significant
		traceEvent(settings.logFilter,"Temperature adjustment (${temp_diff}) between sensors is small, skipping it and exiting",settings.detailedNotif,
			get_LOG_INFO())
		return
	}                

	key = "givenClimate$indiceSchedule"
	def climateName = settings[key]
	if ((climateName) && (thermostat.hasCommand("setClimate"))) {
		try {
			thermostat?.setClimate("", climateName)
			setClimate=true            
			thermostat.refresh() // to get the latest setpoints               
		} catch (any) {
			traceEvent(settings.logFilter,"schedule ${scheduleName},not able to set climate ${climateName} at the thermostat(s) ${thermostat}", true, get_LOG_ERROR(),true)
		}                
	}        
	if (mode in ['heat','auto','emergency heat']) {
		if (setClimate) {
			desiredHeat = thermostat.currentHeatingSetpoint
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredHeat=${desiredHeat}",
				settings.detailedNotif)            
		} else {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for heatingSetpoint",settings.detailedNotif)
			key = "desiredHeatTemp$indiceSchedule"
			def heatTemp = settings[key]
			if (!heatTemp) {
				traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}:about to apply default heat settings",settings.detailedNotif)
				desiredHeat = (scale=='C') ? 21:72 					// by default, 21C/72F is the target heat temp
			} else {
				desiredHeat = heatTemp.toFloat()
			}
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredHeat=${desiredHeat}",settings.detailedNotif)
		} 
		temp_diff = (temp_diff < (0-max_temp_diff)) ? -(max_temp_diff):(temp_diff >max_temp_diff) ?max_temp_diff:temp_diff // determine the temp_diff based on max_temp_diff
		float targetTstatTemp = (desiredHeat - temp_diff).round(1)
		thermostat?.setHeatingSetpoint(targetTstatTemp)
		traceEvent(settings.logFilter,"schedule ${scheduleName},in zones=${zones},heating setPoint now =${targetTstatTemp},adjusted by avg temp diff (${temp_diff.abs()}) between all temp sensors in zone",
			settings.detailedNotif, get_LOG_INFO(),true)
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>lastScheduleName run=${state.lastScheduleName}, current heating baseline=${state?.scheduleHeatSetpoint}",settings.detailedNotif)
		if ((scheduleName != state.lastScheduleName || (!state?.scheduleHeatSetpoint))) {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>saving a new heating baseline of $desiredHeat for schedule=$scheduleName, lastScheduleName=${state.lastScheduleName}",settings.detailedNotif)
			state?.scheduleHeatSetpoint=desiredHeat  // save the desiredHeat in state variable for the current schedule
		}        
        
          
	}
        
	if (mode in ['cool','auto']) {

		if (setClimate) {
			desiredCool = thermostat.currentCoolingSetpoint
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},according to climateName ${climateName}, desiredCool=${desiredCool}",
				settings.detailedNotif)            
		} else {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName}:no climate to be applied for coolingSetpoint",settings.detailedNotif)
			key = "desiredCoolTemp$indiceSchedule"
			def coolTemp = settings[key]
			if (!coolTemp) {
				traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},about to apply default cool settings", settings.detailedNotif)
				desiredCool = (scale=='C') ? 23:75					// by default, 23C/75F is the target cool temp
			} else {
            
				desiredCool = coolTemp.toFloat()
			}
            
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>schedule ${scheduleName},desiredCool=${desiredCool}",settings.detailedNotif)
		} 
		temp_diff = (temp_diff < (0-max_temp_diff)) ? -(max_temp_diff):(temp_diff >max_temp_diff) ?max_temp_diff:temp_diff // determine the temp_diff based on max_temp_diff
		float targetTstatTemp = (desiredCool - temp_diff).round(1)
		thermostat?.setCoolingSetpoint(targetTstatTemp)
		traceEvent(settings.logFilter,"schedule ${scheduleName},in zones=${zones},cooling setPoint now =${targetTstatTemp},adjusted by avg temp diff (${temp_diff.abs()}) between all temp sensors in zone",
			settings.detailedNotif, get_LOG_INFO(),true)
		traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>lastScheduleName run=${state.lastScheduleName}, current cooling baseline=${state?.scheduleCoolSetpoint}",settings.detailedNotif)
		if ((scheduleName != state.lastScheduleName || (!state?.scheduleCoolSetpoint))) {
			traceEvent(settings.logFilter,"adjust_thermostat_setpoint_in_zone>saving a new cooling baseline of $desiredCool for schedule $scheduleName, lastScheduleName=${state.lastScheduleName}",settings.detailedNotif)
			state?.scheduleCoolSetpoint=desiredCool  // save the desiredCool in state variable for the current schedule
		}        
	}

}


private def adjust_vent_settings_in_zone(indiceSchedule) {
	def MIN_OPEN_LEVEL_IN_ZONE=(minVentLevelInZone!=null)?((minVentLevelInZone>=0 && minVentLevelInZone <100)?minVentLevelInZone:10):10
	float desiredTemp, avg_indoor_temp, avg_temp_diff, total_temp_diff=0, total_temp_in_vents=0,median
	def indiceRoom
	boolean closedAllVentsInZone=true
	int nbVents=0,nbRooms=0,total_level_vents=0
	def switchLevel  
	def ventSwitchesOnSet=[]
	def fullyCloseVents = (fullyCloseVentsFlag) ?: false
	def adjustmentBasedOnContact=(settings.setVentAdjustmentContactFlag)?:false


	def key = "scheduleName$indiceSchedule"
	def scheduleName = settings[key]

	key= "openVentsFanOnlyFlag$indiceSchedule"
	def openVentsWhenFanOnly = (settings[key])?:false
	String operatingState = thermostat?.currentThermostatOperatingState           

	if (openVentsWhenFanOnly && (operatingState.toUpperCase().contains("FAN ONLY"))) { 
 		// If fan only and the corresponding flag is true, then set all vents to 100% and finish the processing
		traceEvent(settings.logFilter,"${scheduleName}:set all vents to 100% in fan only mode,exiting",
		 	settings.detailedNotif, get_LOG_INFO(),true)
 		open_all_vents()
		return ventSwitchesOnSet         
	}

	key = "includedZones$indiceSchedule"
	def zones = settings[key]
	def indoor_all_zones_temps=[]
  
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}: zones= ${zones}",settings.detailedNotif)
	key = "setRoomThermostatsOnlyFlag$indiceSchedule"
	def setRoomThermostatsOnlyFlag = settings[key]
	def setRoomThermostatsOnly = (setRoomThermostatsOnlyFlag) ?: false
	if (setRoomThermostatsOnly) {
		traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}:all room Tstats set and setRoomThermostatsOnlyFlag= true,exiting",
			settings.detailedNotif)        
		return ventSwitchesOnSet			    
	}    
	int openVentsCount=0    
	String mode = thermostat?.currentThermostatMode.toString()
	float currentTempAtTstat = thermostat?.currentTemperature.toFloat().round(1)
	if (mode.contains('heat')) {
		desiredTemp = thermostat.currentHeatingSetpoint.toFloat().round(1) 
	} else if (mode=='cool') {    
		desiredTemp = thermostat.currentCoolingSetpoint.toFloat().round(1) 
	} else if (mode=='auto') {    
		median = (thermostat?.currentCoolingSetpoint + thermostat?.currentHeatingSetpoint).toFloat()
		median= (median)? (median/2).round(1): (scale=='C')?72:21
		if (currentTempAtTstat > median) {
			desiredTemp =thermostat.currentCoolingSetpoint.toFloat().round(1)            
		} else {
			desiredTemp =thermostat.currentHeatingSetpoint.toFloat().round(1)                 
		}                        
		if (currentTempAtTstat > median) {
			desiredTemp =thermostat.currentCoolingSetpoint.toFloat().round(1) 
		} else {
			desiredTemp =thermostat.currentHeatingSetpoint.toFloat().round(1)                     
		}                        
	} else {
		desiredTemp = thermostat?.currentHeatingSetpoint
		desiredTemp = (desiredTemp)? desiredTemp.toFloat().round(1): (scale=='C')?72:21
	}    
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, desiredTemp=${desiredTemp}",settings.detailedNotif)
	indoor_all_zones_temps.add(currentTempAtTstat)

	key = "setVentLevel${indiceSchedule}"
	def defaultSetLevel = settings[key]
	key = "resetLevelOverrideFlag${indiceSchedule}"	
	boolean resetLevelOverrideFlag = settings[key]
	state?.activeZones=zones
	int min_open_level=100, max_open_level=0    
	float min_temp_in_vents=200, max_temp_in_vents=0    

	for (zone in zones) {

		def zoneDetails=zone.split(':')
		traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>zone=${zone}: zoneDetails= ${zoneDetails}",settings.detailedNotif)
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		def indoorTemps = getAllTempsForAverage(indiceZone)

		if (indoorTemps != [] ) {
			indoor_all_zones_temps = indoor_all_zones_temps + indoorTemps
			            
		} else {
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, no data from temp sensors, exiting",settings.detailedNotif)
		}        
		traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, all temps collected from sensors=${indoorTemps}",settings.detailedNotif)
	} /* end for zones */

	avg_indoor_temp = (indoor_all_zones_temps.sum() / indoor_all_zones_temps.size()).round(1)
	avg_temp_diff = (avg_indoor_temp - desiredTemp).round(1)
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in all zones, all temps collected from sensors=${indoor_all_zones_temps}, avg_indoor_temp=${avg_indoor_temp}, avg_temp_diff=${avg_temp_diff}",
		settings.detailedNotif)    
	for (zone in zones) {
		def zoneDetails=zone.split(':')
		def indiceZone = zoneDetails[0]
		def zoneName = zoneDetails[1]
		key = "includedRooms$indiceZone"
		def rooms = settings[key]
		key  = "desiredHeatDeltaTemp$indiceZone"
		def desiredHeatDelta =  settings[key]           
		key  = "desiredCoolDeltaTemp$indiceZone"
		def desiredCoolDelta =  settings[key]           

		if (mode.contains('heat')) {
			desiredTemp = desiredTemp + (desiredHeatDelta?:0) 
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
		} else if (mode=='cool') {    
			desiredTemp = desiredTemp + (desiredCoolDelta?:0)
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredCoolDelta}",			
				settings.detailedNotif)     
		} else if (mode=='auto') {    
			if (currentTempAtTstat > median) {
				desiredTemp =desiredTemp + (desiredCoolDelta?:0)              
			} else {
				desiredTemp =desiredTemp + (desiredHeatDelta?:0)                    
			}                        
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredHeatDelta=${desiredHeatDelta}",			
				settings.detailedNotif)     
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, zone=${zoneName}, desiredCoolDelta=${desiredCoolDelta}",			
				settings.detailedNotif)     
		} else {
			desiredTemp = desiredTemp +  (desiredHeatDelta?:0)         
		}    
		for (room in rooms) {
			nbRooms++ 		       	
			switchLevel =null	// initially set to null for check later
			def roomDetails=room.split(':')
			indiceRoom = roomDetails[0]
			def roomName = roomDetails[1]           
			if (!roomName) {
				continue
			}
         
			key = "needOccupiedFlag$indiceRoom"
			def needOccupied = (settings[key]) ?: false
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>looping thru all rooms,now room=${roomName},indiceRoom=${indiceRoom}, needOccupied=${needOccupied}",
				settings.detailedNotif)            
			if (needOccupied) {
				key = "motionSensor$indiceRoom"
				def motionSensor = settings[key]
				if (motionSensor != null) {
					if (!isRoomOccupied(motionSensor, indiceRoom)) {
						switchLevel = (fullyCloseVents)? 0 :MIN_OPEN_LEVEL_IN_ZONE // setLevel at a minimum as the room is not occupied.
						traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room ${roomName} is not occupied,vents set to mininum level=${switchLevel}",
							settings.detailedNotif,get_LOG_INFO(),true)                        
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
				total_temp_diff = total_temp_diff + temp_diff_at_sensor 
				traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>thermostat mode = ${mode}, schedule ${scheduleName}, in zone ${zoneName}, room ${roomName}, temp_diff_at_sensor=${temp_diff_at_sensor}, avg_temp_diff=${avg_temp_diff}",
					settings.detailedNotif)                
				if ((mode=='cool') || ((mode=='auto') && (currentTempAtTstat> median))) {
					avg_temp_diff = (avg_temp_diff !=0) ? avg_temp_diff : (0.1)  // to avoid divided by zero exception
					switchLevel = ((temp_diff_at_sensor / avg_temp_diff) * 100).round()
					switchLevel =( switchLevel >=0)?((switchLevel<100)? switchLevel: 100):0
					switchLevel=(temp_diff_at_sensor <=0)? ((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE): ((temp_diff_at_sensor >0) && (avg_temp_diff<0))?100:switchLevel
				} else {
					avg_temp_diff = (avg_temp_diff !=0) ? avg_temp_diff : (-0.1)  // to avoid divided by zero exception
					switchLevel = ((temp_diff_at_sensor / avg_temp_diff) * 100).round()
					switchLevel =( switchLevel >=0)?((switchLevel<100)? switchLevel: 100):0
					switchLevel=(temp_diff_at_sensor >=0)? ((fullyCloseVents)? 0: MIN_OPEN_LEVEL_IN_ZONE): ((temp_diff_at_sensor <0) && (avg_temp_diff>0))?100:switchLevel
				} 
			} 
			traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName}, in zone ${zoneName}, room ${roomName},switchLevel to be set=${switchLevel}",
				settings.detailedNotif)            
			for (int j = 1;(j <= get_MAX_VENTS()); j++)  {
				key = "ventSwitch${j}$indiceRoom"
				def ventSwitch = settings[key]
				if (ventSwitch != null) {
					def temp_in_vent=getTemperatureInVent(ventSwitch)    
					// compile some stats for the dashboard                    
					if (temp_in_vent) {                                   
						min_temp_in_vents=(temp_in_vent < min_temp_in_vents)? temp_in_vent.toFloat().round(1) : min_temp_in_vents
						max_temp_in_vents=(temp_in_vent > max_temp_in_vents)? temp_in_vent.toFloat().round(1) : max_temp_in_vents
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
					traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>in zone=${zoneName},room ${roomName},set ${ventSwitch} at switchLevel =${switchLevel}%",
						settings.detailedNotif)                    
					// compile some stats for the dashboard                    
					min_open_level=(switchLevel < min_open_level)? switchLevel.toInteger() : min_open_level
					max_open_level=(switchLevel > max_open_level)? switchLevel.toInteger() : max_open_level
					total_level_vents=total_level_vents + switchLevel.toInteger()
					if (switchLevel > MIN_OPEN_LEVEL_IN_ZONE) {      // make sure that the vents are set to a minimum level in zone, otherwise they are considered to be closed              
						ventSwitchesOnSet.add(ventSwitch)
						closedAllVentsInZone=false
						openVentsCount++    
					}                        
				}                
			} /* end for ventSwitch */                             
		} /* end for rooms */
	} /* end for zones */

	if ((!fullyCloseVents) && (closedAllVentsInZone) && (nbVents)) {
		    	
		switchLevel=MIN_OPEN_LEVEL_IN_ZONE        
		ventSwitchesOnSet=control_vent_switches_in_zone(indiceSchedule, switchLevel)		    
		traceEvent(settings.logFilter,"schedule ${scheduleName}, safeguards on: set all ventSwitches at ${switchLevel}% to avoid closing all of them",
			settings.detailedNotif, get_LOG_INFO(),true)       
	}    
	traceEvent(settings.logFilter,"adjust_vent_settings_in_zone>schedule ${scheduleName},ventSwitchesOnSet=${ventSwitchesOnSet}",settings.detailedNotif)
	
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
					if ((ventLevel!=null) && (ventLevel < MIN_OPEN_LEVEL_IN_ZONE)) { // below minimum level is considered as closed.
						nbClosedVents++ 
						closedVentsSet.add(ventSwitch)                        
						traceEvent(settings.logFilter,"turn_off_all_other_vents>${ventSwitch}'s level=${ventLevel} is lesser than minimum level ${MIN_OPEN_LEVEL_IN_ZONE}",
							settings.detailedNotif)                        
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
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} using the safeguards as requested to create the desired zone(s)",
				settings.detailedNotif, get_LOG_INFO())
			closedVentsSet.each {
				setVentSwitchLevel(null, it, MIN_OPEN_LEVEL_SMALL)
			}                
		}            
		if (fullyCloseVents) {
			traceEvent(settings.logFilter,"turn_off_all_other_vents>closing ${closedVentsSet} totally as requested to create the desired zone(s)",settings.detailedNotif,
				get_LOG_INFO())            
			closedVentsSet.each {
				setVentSwitchLevel(null, it, 0)
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
	int MAX_LEVEL_DELTA=5
    
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
			ventSwitch.off() // alternate off/on to clear potential obstruction        
			ventSwitch.on()        
			traceEvent(settings.logFilter, "setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it on",
				true, get_LOG_WARN())  
			return false                
		} else {
			ventSwitch.on() // alternate on/off to clear potential obstruction             
			ventSwitch.off()        
			traceEvent(settings.logFilter, "setVentSwitchLevel>not able to set ${ventSwitch} at ${switchLevel} (exception $e), trying to turn it off",
				true, get_LOG_WARN())           
			return false                
		}
	}        
	int currentLevel=ventSwitch.currentValue("level")    
	def currentStatus=ventSwitch.currentValue("switch")    
	if (currentStatus=="obstructed") {
		ventSwitch.off() // alternate off/on to clear obstruction        
		ventSwitch.on()  
		traceEvent(settings.logFilter, "setVentSwitchLevel>error while trying to send setLevel command, switch ${ventSwitch} is obstructed",
			true, get_LOG_WARN())            
		return false   
	}    
	if ((currentLevel < (switchLevel - MAX_LEVEL_DELTA)) ||  (currentLevel > (switchLevel + MAX_LEVEL_DELTA))) {
		traceEvent(settings.logFilter, "setVentSwitchLevel>error not able to set ${ventSwitch} at ${switchLevel}",
			true, get_LOG_WARN())           
		return false           
	}    
	return true    
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

private def cToF(temp) {
	return (temp * 1.8 + 32)
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
	if ((msg.contains("exception")) || (msg.contains("error"))) {
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
	return "ScheduleTstatZones"
}