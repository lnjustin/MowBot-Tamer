/**
 *  MowBot Tamer Rule
 *
 *  Copyright 2021 Justin Leonard
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *  v0.0.1 - Beta

 */
import java.text.SimpleDateFormat
import groovy.transform.Field
import groovy.time.TimeCategory

definition(
    name: "MowBot Tamer Rule",
    parent: "lnjustin:MowBot Tamer Instance",
    namespace: "lnjustin",
    author: "Justin Leonard",
    description: "Robot Mower Director",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"
@Field daysOfWeekList = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]

preferences {
    page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
       
         section (getInterface("header", " MowBot Tamer Rule")) {  
             label title: "Unique Rule Name:", required: true, submitOnChange: true
             input(name:"husqvarnaMowers", type: "device.HusqvarnaAutoMower", title: "Select Husqvarna Mower(s) for which this MowBot Tamer Rule is to apply", required:true, submitOnChange:true, multiple: true)
             input name: "ruleType", type: "enum", title: "Rule Type", width: 4, options: ["Primary Rule", "Backup Rule"], submitOnChange: true, required: true
             paragraph getInterface("note", "A Primary Rule sets the preferred mowing schedule and conditions. If the mowing conditions (e.g., rain) prevent the mower(s) from mowing as much as required by the Primary Rule, a Backup Rule sets a backup mowing schedule and conditions, to mow as needed to meet the requirements of the Primary Rule.")
             
             if (ruleType == "Backup Rule") {
                 input name: "ruleToBackup", type: "enum", title: "Rule to Backup", options: parent.getChildAppNames(), width: 8
             }
             input name: "activationSwitch", type: "capability.switch", title: "Activate/De-activate Switch (on=activated, off=deactivated)", submitOnChange:false, width: 6
             input name: "isDeactivated", type: "bool", title: "Manually Deactivate Rule (Overrides Switch)?", defaultValue: false, width: 6
         }
        section (getInterface("header", " Mowing Schedule")) { 
             input name:"daysOfWeek", type: "enum", title: "Days of the Week on which to Start Mowing", options:daysOfWeekList, required:true, multiple:true, width: 12
             paragraph getInterface("note", "Example: A Starting Day on Saturday, with a Start Time of Sunset and an End Time of Sunrise will mow from Sunset on Saturday, through the night, until Sunrise on Sunday.")
             input name: "startTime", type: "enum", width: 4, title: "Mowing Window Start Time", options: ["Sunrise", "Sunset", "Certain Time"], submitOnChange: true, required: true
             if (startTime == "Certain Time") {
                 input name: "startTimeValue", type: "time", title: "Certain Start Time", width: 8, submitOnChange: true
             }
            else if (startTime == "Sunrise" || startTime == "Sunset") {
                input name: "startTimeOffset", type: "number", title: "Offset (Mins)", width: 8, submitOnChange: true
            }
             input name: "endTime", type: "enum", title: "Mowing Window End Time", options: ["Sunrise", "Sunset", "Certain Time"], submitOnChange: true, width: 4, required: true
             if (endTime == "Certain Time") {
                 input name: "endTimeValue", type: "time", title: "Certain End Time", width: 8, submitOnChange: true
             }
            else if (endTime == "Sunrise" || endTime == "Sunset") {
                input name: "endTimeOffset", type: "number", title: "Offset (Mins)", width: 8, submitOnChange: true
            }
            if (ruleType == "Primary Rule") {
                 input name: "durationType", type: "enum", title: "Mowing Duration", options: ["Full Mowing Window", "Certain Duration"], submitOnChange: true, width: 4, required: true
                 if (durationType == "Certain Duration") {
                     input name: "duration", type: "number", title: "Minutes to Mow", width: 4, required: true
                 }
             }
             else if (ruleType == "Backup Rule") {
                 paragraph getInterface("note", "If the mower(s) are each unable to mow for the Primary Rule's whole Mowing Duration, this Backup Rule will inherit the deficit as the Backup Rule's Mowing Duration.")
             }
             paragraph getInterface("note", "This Rule commands the mower(s) to mow by sending a resume schedule command to the mower(s). So make sure the native schedule at least encompasses the Mowing Window specified in this Rule.")

         }
        
         section (getInterface("header", " Park Mower(s) When...")) {  
             paragraph getInterface("boldText", "Mower(s) will park (or remain parked) during times when ANY of the condition(s) selected below are met. If the Mowing Window is still open when ALL of the condition(s) are no longer met, the mower(s) will resume mowing.")
             input name: "parkWhenGrassWet", type: "bool", title: getInterface("highlightedInput", "Grass is Wet?"), defaultValue: false, submitOnChange:true
             if (parkWhenGrassWet == true) {
                  input name: "leafWetnessSensor", type: "device.EcowittRFSensor", title: "Ecowitt Leaf Wetness Sensor(s)", width: leafWetnessSensor ? 4 : 12, submitOnChange: true, multiple: true
                 if (leafWetnessSensor) {
                     input name: "leafWetnessThreshold", type: "number", title: "Threshold Value", width: 4
                 }
                  input name: "humidityMeasurement", type: "capability.relativeHumidityMeasurement", title: "Humidity or Soil Moisture Sensor(s)", width: humidityMeasurement ? 4 : 12, submitOnChange: true, multiple: true
                  if (humidityMeasurement) {
                     input name: "humidityThreshold", type: "number", title: "Threshold Value", width: 4
                  }
                  input name: "waterSensor", type: "capability.waterSensor", title: "Water/Rain Sensor", width: 12, submitOnChange: true, multiple: true
                  input name: "irrigationValves", type: "capability.valve", title: "Irrigation Valve(s)", multiple: true, submitOnChange:true, width: irrigationValves ? 4 : 12 
                  if (irrigationValves) {
                     input name: "irrigationWetDuration", type: "number", title: "Duration (mins) for which to consider grass wet after irrigation event", width: 4
                     input name: "irrigationWetDuringDay", type: "bool", title: "Daytime Duration?", defaultValue: false, width: 4
                  }
                  input name: "openWeatherDevice", type: "device.OpenWeatherMap-AlertsWeatherDriver", title: "Open Weather Map Device", submitOnChange: true, width: openWeatherDevice ? 4 : 12
                  if (openWeatherDevice) {
                     input name: "weatherWetDuration", type: "number", title: "Duration (mins) for which to consider grass wet after weather event", width: 4
                     input name: "weatherWetDuringDay", type: "bool", title: "Duration only when daytime?", defaultValue: false, width: 4
                     // input name: "weatherWetDuringSun", type: "bool", title: "Duration only when Sunny or Partly Sunny Sky?", defaultValue: false, width: 3
                  }
                  paragraph getInterface("note", "Open Weather Map Device will detect grass is wet upon condition code attribute changing to thunderstorm, drizzle, rain, thunderstorm, or snow.")

                  // TO DO: option to proactively park if rain is forecasted as imminent (requires pulling own hourly data from OWM - very often...)
              }
                    
             input name: "parkWhenTempHot", type: "bool", title: getInterface("highlightedInput", "Temperature is Too Hot?"), defaultValue: false, submitOnChange:true
             if (parkWhenTempHot == true) {
                 paragraph getInterface("note", "Mower(s) will park when the temperature meets or exceeds the temperature threshold.")
                 input name: "tempSensor", type: "capability.temperatureMeasurement", title: "Temperature Sensor", submitOnChange:false, width: 4
                 input name: "tempThreshold", type: "number", title: "Temperature Threshold", submitOnChange:false, width: 4
             }
         /*
             input name: "parkWhenMotion", type: "bool", title: getInterface("highlightedInput", "Motion is Consistently Detected?"), defaultValue: false, submitOnChange:true
             if (parkWhenMotion == true) {
                 paragraph getInterface("note", "Mower(s) will park when motion events have been consistently detected for at least the duration threshold, with motion events being consistent if they occur within the Consistency Threshold of one another.")
                 input name: "motionSensors", type: "capability.motionSensor", title: "Motion Sensor(s)", submitOnChange:false, width: 4, multiple: true
                 input name: "motionDurationThreshold", type: "number", title: "Duration Threshold (mins)", submitOnChange:false, width: 4
                 input name: "motionConsistencyThreshold", type: "number", title: "Consistency Threshold (secs)", submitOnChange:false, width: 4 
             }
        */
             input name: "parkWhenPresenceArrivesLeaves", type: "bool", title: getInterface("highlightedInput", "Presence Sensor Arrives/Leaves?"), defaultValue: false, submitOnChange:true
             if (parkWhenPresenceArrivesLeaves == true) {
                 paragraph getInterface("note", "Mower(s) will park when ANY of the selected Presence Sensors are Present/Not Present.")
                 input name: "presenceSensors", type: "capability.presenceSensor", title: "Presence Sensor(s)", submitOnChange:false, width: 4, multiple: true
                 input name: "presencePresentAbsent", type: "enum", title: "Park When Sensor is.", options: ["Present", "Not Present"], width: 4
                 
             }
             input name: "parkWhenSwitchOnOff", type: "bool", title: getInterface("highlightedInput", "Switch is On/Off?"), defaultValue: false, submitOnChange:true
                if (parkWhenSwitchOnOff) {
                    input name: "parkSwitches", type: "capability.switch", title: "Switch(es)", submitOnChange:false, width: 4, multiple: true
                    input name: "switchOnOff", type: "enum", title: "Park When Switch Is", options: ["on", "off"], width: 4
                 }
             }
             section (getInterface("header", " Pause Mower(s) When...")) {  
                paragraph getInterface("boldText", "Mower(s) will temporarily pause in place when ANY of the condition(s) below are met, and resume mowing when ALL of the condition(s) are no longer met.")
                input name: "pauseWhenMotion", type: "bool", title: getInterface("highlightedInput", "Motion is Detected?"), defaultValue: false, submitOnChange:true
                if (pauseWhenMotion == true) {
                     input name: "pause_motionSensors", type: "capability.motionSensor", title: "Motion Sensor(s)", submitOnChange:false, width: 4, multiple: true
                }
                input name: "pauseWhenOpenCloseSensor", type: "bool", title: getInterface("highlightedInput", "Contact Sensor is Opened/Closed?"), defaultValue: false, submitOnChange:true
                 if (pauseWhenOpenCloseSensor) {
                     input name: "pauseContactSensors", type: "capability.contactSensor", title: "Contact Sensor(s)", submitOnChange:false, width: 4, multiple: true
                     input name: "pauseSensorOpenClose", type: "enum", title: "Pause when any sensor becomes", options: ["open", "closed"], width: 4
                     input name: "openClosePauseDuration", type: "number", title: "Pause Duration (Seconds) on Sensor Event", submitOnChange:false, width: 4
                 }
                input name: "pauseWhenPresenceArrivesLeaves", type: "bool", title: getInterface("highlightedInput", "Presence Sensor Arrives/Leaves?"), defaultValue: false, submitOnChange:true
                if (parkWhenPresenceArrivesLeaves == true) {
                     input name: "pause_presenceSensors", type: "capability.presenceSensor", title: "Presence Sensor(s)", submitOnChange:false, width: 4, multiple: true
                     paragraph getInterface("note", "Mower(s) will pause when ANY of the selected Presence Sensors arrive.")
                    input name: "presencePauseDuration", type: "number", title: "Pause Duration (Seconds) on Presence Arrival", submitOnChange:false, width: 4
                } 
                input name: "pauseWhenSwitchOnOff", type: "bool", title: getInterface("highlightedInput", "Switch is Flipped?"), defaultValue: false, submitOnChange:true
                 if (pauseWhenSwitchOnOff) {
                    input name: "pauseSwitches", type: "capability.switch", title: "Switch(es)", submitOnChange:false, width: 4, multiple: true
                    input name: "pause_switchOnOff", type: "enum", title: "Pause While Switch Is", options: ["on", "off"], width: 4
                 }
                input name: "pauseWhenButtonPressed", type: "bool", title: getInterface("highlightedInput", "Button is Pressed?"), defaultValue: false, submitOnChange:true
                 if (pauseWhenButtonPressed) {
                    input name: "pauseButtons", type: "capability.pushableButton", title: "Pushable Button(s)", submitOnChange:false, width: 4, multiple: true
                    input name: "buttonNumber", type: "number", title: "Button Number", width: 4
                    input name: "buttonPauseDuration", type: "number", title: "Pause Duration (Seconds) on Button Press", submitOnChange:false, width: 4
                 }
            }             
            section (getInterface("header", " Dynamic Cutting Height")) {  
                paragraph getInterface("note", " Adjust cutting height dynamically depending on how long it's been since the mower(s) have been able to mow.")
               if (ruleType == "Primary Rule") {                   
                   input name: "dynamicCuttingHeight", type: "bool", title: "Dynamic Cutting Height?", width: 12, submitOnChange: true
                   if (dynamicCuttingHeight) {
                       input name: "cuttingHeight", type: "number", title: "Cutting Height", width: 4
                       paragraph getInterface("note", "Mowing window deemed missed if mower(s) do not mow at least a minimum % of the Mowing Window, between the Primary Rule and any Backup Rule(s)")
                       input name: "minPercentWindow", type: "number", title: "Minimum % of Mowing Window", width: 4
                       input name: "numMissedWindows", type: "number", title: "Number of consecutively missed Mowing Windows that triggers a cutting height increase event", width: 4
                       input name: "numLevelsIncrease", type: "number", title: "Number of Levels to increase Mowing Height Per Event", width: 4
                       input name: "numFulfilledWindows", type: "number", title: "Number of consecutively fulfilled Mowing Windows that triggers a cutting height decrease event", width: 4
                       input name: "numLevelsDecrease", type: "number", title: "Number of Levels to decrease Mowing Height Per Event", width: 4
                   }
               }
               else if (ruleType == "Backup Rule") {
                   paragraph getInterface("note", " Cutting height will follow the Primary Rule.")
               }
            }
            section("") {
                
                footer()
            }
    }
}

