/**
 *  Virtual Countdown Switch
 *
 */
public static String version()      {  return "v1.0"  }

metadata {
    definition (name: "VirtualCountdownSwitch", namespace: "israelforst-hubitat", author: "Israel Forst") {
        capability "Switch"
        capability "TimedSession"
        attribute  "timeElapsed", "number"
        attribute  "timeStarted", "text"
        attribute  "dateInstalled", "number"
        attribute  "totalTimeOn", "number"
    }

    preferences {
        input name: "autoStop", type: "bool", title: "Auto Stop", defaultValue: true
        input name: "autoStopTimeSec", type: "number", title: "Auto Stop Time (seconds)", defaultValue: 1800, required: true
        input name: "ignoreStart", type: "bool", title: "Ignore Start When Already Running", defaultValue: false
        input name: "maxRunTime", type: "number", title: "Maximum run time before shutoff (seconds)", defaultValue: 7200, required: true

        //standard logging options
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}



def installed() {
    log.debug "${device.displayName} installed()"
    state.dateInstalled = now()
    updated()  // since installed() rather than updated() will run the first time the user selects "Done"
}

def updated() {
    log.debug "${device.displayName}  updated()"
}

def uninstalled() {}

def logsStop(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}


//capability and device methods
def stop() {
    if (device.currentValue("sessionStatus") != "running") return

    state.endTime = now()
    state.timeElapsed = (state.endTime - state.startTime) / 1000
    
    if (logEnable) log.info "${device.displayName} checking if totalTimeOn is null"         
    if( state.totalTimeOn == null) { 
        if (logEnable) log.info "${device.displayName} Total time elapsed is null "       
        state.totalTimeOn = state.timeElapsed
         
    } else {
        if (logEnable) log.info "${device.displayName} Total time elapsed ${state.totalTimeOn} seconds"    
        state.totalTimeOn += state.timeElapsed        
    
    }
    



    if (logEnable) log.info "${device.displayName} time elapsed ${state.timeElapsed} seconds"
    sendEvent(name: "timeElapsed",
              value: state.timeElapsed,
              descriptionText: "${device.displayName} timeElapsed: ${state.timeElapsed}",
              unit: "s")
    sendEvent(name: "sessionStatus", value: "stop")
    sendEvent(name: "switch", value: "off")
}

def start() {
    if (ignoreStart && (device.currentValue("sessionStatus") == "running")) return
    sendEvent(name: "sessionStatus", value: "running")
    sendEvent(name: "switch", value: "on")
    state.startTime = now()
    state.timeStarted = new Date().format("MM/dd/yyyy HH:mm:ss")
    if (autoStop) runIn(autoStopTimeSec, stop)
    runIn(maxRunTime, checkRunTime)
}

def cancel() {
    sendEvent(name: "sessionStatus", value: "cancel")
    sendEvent(name: "switch", value: "off")
}

def pause() {
    if (logEnable) log.info "${device.displayName} doesn't support pause"
}

def checkRunTime() {
    if (logEnable) log.info "checkRunTime: ${device.displayName} is running" 
    if (device.currentValue("sessionStatus") == "running" ) {
        if (logEnable) log.info "checkRunTime: ${device.displayName} Checking if devices exceeded max runtime"         
        if (((now() - state.startTime) / 1000) > maxRunTime ) {
            if (logEnable) log.info "${device.displayName} time elapsed exceeds Max Run Time. Stopping..." 
            stop()
        }
    }
    
}
def setTimeRemaining(timeRemaining) {
    if (timeRemaining) {
        device.updateSetting("autoStopTimeSec",[type:"number", value:timeRemaining])
        device.updateSetting("autoStop",[type:"bool", value: true])
        if (logEnable) log.info "${device.displayName} Enable and set autoStopTimeSec to ${autoStopTime}"
    } else {
        device.updateSetting("autoStop",[type:"bool", value: false])
        if (logEnable) log.info "${device.displayName} Disabled autoStop"
    }
}

// Implement switch on/off so we can use the device in RM4
def on() {
    sendEvent(name: "switch", value: "on")
    start()
}

def off() {
    stop()
    sendEvent(name: "switch", value: "off")
}
