/**
 *  MowBot Tamer Instance
 *
 *  Copyright 2022 Justin Leonard
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
 *  v0.0.2 - Bug fixes
 *  v0.0.3 - Delayed handling of window expiration more; More efficient handling of mowing windows
 *  v0.0.4 - Added Companion Device; More control over parking conditions during backup window and during forced mowing; Bug fixes
 */
import java.text.SimpleDateFormat
import groovy.transform.Field
import groovy.time.TimeCategory

definition(
    name: "MowBot Tamer Instance",
    parent: "lnjustin:MowBot Tamer",
    namespace: "lnjustin",
    author: "Justin Leonard",
    description: "Robot Mower Director",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)
        
@Field daysOfWeekList = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
@Field dayOfWeekMap = ["Sunday":"1", "Monday":"2", "Tuesday":"3", "Wednesday":"4", "Thursday":"5", "Friday":"6", "Saturday":"7"] // map to cron expression
@Field months = ["JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"]
@Field monthsMap = [
    "JAN" : 1,
    "FEB" : 2,
    "MAR" : 3,
    "APR" : 4,
    "MAY" : 5,
    "JUN" : 6,
    "JUL" : 7,
    "AUG" : 8,
    "SEP" : 9,
    "OCT" : 10,
    "NOV" : 11,
    "DEC" : 12]
@Field days29 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29]
@Field days30 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30]    
@Field days31 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31]

preferences {
    page name: "instancePage", title: "", install: true, uninstall: true
}