def footer() {
    paragraph getInterface("line", "") + '<div style="display: block;margin-left: auto;margin-right: auto;text-align:center">&copy; 2022 Justin Leonard.<br>'
}
    
def installed() {
	initialize()
}

def updated() {
    unschedule()
	unsubscribe()
    state.clear()
    parent.updated()
	initialize()
}

def uninstalled() {
	logDebug "Uninstalled app"
}

def initialize() {
    logDebug("Initializing ${app.label}")
    if (!state.currentWindow) state.currentWindow = [:]  // currentWindow is the mowing window that is currently in progress, if any
    if (!state.todaysWindow) state.todaysWindow = [:]    // todaysWindow is the mwoing window that starts today, if any. It starts today, but may span into tomorrow.
    if (!state.parkConditions) state.parkConditions = [:]
    if (!state.pauseConditions) state.pauseConditions = [:]
    if (!state.consecutiveMissedWindows) state.consecutiveMissedWindows = 0
    if (!state.consecutiveFulfilledWindows) state.consecutiveFulfilledWindows = 0 
    if (settings["husqvarnaMowers"]) {
        if (!state.mowers) state.mowers = [:]
        for (mower in settings["husqvarnaMowers"]) {
            def serial = mower.currentValue("serialNumber")
            state.mowers[serial] = [name: mower.currentValue("name"), timeStartedMowing: null, timeStoppedMowing: null, mowedDurationSoFar: 0]
        }
    }
    subscribe(settings["activationSwitch"], "switch", activationSwitchHandler)    
    if (isDeactivated == true) deActivateRule()
    if (state.activated) {
        subscribeAndSchedule()
        update()
    }    
}

