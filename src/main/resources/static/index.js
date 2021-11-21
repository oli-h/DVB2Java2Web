var dvbApp = angular.module('dvbApp', []);

dvbApp.controller('IndexController', function IndexController($scope, $http, $timeout, $interval) {
    const index = this;
    const nowSeconds = Date.now()/1000 ;

    index.tuneParams = {
        frequency  : 426000000,
        symbol_rate:   6900000,
        modulation : "QAM_64" ,
    }
    index.transportStreams = [];
    var serviceDescriptors = {};
    var logicalChannelNumbers = {};
    var ei4sMap = {};

    index.tune = function() {
        index.tuneResponse = "...";
        $http.post("tune", index.tuneParams).then(function(resp) {
            index.tuneResponse =  resp.data;
        });
    }
    index.stopFrontend = () => {
        $http.post("stopFrontend");
    }
    index.startOver = () => {
        index.transportStreams = [];
        index.events = [];
        serviceDescriptors = {};
        logicalChannelNumbers = {};
        ei4sMap = {};
    }

    index.tuneTs = function(ts) {
        index.tuneParams.frequency   = ts.frequency  ;
        index.tuneParams.symbol_rate = ts.symbol_rate;
        index.tuneParams.modulation  = ts.modulation ;
        index.tune();
    }

    var msgQueue = [];
    $interval(() => {
        msgQueue.forEach(msg => {
            if (msg.type == "eventInformation") {
                var ei4s = ei4sMap[msg.service_id];
                var modified = false;
                if (!ei4s) {
                    modified = true;
                    ei4s = ei4sMap[msg.service_id] = {
                            service_id: msg.service_id,
                            eiMap: {},
                            eiList: []
                        };
                }

                msg.eiList.forEach(newEi => {
                    var ei = ei4s.eiMap[newEi.event_id];
                    if (!ei) {
                        ei = ei4s.eiMap[newEi.event_id] = {};
                    }
                    if (ei.start_time != newEi.start_time || ei.duration != newEi.duration || ei.event_name != newEi.event_name) {
                        modified = true;
                        Object.assign(ei, newEi);
                        ei.xpos = (ei.start_time - nowSeconds) / 400 + 50;
                        ei.width = ei.duration / 400;
                        if (ei.width < 2) {
                            ei.width = 2;
                        }
                    }
                });
                if (modified) {
                    ei4s.eiList.length = 0;
                    for (var event_id in ei4s.eiMap) {
                        var ei = ei4s.eiMap[event_id]
                        ei4s.eiList.push(ei);
                    }
//                    ei4s.eiList.sort((ei1, ei2) => ei1.start_time - ei2.start_time)
//                    console.log(ei4s.service_id+" "+ei4s.eiList.length);
//                    console.log(ei4s.eiMap)

                    index.events = [];
                    for (var service_id in ei4sMap) {
                        var ei4s = ei4sMap[service_id]; // ei4a re-used !
                        var sd = serviceDescriptors[ei4s.service_id];
                        if (sd) {
                            ei4s.serviceDescriptor = sd;
                        }
                        index.events.push(ei4s);
                    }
                    index.events.sort((o1, o2) => o1.service_id - o2.service_id);
                }
            }
            if (msg.type == "serviceDescriptors") {
                msg.serviceDescriptors.forEach(sd => {
                    serviceDescriptors[sd.service_id] = sd;
                })
            }
            if (msg.type == "transportStream") {
                var existing = index.transportStreams.find(el => {
                    return el.network_id          == msg.network_id
                        && el.transport_stream_id == msg.transport_stream_id
                        && el.frequency           == msg.frequency
                        && el.modulation          == msg.modulation
                        && el.symbol_rate         == msg.symbol_rate
                        && el.FEC_inner           == msg.FEC_inner
                        && el.services.length     == msg.services.length
                });
                if (!existing) {
                    index.transportStreams.push(msg);
                    index.transportStreams.sort((ts1, ts2)=>ts1.frequency-ts2.frequency);
                    existing = msg;
                    existing.clazz = {};
                }
                existing.lcns.forEach(lcn => {
                    logicalChannelNumbers[lcn.service_id] = lcn;
                });
                existing.services.forEach(s => {
                    var sd = serviceDescriptors[s.service_id];
                    if (sd) {
                        s.serviceDescriptor = sd;
                    }
                    var lcn = logicalChannelNumbers[s.service_id];
                    if (lcn) {
                        s.logicalChannelNumber = lcn;
                    }
                });
//                existing.clazz.highlight = true;
//                $timeout(()=>existing.clazz.highlight=false, 100);
            }
        });
        msgQueue.length = 0;
    }, 500);

    var ws = {};
    function ensureWebSocketConnected() {
        if (ws.readyState === WebSocket.OPEN) {
            return;
        }
        ws = new WebSocket('ws://' + location.host + '/pushChannel');
        ws.onmessage = (ev) => {
            // console.log(ev);
            var msg = JSON.parse(ev.data);
            msgQueue.push(msg);
        }

    }
    ensureWebSocketConnected();

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            ensureWebSocketConnected();
        }
    });

});