def instancePage() {
    dynamicPage(name: "instancePage") {
         section (getInterface("header", " MowBot Tamer Rule")) {  
             label title: "Unique Rule Name:", required: true, submitOnChange: true
             input(name:"husqvarnaMowers", type: "device.HusqvarnaAutoMower", title: "Select Husqvarna Mower(s) for which this MowBot Tamer Rule is to apply", required:true, submitOnChange:true, multiple: true)
             input name: "pollingInterval", type: "number", title: "Mower Polling Interval (Secs)", required: true
             input name: "trigger", type: "enum", title: "Instance Activation Type", width: 4, options: ["By Date", "By Avg High/Low Temp", "By Switch", "Always"], submitOnChange:true, description: "Specify how to activate the MowBot Tamer Instance."
             if (trigger == "By Date") {
                 paragraph "Trigger MowBot Tamer Instance when the date is between:"
                 input(name:"startMonth", type:"enum", options:months, title: "Start Month", required: true, width: 3)
                 input(name:"startDay", type:"enum", options:getNumDaysInMonth(settings["startMonth"]), title: "Start Date", required: true, width: 3)                            
                 input(name:"endMonth", type:"enum", options:months, title: "End Month", required: true, width: 3)
                 input(name:"endDay", type:"enum", options:getNumDaysInMonth(settings["endMonth"]), title: "End Date", required: true, width: 3)        
             }
             else if (trigger == "By Avg High/Low Temp") {
                 paragraph "Trigger MowBot Tamer Instance when the 7-day average:"
                 input name: "tempHighLow", type: "enum", title: "", options: ["High Temp", "Low Temp"], width: 3
                 input name: "tempDirection", type: "enum", title: "", options: ["Falls Below", "Rises Above"], width: 3
                 input name: "tempThreshold", type: "number", title: "Temp Threshold", width: 3
                 input name: "tempSensor", type: "capability.temperatureMeasurement", title: "Temperature Sensor", submitOnChange:false, width: 4
             }
             else if (trigger == "By Switch") {
                 paragraph "Trigger MowBot Tamer Instance when the MowBot Tamer Device switch is ON:"
             }
             input name: "isDeactivated", type: "bool", title: "Manually Deactivate (Overrides Switch)?", defaultValue: false, width: 6
         }
        section (getInterface("header", " Mowing Schedule")) { 
             input name:"daysOfWeek", type: "enum", title: "Days of the Week on which to Start Mowing", options:daysOfWeekList, required:true, multiple:true, width: 12, submitOnChange: true
            logDebug("days of week: ${daysOfWeek}") 
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
            input name: "durationType", type: "enum", title: "Mowing Duration", options: ["Full Mowing Window", "Certain Duration"], submitOnChange: true, width: 4, required: true
            paragraph getInterface("note", "A Mowing Duration smaller than the duration of the Full Mowing Window will park the mower(s) once that duration of mowing has been reached, even if the Mowing Window has not expired yet.") 
            if (durationType == "Certain Duration") {
                 input name: "duration", type: "number", title: "Minutes to Mow", width: 4, required: true
             }        
         }
        
         section (getInterface("header", " Park Mower(s) When...")) {  
             paragraph getInterface("boldText", "Mower(s) will park (or remain parked) during times when ANY of the condition(s) selected below are met. If the Mowing Window is still open when ALL of the condition(s) are no longer met, the mower(s) will resume mowing.")
             input name: "parkWhenGrassWet", type: "bool", title: getInterface("highlightedInput", "Grass is Wet?"), defaultValue: false, submitOnChange:true, width: 12
             if (parkWhenGrassWet == true) {
                  input name: "leafWetnessSensor", type: "device.EcowittRFSensor", title: "Ecowitt Leaf Wetness Sensor(s)", width: leafWetnessSensor ? 4 : 12, submitOnChange: true, multiple: true
                 if (leafWetnessSensor) {
                     input name: "leafWetnessThreshold", type: "number", title: "Threshold Value", width: 4, required: true
                     input name: "leafWetnessThresholdTimes", type: "number", title: "Required # Consecutive Readings Above/Below Threshold", width: 4, required: true
                 }
                  input name: "humidityMeasurement", type: "capability.relativeHumidityMeasurement", title: "Humidity or Soil Moisture Sensor(s)", width: humidityMeasurement ? 4 : 12, submitOnChange: true, multiple: true
                  if (humidityMeasurement) {
                     input name: "humidityThreshold", type: "number", title: "Threshold Value", width: 4, required: true
                     input name: "humidityThresholdTimes", type: "number", title: "Required # Consecutive Readings Above/Below Threshold", width: 4, required: true
                  }
                  input name: "waterSensor", type: "capability.waterSensor", title: "Water/Rain Sensor", width: 12, submitOnChange: true, multiple: true
                  input name: "irrigationValves", type: "capability.valve", title: "Irrigation Valve(s)", multiple: true, submitOnChange:true, width: irrigationValves ? 4 : 12 
                  if (irrigationValves) {
                     input name: "irrigationWetDuration", type: "number", title: "Duration (mins) for which to consider grass wet after irrigation event", width: 4, required: true
                     input name: "irrigationWetDuringDay", type: "bool", title: "Daytime Duration?", defaultValue: false, width: 4, required: true
                  }
                  input name: "openWeatherDevice", type: "device.OpenWeatherMap-AlertsWeatherDriver", title: "Open Weather Map Device", submitOnChange: true, width: openWeatherDevice ? 4 : 12
                  if (openWeatherDevice) {
                     input name: "weatherWetDuration", type: "number", title: "Duration (mins) for which to consider grass wet after weather event", width: 4, required: true
                     input name: "weatherWetDuringDay", type: "bool", title: "Duration only when daytime?", defaultValue: false, width: 4, required: true
                  }
                  paragraph getInterface("note", "Open Weather Map Device will detect grass is wet upon condition code attribute changing to thunderstorm, drizzle, rain, thunderstorm, or snow.")
                 
                  // TO DO: option to proactively park if rain is forecasted as imminent (requires pulling own hourly data from OWM - very often...)
              }
                    
             input name: "parkWhenTempHot", type: "bool", title: getInterface("highlightedInput", "Temperature is Too Hot?"), defaultValue: false, submitOnChange:true, width: 12
             if (parkWhenTempHot == true) {
                 paragraph getInterface("note", "Mower(s) will park when the temperature meets or exceeds the temperature threshold.")
                 input name: "parkTempSensor", type: "capability.temperatureMeasurement", title: "Temperature Sensor", submitOnChange:false, width: 4, required: true
                 input name: "parkTempThreshold", type: "number", title: "Temperature Threshold", submitOnChange:false, width: 4, required: true
                 input name: "parkTempThresholdTimes", type: "number", title: "Required # Consecutive Readings Above/Below Threshold", width: 4, required: true
             }
             input name: "parkWhenPresenceArrivesLeaves", type: "bool", title: getInterface("highlightedInput", "Presence Sensor Arrives/Leaves?"), defaultValue: false, submitOnChange:true, width: 12
             if (parkWhenPresenceArrivesLeaves == true) {
                 paragraph getInterface("note", "Mower(s) will park when ANY of the selected Presence Sensors are Present/Not Present.")
                 input name: "presenceSensors", type: "capability.presenceSensor", title: "Presence Sensor(s)", submitOnChange:false, width: 4, multiple: true, required: true
                 input name: "presencePresentAbsent", type: "enum", title: "Park When Sensor is.", options: ["Present", "Not Present"], width: 4, required: true
                
             }
             input name: "parkWhenSwitchOnOff", type: "bool", title: getInterface("highlightedInput", "Switch is On/Off?"), defaultValue: false, submitOnChange:true, width: 12
             if (parkWhenSwitchOnOff) {
                input name: "parkSwitches", type: "capability.switch", title: "Switch(es)", submitOnChange:false, width: 4, multiple: true, required: true
                 input name: "switchOnOff", type: "enum", title: "Park When Switch Is", options: ["on", "off"], width: 4, required: true
             }
         }
         section (getInterface("header", " Pause Mower(s) When...")) {  
                paragraph getInterface("boldText", "Mower(s) will temporarily pause in place when ANY of the condition(s) below are met, and resume mowing when ALL of the condition(s) are no longer met.")
                input name: "pauseWhenMotion", type: "bool", title: getInterface("highlightedInput", "Motion is Detected?"), defaultValue: false, submitOnChange:true
                if (pauseWhenMotion == true) {
                     input name: "pause_motionSensors", type: "capability.motionSensor", title: "Motion Sensor(s)", submitOnChange:false, width: 4, multiple: true, required: true
                }
                input name: "pauseWhenOpenCloseSensor", type: "bool", title: getInterface("highlightedInput", "Contact Sensor is Opened/Closed?"), defaultValue: false, submitOnChange:true
                 if (pauseWhenOpenCloseSensor) {
                     input name: "pauseContactSensors", type: "capability.contactSensor", title: "Contact Sensor(s)", submitOnChange:false, width: 4, multiple: true, required: true
                     input name: "pauseSensorOpenClose", type: "enum", title: "Pause when any sensor becomes", options: ["open", "closed"], width: 4, required: true
                     input name: "openClosePauseDuration", type: "number", title: "Pause Duration (Seconds) on Sensor Event", submitOnChange:false, width: 4, required: true
                 }
                input name: "pauseWhenPresenceArrivesLeaves", type: "bool", title: getInterface("highlightedInput", "Presence Sensor Arrives/Leaves?"), defaultValue: false, submitOnChange:true
                if (parkWhenPresenceArrivesLeaves == true) {
                     input name: "pause_presenceSensors", type: "capability.presenceSensor", title: "Presence Sensor(s)", submitOnChange:false, width: 4, multiple: true, required: true
                     paragraph getInterface("note", "Mower(s) will pause when ANY of the selected Presence Sensors arrive.")
                    input name: "presencePauseDuration", type: "number", title: "Pause Duration (Seconds) on Presence Arrival", submitOnChange:false, width: 4, required: true
                } 
                input name: "pauseWhenSwitchOnOff", type: "bool", title: getInterface("highlightedInput", "Switch is Flipped?"), defaultValue: false, submitOnChange:true
                 if (pauseWhenSwitchOnOff) {
                    input name: "pauseSwitches", type: "capability.switch", title: "Switch(es)", submitOnChange:false, width: 4, multiple: true, required: true
                    input name: "pause_switchOnOff", type: "enum", title: "Pause While Switch Is", options: ["on", "off"], width: 4, required: true
                 }
                input name: "pauseWhenButtonPressed", type: "bool", title: getInterface("highlightedInput", "Button is Pressed?"), defaultValue: false, submitOnChange:true
                 if (pauseWhenButtonPressed) {
                    input name: "pauseButtons", type: "capability.pushableButton", title: "Pushable Button(s)", submitOnChange:false, width: 4, multiple: true, required: true
                    input name: "buttonNumber", type: "number", title: "Button Number", width: 4, required: true
                    input name: "buttonPauseDuration", type: "number", title: "Pause Duration (Seconds) on Button Press", submitOnChange:false, width: 4, required: true
                 }
            }   
        
            def sensorOptions = []
            if (parkWhenGrassWet == true) {
                if (leafWetnessSensor) sensorOptions.add("Wet Grass: Leaf Wetness Sensor")
                if (humidityMeasurement) sensorOptions.add("Wet Grass: Humidity or Soil Moisture Sensor")
                if (waterSensor) sensorOptions.add("Wet Grass: Water Sensor")
                if (irrigationValves) sensorOptions.add("Wet Grass: Irrigation")
                if (openWeatherDevice) sensorOptions.add("Wet Grass: Open Weather Device")
            }
            if (parkWhenTempHot == true && parkTempSensor) sensorOptions.add("Temperature Sensor")
            if (parkWhenPresenceArrivesLeaves == true && presenceSensors) sensorOptions.add("Presence Sensor")
            if (parkWhenSwitchOnOff && parkSwitches) sensorOptions.add("Switch(es)")
        
            section (getInterface("header", " Backup Window")) { 
                input name: "addBackupWindow", type: "bool", title: getInterface("highlightedInput", "Add Backup Mowing Window?"), defaultValue: false, width: 12, submitOnChange: true
                paragraph getInterface("note", "If the mower(s) are each unable to mow for the specified duration during a specific occurrence of the primary Mowing Window, the deficit can be made-up during a Backup Mowing Window specified here. The mower(s) will mow during a single occurrence of the Backup Window that occurs closest to the deficient primary Mowing Window.")
                if (addBackupWindow == true) {
                    input name: "backupStartTime", type: "enum", width: 4, title: "Backup Mowing Window Start Time", options: ["Sunrise", "Sunset", "Certain Time"], submitOnChange: true, required: true
                    if (backupStartTime == "Certain Time") {
                        input name: "backupStartTimeValue", type: "time", title: "Certain Start Time", width: 8, submitOnChange: true, required: true
                    }
                    else if (backupStartTime == "Sunrise" || backupStartTime == "Sunset") {
                        input name: "backupStartTimeOffset", type: "number", title: "Offset (Mins)", width: 8, submitOnChange: true
                    }
                     input name: "backupEndTime", type: "enum", title: "Backup Mowing Window End Time", options: ["Sunrise", "Sunset", "Certain Time"], submitOnChange: true, width: 4, required: true
                     if (backupEndTime == "Certain Time") {
                         input name: "backupEndTimeValue", type: "time", title: "Certain End Time", width: 8, submitOnChange: true
                     }
                    else if (backupEndTime == "Sunrise" || backupEndTime == "Sunset") {
                        input name: "backupEndTimeOffset", type: "number", title: "Offset (Mins)", width: 8, submitOnChange: true
                    }                 
                 }

                input name: "backupParkConditionsIgnored", type: "enum", width: 12, multiple: true, title: "Park Conditions to Ignore for Backup Window", options: sensorOptions, submitOnChange: true, required: true
            }
            section (getInterface("header", " Forced Mowing Options")) { 
                paragraph getInterface("note", " Specify options for how to handle mowing that is forced via the native mower app or the Hubitat mower device")  
                input name: "forcedMowingParkConditionsEnforced", type: "enum", width: 12, multiple: true, title: "Park conditions to enforce even when mowing forced", options: sensorOptions, submitOnChange: true, required: true
                paragraph getInterface("note", " By default, all park conditions will be ignored when mowing is forced. Specify exceptions here, to enforce select park conditions even when mowing is forced. For example, still park the mower upon rain, even if mowing has been forced.") 
            }
            section (getInterface("header", " Dynamic Cutting Height")) {  
                paragraph getInterface("note", " Adjust cutting height dynamically depending on how long it's been since the mower(s) have been able to mow.")             
               input name: "dynamicCuttingHeight", type: "bool", title: "Dynamic Cutting Height?", width: 12, submitOnChange: true
               if (dynamicCuttingHeight) {
                   input name: "cuttingHeight", type: "number", title: "Cutting Height", width: 4, required: true
                   paragraph getInterface("note", "Mowing window deemed missed if mower(s) do not mow at least a minimum % of the Mowing Window, between the Primary Rule and any Backup Rule(s)")
                   input name: "minPercentWindow", type: "number", title: "Minimum % of Mowing Window", width: 4, required: true
                   input name: "numMissedWindows", type: "number", title: "Number of consecutively missed Mowing Windows that triggers a cutting height increase event", width: 4, required: true
                   input name: "numLevelsIncrease", type: "number", title: "Number of Levels to increase Mowing Height Per Event", width: 4, required: true
                   input name: "numFulfilledWindows", type: "number", title: "Number of consecutively fulfilled Mowing Windows that triggers a cutting height decrease event", width: 4, required: true
                   input name: "numLevelsDecrease", type: "number", title: "Number of Levels to decrease Mowing Height Per Event", width: 4, required: true
               }
            }
            section (getInterface("header", " Options")) {  
                input name: "resetBackup", type: "bool", title: "Clear Any Pending Backup Window?", width: 12, submitOnChange: false
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
    selectiveUnschedule(true)
	unsubscribe()
    // state.clear()
    if (resetBackup) state.backup = [:]
    state.parkConditions.valve = false
	initialize()
}

def selectiveUnschedule(unscheduleActivation = false) {
    logDebug("selectiveUnschedule called at ${now()}")
    if (unscheduleActivation) {
        unschedule(tempTriggerCheck)
        unschedule(switchHandler)
        unschedule(dateTriggerCheck)
    }    
    unschedule(scheduleMowers)
    unschedule(scheduleMowingWindowStart)
    unschedule(scheduleMowingWindowEnd)
    unschedule(mowingPreCheck)
    unschedule(handleExpiredMowingWindow)
    unschedule(endMowingWindow)
    unschedule(startMowingWindow)
    unschedule(backupWindowPreCheck)
    unschedule(handleExpiredBackupMowingWindow)
    unschedule(park)
    // keep delayed weather and irrigation valve methods scheduled
}

def uninstalled() {
	logDebug "Uninstalled app"
    deleteCompanionDevice()
}

def initialize() {
    logDebug("Initializing ${app.label}")
    
    createCompanionDevice()

    if (!state.currentWindow) state.currentWindow = [:]  // currentWindow is the mowing window that is currently in progress, if any
    if (!state.todaysWindow) state.todaysWindow = [:]    // todaysWindow is the mwoing window that starts today, if any. It starts today, but may span into tomorrow.
    if (!state.parkConditions) state.parkConditions = [:]
    if (!state.pauseConditions) state.pauseConditions = [:]
    if (!state.consecutiveMissedWindows) state.consecutiveMissedWindows = 0
    if (!state.consecutiveFulfilledWindows) state.consecutiveFulfilledWindows = 0 
    if (!state.apiConnectionLost) state.apiConnectionLost = [:]
    if (settings["husqvarnaMowers"]) {
        if (!state.mowers) state.mowers = [:]
        for (mower in settings["husqvarnaMowers"]) {
            def serial = mower.currentValue("serialNumber")
            if (!state.mowers[serial]) state.mowers[serial] = [name: mower.currentValue("name"), timeStartedMowing: null, timeStoppedMowing: null, mowedDurationSoFar: 0, current: [state: null, activity: null], previous: [state: null, activity: null], parkedByApp: false, pausedByApp: false, userForcingMowing: false]
            state.mowers[serial].userForcingMowing = false
            state.mowers[serial].parkedByApp = false
            state.mowers[serial].pausedByApp = false
        }
    }
    if (isDeactivated == null || isDeactivated == false) {
        if (trigger == "By Avg High/Low Temp") {
            subscribe(tempSensor, "temperature", tempSensorHandler)
            schedule("01 00 00 ? ? *", tempTriggerCheck)
            tempTriggerCheck()
        }
        else if (trigger == "By Switch") {
            // nothing to do
        }
        else if (trigger == "By Date") {
            logDebug("Checking Trigger By Date in ${app.label}")
            schedule("01 00 00 ? ? *", dateTriggerCheck)
            dateTriggerCheck()           
        }
        else if (trigger == "Always") state.activated = true
        
    }
    else if (isDeactivated == true) state.activated = false
    
    updateActivationStatus()   
}

def createCompanionDevice()
{
    def child = getChildDevice("MowBotTamerDevice${app.id}")
    if (child == null) {
        String childNetworkID = "MowBotTamerDevice${app.id}"
        child = addChildDevice("lnjustin", "MowBot Tamer Device", childNetworkID, [label:"MowBot Tamer Device ${app.label}", isComponent:true, name:"MowBot Tamer Device"])
    }
}

def deleteCompanionDevice()
{
    deleteChildDevice("MowBotTamerDevice${app.id}")
}

def updateDeviceData(data) {
    def child = getChildDevice("MowBotTamerDevice${app.id}")  
    if (child) child.updateData(data)
}

def updateActivationStatus() {
    logDebug("updateActivationStatus called at ${now()}")
    if (state.activated == true) activate() 
    else if (state.activated == false) deactivate()
}

def tempTriggerCheck() {
    updateAverageTemp()
    def temp = settings["tempHighLow"] == "High Temp" ? state.averageHigh : state.averageLow
    if (settings["tempDirection"] == "Falls Below") {
        if (temp < settings["tempThreshold"] && areExceptionsMet() == false) state.activated = true
        else state.activated = false
    }
    else if (settings["tempDirection"] == "Rises Above") {
        if (temp > settings["tempThreshold"] && areExceptionsMet() == false) state.activated = true
        else state.activated = false       
    }
    updateActivationStatus()
    
}

def tempSensorHandler(evt) {
    def temp = evt.value
    if (state.todaysHigh == null || state.todaysHigh <= temp) state.todaysHigh = temp
    if (state.todaysLow == null || state.todaysLow >= temp) state.todaysLow = temp
}

def updateAverageTemp() {
    if (!state.highList) state.highList = []
    if (!state.lowList) state.lowList = []
    if (state.todaysHigh != null) {
        logDebug("Existing Daily High List: ${state.highList}")
        state.highList.add(0,todaysHigh)
        if (state.highList.size() > 7) state.highList.pop() // only keep 7 days
        logDebug("New Daily High List: ${state.highList}")
        state.averageHigh = getAverageOfList(state.highList)
    }
    if (state.todaysLow != null) {
        logDebug("Existing Daily Low List: ${state.lowList}")
        state.lowList.add(0,todaysLow)
        if (state.lowList.size() > 7) state.lowList.pop() // only keep 7 days
        logDebug("New Daily Low List: ${state.lowList}")
        state.averageLow = getAverageOfList(state.lowList)        
    }
    state.todaysHigh = null
    state.todaysLow = null        
}

def getAverageOfList(list) {
    def sum = 0
    for (element in list) {
        sum = sum + element
    }
    def size = list.size()
    def avg = null
    if (size > 0) avg = sum / size
    return avg
}

def switchHandler(value) {
    logDebug("Switch Handler executing with value ${value}")
    state.activationSwitch = value
    switchTriggerCheck()
}

def switchTriggerCheck() {
    if (state.activationSwitch == "on" && areExceptionsMet() == false) state.activated = true
    else state.activated = false   
    updateActivationStatus()
}

def dateTriggerCheck() {
    if (isTodayWithinTriggerDates() == true && areExceptionsMet() == false) state.activated = true
    else state.activated = false
    updateActivationStatus()
}

def areExceptionsMet() {
    def exceptionsMet = false
    if (isDeactivated == true) exceptionsMet = true
   // if (exceptHolidays == true) 
    return exceptionsMet
}

def isTodayWithinTriggerDates() {
    def withinDates = false
    def today = timeToday(null, location.timeZone)
	def month = today.month+1
	def day = today.date
    
    def sMonth = monthsMap[startMonth]
    def sDay = startDay.toInteger()
    def eMonth = monthsMap[endMonth]
    def eDay = endDay.toInteger()
        
    if ((sMonth != null && sDay != null) && ((month == sMonth && day >= sDay) || month > sMonth))  {
        if ((eMonth != null && eDay != null) && ((month == eMonth && day <= eDay) || month < eMonth)) {
		     withinDates = true
        }
    }
    return withinDates
}

def deactivate() {
    selectiveUnschedule()
	unsubscribe()        
    parkAll() // no known way to delete schedule from mowers, so park indefinitely
}

def activate() {
    subscribe(settings["husqvarnaMowers"], "mowerActivity", mowerActivityHandler)
    
    def notifications = parent.getNotificationTypes()
    if (notifications.errors) {
        subscribe(settings["husqvarnaMowers"], "mowerState", mowerStateHandler)
        subscribe(settings["husqvarnaMowers"], "stuck", stuckHandler)
        subscribe(settings["husqvarnaMowers"], "apiConnected", apiConnectedHandler)
        subscribe(settings["husqvarnaMowers"], "mowerConnected", mowerConnectedHandler)
    }
    
    if (settings["parkWhenGrassWet"] == true) {
        if (settings["irrigationValves"] != null) subscribe(settings["irrigationValves"], "valve", irrigationValveHandler)
        if (settings["openWeatherDevice"] != null) subscribe(settings["openWeatherDevice"], "condition_code", openWeatherHandler)
    }

    scheduleMowers()
    // if primary schedule involves sunrise or sunset, update the schedule each mow day
    if (settings["startTime"] == "Sunrise" || settings["endTime"] == "Sunrise" || settings["startTime"] == "Sunset" || settings["endTime"] == "Sunset") {
        schedule("10 00 00 ? * " + getDayOfWeekCron(settings["daysOfWeek"]), scheduleMowers) // schedule 10 seconds after midnight to give the activation code a chance to run 1 second after midnight
    }
    if (settings["startTime"] == "Sunrise" || settings["startTime"] == "Sunset") {
        schedule("15 00 00 ? * " + getDayOfWeekCron(settings["daysOfWeek"]), scheduleMowingWindowStart) // schedule 10 seconds after midnight to give the activation code a chance to run 1 second after midnight
    }
    else {
        scheduleMowingWindowStart()
    }
    if (settings["endTime"] == "Sunrise" || settings["endTime"] == "Sunset") {
        if (doesMowingEndSameDay()) schedule("20 00 00 ? * " + getDayOfWeekCron(settings["daysOfWeek"]), scheduleMowingWindowEnd) // schedule 10 seconds after midnight to give the activation code a chance to run 1 second after midnight
        else schedule("20 00 00 ? * " + getDayOfWeekCron(getShiftedDaysOfWeek()), scheduleMowingWindowEnd)
    }
    else {
        scheduleMowingWindowEnd()
    }    

    if (isInMowingWindow()) {
        startMowingWindow()
        mowingPreCheck() // if app activated in the middle of the mowing window, park mowers if park conditions me        
        
        for (mower in settings["husqvarnaMowers"]) { 
            def serial = mower.currentValue("serialNumber")
            def activity = mower.currentValue("mowerActivity")
            state.mowers[serial]?.current.activity = activity
            
            if (state.mowers[serial] != null && state.mowers[serial]?.parkedByApp == false && state.mowers[serial].userForcingMowing == false && (activity == "MOWING" || activity == "LEAVING")) {              
                logDebug("Checking if need to park before window ends")
                state.mowers[serial]?.timeStartedMowing = state.mowers[serial]?.timeStartedMowing == null ? now().getTime() : state.mowers[serial]?.timeStartedMowing
                def durationLeftToMow = getRequiredMowingDuration() - state.mowers[serial]?.mowedDurationSoFar
                def stopByDuration = state.mowers[serial]?.timeStartedMowing + (durationLeftToMow > 0 ? durationLeftToMow : 0)
                def stopByDurationDate = new Date(stopByDuration)
                def windowEnd = getNextMowingWindowEnd()
                logDebug("Window Ends ${windowEnd}. Stop By Time is ${stopByDurationDate}")
                if (windowEnd.after(stopByDurationDate) && stopByDurationDate.after(now())) {
                    logDebug("Scheduling premature park for ${stopByDurationDate}")
                    runOnce(stopByDurationDate, park, [data: [serial: serial], overwrite: false])
                }
            }
        } 
        scheduleMowingWindowEnd()
    }
    else endMowingWindow()
}

def subscribeForParkPause(forcedMowing = false) {
    if (!forcedMowing) {
        if (settings["parkWhenGrassWet"] == true) {
            if (settings["leafWetnessSensor"] != null) subscribe(settings["leafWetnessSensor"], "leafWetness", leafWetnessHandler)
            if (settings["humidityMeasurement"] != null) subscribe(settings["humidityMeasurement"], "humidity", humidityHandler)
            if (settings["waterSensor"] != null) subscribe(settings["waterSensor"], "water", waterSensorHandler)
        }
        if (settings["parkWhenTempHot"] == true && settings["parkTempSensor"] != null) subscribe(settings["parkTempSensor"], "temperature", temperatureHandler)  
        if (settings["parkWhenPresenceArrivesLeaves"] == true && settings["presenceSensors"] != null) subscribe(settings["presenceSensors"], "presence", parkOnPresenceHandler)
        if (settings["parkWhenSwitchOnOff"] == true && settings["parkSwitches"] != null) subscribe(settings["parkSwitches"], "switch", parkOnSwitchHandler)  
    }
    else {
        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Leaf Wetness Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.leafWetness)) subscribe(settings["leafWetnessSensor"], "leafWetness", leafWetnessHandler)
        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Humidity or Soil Moisture Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.humidity)) subscribe(settings["humidityMeasurement"], "humidity", humidityHandler)
        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Water Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.water)) subscribe(settings["waterSensor"], "water", waterSensorHandler)
        if (settings["forcedMowingParkConditionsEnforced"].contains("Temperature Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.temperature)) subscribe(settings["parkTempSensor"], "temperature", temperatureHandler)  
        if (settings["forcedMowingParkConditionsEnforced"].contains("Presence Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.presence)) subscribe(settings["presenceSensors"], "presence", parkOnPresenceHandler)
        if (settings["forcedMowingParkConditionsEnforced"].contains("Switch(es)") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.switchSensors)) subscribe(settings["parkSwitches"], "switch", parkOnSwitchHandler)      
    }
    
    if (settings["pauseWhenMotion"] == true && settings["pause_motionSensors"] != null) subscribe(settings["pause_motionSensors"], "motion", pauseOnMotionHandler)
    if (settings["pauseWhenOpenCloseSensor"] == true && settings["pauseContactSensors"] != null) subscribe(settings["pauseContactSensors"], "contact", pauseOnOpenCloseHandler)
    if (settings["pauseWhenPresenceArrivesLeaves"] == true && settings["pause_presenceSensors"] != null) subscribe(settings["pause_presenceSensors"], "presence", pauseOnPresenceHandler)
    if (settings["pauseWhenSwitchOnOff"] == true && settings["pauseSwitches"] != null) subscribe(settings["pauseSwitches"], "switch", pauseOnSwitchHandler)
    if (settings["pauseWhenButtonPressed"] == true && settings["pauseButtons"] != null) subscribe(settings["pauseButtons"], "pushed", pauseOnButtonHandler)    
}

def unsubscribeForParkPause() {
    unsubscribe(settings["leafWetnessSensor"])
    unsubscribe(settings["humidityMeasurement"])
    unsubscribe(settings["waterSensor"])
    // weather and irrigation persistently scheduled

    unsubscribe(settings["parkTempSensor"])  
    unsubscribe(settings["presenceSensors"])
    unsubscribe(settings["parkSwitches"])
    
    unsubscribe(settings["pause_motionSensors"])
    unsubscribe(settings["pauseContactSensors"])
    unsubscribe(settings["pause_presenceSensors"])
    unsubscribe(settings["pauseSwitches"])
    unsubscribe(settings["pauseButtons"])        
}

def isMowingScheduledForNow(withGracePeriod = false) {
    if (!withGracePeriod) return state.isInMowingWindow
    else {
        def inWindow = false
        if (state.isInMowingWindow == true) inWindow = true
        else if (withGracePeriod == true) inWindow = (state.isInMowingWindowGracePeriod != null) ? state.isInMowingWindowGracePeriod : false
        return inWindow
    }
}

def isInMowingWindow() {
    def isScheduled = false
    def now = new Date()
    def window = getMowingWindow()
    logDebug("isInMowingWindow(): now ${now}. mowing window is ${window}. IsMowDay = ${isMowDay(new Date())} with timeOfDayIsBetween = ${timeOfDayIsBetween(window.start, window.end, now, location.timeZone)}")
    if (isMowDay(new Date()) && timeOfDayIsBetween(window.start, window.end, now, location.timeZone)) {
        // In the middle of today's mowing window.
        isScheduled = true
    }
    else if (isMowDay(new Date() - 1)) {
        // if yesterday's mowing window started yesterday but ended today, figure out if within yesterday's mowing window    
        def yesterdaysWindow = [start: window.start - 1, end: window.end - 1]  // approximate yesterday's window one day less than today's window
        if(timeOfDayIsBetween(yesterdaysWindow.start, yesterdaysWindow.end, now, location.timeZone)) {    
            isScheduled = true
        }
    }
    logDebug("isInMowingWindow() = ${isScheduled}")
    return isScheduled    
}

def isInMowingWindowStartedToday(gracePeriodSecs = null) {
    def isScheduled = false
    def now = new Date()
    if (gracePeriodSecs != null) now = adjustDateBySecs(now, gracePeriodSecs.toInteger())
    def window = getMowingWindow()
    logDebug("isInMowingWindowStartedToday: mowing window is ${window}. IsMowDay = ${isMowDay(new Date())} with timeOfDayIsBetween = ${timeOfDayIsBetween(window.start, window.end, now, location.timeZone)}")
    if (isMowDay(new Date()) && timeOfDayIsBetween(window.start, window.end, now, location.timeZone)) {
        // In the middle of today's mowing window.
        isScheduled = true
    }
    return isScheduled    
}

def isInMowingWindowStartedYesterday(gracePeriodSecs = null) {
    def isScheduled = false
    def now = new Date()
    if (gracePeriodSecs != null) now = adjustDateBySecs(now, gracePeriodSecs.toInteger())
    def window = getMowingWindow()
    if (isMowDay(new Date() - 1)) {
        // if yesterday's mowing window started yesterday but ended today, figure out if within yesterday's mowing window    
        def yesterdaysWindow = [start: window.start - 1, end: window.end - 1]  // approximate yesterday's window one day less than today's window
        if(timeOfDayIsBetween(yesterdaysWindow.start, yesterdaysWindow.end, now, location.timeZone)) {    
            isScheduled = true
        }
    }
    return isScheduled    
}

def getYesterdaysMowingWindow() {
    def window = getMowingWindow()
    def yesterdaysWindow = [start: window.start - 1, end: window.end - 1]  // approximate yesterday's window one day less than today's window
    return yesterdaysWindow
}

def getNextMowingWindowEnd() {
    def nextEnd = null
    if (isInMowingWindowStartedToday()) {
        def todaysWindow = getMowingWindow()
        nextEnd = todaysWindow.end
    }
    else if (isInMowingWindowStartedYesterday()) {
        // yesterday's mowing window ends later today
        def yesterdaysWindow = getYesterdaysMowingWindow()
        return yesterdaysWindow.end
    }
    return nextEnd
}

def setNextBackupMowingWindow() {
    def nextBackupWindow = null
    
    def todaysBackup = null
    if (backupStartTime == "Certain Time" && backupStartTimeValue != null) todaysBackup = toDateTime(backupStartTimeValue)
    else if (backupStartTime == "Sunrise") todaysBackup = getSunriseAndSunset([sunriseOffset: backupStartTimeOffset ? backupStartTimeOffset : 0]).sunrise
    else if (backupStartTime == "Sunset") todaysBackup =  getSunriseAndSunset([sunsetOffset: backupStartTimeOffset ? backupStartTimeOffset : 0]).sunset
        
    def backupEndAt = null
    if (backupEndTime == "Certain Time" && backupEndTimeValue != null) backupEdAt = toDateTime(backupEndTimeValue)
    else if (backupEndTime == "Sunrise") backupEndAt = getSunriseAndSunset([sunriseOffset: backupEndTimeOffset ? backupEndTimeOffset : 0]).sunrise
    else if (backupEndTime == "Sunset") backupEndAt = getSunriseAndSunset([sunsetOffset: backupEndTimeOffset ? backupEndTimeOffset : 0]).sunset
    if (backupEndAt && todaysBackup.after(backupEndAt)) backupEndAt = backupEndAt + 1  // end mowing tomorrow. Tomorrow's sunrise and sunset closely approximated based on today's sunrise and sunset
  
    def now = new Date()
    if (todaysBackup.after(now)) nextBackupWindow = [start: todaysBackup, end: backupEndAt]
    else nextBackupWindow = [start: todaysBackup + 1, end: backupEndAt + 1] // if today's backup window has already passed, defer until tomorrow
        logDebug("Backup window set to start: ${nextBackupWindow.start} and end: ${nextBackupWindow.end} with todaysBackup = ${todaysBackup}")
    state.backup?.window = [start: nextBackupWindow.start.getTime(), end: nextBackupWindow.end.getTime()]
}

def isBackupMowingScheduledForNow(withGracePeriod = false) {
    if (state.backup == null) return false
    else {
        def inBackupWindow = false
        if (state.backup.inProgress == true) inBackupWindow = true
        else if (withGracePeriod == true) inBackupWindow = (state.backup.inGracePeriod != null || state.backup.inGracePeriod == true) ? true : false
        return inBackupWindow
    }
}

def scheduleMowers() {
    logDebug("${app.label} scheduleMowers() executing")
    
    // Primary Schedule Window
    def window = getMowingWindow()
    
    def midnight = new Date().clearTime()    
    def start = getMinutesBetweenDates(midnight, window.start)
    def duration = getMinutesBetweenDates(window.start, window.end)
    
    // Determine Days of Week Parameters
    def primaryScheduleList = []
    if (start != null && duration != null && start + duration <= 1440) {
        def dayMap = parseDaysOfWeek()
        def schedule = [start:start, duration:duration, monday:dayMap["monday"], tuesday:dayMap["tuesday"], wednesday:dayMap["wednesday"], thursday:dayMap["thursday"], friday:dayMap["friday"], saturday:dayMap["saturday"], sunday:dayMap["sunday"]]            
        primaryScheduleList.add(schedule)
    }
    else if (start != null && duration != null && start + duration > 1440) {
        // need to break up into two tasks since API doesn't allow a single task to span midnight
        def firstDuration = 1440 - start
        def firstDayMap = parseDaysOfWeek(false)
        def firstSchedule = [start:start, duration:firstDuration, monday:firstDayMap["monday"], tuesday:firstDayMap["tuesday"], wednesday:firstDayMap["wednesday"], thursday:firstDayMap["thursday"], friday:firstDayMap["friday"], saturday:firstDayMap["saturday"], sunday:firstDayMap["sunday"]]
        primaryScheduleList.add(firstSchedule)
            
        def secondDuration = duration - firstDuration
        def secondDayMap = parseDaysOfWeek(true)
        def secondSchedule = [start:0, duration:secondDuration, monday:secondDayMap["monday"], tuesday:secondDayMap["tuesday"], wednesday:secondDayMap["wednesday"], thursday:secondDayMap["thursday"], friday:secondDayMap["friday"], saturday:secondDayMap["saturday"], sunday:secondDayMap["sunday"]]            
        primaryScheduleList.add(secondSchedule)
    }
    
    def backupScheduleList = []
    if (state.backup && state.backup.isPending == true && state.backup.window != null) {
        def backupStartDate = new Date(state.backup.window.start)
        def backupMidnight = new Date(state.backup.window.start).clearTime()
        def backupEndDate = new Date(state.backup.window.end)
        def backupStart = getMinutesBetweenDates(backupMidnight, backupStartDate)
        def backupDuration = getMinutesBetweenDates(backupStartDate, backupEndDate)
        def dayOfWeek = getDayOfWeek(backupStartDate)
        logDebug("scheduling backup window with backupStartDate = ${backupStartDate}, backupMidnight = ${backupMidnight}, backupEndDate = ${backupEndDate}, day of week = ${dayOfWeek}")
        if (backupStart != null && backupDuration != null && backupStart + backupDuration <= 1440) {
            def backupSchedule = [start:backupStart, duration:backupDuration, monday:(dayOfWeek == 2 ? true : false), tuesday:(dayOfWeek == 3 ? true : false), wednesday:(dayOfWeek == 4 ? true : false), thursday:(dayOfWeek == 5 ? true : false), friday:(dayOfWeek == 6 ? true : false), saturday:(dayOfWeek == 7 ? true : false), sunday:(dayOfWeek == 1 ? true : false)]            
            backupScheduleList.add(backupSchedule)
        }
        else if (backupStart != null && backupDuration != null && backupStart + backupDuration > 1440) {
            // need to break up into two tasks since API doesn't allow a single task to span midnight
            def firstDuration = 1440 - backupStart
            def firstSchedule = [start:backupStart, duration:firstDuration, monday:(dayOfWeek == 2 ? true : false), tuesday:(dayOfWeek == 3 ? true : false), wednesday:(dayOfWeek == 4 ? true : false), thursday:(dayOfWeek == 5 ? true : false), friday:(dayOfWeek == 6 ? true : false), saturday:(dayOfWeek == 7 ? true : false), sunday:(dayOfWeek == 1 ? true : false)]  
            backupScheduleList.add(firstSchedule)
            
            def secondDuration = backupDuration - firstDuration
            dayOfWeek++
            def secondSchedule = [start:0, duration:secondDuration, monday:(dayOfWeek == 2 ? true : false), tuesday:(dayOfWeek == 3 ? true : false), wednesday:(dayOfWeek == 4 ? true : false), thursday:(dayOfWeek == 5 ? true : false), friday:(dayOfWeek == 6 ? true : false), saturday:(dayOfWeek == 7 ? true : false), sunday:(dayOfWeek == 1 ? true : false)]             
            backupScheduleList.add(secondSchedule)
        }       
    }
    
    def schedList = primaryScheduleList + backupScheduleList
    if (schedList != []) {
        logDebug("${app.label} scheduling mowers with: ${schedList}")
        for (mower in settings["husqvarnaMowers"]) { 
            mower.setSchedule(schedList)
        } 
    }
}

def startMowingWindow() {
    state.isInMowingWindow = true
}

def endMowingWindow() {
    state.isInMowingWindow = false
    state.isInMowingWindowGracePeriod = true
    runIn(settings["pollingInterval"], endGracePeriod)
}

def endGracePeriod() {
    state.isInMowingWindowGracePeriod = false
}

def scheduleMowingWindowStart() {
    if (settings["startTime"] == "Sunrise" || settings["startTime"] == "Sunset") {
        def window = getMowingWindow()
        def preCheckTime = adjustDateBySecs(window.start, -60) // schedule precheck 30 seconds before mowing start time
        logDebug("Scheduling pre-check for preCheckTime = ${preCheckTime}")
        runOnce(preCheckTime, mowingPreCheck)     
        runOnce(window.start, startMowingWindow) 
    }
    else {
        def preCheckTime = adjustTimeBySecs(startTimeValue, -60) // schedule precheck 30 seconds before mowing start time
        logDebug("Scheduling pre-check for preCheckTime = ${preCheckTime}")
        def cron = getTimeOfDayCron(preCheckTime) + " ? * " + getDayOfWeekCron(settings["daysOfWeek"])
        schedule(cron, mowingPreCheck)
        cron = getTimeOfDayCron(toDateTime(startTimeValue)) + " ? * " + getDayOfWeekCron(settings["daysOfWeek"])
        schedule(cron, startMowingWindow)
    }
}

def getTimeOfDayCron(dateTime) {
    if (dateTime == null) return null
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.setTime(dateTime)
    String hours = String.valueOf(cal.get(Calendar.HOUR_OF_DAY))
    String mins = String.valueOf(cal.get(Calendar.MINUTE))
    String secs = String.valueOf(cal.get(Calendar.SECOND))
    logDebug("Time of Day Cron: " + secs + " " + mins + " " + hours)
    return secs + " " + mins + " " + hours   
}

def getDayOfWeekCron(daysOfWeek) {
    def dayCron = ""
    for (dayOfWeek in daysOfWeek) {
        dayCron += dayOfWeekMap[dayOfWeek] + ","
    }
    if (dayCron != "") dayCron = dayCron.substring(0,dayCron.length()-1)
    return dayCron
}

def scheduleMowingWindowEnd() {
    def window = getMowingWindow()
    def windowExtension = settings["pollingInterval"].toInteger() + 120
    def postWindowTime = adjustDateBySecs(window.end, windowExtension) // schedule postcheck after mowing end time, corresponding to polling interval, plus 120 seconds, to make sure mower activity updated
    if (settings["endTime"] == "Sunrise" || settings["endTime"] == "Sunset") {    
        runOnce(window.end, endMowingWindow)  
        runOnce(postWindowTime, handleExpiredMowingWindow)  
    }
    else {
        def startDayOfWeek = getDayOfWeek(window.start)
        def endDayOfWeek = getDayOfWeek(window.end)
        if (startDayOfWeek == endDayOfWeek) {
            def cron = getTimeOfDayCron(postWindowTime) + " ? * " + getDayOfWeekCron(settings["daysOfWeek"])
            logDebug("Scheduling handleExpiredMowingWindow() with cron string: ${cron}")
            schedule(cron, handleExpiredMowingWindow)
            
            cron = getTimeOfDayCron(window.end) + " ? * " + getDayOfWeekCron(settings["daysOfWeek"])
            logDebug("Scheduling endMowingWindow() with cron string: ${cron}")
            schedule(cron, endMowingWindow)
        }
        else {
            // window ends the next day after it starts  
            def shiftedDays = getShiftedDaysOfWeek()
            def cron = getTimeOfDayCron(postWindowTime) + " ? * " + getDayOfWeekCron(shiftedDays)
            logDebug("Scheduling handleExpiredMowingWindow() with cron string: ${cron}")
            schedule(cron, handleExpiredMowingWindow)    
            
            cron = getTimeOfDayCron(window.end) + " ? * " + getDayOfWeekCron(shiftedDays)
            logDebug("Scheduling endMowingWindow() with cron string: ${cron}")
            schedule(cron, endMowingWindow)
        }
    }
}

def getDayOfWeek(Date date) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.setTime(date)
    def dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)    
    logDebug("Converted ${date} to day of week = ${dayOfWeek}")
    return dayOfWeek
}

