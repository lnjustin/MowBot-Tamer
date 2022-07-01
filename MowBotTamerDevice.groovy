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
 *  v0.0.4 - beta
**/

metadata
{
    definition(name: "MowBot Tamer Device", namespace: "lnjustin", author: "Justin Leonard", importUrl: "")
    {
        capability "Switch"
        
        attribute "backupTriggered", "boolean" 
        attribute "backupDuration", "number" 
 
        attribute "forcingPark", "boolean" 
        attribute "forcingPause", "boolean" 
        attribute "forcingMowing", "boolean" 
        
        attribute "leafWetness", "number"
        attribute "weather", "string"
        attribute "temperature", "number"
        attribute "humidity", "number"
        attribute "valve", "string"
        attribute "parkPresence", "string"
        attribute "water", "string"
        attribute "parkSwitch", "string"        
        
        attribute "parkFromLeafWetness", "boolean"
        attribute "parkFromWeather", "boolean"
        attribute "parkFromWeatherExpires", "string"
        attribute "parkFromTemp", "boolean"
        attribute "parkFromHumidity", "boolean"
        attribute "parkFromValve", "boolean"
        attribute "parkFromValveExpires", "string"
        attribute "parkFromPresence", "boolean"
        attribute "parkFromWaterSensor", "boolean"
        attribute "parkFromSwitch", "boolean"
        
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