def subscribeAndSchedule() {
    subscribe(settings["husqvarnaMowers"], "mowerActivity", mowerActivityHandler)
  //  subscribe(settings["husqvarnaMowers"], "mowerStatus", mowerStatusHandler)
    
    def notifications = parent.getNotificationTypes()
    if (notifications.errors) {
        subscribe(settings["husqvarnaMowers"], "mowerState", mowerStateHandler)
        subscribe(settings["husqvarnaMowers"], "stuck", stuckHandler)
        subscribe(settings["husqvarnaMowers"], "apiConnected", apiConnectedHandler)
        subscribe(settings["husqvarnaMowers"], "mowerConnected", mowerConnectedHandler)
    }
 

    if (settings["parkWhenGrassWet"] == true) {
        if (settings["leafWetnessSensor"] != null) subscribe(settings["leafWetnessSensor"], "leafWetness", leafWetnessHandler)
        if (settings["humidityMeasurement"] != null) subscribe(settings["humidityMeasurement"], "humidity", humidityHandler)
        if (settings["waterSensor"] != null) subscribe(settings["waterSensor"], "water", waterSensorHandler)
        if (settings["irrigationValves"] != null) subscribe(settings["irrigationValves"], "valve", irrigationValveHandler)
        if (settings["openWeatherDevice"] != null) subscribe(settings["openWeatherDevice"], "condition_code", openWeatherHandler)
    }
    if (settings["parkWhenTempHot"] == true && settings["tempSensor"] != null) subscribe(settings["tempSensor"], "temperature", temperatureHandler)  
   // if (settings["parkWhenMotion"] == true && settings["motionSensors"] != null) subscribe(settings["motionSensors"], "motion", parkOnMotionHandler)     
    if (settings["parkWhenPresenceArrivesLeaves"] == true && settings["presenceSensors"] != null) subscribe(settings["presenceSensors"], "presence", parkOnPresenceHandler)
    if (settings["parkWhenSwitchOnOff"] == true && settings["parkSwitches"] != null) subscribe(settings["parkSwitches"], "switch", parkOnSwitchHandler)
    
    if (settings["pauseWhenMotion"] == true && settings["pause_motionSensors"] != null) subscribe(settings["pause_motionSensors"], "motion", pauseOnMotionHandler)
    if (settings["pauseWhenOpenCloseSensor"] == true && settings["pauseContactSensors"] != null) subscribe(settings["pauseContactSensors"], "contact", pauseOnOpenCloseHandler)
    if (settings["pauseWhenPresenceArrivesLeaves"] == true && settings["pause_presenceSensors"] != null) subscribe(settings["pause_presenceSensors"], "presence", pauseOnPresenceHandler)
    if (settings["pauseWhenSwitchOnOff"] == true && settings["pauseSwitches"] != null) subscribe(settings["pauseSwitches"], "switch", pauseOnSwitchHandler)
    if (settings["pauseWhenButtonPressed"] == true && settings["pauseButtons"] != null) subscribe(settings["pauseButtons"], "pushed", pauseOnButtonHandler)
    
   schedule("01 00 00 ? * *", update)      
}

def update() {
    // Update Mow Day
    def mowDay = false    
    def today = new Date()
    def dayOfWeek = today.format('EEEE') 
    if(settings["daysOfWeek"] && settings["daysOfWeek"].contains(dayOfWeek)) mowDay = true 
    state.isMowDay = mowDay
    
    // Update start and end of the Mowing Window that starts today
    if (state.isMowDay == true) {
        if (startTime == "Certain Time" && startTimeValue != null) state.todaysWindow.start = toDateTime(startTimeValue).getTime()
        else if (startTime == "Sunrise") state.todaysWindow.start = getSunriseAndSunset([sunriseOffset: startTimeOffset ? startTimeOffset : 0]).sunrise.getTime()
        else if (startTime == "Sunset") state.todaysWindow.start =  getSunriseAndSunset([sunsetOffset: startTimeOffset ? startTimeOffset : 0]).sunset.getTime()
        else state.todaysWindow.start = null
        def endToday = null
        if (endTime == "Certain Time" && endTimeValue != null) endToday = toDateTime(endTimeValue).getTime()
        else if (endTime == "Sunrise") endToday = getSunriseAndSunset([sunriseOffset: endTimeOffset ? endTimeOffset : 0]).sunrise.getTime()
        else if (endTime == "Sunset") endToday = getSunriseAndSunset([sunsetOffset: endTimeOffset ? endTimeOffset : 0]).sunset.getTime()
        if (endToday && endToday >= state.todaysWindow.start) state.todaysWindow.end = endToday    // start and end mowing on the same day
        else if (endToday && endToday < state.todaysWindow.start) state.todaysWindow.end = (new Date(endToday) + 1).getTime()  // end mowing tomorrow. Tomorrow's sunrise and sunset closely approximated based on today's sunrise and sunset
        else state.todaysWindow.end = null
    }
    else {
        state.todaysWindow = [:]
    }
    
    // Schedule mowing window that starts today
    if (state.todaysWindow != [:] && isBeforeTime(state.todaysWindow.start)) runOnce(new Date(state.todaysWindow.start), startTodaysMowingWindow)
    else if (state.todaysWindow != [:] && isWithinTime(state.todaysWindow.start, state.todaysWindow.end)) startTodaysMowingWindow()
}