def doesMowingEndSameDay() {
    def window = getMowingWindow()
    def startDayOfWeek = getDayOfWeek(window.start)
    def endDayOfWeek = getDayOfWeek(window.end)
    return (startDayOfWeek == endDayOfWeek) ? true : false
}

def mowingPreCheck() {
    logDebug("Executing mowing precheck")
    updateAllParkConditions()
    updateAllPauseConditions()
    subscribeForParkPause()
    if (isAnyParkConditionMet()) {
        for (mower in settings["husqvarnaMowers"]) { 
            def serialNum = mower.currentValue("serialNumber")
            if (state.mowers[serialNum]?.userForcingMowing == false) {
                parkOne(serialNum, true) // park mower if park conditions become met during a scheduled mowing window, except if the user has forced mowing to override the schedule
                logDebug("Parking conditions met during pre-check. Parking mowers.")
            }
        } 
    }
}

def getShiftedDaysOfWeek() {
    def shiftedDays = []
    if (settings["daysOfWeek"].contains("Monday")) shiftedDays.add("Tuesday")
    if (settings["daysOfWeek"].contains("Tuesday")) shiftedDays.add("Wednesday")
    if (settings["daysOfWeek"].contains("Wednesday")) shiftedDays.add("Thursday")
    if (settings["daysOfWeek"].contains("Thursday")) shiftedDays.add("Friday")
    if (settings["daysOfWeek"].contains("Friday")) shiftedDays.add("Saturday")
    if (settings["daysOfWeek"].contains("Saturday")) shiftedDays.add("Sunday")
    if (settings["daysOfWeek"].contains("Sunday")) shiftedDays.add("Monday")
    return shiftedDays
}

