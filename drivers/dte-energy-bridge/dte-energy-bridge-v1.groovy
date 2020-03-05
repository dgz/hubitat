/*
DTE Energy Bridge v1 Sensor

3/4/2020 Josh Sutinen
- Released
*/

metadata {
    definition (
        name: "DTE Energy Bridge v1", 
        namespace: "dgz", 
        author: "Josh Sutinen") {
        
        capability "Sensor"
        capability "PowerMeter"
        capability "Refresh"
    }
}

preferences {
    input name: "bridgeIp", type: "text", title: "DTE Energy Bridge IP Address", required: true
    input name: "pollRate", type: "number", title: "Poll the bridge for the current power usage every n seconds", range: 1..60, defaultValue: 5, required: true
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed() {
    updated()
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    unschedule()
    if (bridgeIp && pollRate > 0) {
        schedule("0/${pollRate} * * * * ?", refresh)
    }
    if (logEnable) runIn(1800, logsOff)
}

def refresh() {
    if (logEnable) log.trace "Executing 'refresh' on http://${bridgeIp}/instantaneousdemand"
    asynchttpGet(parsePower, [uri: "http://${bridgeIp}/instantaneousdemand"])
}

def parsePower(response, data) {
    def status = response.getStatus();
    if (logEnable) log.debug "Response status was ${status}"
    if (status != 200)
        return
    
    def body = response.getData().split()
    def powerValue = Double.parseDouble(body[0]) * 1000
    if (logEnable) log.info "Home power usage is ${powerValue} W"
    sendEvent(name: "power", value: powerValue, unit: "W")
}