def startTodaysMowingWindow() {
    state.currentWindow = state.todaysWindow
    runOnce(new Date(state.currentWindow.end), handleExpiredMowingWindow)  // schedule handling of mowing window expiration in case mowing not started during mowing window
    mowAll()  // command to start mowing, but will only start mowing if exceptions not met
}

def activationSwitchHandler(evt) {
    def switchValue = evt.value
    if (switchValue == "on" && (isDeactivated == null || isDeactivated == false)) activateRule()
    else if (switchValue == "off") deActivateRule()
}

def mowerActivityHandler(evt) {
    // control mowing based on reported mower activity, rather than based on commands that this app sends, in order to account for any mowing events triggered by the user via the native mowing app (not this app)
    def mower = evt.getDevice()
    def serial = mower.currentValue("serialNumber")
    def activityTime = evt.getDate().getTime()
    def activity = evt.value        
    
    if (state.currentWindow != [:] && isWithinTime(state.currentWindow.start, state.currentWindow.end)) {
      // mowing activity occurred during current mowing window. Attribute to current mowing window and count mowing duration toward current mowing window's duration  
        if (activity == "MOWING") {          
            state.mowers[serial]?.timeStartedMowing = activityTime
            handleMowingStarted()
        }
        else { // TO DO: confirm pause results in NOT_APPLICABLE
            state.mowers[serial]?.timeStoppedMowing = activityTime
            state.mowers[serial]?.mowedDurationSoFar = state.mowers[serial]?.mowedDurationSoFar + (state.mowers[serial]?.timeStoppedMowing - state.mowers[serial]?.timeStartedMowing)
            handleMowingEnded()
        }
    }
    else {
        // mowing started/stopped outside of current mowing window, e.g., manually via the native mowing app. Don't count mowing duration towards the current mowing window's duration.
    }
}

def handleMowingStarted() {
    // handle mowing that is started within the current mowing window
    unschedule(park)
    unschedule(handleExpiredMowingWindow)
    
    // schedule when to park. Scheduling this only after mowing has begun during the current mowing window prevents overlapping scheduling when mowing window starts on one day and ends on the next day. That is, only one mowing start call needs to be scheduled at a time and only one mowing end call needs to be scheduled at a time, even if the mowing window straddles midnight
    state.mowers.each { serial, mower ->        
        def durationLeftToMow = getRequiredMowingDuration() - mower.mowedDurationSoFar
        def stopByDuration = mower.timeStartedMowing + (durationLeftToMow > 0 ? durationLeftToMow : 0)
        def stopAt = (stopByDuration < state.currentWindow.end) ? stopByDuration : state.currentWindow.end
        runOnce(new Date(stopAt), park, [data: [serial: serial], overwrite: false])
   }       
}
    
def handleMowingEnded() {
    if (state.currentWindow.end && isAfterTime(state.currentWindow.end)) handleExpiredMowingWindow()   // mowing window has ended 
    else if (state.currentWindow != [:] && isWithinTime(state.currentWindow.start, state.currentWindow.end)) {  // mowing stopped in the middle of the current mowing window
        // mowing might be started again later in the current window, but schedule handling of mowing window expiration in case mowing not started again before end of mowing window
        runOnce(new Date(state.currentWindow.end), handleExpiredMowingWindow)  
    }
}

def handleExpiredMowingWindow() {
    // clean up from current mowing window
    // trigger backup rule if needed
    
    def maxDurationLeftToMow = 0
    def requiredDuration = getRequiredMowingDuration() 
    state.mowers.each { serial, mower ->   
        def durationLeftToMow = requiredDuration - mower.mowedDurationSoFar
        if (durationLeftToMow > 0) maxDurationLeftToMow = Math.max(durationLeftToMow, maxDurationLeftToMow)
        
        mower.mowedDurationSoFar = 0 
        mower.timeStartedMowing = null
        mower.timeStoppedMowing = null
            
    }    
    def percentWindowMowed = (1 - (maxDurationLeftToMow / requiredDuration)) * 100
     if (maxDurationLeftToMow > 0) {
        if (parent.hasBackupRule(app.name)) {
            parent.activateBackupRule(app.name, maxDurationLeftToMow) // activate backup rule for this rule (including backups of backups) if mowing duration not met
            parent.notify("${app.name} Backup Rule Triggered: ${msToMins(maxDurationLeftToMow)} mins left to mow", "backupRule")
        }
    }
    if (dynamicCuttingHeight) {           
        def isMissed = percentWindowMowed < settings["minPercentWindow"]
        parent.setHeightStateInPrimaryRule(ruleName, isMissed)
    }
    
    if (isBackupRule()) deActivateRule() // only activate backup rule to backup a single instance of another rule, so deactivate after a single mowing window of the backup rule
    state.backupDuration = 0
}

def incrementMissedWindows() {
    state.consecutiveMissedWindows++   
    state.consecutiveFulfilleddWindows = 0    
    if (state.consecutiveMissedWindows == settings["numMissedWindows"]) {
        incrementCuttingHeight()       
        state.consecutiveMissedWindows = 0  // reset state for detecting next
    }
}

def incrementFulfilledWindows() {
    state.consecutiveMissedWindows = 0    
    state.consecutiveFulfilleddWindows++    
    if (state.consecutiveFulfilledWindows == settings["numFulfilledWindows"]) {
        decrementCuttingHeight()
        state.consecutiveFulfilledWindows = 0  // reset state for detecting next
    }
}

def incrementCuttingHeight() {
    def maxHeight = 9 // max height for currently supported Husqvarna mowers   
    def isIncrementingHeight = false
    def setPoint = 0
    state.mowers.each { serial, mower ->   
        def currentHeight = mower.currentValue("cuttingHeight")
        if (currentHeight < 9) {
            def dynamicHeight = Math.min(maxHeight, currentHeight + settings["numLevelsIncrease"])
            setPoint = Math.max(dynamicHeight, setPoint)
            mower.setCuttingHeight(dynamicHeight)
            isIncrementingHeight = true
        }
    }    
    if (isIncrementingHeight) parent.notify("${app.name} Dynamically Increased Cutting Height up to ${setPoint}", "cuttingHeight")
}