def startBackupWindow() {
    state.backup?.inProgress = true
}

def endBackupWindow() {
    if (state.backup != null && state.backup != [:]) {
        state.backup?.inProgress = false
        state.backup?.inGracePeriod = true
        runIn(settings["pollingInterval"], endBackupGracePeriod)
    }
}

def endBackupGracePeriod() {
    if (state.backup != null && state.backup != [:]) {
        state.backup?.inGracePeriod = false   
    }
}

def activateBackupWindow(duration) {
    if (!state.backup) state.backup = [:]
    state.backup.isPending = true
    state.backup.activatedAt = (new Date()).getTime()
    state.backup.duration = duration
    state.backup.inProgress = false
    setNextBackupMowingWindow()

    scheduleMowers()
    
    def backupStart = new Date(state.backup.window.start)
    def preCheckTime = adjustDateBySecs(backupStart, -60)
    runOnce(preCheckTime, backupWindowPreCheck) 
    runOnce(backupStart, startBackupWindow) 
    
    def backupEnd = new Date(state.backup.window.end)
    Integer windowExtension = settings["pollingInterval"].toInteger() + 120
    def postWindowTime = adjustDateBySecs(backupEnd, windowExtension) // schedule postcheck after mowing end time, corresponding to polling interval, plus 120 seconds, to make sure mower activity updated              
    runOnce(postWindowTime, handleExpiredBackupMowingWindow)  
    runOnce(backupEnd, endBackupWindow)  
    
    updateDeviceData([backupTriggered: state.backup.isPending, backupDuration: msToMins(state.backup.duration)])
}

