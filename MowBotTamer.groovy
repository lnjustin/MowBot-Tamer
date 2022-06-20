/**
 *  MowBot Tamer
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
 */
import java.text.SimpleDateFormat
import groovy.transform.Field

definition(
    name: "MowBot Tamer",
    namespace: "lnjustin",
    author: "Justin Leonard",
    description: "Robot Mower Tamer",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"

preferences {
    page name: "mainPage", title: "", install: true, uninstall: false
    page name: "removePage", title: "", install: false, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	    installCheck()
		    if(state.appInstalled == 'COMPLETE'){       
                section (getInterface("header", " MowBot Tamer")) {
			        section(getInterface("header", " Mowers")) {
				        app(name: "anyOpenApp", appName: "MowBot Tamer Instance", namespace: "lnjustin", title: "<b>Add a new MowBot Tamer instance</b>", multiple: true)
			        }
                }
                section (getInterface("header", " Notifications")) {
                   input "notificationDevices", "capability.notification", title: "Notification Devices", multiple:true, required:false, submitOnChange:true
                   input("notifyErrors", "bool", title: "Error Notifications?", defaultValue: true, displayDuringSetup: false, required: false)
                   input("notifyCuttingHeight", "bool", title: "Dynamic Cutting Height Notifications?", defaultValue: true, displayDuringSetup: false, required: false)
                    input("notifyBackupRule", "bool", title: "Mowing Backup Rule Notifications?", defaultValue: true, displayDuringSetup: false, required: false)
                   input("notifyStopStart", "bool", title: "Mower Stop/Start Notifications?", defaultValue: true, displayDuringSetup: false, required: false)

                }
			    section (getInterface("header", " General Settings")) {
                    input("debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false)
		        }
            }
            section("") {
                href(name: "removePage", title: getInterface("boldText", "Remove MowBot Tamer"), description: "", required: false, page: "removePage")
                
                footer()
            }
    }
}

def removePage() {
	dynamicPage(name: "removePage", title: "Remove MowBot Tamer", install: false, uninstall: true) {
		section ("WARNING!\n\nRemoving MowBot Tamer\n") {
		}
	}
}

def getNotificationTypes() {
    return [errors: notifyErrors, cuttingHeight: notifyCuttingHeight, stopStart: notifyStopStart]    
}

def notify(message, type) {
    if (type == "error" && settings["notifyErrors"] == true) notificationDevices.deviceNotification(message)
    else if (type == "cuttingHeight" && settings["notifyCuttingHeight"] == true) notificationDevices.deviceNotification(message)
    else if (type == "backupRule" && settings["notifyBackupRule"] == true) notificationDevices.deviceNotification(message)
    else if (type == "stopStart" && settings["notifyStopStart"] == true) notificationDevices.deviceNotification(message)
}

def footer() {
    paragraph getInterface("line", "") + '<div style="display: block;margin-left: auto;margin-right: auto;text-align:center">&copy; 2020 Justin Leonard.<br>'
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
    childApps.each { child ->
        child.updated()                
    }
}

def installCheck(){
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
		section{paragraph "Please hit 'Done' to install '${app.label}' parent app "}
  	}
  	else{
    	log.info "Parent Installed"
  	}
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}

def getLogDebugEnabled() {
    return settings?.debugOutput
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