def decrementCuttingHeight() {
    def minHeight = settings["cuttingHeight"]
    def isDecrementingHeight = false
    def setPoint = 10
    state.mowers.each { serial, mower ->   
        def currentHeight = mower.currentValue("cuttingHeight")       
        if (currentHeight > minHeight) {
            def dynamicHeight = Math.max(minHeight, currentHeight - settings["numLevelsDecrease"])
            setPoint = Math.min(dynamicHeight, setPoint)
            mower.setCuttingHeight(dynamicHeight)
            isDecrementingHeight = true
        }
    }   
    if (isDecrementingHeight) parent.notify("${app.name} Dynamically Decreased Cutting Height up to ${setPoint}", "cuttingHeight")
}

def isBackupRule(toRule = null) {
    def isBackup = false
    if (ruleType == "Backup Rule" && toRule != null && toRule == settings["ruleToBackup"]) isBackup = true
    else if (ruleType == "Backup Rule" && toRule == null) isBackup = true
    return isBackup
}

def isPrimaryRule() {
    def isPrimary = false
    if (ruleType == "Primary Rule") isPrimary = true
    return isPrimary
}

def ruleBacksUp() {
    def ruleBacksUp = null
    if (ruleType == "Backup Rule") ruleBacksUp = settings["ruleToBackup"]
    return ruleBacksUp
}

def mowerStatusHandler(evt) {
    def mower = evt.getDevice()
    def statusTime = evt.getDate().getTime()
    def status = evt.value      
}

def mowerStateHandler(evt) {
    def mower = evt.getDevice()
    def mowerName = mower.currentValue("name")
    def stateTime = evt.getDate().getTime()
    def state = evt.value 
    
    if (state == "ERROR" || state == "FATAL_ERROR" || state == "ERROR_AT_POWER_UP") {
         def errorCode = mower.currentValue("errorCode")
         def errorMessage = husqvarnaErrorMap[errorCode]
         def timestamp = mower.currentValue("errorTimeStamp")
         parent.notify("${mowerName} Error: ${errorMessage} @ ${timestamp}", "error")
    }
}

def apiConnectedHandler(evt) {
    def mower = evt.getDevice()
    def mowerName = mower.currentValue("name")
    def timestamp = evt.getDate().getTime()
    def apiConnected = evt.value     
    
    if (apiConnected == "lost") {
         parent.notify("${mowerName} API Connection Lost @ ${timestamp}", "error")
         state.apiConnectionLost[mowerName] = true
    }
    else if (apiConnected == "full" && state.apiConnectionLost && state.apiConnectionLost[mowerName]) {
         parent.notify("${mowerName} API Connection Restored @ ${timestamp}", "error")
        state.apiConnectionLost[mowerName] = false
    }
}

def mowerConnectedHandler(evt) {
    def mower = evt.getDevice()
    def mowerName = mower.currentValue("name")
    def timestamp = evt.getDate().getTime()
    def mowerConnected = evt.value     
    
    if (mowerConnected == "lost") {
         parent.notify("${mowerName} Mower Connection Lost @ ${timestamp}", "error")
         state.mowerConnectionLost[mowerName] = true
    }
    else if (mowerConnected == "full" && state.mowerConnectionLost && state.mowerConnectionLost[mowerName]) {
         parent.notify("${mowerName} Mower Connection Restored @ ${timestamp}", "error")
        state.mowerConnectionLost[mowerName] = false
    }
}

def stuckHandler(evt) {
    def mower = evt.getDevice()
    def mowerName = mower.currentValue("name")
    def stuckTime = evt.getDate().getTime()
    def stuck = evt.value 
    
    if (stuck == true) {
        parent.notify("${mowerName} stuck @ ${timestamp}", "error")
        state.stuck[mowerName] = true
    }
    else if (stuck == false && state.stuck && state.stuck[mowerName]) {
        parent.notify("${mowerName} unstuck @ ${timestamp}", "error")
        state.stuck[mowerName] = false
    }
}

def getRequiredMowingDuration() {
    def durationMS = null
    if (ruleType == "Primary Rule") {
        if(settings["durationType"] == "Certain Duration" && settings["duration"] != null) durationMS = settings["duration"] * 60000
        else if (settings["durationType"] == "Full Mowing Window") durationMS = state.currentWindow.end - state.currentWindow.start
    }
    else if (ruleType == "Backup Rule") {
        durationMS = state.backupDuration
    }
    return durationMS
}

def getRequiredMowingDurationMins() {
    def durationMS = getRequiredMowingDuration()
    return durationMS ? msToMins(durationMS) : null
}
    
def mowAll() {
    if (isAnyParkConditionMet() == false && isAnyPauseConditionMet() == false) {
        logDebug("${app.label} mowAll() executing")
        for (mower in settings["husqvarnaMowers"]) { 
            def isMowing = (mower.currentValue("mowerActivity") == "MOWING" || mower.currentValue("LEAVING") == true) ? true : false
            if (!isMowing) mower.resumeSchedule()
        } 
        parent.notify("${app.label} Mowing Started", "stopStart")
    }
}

def mowOne(serial) {
    if (isAnyParkConditionMet() == false && isAnyPauseConditionMet() == false) {    
        for (mower in settings["husqvarnaMowers"]) { 
            def serialNum = mower.currentValue("serialNumber")
            def isMowing = (mower.currentValue("mowerActivity") == "MOWING" || mower.currentValue("LEAVING") == true) ? true : false
            if (serial == serialNum && !isMowing) {
                logDebug("${app.label} mowOne() executing")
                mower.resumeSchedule()
                parent.notify("${app.label} Mowing Started", "stopStart")
            }
        }
    }    
}


def mow(data) {
    def serial = data.serial
    
    if (isAnyParkConditionMet() == false && isAnyPauseConditionMet() == false) {
        for (mower in settings["husqvarnaMowers"]) { 
            def serialNum = mower.currentValue("serialNumber")
            def isMowing = (mower.currentValue("mowerActivity") == "MOWING" || mower.currentValue("LEAVING") == true) ? true : false
            if (serial == serialNum && !isMowing) {
                logDebug("${app.label} mow(data) executing")
                mower.resumeSchedule()
                parent.notify("${app.label} Mowing Started", "stopStart")
            }
        }
    }
}

def parkAll() {
    for (mower in settings["husqvarnaMowers"]) { 
        def isParked = (mower.currentValue("mowerActivity") == "PARKED_IN_CS" || mower.currentValue("parked") == true) ? true : false
        if (!isParked) mower.parkindefinite()
        parent.notify("${app.label} Mowing Stopped", "stopStart")
    }     
}