def backupWindowPreCheck() {
    updateAllParkConditions()
    updateAllPauseConditions()
    subscribeForParkPause()
    if (isAnyParkConditionMet(true)) {
        for (mower in settings["husqvarnaMowers"]) { 
            def serialNum = mower.currentValue("serialNumber")
            if (state.mowers[serialNum]?.userForcingMowing == false) parkOne(serialNum, true) // park mower if park conditions become met during a scheduled backup mowing window, except if the user has forced mowing to override the schedule
        } 
    }    
}

def isMowDay(Date date) {
    def isMowDay = false    
    def dayOfWeek = date.format('EEEE') 
    if(settings["daysOfWeek"] && settings["daysOfWeek"].contains(dayOfWeek)) isMowDay = true 
    return isMowDay
}

def getMowingWindow() {  
    def startTodayAt = null
    if (startTime == "Certain Time" && startTimeValue != null) startTodayAt = toDateTime(startTimeValue)
    else if (startTime == "Sunrise") startTodayAt = getSunriseAndSunset([sunriseOffset: startTimeOffset ? startTimeOffset : 0]).sunrise
    else if (startTime == "Sunset") startTodayAt =  getSunriseAndSunset([sunsetOffset: startTimeOffset ? startTimeOffset : 0]).sunset

    def endAt = null
    if (endTime == "Certain Time" && endTimeValue != null) endAt = toDateTime(endTimeValue)
    else if (endTime == "Sunrise") endAt = getSunriseAndSunset([sunriseOffset: endTimeOffset ? endTimeOffset : 0]).sunrise
    else if (endTime == "Sunset") endAt = getSunriseAndSunset([sunsetOffset: endTimeOffset ? endTimeOffset : 0]).sunset
    if (endAt && startTodayAt.after(endAt)) endAt = endAt + 1  // end mowing tomorrow. Tomorrow's sunrise and sunset closely approximated based on today's sunrise and sunset
    return [start: startTodayAt, end: endAt]
}

def notify(message, type) {
    parent.notify(message, type)    
}

def getNotificationTypes() {
    return parent.getNotificationTypes()    
}

def getNumDaysInMonth(month) {
    def days = days31
    if (month && month == "FEB") days = days29
    else if (month && (month == "APR" || month == "JUN" || month == "SEP" || month == "NOV")) days = days30
    return days        
}

def getLogDebugEnabled() {
   return parent.getLogDebugEnabled()
}

def parseDaysOfWeek(rightShift = false) {
    def dayMap = [monday:settings["daysOfWeek"].contains("Monday"), tuesday:settings["daysOfWeek"].contains("Tuesday"), wednesday:settings["daysOfWeek"].contains("Wednesday"), thursday:settings["daysOfWeek"].contains("Thursday"), friday:settings["daysOfWeek"].contains("Friday"), saturday:settings["daysOfWeek"].contains("Saturday"), sunday:settings["daysOfWeek"].contains("Sunday")]
    def dayMapBool = [:]
    if (!rightShift) {
        dayMapBool = dayMap
    }
    else if (rightShift == true) {
        // shift days of week to the right, for translating days of week for a window that spans midnight
        dayMapBool["tuesday"] = dayMap["monday"]
        dayMapBool["wednesday"] = dayMap["tuesday"]
        dayMapBool["thursday"] = dayMap["wednesday"]
        dayMapBool["friday"] = dayMap["thursday"]
        dayMapBool["saturday"] = dayMap["friday"]
        dayMapBool["sunday"] = dayMap["saturday"]
        dayMapBool["monday"] = dayMap["sunday"]
    }
    return dayMapBool
}

def forceMowing(serial) {
    state.mowers[serial]?.userForcingMowing = true
    state.mowers[serial]?.parkedByApp = false
    state.mowers[serial]?.pausedByApp = false    
    
    updateAllParkConditions()
    state.forcedMowingParkConditionSnapshot = state.parkConditions // TO DO: ignore any conditions that were already met when the user forced mowing, since user obviously doesn't want to enforce these
    
    subscribeForParkPause(true)
}

def stopForcedMowing(serial) {
    state.mowers[serial]?.userForcingMowing = false
    state.forcedMowingParkConditionSnapshot = null
    if (anyMowerForcingMowing() == false && isMowingScheduledForNow() == false && isBackupMowingScheduledForNow() == false) unsubscribeForParkPause()
    // unsubscribe if outside mowing window and no other mowers being forced to mow
}

def anyMowerForcingMowing() {
    def anyBeingForced = false
    state.mowers.each { serialNum, mower ->  
        if (state.mowers[serialNum]?.userForcingMowing == true) anyBeingForced = true
    }
    return anyBeingForced
}

def mowerActivityHandler(evt) {
    // control mowing based on reported mower activity, rather than based on commands that this app sends, in order to account for any mowing events triggered by the user via the native mowing app (not this app)
    def mower = evt.getDevice()
    def serial = mower.currentValue("serialNumber")
    def activityTime = evt.getDate().getTime()
    def activity = evt.value        
    
    def now = new Date(now())
    
    state.mowers[serial]?.previous.activity = state.mowers[serial]?.current.activity
    state.mowers[serial]?.current.activity = activity
    
    logDebug("mowerActivityHandler: mower previous activity = ${state.mowers[serial]?.previous.activity}. Mower current activity = ${state.mowers[serial]?.current.activity}")
    
    if (isMowingScheduledForNow() && (state.mowers[serial]?.current.activity == "MOWING" || state.mowers[serial]?.current.activity == "LEAVING")) {
        logDebug("isMowingScheduledForNow() without cushion is true. Mower started mowing.")
      // mowing activity occurred during current mowing window. Attribute to current mowing window and count mowing duration toward current mowing window's duration         
        state.mowers[serial]?.timeStartedMowing = activityTime

        unschedule(park)
    
        // If mowing for the full mowing window, mower will park itself according to the schedule. 
        // But if mowing for a duration less than the full mowing window, schedule pre-mature park here
        // Scheduling this only after mowing has begun during the current mowing window prevents overlapping scheduling when mowing window starts on one day and ends on the next day. That is, only one mowing start call needs to be scheduled at a time and only one mowing end call needs to be scheduled at a time, even if the mowing window straddles midnight     
        def durationLeftToMow = getRequiredMowingDuration() - state.mowers[serial]?.mowedDurationSoFar
        def stopByDuration = state.mowers[serial]?.timeStartedMowing + (durationLeftToMow > 0 ? durationLeftToMow : 0)
        def stopByDurationDate = new Date(stopByDuration)
        def windowEnd = getNextMowingWindowEnd()
        if (windowEnd.after(stopByDurationDate) && stopByDurationDate.after(now)) runOnce(stopByDurationDate, park, [data: [serial: serial], overwrite: false])
        
        if (state.mowers[serial]?.parkedByApp == true || state.mowers[serial]?.pausedByApp == true) {
            // deduce user forced mowing, since mowing started even though this app forced parking
            forceMowing(serial)

        }
    }
    else if (isMowingScheduledForNow(true) && (state.mowers[serial]?.previous.activity == "MOWING" ||state.mowers[serial]?.previous.activity == "LEAVING") && state.mowers[serial]?.current.activity != "MOWING") { // mower was mowing, but has now stopped mowing. Give grace period for activity having been updated, corresponding to the polling interval
        logDebug("isMowingScheduledForNow() with polling interval cushion is true. Mower stopped mowing.")
        state.mowers[serial]?.timeStoppedMowing = activityTime
        state.mowers[serial]?.mowedDurationSoFar = state.mowers[serial]?.mowedDurationSoFar + (state.mowers[serial]?.timeStoppedMowing - state.mowers[serial]?.timeStartedMowing)
        stopForcedMowing(serial)
        
    }
    else if (isBackupMowingScheduledForNow() && (state.mowers[serial]?.current.activity == "MOWING" || state.mowers[serial]?.current.activity == "LEAVING")) {
        logDebug("isBackupMowingScheduledForNow() without cushion is true. Mower started mowing.")
        // mowing activity occurred during backup mowing window. Attribute to backup mowing window and count mowing duration toward current backup mowing window's duration 
        state.mowers[serial]?.timeStartedMowing = activityTime
        
        unschedule(park)
        
        // If mowing for the full mowing window, mower will park itself according to the schedule. 
        // But if mowing for a duration less than the full mowing window, schedule pre-mature park here
        // Scheduling this only after mowing has begun during the current mowing window prevents overlapping scheduling when mowing window starts on one day and ends on the next day. That is, only one mowing start call needs to be scheduled at a time and only one mowing end call needs to be scheduled at a time, even if the mowing window straddles midnight        
        def durationLeftToMow = state.backup.duration - state.mowers[serial]?.mowedDurationSoFar
        def stopByDuration = state.mowers[serial]?.timeStartedMowing + (durationLeftToMow > 0 ? durationLeftToMow : 0)
        def stopByDurationDate = new Date(stopByDuration)
        def windowEnd = new Date(state.backup.window.end)
        if (windowEnd.after(stopByDurationDate) && stopByDurationDate.after(now)) runOnce(stopByDurationDate, park, [data: [serial: serial], overwrite: false])
        
        if (state.mowers[serial]?.parkedByApp == true || state.mowers[serial]?.pausedByApp == true) {
            // deduce user forced mowing, since mowing started even though this app forced parking
            forceMowing(serial)
        }
    }
    else if (isBackupMowingScheduledForNow(true) && (state.mowers[serial]?.previous.activity == "MOWING" || state.mowers[serial]?.previous.activity == "LEAVING") && state.mowers[serial]?.current.activity != "MOWING") { // mower was mowing, but has now stopped mowing. Give grace period for activity having been updated, corresponding to the polling interval
        logDebug("isBackupMowingScheduledForNow() with polling interval cushion is true. Mower stopped mowing.")
        state.mowers[serial]?.timeStoppedMowing = activityTime
        state.mowers[serial]?.mowedDurationSoFar = state.mowers[serial]?.mowedDurationSoFar + (state.mowers[serial]?.timeStoppedMowing - state.mowers[serial]?.timeStartedMowing)
        stopForcedMowing(serial)
    }
    else if (state.mowers[serial]?.current.activity == "MOWING") {
        logDebug("Mower started mowing outside of any window")
        // mowing started outside of any mowing window. Assume user forced mowing and wants to keep mowing irrespective of parking/pausing conditions. 
        forceMowing(serial)
    }
    else {
        // TO DO: anything else here?    
    }
}

