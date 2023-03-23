/**
 *  MowBot Tamer Device
 *
 *  Copyright\u00A9 2022 Justin Leonard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
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
 *  v0.0.5 - Bug fixes
 *  v0.0.6 - Added delay for water sensor; Bug fixes
 *  v0.0.7 - Bug fixes
 *  v0.0.8 - Added threshold options for backup window; Bug fixes
 *  v0.0.9 - Static cutting height
 *  v0.0.10 - Bug Fixes
 *  v0.0.11 - Bug Fixes; Added threshold options for forced mowing
 *  v0.0.12 - Bug Fixes; Enhancements to companion device
**/

metadata
{
    definition(name: "MowBot Tamer Device", namespace: "lnjustin", author: "Justin Leonard", importUrl: "")
    {
        capability "Switch"
        
        attribute "isInMowingWindow", "boolean"
        attribute "windowStart", "string"
        attribute "windowEnd", "string"
        
        attribute "lastStartedMowing", "string"
        attribute "lastStoppedMowing", "string"
        attribute "nextMowingStart", "string"
        attribute "nextMowingStop", "string"
        
        attribute "minsMowedToday", "number" 
        attribute "minsMowedTodayString", "string" 
        
        attribute "backupTriggered", "boolean" 
        attribute "backupDuration", "number" 
 
        attribute "forcingPark", "boolean" 
        attribute "forcingPause", "boolean" 
        attribute "forcingMowing", "boolean" 
        
        attribute "leafWetnessStatus", "enum", ["Transitioning Below", "Transitioning Above", "Above", "Below"]
        attribute "temperatureStatus", "enum", ["Transitioning Below", "Transitioning Above", "Above", "Below"]
        attribute "humidityStatus", "enum", ["Transitioning Below", "Transitioning Above", "Above", "Below"]     
        
        attribute "parkFromLeafWetness", "boolean"
        attribute "parkFromWeather", "boolean"
        attribute "parkFromWeatherExpires", "string"
        attribute "parkFromTemp", "boolean"
        attribute "parkFromHumidity", "boolean"
        attribute "parkFromValve", "boolean"
        attribute "parkFromValveExpires", "string"
        attribute "parkFromPresence", "boolean"
        attribute "parkFromWaterSensor", "boolean"
        attribute "parkFromWaterSensorExpires", "string"
        attribute "parkFromSwitch", "boolean"
        attribute "parkFromGrassWet", "boolean"
        
        attribute "motion", "string"
        attribute "contact", "string"
        attribute "pausePresence", "string"
        attribute "pauseSwitch", "string"
        
        attribute "pauseFromButton", "boolean"
        attribute "pauseFromMotion", "boolean"
        attribute "pauseFromContact", "boolean"
        attribute "pauseFromPresence", "boolean"
        attribute "pauseFromSwitch", "boolean"
    }
}

preferences
{
    section
    {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def logDebug(msg) 
{
    if (logEnable)
    {
        log.debug(msg)
    }
}    

def on() {
    sendEvent(name: "switch", value: "on")
    parent.switchHandler("on")
}

def off() {
    sendEvent(name: "switch", value: "off")
    parent.switchHandler("off")
}

def updateData(data) {
    data.each { key, value ->
        sendEvent(name: key, value: value)
    }
}

def updated()
{
    configure()
}

def parse(String description)
{
    logDebug(description)
}