def park(data) {
    def serial = data.serial    
    for (mower in settings["husqvarnaMowers"]) { 
        def serialNum = mower.currentValue("serialNumber")
        if (serial == serialNum) {
            def isParked = (mower.currentValue("mowerActivity") == "PARKED_IN_CS" || mower.currentValue("parked") == true) ? true : false
            if (!isParked) {
                mower.parkindefinite()
                parent.notify("${app.label} Mowing Stopped", "stopStart")
            }
        }
    }
}

def pauseAll() {
    for (mower in settings["husqvarnaMowers"]) { 
        if (mower.currentValue("mowerState") != "PAUSED") {
            mower.pause()
            parent.notify("${app.label} Mowing Paused", "stopStart")
        }
    }     
}

def isAnyParkConditionMet() {
    def isMet = false
    for (condition in state.parkConditions) {
        if (condition == true) isMet = true
    }
    return isMet
}

def areMowingConditionsMet() {
    def conditionsMet = false
    if (isAnyParkConditionMet() == false && state.currentWindow != [:] && isWithinTime(state.currentWindow.start, state.currentWindow.end)) conditionsMet = true
    return conditionsMet
}

def handleParkConditionChange() {
    if (isAnyParkConditionMet()) parkAll()
    else if (state.currentWindow != [:] && isWithinTime(state.currentWindow.start, state.currentWindow.end)) {
        // no park conditions met and within current mowing window, so mow if required mowing duration not met
        state.mowers.each { serial, mower ->        
            def durationLeftToMow = getRequiredMowingDuration() - mower.mowedDurationSoFar
            if (durationLeftToMow > 0) mowOne(serial)
        }
    }
}

def temperatureHandler(evt) {
    def tempTime = evt.getDate().getTime()
    def temp = evt.value   
    if (temp >= settings["tempThreshold"]) state.parkConditions.temperature = true
    else state.parkConditions.temperature = false
    handleParkConditionChange()
}

/*
def parkOnMotionHandler(evt) {
    def motionTime = evt.getDate().getTime()
    def motion = evt.value  
    
    handleParkConditionChange()
}
*/

def parkOnPresenceHandler(evt) { 
    def anyMet = false
    for (sensor in settings["presenceSensors"]) {
        if (sensor.currentValue("presence") == settings["presencePresentAbsent"]) anyMet = true
    }
    state.parkConditions.presence = anyMet
    handleParkConditionChange()
}

def parkOnSwitchHandler(evt) {   
    def anyMet = false
    for (sw in settings["parkSwitches"]) {
        if (sw.currentValue("switch") == settings["switchOnOff"]) anyMet = true
    }
    state.parkConditions.switchSensors = anyMet
    handleParkConditionChange()
}

def leafWetnessHandler(evt) {
    def anyMet = false
    for (sensor in settings["leafWetnessSensor"]) {
       def leafWetness = sensor.currentValue("leafWetness")
        if (leafWetness >= settings["leafWetnessThreshold"]) anyMet = true
    }
    state.parkConditions.leafWetness = anyMet
    handleParkConditionChange()
}

def openWeatherHandler(evt) {
    def weatherTime = evt.getDate().getTime()
    def weather = evt.value      
    if (weather == "thunderstorm" || weather == "drizzle" || weather == "rain" || weather == "thunderstorm" || weather == "snow") {
        state.parkConditions.weather = true
        unschedule(delayedWeather)
        handleParkConditionChange()
    }
    else if (state.parkConditions?.weather == null) {
        state.parkConditions.weather = false
        handleParkConditionChange()
    }
    else if (state.parkConditions?.weather != null && state.parkConditions.weather == true) {
      if (settings["weatherWetDuringDay"] == false) runIn(settings["weatherWetDuration"]*60, delayedWeather)
        else if (settings["weatherWetDuringDay"] == true) {
             def dayTime = getSunriseAndSunset()
             def now = new Date()
             def delayedTime = adjustDateByMins(now, settings["weatherWetDuration"])
            if (now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                if (delayedTime.after(dayTime.sunrise) && dayTime.sunset.after(delayedTime)) {
                    // full delay in the daytime, so just schedule for delayedTime
                    runOnce(delayedTime, delayedWeatherd)
                }
                else if (delayedTime.after(dayTime.sunrise) && !dayTime.sunset.after(delayedTime)) {
                    // sun sets before delay ends
                    def partialDelay = getSecondsBetweenDates(now, dayTime.sunset)
                    def delayRemainder = (settings["weatherWetDuration"]*60) - partialDelay
                    Integer delayRemainderMins = Math.round(delayRemainder/60)
                    def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: delayRemainderMins])
                    def offsetSunriseTomorrow = offsetSunriseToday + 1 // approximate tomorrow's sunrise as today's sunrise
                    runOnce(offsetSunriseTomorrow, delayedWeather)                    
                }
            }
            else if (now.after(dayTime.sunrise) && !dayTime.sunset.after(now)) {
                // after sunset already, so full delay will be after sunrise tomorrow
                def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: settings["weatherWetDuration"]])
                def offsetSunriseTomorrow = offsetSunriseToday + 1 // approximate tomorrow's sunrise as today's sunrise
                runOnce(offsetSunriseTomorrow, delayedWeather)
            }
            else if (!now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                // before sunrise, so full delay will be after sunrise today
                def offsetSunrise = getSunriseAndSunset([sunriseOffset: settings["weatherWetDuration"]])
                runOnce(offsetSunrise, delayedWeather)
            }
        }        
    }
}

def delayedWeather() {
    state.parkConditions.weather = false
    handleParkConditionChange()   
}

def humidityHandler(evt) {
    def anyMet = false
    for (sensor in settings["humidityMeasurement"]) {
       def humidity = sensor.currentValue("humidity")
        if (humidity >= settings["humidityThreshold"])  anyMet = true
    }
    state.parkConditions.humidity = anyMet
    handleParkConditionChange()
}

def waterSensorHandler(evt) {
    def anyMet = false
    for (sensor in settings["waterSensor"]) {
       def water = sensor.currentValue("water")
        if (water == "wet") anyMet = true
    }
    state.parkConditions.water = anyMet
    handleParkConditionChange()
}

