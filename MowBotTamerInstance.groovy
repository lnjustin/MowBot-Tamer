/**
 *  MowBot Tamer Instance
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

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"
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
    page name: "instancePage", title: "", install: false, uninstall: true
}

def instancePage() {
    dynamicPage(name: "instancePage") {
       
            section {
              //  header()    
                paragraph getInterface("header", " MowBot Tamer Instance")
                label title: "Customize Instance Name:", required: true                
                
                input name: "trigger", type: "enum", title: "Instance Trigger Type", width: 4, options: ["By Date", "By Avg High/Low Temp", "By Switch", "Always"], submitOnChange:true, description: "Specify how to trigger the MowBot Tamer Instance."
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
                    paragraph "Trigger MowBot Tamer Instance when ANY of the selected switch(es) are ON:"
                    input name: "triggerSwitch", type: "capability.switch", title: "Trigger Switch", width: 12, multiple: true
                }
             //   input name: "exceptHolidays", type: "bool", title: "Except on Holidays?", submitOnChange: true, width: 3
                input name: "isDeactivated", type: "bool", title: "Manually Deactivate?", width: 3

            }
            if (trigger != null) {
                section (getInterface("header", " MowBot Tamer Rules")) {  
                    paragraph getInterface("note", " Manage rules for when to mow and when not to mow.")
                    app(name: "anyOpenApp", appName: "MowBot Tamer Rule", namespace: "lnjustin", title: "<b>Add a new MowBot Tamer Rule</b>", multiple: true)
                }
            }
            section("") {                    
                footer()   
                href url: "/installedapp/configure/$parent.id/mainPage", title: "Done", width: 2, description: ""
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
	initialize()
}

def uninstalled() {
	logDebug "Uninstalled app"
}

def initialize() {
    logDebug("Initializing ${app.label}")
    if (isDeactivated == null || isDeactivated == false) {
        if (trigger == "By Avg High/Low Temp") {
            subscribe(tempSensor, "temperature", tempSensorHandler)
            schedule("01 00 00 ? * *", tempTriggerCheck)
            tempTriggerCheck()
        }
        else if (trigger == "By Switch" && triggerSwitch != null) {
            state.switch = triggerSwitch.currentValue("switch")
            subscribe(triggerSwitch, "switch", switchHandler)
            switchTriggerCheck()
        }
        else if (trigger == "By Date") {
            logDebug("Checking Trigger By Date in ${app.label}")
            dateTriggerCheck()
            schedule("01 00 00 ? * *", dateTriggerCheck)
        }
        
    }
    else {
        deActivateRules()
    }
}

def tempTriggerCheck() {
    updateAverageTemp()
    def temp = settings["tempHighLow"] == "High Temp" ? state.averageHigh : state.averageLow
    if (settings["tempDirection"] == "Falls Below") {
        if (temp < settings["tempThreshold"]) activateRules()
        else deactivateRules()
    }
    else if (settings["tempDirection"] == "Rises Above") {
        if (temp > settings["tempThreshold"]) activateRules()
        else deactivateRules()        
    }
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

def switchHandler(evt) {
    state.switch = evt.value
    switchTriggerCheck()
}

def switchTriggerCheck() {
    if (state.switch == "on" && areExceptionsMet() == false) activateRules()
    else deActivateRules()    
}

def dateTriggerCheck() {
    if (isTodayWithinTriggerDates() == true && areExceptionsMet() == false) activateRules()
    else deActivateRules()
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

def activateRules() {
    childApps.each { child ->
        child.activateRule()
    }  
}

def deActivateRules() {
    childApps.each { child ->
        child.deActivateRule()
    }  
}

def getChildAppNames() {
    def childNames = []
    childApps.each { child ->
        childNames.add(child.label)
    }    
    return childNames
}

def activateBackupRule(appName, duration) {
    def childNames = getChildAppNames()
    childApps.each { child ->
        if (child.isBackupRule() && child.ruleBacksUp()?.contains(appName)) child.activateRule(duration)       
    } 
}

def hasBackupRule(ruleName) {
    def hasBackup = false
    def childNames = getChildAppNames()
    childApps.each { child ->
        if (child.isBackupRule() && child.ruleBacksUp()?.contains(ruleName)) hasBackup = true      
    }    
    return hasBackup
}

def setHeightStateInPrimaryRule(ruleName, isMissed, isFulfilled) {
    def childNames = getChildAppNames()
    childApps.each { child ->
        if (child.name.contains(ruleName) && child.isPrimaryRule()) {
            if (isMissed) incrementMissedWindows()
            else incrementFulfilledWindows()
        }
        else if (child.isBackupRule() && child.ruleBacksUp()?.contains(ruleName)) {
           setHeightStateInPrimaryRule(child.name, isMissed, isFulfilled)   
            // recursively traverse the parent-child tree up to the primary rule
        }
    }        
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

def logDebug(msg) {
    if (parent.getLogDebugEnabled()) {
		log.debug msg
	}
}

def getLogDebugEnabled() {
   return parent.getLogDebugEnabled()
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