def handleExpiredMowingWindow() {   
    // clean up from current mowing window
    // trigger backup rule if needed
    
    def maxDurationLeftToMow = 0
    def requiredDuration = getRequiredMowingDuration() 
    logDebug("Required mowing duration: ${msToMins(requiredDuration)} mins")
    state.mowers.each { serial, mower ->   
        def durationLeftToMow = requiredDuration - mower.mowedDurationSoFar
        logDebug("Duration mowed so far: ${msToMins(mower.mowedDurationSoFar)} mins. Duration Left to mow: ${msToMins(durationLeftToMow)} mins")
        if (durationLeftToMow > 0) maxDurationLeftToMow = Math.max(durationLeftToMow, maxDurationLeftToMow)
        
        state.mowers[serial].mowedDurationSoFar = 0 
        state.mowers[serial].timeStartedMowing = null
        state.mowers[serial].timeStoppedMowing = null
        
        if (mower.parkedByApp == true) {  // mower was parked or paused by the app. need to resume schedule for potentially mowing next mowing window
            mowOne(serial)                // call resume schedule so no longer forced to park and can mow according to schedule if mowing conditions later met
        }    
        
        // reset these state variables each mowing window
        state.mowers[serial].parkedByApp = false    // reset park status for app
        state.mowers[serial].pausedByApp = false    // reset pause status for app
        state.mowers[serial].userForcingMowing = false
    }     
    // give grace period corresponding to the polling interval (Since might have detected mowing late due to polling interval)
    logDebug("Duration Left To Mow: ${maxDurationLeftToMow} ms, compared to ${settings["pollingInterval"]*1000} ms polling interval")
     if (maxDurationLeftToMow > settings["pollingInterval"]*1000) {
         if (addBackupWindow == true) {
             activateBackupWindow(maxDurationLeftToMow)
             notify("${app.label} Backup Window Triggered: ${msToMins(maxDurationLeftToMow)} mins left to mow", "backupRule")
         }
         else {
             notify("${app.label} Mowing Deficiency: ${msToMins(maxDurationLeftToMow)} mins mowing missed.", "backupRule")
         }
    }
    if (dynamicCuttingHeight) {           
        if (addBackupWindow == false) {
            // update height state if there is no backup window. If there is a backup window, wait until the backup window to determine whether the window is missed
            def percentWindowMowed = (1 - (maxDurationLeftToMow / requiredDuration)) * 100  // maxDurationLeftToMow reflects the duration left to mow to meet the required duration
            def isMissed = percentWindowMowed < getMinPercentWindowSetting() ? true : false  // determine if window would be declared missed when accounting for mowing completed
            if (isMissed) incrementMissedWindows()
            else incrementFulfilledWindows()
        }
    }
    
    unsubscribeForParkPause()
}

def handleExpiredBackupMowingWindow() {
    def maxDurationLeftToMow = 0
    def requiredDuration = state.backup.duration
    state.mowers.each { serial, mower ->   
        def durationLeftToMow = requiredDuration - mower.mowedDurationSoFar
        if (durationLeftToMow > 0) maxDurationLeftToMow = Math.max(durationLeftToMow, maxDurationLeftToMow)
        
        state.mowers[serial].mowedDurationSoFar = 0 
        state.mowers[serial].timeStartedMowing = null
        state.mowers[serial].timeStoppedMowing = null
        
        if (mower.parkedByApp == true) {  // mower was parked or paused by the app. need to resume schedule for potentially mowing next mowing window
            mowOne(serial)                // call resume schedule so no longer forced to park and can mow according to schedule if mowing conditions later met
        }
        
        // reset these state variables each mowing window
        state.mowers[serial].parkedByApp = false    // reset park status for app
        state.mowers[serial].pausedByApp = false    // reset pause status for app
        state.mowers[serial].userForcingMowing = false
    }  
    state.backup = [:]
    
    scheduleMowers()
    
    if (dynamicCuttingHeight) {     
        requiredDuration = getRequiredMowingDuration()  // replace required duration with the required duration of the primary window
        def percentWindowMowed = (1 - (maxDurationLeftToMow / requiredDuration)) * 100  // maxDurationLeftToMow reflects the duration left to mow to meet the required duration of the primary window
        def isMissed = percentWindowMowed < getMinPercentWindowSetting() ? true : false  // determine if window would be declared missed when accounting for mowing completed, acorss primary window and backup window
        if (isMissed) incrementMissedWindows()
        else incrementFulfilledWindows()
    }
    
    unsubscribeForParkPause()
    
    updateDeviceData([backupTriggered: false, backupDuration: "none"])
}

def getMinPercentWindowSetting() {
    return settings["minPercentWindow"]
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
    def setPoint = 0
    state.mowers.each { serial, mower ->   
        def currentHeight = mower.currentValue("cuttingHeight")
        if (currentHeight < 9) {
            def dynamicHeight = Math.min(maxHeight, currentHeight + settings["numLevelsIncrease"])
            setPoint = Math.max(dynamicHeight, setPoint)
            mower.setCuttingHeight(dynamicHeight)
            notify("${mower.name} Dynamically Increased Cutting Height up to ${setPoint}", "cuttingHeight")
        }
    }    
}

def decrementCuttingHeight() {
    def minHeight = settings["cuttingHeight"]
    def setPoint = 10
    state.mowers.each { serial, mower ->   
        def currentHeight = mower.currentValue("cuttingHeight")       
        if (currentHeight > minHeight) {
            def dynamicHeight = Math.max(minHeight, currentHeight - settings["numLevelsDecrease"])
            setPoint = Math.min(dynamicHeight, setPoint)
            mower.setCuttingHeight(dynamicHeight)
            notify("${mower.name} Dynamically Decreased Cutting Height up to ${setPoint}", "cuttingHeight")
        }
    }   
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
         notify("${mowerName} Error: ${errorMessage} @ ${timestamp}", "error")
    }
}

def apiConnectedHandler(evt) {
    def mower = evt.getDevice()
    def mowerName = mower.currentValue("name")
    def timestamp = evt.getDate().getTime()
    def apiConnected = evt.value     
    
    if (apiConnected == "lost") {
         notify("${mowerName} API Connection Lost @ ${timestamp}", "error")
         state.apiConnectionLost[mowerName] = true
    }
    else if (apiConnected == "full" && state.apiConnectionLost && state.apiConnectionLost[mowerName]) {
         notify("${mowerName} API Connection Restored @ ${timestamp}", "error")
        state.apiConnectionLost[mowerName] = false
    }
}

def mowerConnectedHandler(evt) {
    def mower = evt.getDevice()
    def mowerName = mower.currentValue("name")
    def timestamp = evt.getDate().getTime()
    def mowerConnected = evt.value     
    
    if (mowerConnected == "lost") {
         notify("${mowerName} Mower Connection Lost @ ${timestamp}", "error")
         state.mowerConnectionLost[mowerName] = true
    }
    else if (mowerConnected == "full" && state.mowerConnectionLost && state.mowerConnectionLost[mowerName]) {
         notify("${mowerName} Mower Connection Restored @ ${timestamp}", "error")
        state.mowerConnectionLost[mowerName] = false
    }
}

def stuckHandler(evt) {
    def mower = evt.getDevice()
    def mowerName = mower.currentValue("name")
    def stuckTime = evt.getDate().getTime()
    def stuck = evt.value 
    
    if (stuck == true) {
        notify("${mowerName} stuck @ ${timestamp}", "error")
        state.stuck[mowerName] = true
    }
    else if (stuck == false && state.stuck && state.stuck[mowerName]) {
        notify("${mowerName} unstuck @ ${timestamp}", "error")
        state.stuck[mowerName] = false
    }
}

def getRequiredMowingDuration() {
    def durationMS = null
    if(settings["durationType"] == "Certain Duration" && settings["duration"] != null) durationMS = settings["duration"] * 60000
    else if (settings["durationType"] == "Full Mowing Window") {
        def window = getMowingWindow()
        durationMS = window.end.getTime() - window.start.getTime()
    }
    return durationMS
}

def getRequiredMowingDurationMins() {
    def durationMS = getRequiredMowingDuration()
    return durationMS ? msToMins(durationMS) : null
}

// TO DO: consider buffering mow command if mower is going to the charging station (not sure if can command to mow when in that state)
def mowOne(serial) {
    for (mower in settings["husqvarnaMowers"]) { 
        def serialNum = mower.currentValue("serialNumber")        
        if (serial == serialNum) {
            logDebug("${app.label} mowOne() executing")
            def isMowing = (mower.currentValue("mowerActivity") == "MOWING" || mower.currentValue("LEAVING") == true) ? true : false
            def requiresManualAction =  (mower.currentValue("mowerActivity") == "STOPPED_IN_GARDEN" || mower.currentValue("mowerActivity") == "NOT_APPLICABLE") ? true : false
            if (!isMowing && !requiresManualAction) {
                // TO DO: figure out if still need to send resumeSchedule command if mowing currently forced
                state.mowers[serialNum]?.parkedByApp = false
                state.mowers[serialNum]?.pausedByApp = false
                state.mowers[serialNum]?.userForcingMowing = false
                mower.resumeSchedule()
                notify("Mower Serial Num ${serialNum} resuming schedule", "stopStart")
            }
        }
        else logDebug("Mow command not sent. Mower either already mowing or requires manual action.")
    }  
}

def parkAll(preCheckPark = false) {
    for (mower in settings["husqvarnaMowers"]) { 
        def mowerState = mower.currentValue("mowerState")
        def serialNum = mower.currentValue("serialNumber")
        if (state.mowers[serialNum]?.parkedByApp == false) { // avoids sending duplicate commands
            def isParked = (mower.currentValue("mowerActivity") == "PARKED_IN_CS" || mower.currentValue("mowerActivity") == "GOING_HOME" || mower.currentValue("mowerActivity") == "CHARGING" || mower.currentValue("parked") == true) ? true : false
            def requiresManualAction =  (mower.currentValue("mowerActivity") == "STOPPED_IN_GARDEN" || mower.currentValue("mowerActivity") == "NOT_APPLICABLE") ? true : false
            if ((mowerState == "IN_OPERATION" && !requiresManualAction) || mowerState == "PAUSED" || mowerState == "WAIT_UPDATING" || mowerState == "WAIT_POWER_UP" || (isParked && preCheckPark)) {
                // send park command even if already parked, because park command is to park indefintely, and may be parked temporarily
                state.mowers[serialNum]?.parkedByApp = true
                state.mowers[serialNum]?.pausedByApp = false
                state.mowers[serialNum]?.userForcingMowing = false
                mower.parkindefinite()
                notify("${app.label} Mowing Stopped", "stopStart")            
            }
            else logDebug("Park command not sent. Mower either already parked or going to park, or requires manual action.")
        }
        else logDebug("Park command not sent. Mower already parked by app.")
    }     
}