def irrigationValveHandler(evt) {
    def anyOpen = false
    for (valve in settings["irrigationValves"]) {
       def status = valve.currentValue("valve")
       if (status == "open") anyOpen = true
    }
    
    if (anyOpen == true) {
        state.parkConditions.valve = true
        unschedule(delayedIrrigationValveClosed)
        handleParkConditionChange()
    }
    else if (anyOpen == false && state.parkConditions?.valve == null) {
        state.parkConditions.valve = false
        handleParkConditionChange()
    }
    else if (anyOpen == false && state.parkConditions?.valve != null && state.parkConditions.valve == true) {
        if (settings["irrigationWetDuringDay"] == false) runIn(settings["irrigationWetDuration"]*60, delayedIrrigationValveClosed)
        else if (settings["irrigationWetDuringDay"] == true) {
             def dayTime = getSunriseAndSunset()
             def now = new Date()
             def delayedTime = adjustDateByMins(now, settings["irrigationWetDuration"])
            if (now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                if (delayedTime.after(dayTime.sunrise) && dayTime.sunset.after(delayedTime)) {
                    // full delay in the daytime, so just schedule for delayedTime
                    runOnce(delayedTime, delayedIrrigationValveClosed)
                }
                else if (delayedTime.after(dayTime.sunrise) && !dayTime.sunset.after(delayedTime)) {
                    // sun sets before delay ends
                    def partialDelay = getSecondsBetweenDates(now, dayTime.sunset)
                    def delayRemainder = (settings["irrigationWetDuration"]*60) - partialDelay
                    Integer delayRemainderMins = Math.round(delayRemainder/60)
                    def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: delayRemainderMins])
                    def offsetSunriseTomorrow = offsetSunriseToday + 1 // approximate tomorrow's sunrise as today's sunrise
                    runOnce(offsetSunriseTomorrow, delayedIrrigationValveClosed)                    
                }
            }
            else if (now.after(dayTime.sunrise) && !dayTime.sunset.after(now)) {
                // after sunset already, so full delay will be after sunrise tomorrow
                def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: settings["irrigationWetDuration"]])
                def offsetSunriseTomorrow = offsetSunriseToday + 1 // approximate tomorrow's sunrise as today's sunrise
                runOnce(offsetSunriseTomorrow, delayedIrrigationValveClosed)
            }
            else if (!now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                // before sunrise, so full delay will be after sunrise today
                def offsetSunrise = getSunriseAndSunset([sunriseOffset: settings["irrigationWetDuration"]])
                runOnce(offsetSunrise, delayedIrrigationValveClosed)
            }
        }
    }
    
}

def delayedIrrigationValveClosed() {
    state.parkConditions.valve = false
    handleParkConditionChange()
}

def isAnyPauseConditionMet() {
    def isMet = false
    for (condition in state.pauseConditions) {
        if (condition == true) isMet = true
    }
    return isMet
}

def handlePauseConditionChange() {
    if (isAnyPauseConditionMet()) pauseAll()
    else if (state.currentWindow != [:] && isWithinTime(state.currentWindow.start, state.currentWindow.end)) {
        // no pause conditions met and within current mowing window, so resume mowing
        state.mowers.each { serial, mower ->        
            def durationLeftToMow = getRequiredMowingDuration() - mower.mowedDurationSoFar
            if (durationLeftToMow > 0) mowOne(serial)    // only resume mowing on the mowers that have not mowed the required duration
        }
    }
}

def pauseOnMotionHandler(evt) {
    def anyMet = false
    for (sensor in settings["pause_motionSensors"]) {
        if (sensor.currentValue("motion") == "active") anyMet = true
    }
    state.pauseConditions.motion = anyMet
    handlePauseConditionChange()   
}

def pauseOnOpenCloseHandler(evt) {
    def contact = evt.value
    if (contact == settings["pauseSensorOpenClose"]) {
        unschedule(delayedUnpauseFromContact)
        runIn(settings["openClosePauseDuration"], delayedUnpauseFromContact)  
        state.pauseConditions.contact = true
        handlePauseConditionChange()   
    }
}

def delayedUnpauseFromContact() {
    state.pauseConditions.contact = false
    handlePauseConditionChange()
}

def pauseOnPresenceHandler(evt) {
    def presence = evt.value
    if (presence == "present") {
        unschedule(delayedUnpauseFromArrival)
        runIn(settings["presencePauseDuration"], delayedUnpauseFromArrival)  
        state.pauseConditions.presence = true
        handlePauseConditionChange()   
    }
}

def delayedUnpauseFromArrival() {
    state.pauseConditions.presence = false
    handlePauseConditionChange()    
}

def pauseOnSwitchHandler(evt) {
    def anyMet = false
    for (sensor in settings["pauseSwitches"]) {
        if (sensor.currentValue("switch") == settings["pause_switchOnOff"]) anyMet = true
    }
    state.pauseConditions.switchSensors = anyMet
    handlePauseConditionChange()   
}

def pauseOnButtonHandler(evt) {
    def buttonNum = evt.value
    if (buttonNum == settings["buttonNumber"]) {
        unschedule(delayedUnpauseFromButton)
        runIn(settings["buttonPauseDuration"], delayedUnpauseFromButton)  
        state.pauseConditions.button = true
        handlePauseConditionChange()   
    }
}

def delayedUnpauseFromButton() {
    state.pauseConditions.button = false
    handlePauseConditionChange()    
}

def isWithinTime(time1, time2) {
    def withinTime = false
    
    def windowStart = new Date(time1)
    def windowEnd = new Date(time2)
    if (timeOfDayIsBetween(windowStart, windowEnd, new Date(), location.timeZone)) {
        withinTime = true
    }
    return withinTime
}

def isAfterTime(time) {
    def afterTime = false
    def timeDate = new Date(time)
    def now = new Date()
    if (now.after(timeDate)) {
        afterTime = true
    }
    return afterTime      
}

def isBeforeTime(time) {
    def beforeTime = false
    def timeDate = new Date(time)
    def now = new Date()
    if (timeDate.after(now)) {
        beforeTime = true
    }
    return beforeTime      
}

def activateRule(backupDuration = null) {
    if (isDeactivated == null || isDeactivated == false) {
        state.activated = true
        state.backupDuration = backupDuration
        subscribeAndSchedule()
        update()
    }
}

def deActivateRule() {
    state.activated = false
    unschedule()
	unsubscribe()
    state.backupDuration = null
}

def getSecondsBetweenDates(Date startDate, Date endDate) {
    try {
        def difference = endDate.getTime() - startDate.getTime()
        return Math.round(difference/1000)
    } catch (ex) {
        log.error "getSecondsBetweenDates Exception: ${ex}"
        return 1000
    }
}

def getSecondsBetweenTimes(start, end) {
    try {
        def difference = end - start
        return Math.round(difference/1000)
    } catch (ex) {
        log.error "getSecondsBetweenTimes Exception: ${ex}"
        return 1000
    }
}

def msToMins(ms) {
    return Math.round(ms/60000)    
}

def adjustDateBySecs(Date date, Integer secs) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.setTime(date)
    cal.add(Calendar.SECOND, secs)
    Date newDate = cal.getTime()
    return newDate
}

