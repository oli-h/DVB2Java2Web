var dvbApp = angular.module('dvbApp', []);

dvbApp.controller('IndexController', function IndexController($scope, $http, $timeout, $interval) {
    const index = this;
    const nowSeconds = Date.now()/1000 ;

    index.tuneParams = {
        frequency  : 426000000,
        symbol_rate:   6900000,
        modulation : "QAM_64" ,
    }
    index.tuneParams = {
        frequency  : 322000000,
        symbol_rate:   6900000,
        modulation : "QAM_256" ,
    }

    index.transportStreams = [];
    var serviceDescriptors = {};
    var logicalChannelNumbers = {};

    var ei4sMap = {};
    index.events = [];

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
        serviceDescriptors = {};
        logicalChannelNumbers = {};

        index.events = [];
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
            if (msg.type == "eit") {
                var ei4s = ei4sMap[msg.service_id];
                if (!ei4s) {
                    modified = true;
                    ei4s = ei4sMap[msg.service_id] = {
                            service_id: msg.service_id,
                            eiMap: {},
                            eiList: []
                        };
                }

                var ei = ei4s.eiMap[msg.event_id];
                if (!ei) {
                    ei = ei4s.eiMap[msg.event_id] = {};
                }
                if (ei.start_time != msg.start_time || ei.duration != msg.duration || ei.event_name != msg.event_name) {
                    Object.assign(ei, msg);
                    ei.xpos = (ei.start_time - nowSeconds) / 400 + 50;
                    ei.width = ei.duration / 400;
                    if (ei.width < 2) {
                        ei.width = 2;
                    }
                }
            }
            else if (msg.type == "sdt") {
                serviceDescriptors[msg.service_id] = msg;
            }
            else if (msg.type == "transportStream") {
                var ts = index.transportStreams.find(el => {
                    return el.network_id          == msg.network_id
                        && el.transport_stream_id == msg.transport_stream_id
                        && el.frequency           == msg.frequency
                        && el.modulation          == msg.modulation
                        && el.symbol_rate         == msg.symbol_rate
                        && el.FEC_inner           == msg.FEC_inner
                        && el.services.length     == msg.services.length
                });
                if (!ts) {
                    index.transportStreams.push(msg);
                    index.transportStreams.sort((ts1, ts2) => ts1.frequency - ts2.frequency);
                    ts = msg;
//                    existing.clazz = {};
                }
                ts.lcns.forEach(lcn => {
                    logicalChannelNumbers[lcn.service_id] = lcn;
                });
                ts.services.forEach(s => {
                    s.serviceDescriptor = serviceDescriptors[s.service_id];
                    s.logicalChannelNumber = logicalChannelNumbers[s.service_id];
                });
//                existing.clazz.highlight = true;
//                $timeout(()=>existing.clazz.highlight=false, 100);
            }
        });
        msgQueue.length = 0;

        index.events = [];
        for (var service_id in ei4sMap) {
            var ei4s = ei4sMap[service_id]; // ei4a re-used !

            ei4s.eiList.length = 0;
            for (var event_id in ei4s.eiMap) {
                var ei = ei4s.eiMap[event_id]
                ei4s.eiList.push(ei);
            }
//            ei4s.eiList.sort((ei1, ei2) => ei1.start_time - ei2.start_time)

            var sd = serviceDescriptors[ei4s.service_id];
            if (sd) {
                ei4s.serviceDescriptor = sd;
            }
            index.events.push(ei4s);
        }
        index.events.sort((o1, o2) => o1.service_id - o2.service_id);
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
