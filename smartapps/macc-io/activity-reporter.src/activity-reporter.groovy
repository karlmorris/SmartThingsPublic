/**
 *  Copyright 2019 Morris and Company Consulting LLC
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
 *  Report Activity
 *
 *  Author: Karl Morris
 */

definition(
    name: "Activity Reporter",
    namespace: "macc.io",
    author: "Karl Morris",
    description: "Activity Reporter configuration client",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health & Wellness/health12-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health12-icn@2x.png" ){
    appSetting "registerUrl"
    appSetting "apiUrl"
   }

preferences {
	page(name: "pageOne", title: "Activity Monitor", nextPage: "pageTwo", uninstall: true) {
		section("Monitor Information"){
			paragraph "Click below to sign-up/log-in and retrieve your access token"
			href(name: "hrefNotRequired",
             title: "Get token...",
             required: false,
             style: "external",
             url: "https://macc.io/ha/account.php",
             description: "Tap to open your browser")
			 
			input "accessToken", "text", title: "Enter token", required: true
		}
	}
	
	page(name: "pageTwo", title: "When there's activity on any of these sensors", install: true, uninstall: true) {
		section("Monitor these sensors..."){
			input "motionSensors", "capability.motionSensor", multiple: true, title: "Where?"
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(motionSensors, "motion", motionHandler)
	subscribe(motionSensors, "battery", batteryHandler)
    registerSensors(motionSensors)
}

def batteryHandler(evt) {
	log.debug "Battery device ID: ${evt.device.id}"
	log.debug "Battery device display name: ${evt.device.displayName}"
	log.debug "Battery event information: ${evt.value}"
    try {
		httpPost(appSettings.apiUrl, "event=battery&value=${evt.value}&device_id=${evt.device.id}&user_token=$accessToken") { resp ->
			log.debug "response data: ${resp.data}"
			log.debug "response contentType: ${resp.contentType}"
		}
	} catch (e) {
		log.debug "something went wrong: ${evt.device}"
	}
}


def motionHandler(evt) {
def d = evt.device
log.debug "Event information: ${d.id}"
log.debug "Event information: ${d.displayName}"

	try {
		httpPost(appSettings.apiUrl, "action=action&event=${evt.value}&device_id=${d.id}&user_token=$accessToken&device_name=${d.displayName}&device_type_name=motionSensor") { resp ->
			log.debug "response data: ${resp.data}"
			log.debug "response contentType: ${resp.contentType}"
		}
	} catch (e) {
		log.debug "something went wrong: ${d}"
	}
}

def registerSensors(sensors) {
	def postString = "user_token=$accessToken&device_type_name=motionSensor&"
	for (sensor in sensors) {
    	postString = postString + "name[]=${sensor.displayName}&id[]=${sensor.id}&"
	}
    postString = postString.substring(0, postString.length() - 1)
    
    log.debug "generated string: $postString"
	
	try {
		httpPost(appSettings.registerUrl, "$postString") { resp ->
			log.debug "response data: ${resp.data}"
			log.debug "response contentType: ${resp.contentType}"
		}
	} catch (e) {
		log.debug "something went wrong"
	}
}