def adjustDateByMins(Date date, Integer mins) {
    def secs = mins * 60
    return adjustDateBySecs(date, secs)
}

def getOrdinal(num) {
    // get ordinal number for num range 1-30
    def ord = null
    if (num == 1 || num == 21) ord = "st"
    else if (num == 2 || num == 22) ord = "nd"
    else if (num == 3 || num == 23) ord = "rd"
    else ord = "th"
    return ord
}

def logDebug(msg) {
    if (parent.getLogDebugEnabled()) {
		log.debug msg
	}
}
    

def getInterface(type, txt="", link="") {
    switch(type) {
        case "line": 
            return "<hr style='background-color:#555555; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#555555;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "error": 
            return "<div style='color:#ff0000;font-weight: bold;'>${txt}</div>"
            break
        case "note": 
            return "<div style='color:#333333;font-size: small;'>${txt}</div>"
            break
        case "subField":
            return "<div style='color:#000000;background-color:#ededed;'>${txt}</div>"
            break     
        case "subHeader": 
            return "<div style='color:#000000;font-weight: bold;background-color:#ededed;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "highlightedInput": 
            return "<div style='color:#000000;font-weight: bold;background-color:#ededed;border: 0px solid'> ${txt}</div>"
            break
        case "subSection1Start": 
            return "<div style='color:#000000;background-color:#d4d4d4;border: 0px solid'>"
            break
        case "subSection2Start": 
            return "<div style='color:#000000;background-color:#e0e0e0;border: 0px solid'>"
            break
        case "subSectionEnd":
            return "</div>"
            break
        case "boldText":
            return "<b>${txt}</b>"
            break
        case "link":
            return '<a href="' + link + '" target="_blank" style="color:#51ade5">' + txt + '</a>'
            break
    }
} 


@Field husqvarnaErrorMap = [
"0" : "Unexpected error",
"1" : "Outside working area",
"2" : "No loop signal",
"3" : "Wrong loop signal",
"4" : "Loop sensor problem, front",
"5" : "Loop sensor problem, rear",
"6" : "Loop sensor problem, left",
"7" : "Loop sensor problem, right",
"8" : "Wrong PIN code",
"9" : "Trapped",
"10" : "Upside down",
"11" : "Low battery",
"12" : "Empty battery",
"13" : "No drive",
"14" : "Mower lifted",
"15" : "Lifted",
"16" : "Stuck in charging station",
"17" : "Charging station blocked",
"18" : "Collision sensor problem, rear",
"19" : "Collision sensor problem, front",
"20" : "Wheel motor blocked, right",
"21" : "Wheel motor blocked, left",
"22" : "Wheel drive problem, right",
"23" : "Wheel drive problem, left",
"24" : "Cutting system blocked",
"25" : "Cutting system blocked",
"26" : "Invalid sub-device combination",
"27" : "Settings restored",
"28" : "Memory circuit problem",
"29" : "Slope too steep",
"30" : "Charging system problem",
"31" : "STOP button problem",
"32" : "Tilt sensor problem",
"33" : "Mower tilted",
"34" : "Cutting stopped - slope too steep",
"35" : "Wheel motor overloaded, right",
"36" : "Wheel motor overloaded, left",
"37" : "Charging current too high",
"38" : "Electronic problem",
"39" : "Cutting motor problem",
"40" : "Limited cutting height range",
"41" : "Unexpected cutting height adj",
"42" : "Limited cutting height range",
"43" : "Cutting height problem, drive",
"44" : "Cutting height problem, curr",
"45" : "Cutting height problem, dir",
"46" : "Cutting height blocked",
"47" : "Cutting height problem",
"48" : "No response from charger",
"49" : "Ultrasonic problem",
"50" : "Guide 1 not found",
"51" : "Guide 2 not found",
"52" : "Guide 3 not found",
"53" : "GPS navigation problem",
"54" : "Weak GPS signal",
"55" : "Difficult finding home",
"56" : "Guide calibration accomplished",
"57" : "Guide calibration failed",
"58" : "Temporary battery problem",
"59" : "Temporary battery problem",
"60" : "Temporary battery problem",
"61" : "Temporary battery problem",
"62" : "Temporary battery problem",
"63" : "Temporary battery problem",
"64" : "Temporary battery problem",
"65" : "Temporary battery problem",
"66" : "Battery problem",
"67" : "Battery problem",
"68" : "Temporary battery problem",
"69" : "Alarm! Mower switched off",
"70" : "Alarm! Mower stopped",
"71" : "Alarm! Mower lifted",
"72" : "Alarm! Mower tilted",
"73" : "Alarm! Mower in motion",
"74" : "Alarm! Outside geofence",
"75" : "Connection changed",
"76" : "Connection NOT changed",
"77" : "Com board not available",
"78" : "Slipped - Mower has Slipped.Situation not solved with moving pattern",
"79" : "Invalid battery combination - Invalid combination of different battery types.",
"80" : "Cutting system imbalance Warning",
"81" : "Safety function faulty",
"82" : "Wheel motor blocked, rear right",
"83" : "Wheel motor blocked, rear left",
"84" : "Wheel drive problem, rear right",
"85" : "Wheel drive problem, rear left",
"86" : "Wheel motor overloaded, rear right",
"87" : "Wheel motor overloaded, rear left",
"88" : "Angular sensor problem",
"89" : "Invalid system configuration",
"90" : "No power in charging station",
"91" : "Switch cord problem",
"92" : "Work area not valid",
"93" : "No accurate position from satellites",
"94" : "Reference station communication problem",
"95" : "Folding sensor activated",
"96" : "Right brush motor overloaded",
"97" : "Left brush motor overloaded",
"98" : "Ultrasonic Sensor 1 defect",
"99" : "Ultrasonic Sensor 2 defect",
"100" : " Ultrasonic Sensor 3 defect",
"101" : "Ultrasonic Sensor 4 defect",
"102" : "Cutting drive motor 1 defect",
"103" : "Cutting drive motor 2 defect",
"104" : "Cutting drive motor 3 defect",
"105" : "Lift Sensor defect",
"106" : "Collision sensor defect",
"107" : "Docking sensor defect",
"108" : "Folding cutting deck sensor defect",
"109" : "Loop sensor defect",
"110" : "Collision sensor error",
"111" : "No confirmed position",
"112" : "Cutting system major imbalance",
"113" : "Complex working area",
"114" : "Too high discharge current",
"115" : "Too high internal current",
"116" : "High charging power loss",
"117" : "High internal power loss",
"118" : "Charging system problem",
"119" : "Zone generator problem",
"120" : "Internal voltage error",
"121" : "High internal temerature",
"122" : "CAN error",
"123" : "Destination not reachable"]