def parkOne(serial, preCheckPark = false) {
    for (mower in settings["husqvarnaMowers"]) { 
        def serialNum = mower.currentValue("serialNumber")
       if (serial == serialNum) {
           def mowerState = mower.currentValue("mowerState")
           if (state.mowers[serialNum]?.parkedByApp == false) { // avoids sending duplicate commands
               def activity = mower.currentValue("mowerActivity")
               // TO DO: avoid sending repeat park commands when already sent command and going home. Maybe hold off until mower state updates? So rate-limit park commands based on poll interval.
               def isParked = (mower.currentValue("mowerActivity") == "PARKED_IN_CS" || mower.currentValue("mowerActivity") == "GOING_HOME" || mower.currentValue("mowerActivity") == "CHARGING" || mower.currentValue("parked") == true) ? true : false
               def requiresManualAction =  (mower.currentValue("mowerActivity") == "STOPPED_IN_GARDEN" || mower.currentValue("mowerActivity") == "NOT_APPLICABLE") ? true : false
                if ((mowerState == "IN_OPERATION" && !requiresManualAction) || mowerState == "PAUSED" || mowerState == "WAIT_UPDATING" || mowerState == "WAIT_POWER_UP" || (isParked && preCheckPark)) {
                // send park command even if already parked, because park command is to park indefintely, and may be parked temporarily
                   state.mowers[serialNum]?.parkedByApp = true
                   state.mowers[serialNum]?.pausedByApp = false
                   state.mowers[serialNum]?.userForcingMowing = false
                   mower.parkindefinite()
                   notify("${app.label} Mowing Stopped", "stopStart")
               }
               else logDebug("Park command not sent. Mower either already parked or going to park, or requires manual action.")
            }
           else logDebug("Park command not sent. Mower already parked by app.")
       }
    }     
}

def park(data) {
    def serial = data.serial
    parkOne(serial)
}

def pauseOne(serial) {
    for (mower in settings["husqvarnaMowers"]) { 
        def serialNum = mower.currentValue("serialNumber")
        if (serial == serialNum) {
            if (state.mowers[serialNum]?.pausedByApp == false) {
                def isPaused = mower.currentValue("mowerState") == "PAUSED" ? true : false
                def isParked = (mower.currentValue("mowerActivity") == "PARKED_IN_CS" || mower.currentValue("mowerActivity") == "CHARGING" || mower.currentValue("parked") == true) ? true : false           
                def requiresManualAction =  (mower.currentValue("mowerActivity") == "STOPPED_IN_GARDEN" || mower.currentValue("mowerActivity") == "NOT_APPLICABLE") ? true : false
                if (!isPaused && !isParked && !requiresManualAction) {
                    state.mowers[serialNum]?.parkedByApp = false
                    state.mowers[serialNum]?.pausedByApp = true
                    state.mowers[serialNum]?.userForcingMowing = false
                    mower.pause()
                    notify("${app.label} Mowing Paused", "stopStart")
                }
            }
            else logDebug("Pause command not sent. Mower either already paused, is parked, or requires manual action.")
        }
    }     
}

def isAnyParkConditionMet(backupPrecheck = false) {
    def isMet = false
    if (anyMowerForcingMowing()) {
        logDebug("Checking if any park conditions are met for forced mowing")

        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Leaf Wetness Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.leafWetness) && state.parkConditions.leafWetness == true) isMet = true
        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Humidity or Soil Moisture Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.humidity) && state.parkConditions.humidity == true) isMet = true
        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Water Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.water) && state.parkConditions.water == true) isMet = true
        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Irrigation") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.valve) && state.parkConditions.valve == true) isMet = true
        if (settings["forcedMowingParkConditionsEnforced"].contains("Wet Grass: Open Weather Device") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.weather) && state.parkConditions.weather == true) isMet = true
        if (settings["forcedMowingParkConditionsEnforced"].contains("Temperature Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.temperature) && state.parkConditions.temperature == true) isMet = true
        if (settings["forcedMowingParkConditionsEnforced"].contains("Presence Sensor") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.presence) && state.parkConditions.presence  == true) isMet = true
        if (settings["forcedMowingParkConditionsEnforced"].contains("Switch(es)") && (state.forcedMowingParkConditionSnapshot == null || !state.forcedMowingParkConditionSnapshot.switchSensors) && state.parkConditions.switchSensors  == true) isMet = true        
    }
    else if (isBackupMowingScheduledForNow() || backupPrecheck == true) {
        // check parking conditions accounting for any that are disabled for the backup window
        logDebug("Checking if any park conditions are met for backup window")

        if (!settings["backupParkConditionsIgnored"].contains("Wet Grass: Leaf Wetness Sensor") && state.parkConditions.leafWetness == true) isMet = true
        if (!settings["backupParkConditionsIgnored"].contains("Wet Grass: Humidity or Soil Moisture Sensor") && state.parkConditions.humidity == true) isMet = true
        if (!settings["backupParkConditionsIgnored"].contains("Wet Grass: Water Sensor") && state.parkConditions.water == true) isMet = true
        if (!settings["backupParkConditionsIgnored"].contains("Wet Grass: Irrigation") && state.parkConditions.valve == true) isMet = true
        if (!settings["backupParkConditionsIgnored"].contains("Wet Grass: Open Weather Device") && state.parkConditions.weather == true) isMet = true
        if (!settings["backupParkConditionsIgnored"].contains("Temperature Sensor") && state.parkConditions.temperature == true) isMet = true
        if (!settings["backupParkConditionsIgnored"].contains("Presence Sensor") && state.parkConditions.presence  == true) isMet = true
        if (!settings["backupParkConditionsIgnored"].contains("Switch(es)") && state.parkConditions.switchSensors  == true) isMet = true
    }
    else {
        logDebug("Checking if any park conditions are met for primary window")
        state.parkConditions.each { key, value ->
            logDebug("checking condition: ${key} with value of ${value}")
            if (value == true) isMet = true
        }
    }
    return isMet
}

def handleParkConditionChange() {
    if (isMowingScheduledForNow() || isBackupMowingScheduledForNow() || anyMowerForcingMowing()) {
        if (isAnyParkConditionMet()) {
            logDebug("Park Conditions Met")
            for (mower in settings["husqvarnaMowers"]) { 
                def serialNum = mower.currentValue("serialNumber")
                logDebug("Commanding mower to park")
                parkOne(serialNum) // park mower if park conditions become met during a scheduled mowing window, except if the user has forced mowing to override the schedule
            } 
        }
        else {
           logDebug("No Park Conditions Met.")
           for (mower in settings["husqvarnaMowers"]) { 
               def serialNum = mower.currentValue("serialNumber")
               if (state.mowers[serialNum]?.parkedByApp == true) mowOne(serialNum) // call mowOne if no park conditions met anymore, will resume schedule (if mowing window already over, resuming schedule will park mower anyway)
               else logDebug("But not commanding to mow, either because mower is already mowing or because app did not park mower.")
            }
        }
    }
    
    updateDeviceData([parkFromLeafWetness: state.parkConditions.leafWetness, parkFromWeather: state.parkConditions.weather, parkFromTemp: state.parkConditions.temperature, parkFromHumidity: state.parkConditions.humidity, parkFromValve: state.parkConditions.valve, parkFromPreence: state.parkConditions.presence, parkFromWaterSensor: state.parkConditions.water, parkFromSwitch: state.parkConditions.switchSensors])
}

def temperatureHandler(evt) {
    def sensorId = evt.getDeviceId().toString()
    def temp = evt.value.toFloat()
    if (state.temp == null) state.temp = [:]
    if (state.temp[sensorId] == null) state.temp[sensorId] = [:] 
    if (temp >= settings["parkTempThreshold"]) {
        if (state.temp[sensorId].numAbove == null) {
           state.temp[sensorId].numAbove = 1
        }
        else state.temp[sensorId].numAbove++
        state.temp[sensorId].numBelow = 0    
    }
    else {
        if (state.temp[sensorId].numBelow  == null) state.temp[sensorId].numBelow  = 1
        else state.temp[sensorId].numBelow ++
        state.temp[sensorId].numAbove = 0  
    }
    
    if (state.temp[sensorId].numAbove != null && state.temp[sensorId].numAbove >= settings["parkTempThresholdTimes"]) {
        state.temp[sensorId]["tooHot"] = true
        handleTempChange()
    }
    else if (state.temp[sensorId].numBelow  != null && state.temp[sensorId].numBelow  >= settings["parkTempThresholdTimes"]) {
        state.temp[sensorId]["tooHot"] = false
        handleTempChange()
    }
}

def handleTempChange() {
    def anyMet = false
    state.temp.each { deviceId, sensor ->
        if (sensor["tooHot"] != null && sensor["tooHot"] == true) {
            anyMet = true
        }
    }   
    state.parkConditions.temperature = anyMet
    handleParkConditionChange()
}

def updateAllParkConditions() {
    if (settings["parkWhenTempHot"] && settings["parkTempSensor"] && settings["parkTempThreshold"]) {
        def temp = settings["parkTempSensor"].currentValue("temperature")
        if (temp >= settings["parkTempThreshold"]) state.parkConditions.temperature = true
        else state.parkConditions.temperature = false
    }   
    else state.parkConditions.temperature = false
    if (settings["parkWhenPresenceArrivesLeaves"] && settings["presenceSensors"] && settings["presencePresentAbsent"]) {
        def anyMet = false
        for (sensor in settings["presenceSensors"]) {
            if (sensor.currentValue("presence") == settings["presencePresentAbsent"]) anyMet = true
        }
        state.parkConditions.presence = anyMet
    }
    else state.parkConditions.presence = false
    if (settings[parkWhenSwitchOnOff] && settings["parkSwitches"] && settings["switchOnOff"]) {
        def anyMet = false
        for (sw in settings["parkSwitches"]) {
            if (sw.currentValue("switch") == settings["switchOnOff"]) anyMet = true
        }
        state.parkConditions.switchSensors = anyMet
    }
    else state.parkConditions.switchSensors = false
    if (settings["parkWhenGrassWet"] && settings["leafWetnessSensor"] && settings["leafWetnessThreshold"]) {
        def anyMet = false
        for (sensor in settings["leafWetnessSensor"]) {
            def leafWetness = sensor.currentValue("leafWetness")
            if (leafWetness >= settings["leafWetnessThreshold"]) anyMet = true
        }
        state.parkConditions.leafWetness = anyMet
    }
    else state.parkConditions.leafWetness = false
    
    if (settings["parkWhenGrassWet"] && settings["humidityMeasurement"] && settings["humidityThreshold"]) {
        def anyMet = false
        for (sensor in settings["humidityMeasurement"]) {
           def humidity = sensor.currentValue("humidity")
           if (humidity >= settings["humidityThreshold"])  anyMet = true
        }
        state.parkConditions.humidity = anyMet
    }
    else state.parkConditions.humidity = false
    if (settings["parkWhenGrassWet"] && settings["waterSensor"]) {
        def anyMet = false
        for (sensor in settings["waterSensor"]) {
           def water = sensor.currentValue("water")
           if (water == "wet") anyMet = true
        }
        state.parkConditions.water = anyMet
    }
    else state.parkConditions.water = false
    
        // NOTE: weather and irrigation persistently scheduled, but update values in case disabled
    if (!settings["parkWhenGrassWet"]) {
        state.parkConditions.weather = false
        state.parkConditions.valve = false
    }
    else if (settings["openWeatherDevice"] == null) state.parkConditions.weather = false
    else if (settings["irrigationValves"] == null) state.parkConditions.valve = null 
        
    updateDeviceData([parkFromLeafWetness: state.parkConditions.leafWetness, parkFromWeather: state.parkConditions.weather, parkFromTemp: state.parkConditions.temperature, parkFromHumidity: state.parkConditions.humidity, parkFromValve: state.parkConditions.valve, parkFromPreence: state.parkConditions.presence, parkFromWaterSensor: state.parkConditions.water, parkFromSwitch: state.parkConditions.switchSensors])
}

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
    def sensorId = evt.getDeviceId().toString()
    def leafWetness = evt.value.toInteger()
    if (state.leafWetness == null) state.leafWetness = [:]
    if (state.leafWetness[sensorId] == null) state.leafWetness[sensorId] = [:] 
    if (leafWetness >= settings["leafWetnessThreshold"]) {
        if (state.leafWetness[sensorId].numAbove == null) {
           state.leafWetness[sensorId].numAbove = 1
        }
        else state.leafWetness[sensorId].numAbove++
        state.leafWetness[sensorId].numBelow = 0    
    }
    else {
        if (state.leafWetness[sensorId].numBelow  == null) state.leafWetness[sensorId].numBelow  = 1
        else state.leafWetness[sensorId].numBelow ++
        state.leafWetness[sensorId].numAbove = 0  
    }
    
    if (state.leafWetness[sensorId].numAbove != null && state.leafWetness[sensorId].numAbove >= settings["leafWetnessThresholdTimes"]) {
        state.leafWetness[sensorId]["wet"] = true
        handleLeafWetnessChange()
    }
    else if (state.leafWetness[sensorId].numBelow  != null && state.leafWetness[sensorId].numBelow  >= settings["leafWetnessThresholdTimes"]) {
        state.leafWetness[sensorId]["wet"] = false
        handleLeafWetnessChange()
    }
}

def handleLeafWetnessChange() {
    def anyMet = false
    state.leafWetness.each { deviceId, sensor ->
        if (sensor["wet"] != null && sensor["wet"] == true) {
            anyMet = true
        }
    }   
    state.parkConditions.leafWetness = anyMet
    handleParkConditionChange()
}

def openWeatherHandler(evt) {
    def weather = evt.value  
    if (weather == "thunderstorm" || weather == "drizzle" || weather == "rain" || weather == "thunderstorm" || weather == "snow") {
        state.parkConditions.weather = true
        unschedule(delayedWeather)
        updateDeviceData([parkFromWeatherExpires: "none"])
        handleParkConditionChange()
    }
    else if (state.parkConditions?.weather == null) {
        state.parkConditions.weather = false
        updateDeviceData([parkFromWeatherExpires: "none"])
        handleParkConditionChange()
    }
    else if (state.parkConditions?.weather != null && state.parkConditions.weather == true) {
      if (settings["weatherWetDuringDay"] == false) runIn(settings["weatherWetDuration"]*60, delayedWeather)
        else if (settings["weatherWetDuringDay"] == true) {
             def dayTime = getSunriseAndSunset()
             def now = new Date()
             def delayedTime = adjustDateByMins(now, settings["weatherWetDuration"].toInteger())
            if (now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                if (delayedTime.after(dayTime.sunrise) && dayTime.sunset.after(delayedTime)) {
                    // full delay in the daytime, so just schedule for delayedTime
                    runOnce(delayedTime, delayedWeather)
                    updateDeviceData([parkFromWeatherExpires: delayedTime.format("h:mm a")])
                }
                else if (delayedTime.after(dayTime.sunrise) && !dayTime.sunset.after(delayedTime)) {
                    // sun sets before delay ends
                    def partialDelay = getSecondsBetweenDates(now, dayTime.sunset)
                    def delayRemainder = (settings["weatherWetDuration"]*60) - partialDelay
                    Integer delayRemainderMins = Math.round(delayRemainder/60)
                    def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: delayRemainderMins])
                    def offsetSunriseTomorrow = offsetSunriseToday.sunrise + 1 // approximate tomorrow's sunrise as today's sunrise
                    runOnce(offsetSunriseTomorrow, delayedWeather) 
                    updateDeviceData([parkFromWeatherExpires: offsetSunriseTomorrow.format("h:mm a")])
                }
            }
            else if (now.after(dayTime.sunrise) && !dayTime.sunset.after(now)) {
                // after sunset already, so full delay will be after sunrise tomorrow
                def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: settings["weatherWetDuration"]])
                def offsetSunriseTomorrow = offsetSunriseToday.sunrise + 1 // approximate tomorrow's sunrise as today's sunrise
                runOnce(offsetSunriseTomorrow, delayedWeather)
                updateDeviceData([parkFromWeatherExpires: offsetSunriseTomorrow.format("h:mm a")])
            }
            else if (!now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                // before sunrise, so full delay will be after sunrise today
                def offsetSunrise = getSunriseAndSunset([sunriseOffset: settings["weatherWetDuration"]])
                runOnce(offsetSunrise.sunrise, delayedWeather)
                updateDeviceData([parkFromWeatherExpires: offsetSunrise.sunrise.format("h:mm a")])
            }
        }        
    }
}

def delayedWeather() {
    def weather = settings["openWeatherDevice"]?.currentValue("condition_code")
    if (weather != "thunderstorm" && weather != "drizzle" && weather != "rain" && weather != "thunderstorm" && weather != "snow") {
        state.parkConditions.weather = false
        updateDeviceData([parkFromWeatherExpires: "none"])
        handleParkConditionChange()   
    }
}

def humidityHandler(evt) {
    def sensorId = evt.getDeviceId().toString()
    def humidity = evt.value.toFloat()
    if (state.humidity == null) state.humidity = [:]
    if (state.humidity[sensorId] == null) state.humidity[sensorId] = [:] 
    if (humidity >= settings["humidityThreshold"]) {
        if (state.humidity[sensorId].numAbove == null) {
           state.humidity[sensorId].numAbove = 1
        }
        else state.humidity[sensorId].numAbove++
        state.humidity[sensorId].numBelow = 0    
    }
    else {
        if (state.humidity[sensorId].numBelow  == null) state.humidity[sensorId].numBelow  = 1
        else state.humidity[sensorId].numBelow ++
        state.humidity[sensorId].numAbove = 0  
    }
    
    if (state.humidity[sensorId].numAbove != null && state.humidity[sensorId].numAbove >= settings["humidityThresholdTimes"]) {
        state.humidity[sensorId]["humid"] = true
        handleHumidityChange()
    }
    else if (state.humidity[sensorId].numBelow  != null && state.humidity[sensorId].numBelow  >= settings["humidityThresholdTimes"]) {
        state.humidity[sensorId]["humid"] = false
        handleHumidityChange()
    }
}

def handleHumidityChange() {
    def anyMet = false
    state.humidity.each { deviceId, sensor ->
        if (sensor["humid"] != null && sensor["humid"] == true) {
            anyMet = true
        }
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
        if (status == "open") {
            anyOpen = true
            logDebug("Irrigation Valve Open: ${valve.label}")
        }
    }
    
    if (anyOpen == true) {
        state.parkConditions.valve = true
        unschedule(delayedIrrigationValveClosed)
        updateDeviceData([parkFromWeatherExpires: "none"])
        handleParkConditionChange()
    }
    else if (anyOpen == false && state.parkConditions?.valve == null) {
        state.parkConditions.valve = false
        updateDeviceData([parkFromWeatherExpires: "none"])
        handleParkConditionChange()
    }
    else if (anyOpen == false && state.parkConditions?.valve != null && state.parkConditions.valve == true) {
        logDebug("Irrigation valve(s) all turned off now. Scheduling delayed update of park conditions according to settings.")
        if (settings["irrigationWetDuringDay"] == false) runIn(settings["irrigationWetDuration"]*60, delayedIrrigationValveClosed)
        else if (settings["irrigationWetDuringDay"] == true) {
             def dayTime = getSunriseAndSunset()
             def now = new Date()
             def wetDuration = settings["irrigationWetDuration"].toInteger()
             def delayedTime = adjustDateByMins(now, wetDuration)
            if (now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                if (delayedTime.after(dayTime.sunrise) && dayTime.sunset.after(delayedTime)) {
                    // full delay in the daytime, so just schedule for delayedTime
                    runOnce(delayedTime, delayedIrrigationValveClosed)
                    updateDeviceData([parkFromValveExpires: delayedTime.format("h:mm a")])
                }
                else if (delayedTime.after(dayTime.sunrise) && !dayTime.sunset.after(delayedTime)) {
                    // sun sets before delay ends
                    def partialDelay = getSecondsBetweenDates(now, dayTime.sunset)
                    def delayRemainder = (settings["irrigationWetDuration"]*60) - partialDelay
                    Integer delayRemainderMins = Math.round(delayRemainder/60)
                    def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: delayRemainderMins])
                    def offsetSunriseTomorrow = offsetSunriseToday.sunrise + 1 // approximate tomorrow's sunrise as today's sunrise
                    runOnce(offsetSunriseTomorrow, delayedIrrigationValveClosed)  
                    updateDeviceData([parkFromValveExpires: offsetSunriseTomorrow.format("h:mm a")])
                }
            }
            else if (now.after(dayTime.sunrise) && !dayTime.sunset.after(now)) {
                // after sunset already, so full delay will be after sunrise tomorrow                
                def offsetSunriseToday = getSunriseAndSunset([sunriseOffset: wetDuration])
                def offsetSunriseTomorrow = offsetSunriseToday.sunrise + 1 // approximate tomorrow's sunrise as today's sunrise
                runOnce(offsetSunriseTomorrow, delayedIrrigationValveClosed)
                updateDeviceData([parkFromValveExpires: offsetSunriseTomorrow.format("h:mm a")])
            }
            else if (!now.after(dayTime.sunrise) && dayTime.sunset.after(now)) {
                // before sunrise, so full delay will be after sunrise today
                def offsetSunrise = getSunriseAndSunset([sunriseOffset: wetDuration])
                runOnce(offsetSunrise.sunrise, delayedIrrigationValveClosed)
                updateDeviceData([parkFromValveExpires: offsetSunrise.sunrise.format("h:mm a")])
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
    if (isAnyPauseConditionMet()) {
        if (isMowingScheduledForNow() || isBackupMowingScheduledForNow()) {
            for (mower in settings["husqvarnaMowers"]) { 
                def serialNum = mower.currentValue("serialNumber")
                if (state.mowers[serialNum].userForcingMowing == false) pauseOne(serialNum) // park mower if park conditions become met during a scheduled mowing window, except if the user has forced mowing to override the schedule
            } 
        }
    }
    else {
       for (mower in settings["husqvarnaMowers"]) { 
          def serialNum = mower.currentValue("serialNumber")
           if (state.mowers[serialNum].pausedByApp == true) mowOne(serialNum) // call mowOne if no pause conditions met anymore, will resume schedule (if mowing window already over, resuming schedule will park mower anyway)
        }
    }
    
    updateDeviceData([pauseFromButton: state.pauseConditions.button, pauseFromMotion: state.pauseCondtions.motion, pauseFromContact: state.pauseConditions.contact, pauseFromPresence: state.pauseConditions.presence, pauseFromSwitch: state.pauseConditions.switchSensors])
}

def updateAllPauseConditions() {
    if(settings["pause_motionSensors"]) {
        def anyMet = false
        for (sensor in settings["pause_motionSensors"]) {
            if (sensor.currentValue("motion") == "active") anyMet = true
        }
        state.pauseConditions.motion = anyMet   
    }
    else state.pauseConditions.motion = false
    // set contact, presence, and button to default false values
    state.pauseConditions.contact = false
    state.pauseConditions.presence = false
    state.pauseConditions.button = false
    if (settings["pauseSwitches"] && settings["pause_switchOnOff"]) {
        def anyMet = false
        for (sensor in settings["pauseSwitches"]) {
            if (sensor.currentValue("switch") == settings["pause_switchOnOff"]) anyMet = true
        }
        state.pauseConditions.switchSensors = anyMet
    }
    else state.pauseConditions.switchSensors = false
    
    updateDeviceData([pauseFromButton: state.pauseConditions.button, pauseFromMotion: state.pauseConditions.motion, pauseFromContact: state.pauseConditions.contact, pauseFromPresence: state.pauseConditions.presence, pauseFromSwitch: state.pauseConditions.switchSensors])
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

def isActivated() {
    return state.activated    
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

def secsToMins(secs) {
    return Math.round(secs/60)    
}

def getMinutesBetweenDates(Date startDate, Date endDate) {
    def secs = getSecondsBetweenDates(startDate, endDate)
    return secs != null ? secsToMins(secs) : null
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

def adjustTimeBySecs(time, Integer secs) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    def dateTime = toDateTime(time)
    cal.setTime(dateTime)
    cal.add(Calendar.SECOND, secs)
    Date newDate = cal.getTime()
    return newDate
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
        case "highlightedInputRed": 
            return "<div style='color:#000000;font-weight: bold;background-color:red;border: 0px solid'> ${txt}</div>"
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

